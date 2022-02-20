package net.dankito.banking.fints

import kotlinx.datetime.LocalDate
import net.dankito.banking.fints.callback.FinTsClientCallback
import net.dankito.banking.fints.model.*
import net.dankito.banking.client.model.parameter.GetAccountDataParameter
import net.dankito.banking.client.model.parameter.RetrieveTransactions
import net.dankito.banking.client.model.response.ErrorCode
import net.dankito.banking.client.model.response.GetAccountDataResponse
import net.dankito.banking.fints.mapper.FinTsModelMapper
import net.dankito.banking.fints.response.client.FinTsClientResponse
import net.dankito.banking.fints.response.client.GetAccountInfoResponse
import net.dankito.banking.fints.response.client.GetAccountTransactionsResponse
import net.dankito.banking.fints.response.segments.AccountType
import net.dankito.banking.fints.webclient.IWebClient
import net.dankito.utils.multiplatform.extensions.minusDays
import net.dankito.utils.multiplatform.extensions.todayAtEuropeBerlin
import kotlin.jvm.JvmOverloads


open class FinTsClient @JvmOverloads constructor(
  open var callback: FinTsClientCallback,
  protected open val jobExecutor: FinTsJobExecutor = FinTsJobExecutor(),
  protected open val product: ProductData = ProductData("15E53C26816138699C7B6A3E8", "1.0.0") // TODO: get version dynamically
) {

  companion object { // TODO: use the English names
    val SupportedAccountTypes = listOf(AccountType.Girokonto, AccountType.Festgeldkonto, AccountType.Kreditkartenkonto, AccountType.Sparkonto)
  }


  constructor(callback: FinTsClientCallback) : this(callback, FinTsJobExecutor()) // Swift does not support default parameter values -> create constructor overloads

  constructor(callback: FinTsClientCallback, webClient: IWebClient) : this(callback, FinTsJobExecutor(RequestExecutor(webClient = webClient)))


  protected open val mapper = FinTsModelMapper()


  open suspend fun getAccountData(param: GetAccountDataParameter): GetAccountDataResponse {
    val bank = BankData(param.bankCode, param.loginName, param.password, param.finTsServerAddress, "")
    val accounts = param.accounts

    if (accounts.isNullOrEmpty() || param.retrieveOnlyAccountInfo) { // then first retrieve customer's bank accounts
      val getAccountInfoResponse = getAccountInfo(param, bank)

      if (getAccountInfoResponse.successful == false || param.retrieveOnlyAccountInfo) {
        return GetAccountDataResponse(mapper.mapErrorCode(getAccountInfoResponse), mapper.mapErrorMessages(getAccountInfoResponse), null,
          getAccountInfoResponse.messageLogWithoutSensitiveData, bank)
      } else {
        return getAccountData(param, getAccountInfoResponse.bank, getAccountInfoResponse.bank.accounts, getAccountInfoResponse)
      }
    } else {
      return getAccountData(param, bank, accounts.map { mapper.mapToAccountData(it, param) }, null)
    }
  }

  protected open suspend fun getAccountData(param: GetAccountDataParameter, bank: BankData, accounts: List<AccountData>, previousJobResponse: FinTsClientResponse?): GetAccountDataResponse {
    val retrievedTransactionsResponses = mutableListOf<GetAccountTransactionsResponse>()

    val accountsSupportingRetrievingTransactions = accounts.filter { it.supportsRetrievingBalance || it.supportsRetrievingAccountTransactions }

    if (accountsSupportingRetrievingTransactions.isEmpty()) {
      val errorMessage = "None of the accounts ${accounts.map { it.productName }} supports retrieving balance or transactions" // TODO: translate
      return GetAccountDataResponse(ErrorCode.NoneOfTheAccountsSupportsRetrievingData, errorMessage, mapper.map(bank), previousJobResponse?.messageLogWithoutSensitiveData ?: listOf(), bank)
    }

    accountsSupportingRetrievingTransactions.forEach { account ->
      retrievedTransactionsResponses.add(getAccountData(param, bank, account))
    }

    val unsuccessfulJob = retrievedTransactionsResponses.firstOrNull { it.successful == false }
    val errorCode = unsuccessfulJob?.let { mapper.mapErrorCode(it) }
      ?: if (retrievedTransactionsResponses.size < accountsSupportingRetrievingTransactions.size) ErrorCode.DidNotRetrieveAllAccountData else null
    return GetAccountDataResponse(errorCode, mapper.mapErrorMessages(unsuccessfulJob), mapper.map(bank, retrievedTransactionsResponses),
      mapper.mergeMessageLog(previousJobResponse, *retrievedTransactionsResponses.toTypedArray()), bank)
  }

  protected open suspend fun getAccountData(param: GetAccountDataParameter, bank: BankData, account: AccountData): GetAccountTransactionsResponse {
    val context = JobContext(JobContextType.GetTransactions, this.callback, product, bank, account)

    val retrieveTransactionsFrom = when (param.retrieveTransactions) {
      RetrieveTransactions.No -> LocalDate.todayAtEuropeBerlin() // TODO: implement RetrieveTransactions.No
      RetrieveTransactions.OfLast90Days -> calculate90DaysAgo()
      RetrieveTransactions.AccordingToRetrieveFromAndTo -> param.retrieveTransactionsFrom
      else -> null
    }

    val retrieveTransactionsTo = when (param.retrieveTransactions) {
      RetrieveTransactions.AccordingToRetrieveFromAndTo -> param.retrieveTransactionsTo
      else -> null
    }

    return jobExecutor.getTransactionsAsync(context, GetAccountTransactionsParameter(bank, account, param.retrieveBalance, retrieveTransactionsFrom,
      retrieveTransactionsTo, abortIfTanIsRequired = param.abortIfTanIsRequired))
  }

  private fun calculate90DaysAgo(): LocalDate? {
    // Europe/Berlin: we're communicating with German bank servers, so we have to use their time zone
    return LocalDate.todayAtEuropeBerlin().minusDays(90)
  }

  protected open suspend fun getAccountInfo(param: GetAccountDataParameter, bank: BankData): GetAccountInfoResponse {
    val context = JobContext(JobContextType.AddAccount, this.callback, product, bank) // TODO: add / change JobContextType

    /*      First dialog: Get user's basic data like BPD, customer system ID and her TAN methods     */

    val newUserInfoResponse = jobExecutor.retrieveBasicDataLikeUsersTanMethods(context, param.preferredTanMethods, param.preferredTanMedium)

    if (newUserInfoResponse.successful == false) { // bank parameter (FinTS server address, ...) already seem to be wrong
      return GetAccountInfoResponse(context, newUserInfoResponse)
    }

    /*      Second dialog, executed in retrieveBasicDataLikeUsersTanMethods() if required: some banks require that in order to initialize a dialog with
    strong customer authorization TAN media is required       */

    val getAccountsResponse = jobExecutor.getAccounts(context)

    return GetAccountInfoResponse(context, getAccountsResponse)
  }

}