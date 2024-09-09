package net.codinux.banking.fints

import net.dankito.banking.client.model.parameter.FinTsClientParameter
import net.dankito.banking.client.model.parameter.GetAccountDataParameter
import net.dankito.banking.client.model.parameter.TransferMoneyParameter
import net.dankito.banking.client.model.response.ErrorCode
import net.dankito.banking.client.model.response.GetAccountDataResponse
import net.dankito.banking.client.model.response.TransferMoneyResponse
import net.codinux.banking.fints.callback.FinTsClientCallback
import net.codinux.banking.fints.config.FinTsClientConfiguration
import net.codinux.banking.fints.mapper.FinTsModelMapper
import net.codinux.banking.fints.messages.datenelemente.implementierte.KundensystemID
import net.codinux.banking.fints.model.*
import net.codinux.banking.fints.response.client.FinTsClientResponse
import net.codinux.banking.fints.response.client.GetAccountInfoResponse
import net.codinux.banking.fints.response.client.GetAccountTransactionsResponse
import net.codinux.banking.fints.response.segments.AccountType
import net.codinux.banking.fints.response.segments.BankParameters
import net.codinux.banking.fints.util.BicFinder


open class FinTsClient(
  protected open val config: FinTsClientConfiguration,
  open var callback: FinTsClientCallback
) {

  companion object { // TODO: use the English names
    val SupportedAccountTypes = listOf(AccountType.Girokonto, AccountType.Festgeldkonto, AccountType.Kreditkartenkonto, AccountType.Sparkonto)
  }


  constructor(callback: FinTsClientCallback) : this(FinTsClientConfiguration(), callback)


  protected open val mapper = FinTsModelMapper()

  protected open val bicFinder = BicFinder()


  open suspend fun getAccountDataAsync(bankCode: String, loginName: String, password: String): GetAccountDataResponse {
    return getAccountDataAsync(GetAccountDataParameter(bankCode, loginName, password))
  }

  open suspend fun getAccountDataAsync(param: GetAccountDataParameter): GetAccountDataResponse {
    val basicAccountDataResponse = getRequiredDataToSendUserJobs(param)

    if (basicAccountDataResponse.successful == false || param.retrieveOnlyAccountInfo || basicAccountDataResponse.finTsModel == null) {
      return GetAccountDataResponse(basicAccountDataResponse.error, basicAccountDataResponse.errorMessage, null,
        basicAccountDataResponse.messageLogWithoutSensitiveData, basicAccountDataResponse.finTsModel)
    } else {
      val bank = basicAccountDataResponse.finTsModel!!
      return getAccountData(param, bank, bank.accounts, basicAccountDataResponse.messageLogWithoutSensitiveData)
    }
  }

  protected open suspend fun getAccountData(param: GetAccountDataParameter, bank: BankData, accounts: List<AccountData>, previousJobMessageLog: List<MessageLogEntry>?): GetAccountDataResponse {
    val retrievedTransactionsResponses = mutableListOf<GetAccountTransactionsResponse>()

    val accountsSupportingRetrievingTransactions = accounts.filter { it.supportsRetrievingBalance || it.supportsRetrievingAccountTransactions }

    if (accountsSupportingRetrievingTransactions.isEmpty()) {
      val errorMessage = "None of the accounts ${accounts.map { it.productName }} supports retrieving balance or transactions" // TODO: translate
      return GetAccountDataResponse(ErrorCode.NoneOfTheAccountsSupportsRetrievingData, errorMessage, mapper.map(bank), previousJobMessageLog ?: listOf(), bank)
    }

    for (account in accountsSupportingRetrievingTransactions) {
      val response = getAccountTransactions(param, bank, account)
      retrievedTransactionsResponses.add(response)

      if (response.tanRequiredButWeWereToldToAbortIfSo || response.userCancelledAction) { // if user cancelled action or TAN is required but we were told to abort then, then don't continue with next account
        break
      }
    }

    val unsuccessfulJob = retrievedTransactionsResponses.firstOrNull { it.successful == false }
    val errorCode = unsuccessfulJob?.let { mapper.mapErrorCode(it) }
      ?: if (retrievedTransactionsResponses.size < accountsSupportingRetrievingTransactions.size) ErrorCode.DidNotRetrieveAllAccountData else null
    return GetAccountDataResponse(errorCode, mapper.mapErrorMessages(unsuccessfulJob), mapper.map(bank, retrievedTransactionsResponses, param.retrieveTransactionsTo),
      mapper.mergeMessageLog(previousJobMessageLog, *retrievedTransactionsResponses.map { it.messageLog }.toTypedArray()), bank)
  }

  protected open suspend fun getAccountTransactions(param: GetAccountDataParameter, bank: BankData, account: AccountData): GetAccountTransactionsResponse {
    val context = JobContext(JobContextType.GetTransactions, this.callback, config, bank, account)

    return config.jobExecutor.getTransactionsAsync(context, mapper.toGetAccountTransactionsParameter(param, bank, account))
  }


  open suspend fun transferMoneyAsync(bankCode: String, loginName: String, password: String, recipientName: String, recipientAccountIdentifier: String,
                                      amount: Money, reference: String? = null): TransferMoneyResponse {
    return transferMoneyAsync(TransferMoneyParameter(bankCode, loginName, password, null, recipientName, recipientAccountIdentifier, null, amount, reference))
  }

  open suspend fun transferMoneyAsync(param: TransferMoneyParameter): TransferMoneyResponse {
    val finTsServerAddress = config.finTsServerAddressFinder.findFinTsServerAddress(param.bankCode)
    if (finTsServerAddress.isNullOrBlank()) {
      return TransferMoneyResponse(ErrorCode.BankDoesNotSupportFinTs3, "Either bank does not FinTS 3.0 or we don't know its FinTS server address", listOf(), null)
    }
    val recipientBankIdentifier = getRecipientBankCode(param)
    if (recipientBankIdentifier == null) {
      return TransferMoneyResponse(ErrorCode.CanNotDetermineBicForIban, "We can only determine recipient's BIC automatically for German IBANs. If it's a German IBAN, either we " +
        "cannot extract the bank code from IBAN ${param.recipientAccountIdentifier} (fourth to twelfth position) or don't know the BIC to this bank code. Please specify recipient's IBAN explicitly.", listOf())
    }


    val bank = mapper.mapToBankData(param, finTsServerAddress)
    val remittanceAccount = param.remittanceAccount

    if (remittanceAccount == null) { // then first retrieve customer's bank accounts
      val getAccountInfoResponse = getAccountInfo(param, bank)

      if (getAccountInfoResponse.successful == false) {
        return TransferMoneyResponse(mapper.mapErrorCode(getAccountInfoResponse), mapper.mapErrorMessages(getAccountInfoResponse),
          getAccountInfoResponse.messageLog, bank)
      } else {
        return transferMoneyAsync(param, recipientBankIdentifier, getAccountInfoResponse.bank, getAccountInfoResponse.bank.accounts, getAccountInfoResponse)
      }
    } else {
      return transferMoneyAsync(param, recipientBankIdentifier, bank, listOf(mapper.mapToAccountData(remittanceAccount, param)), null)
    }
  }

  protected open suspend fun transferMoneyAsync(param: TransferMoneyParameter, recipientBankIdentifier: String, bank: BankData, accounts: List<AccountData>, previousJobResponse: FinTsClientResponse?): TransferMoneyResponse {
    val accountsSupportingTransfer = accounts.filter { it.supportsTransferringMoney }
    val accountToUse: AccountData

    if (accountsSupportingTransfer.isEmpty()) {
      return TransferMoneyResponse(ErrorCode.NoAccountSupportsMoneyTransfer, "None of the accounts $accounts supports money transfer", previousJobResponse?.messageLog ?: listOf(), bank)
    } else if (accountsSupportingTransfer.size == 1) {
      accountToUse = accountsSupportingTransfer.first()
    } else {
      val selectedAccount = param.selectAccountToUseForTransfer?.invoke(accountsSupportingTransfer)

      if (selectedAccount == null) {
        return TransferMoneyResponse(ErrorCode.MoreThanOneAccountSupportsMoneyTransfer, "More than one of the accounts $accountsSupportingTransfer supports money transfer, so we cannot clearly determine which one to use for this transfer", previousJobResponse?.messageLog ?: listOf(), bank)
      }

      accountToUse = selectedAccount
    }

    val context = JobContext(JobContextType.TransferMoney, this.callback, config, bank, accountToUse)

    val response = config.jobExecutor.transferMoneyAsync(context, BankTransferData(param.recipientName, param.recipientAccountIdentifier, recipientBankIdentifier,
      param.amount, param.reference, param.instantPayment))

    return TransferMoneyResponse(mapper.mapErrorCode(response), mapper.mapErrorMessages(response), mapper.mergeMessageLog(previousJobResponse, response), bank)
  }

  private fun getRecipientBankCode(param: TransferMoneyParameter): String? {
    param.recipientBankIdentifier?.let { return it }

    val probablyIban = param.recipientAccountIdentifier.replace(" ", "")
    if (probablyIban.length > 12) {
      val bankCode = probablyIban.substring(4, 4 + 8) // extract bank code from IBAN. For German IBAN bank code starts at fourth position and has 8 digits

      bicFinder.findBic(bankCode)?.let { return it }
    }

    return null
  }

  /**
   * Ensures all basic data to initialize a dialog with strong customer authorization is retrieved so you can send your
   * actual jobs (Geschäftsvorfälle) to your bank's FinTS server.
   *
   * These data include:
   * - Bank communication data like FinTS server address, BIC, bank name, bank code used for FinTS.
   * - BPD (BankParameterDaten): bank name, BPD version, supported languages, supported HBCI versions, supported TAN methods,
   * max count jobs per message (Anzahl Geschäftsvorfallsarten) (see [BankParameters] [BankParameters](src/commonMain/kotlin/net/codinux/banking/fints/response/segmentsBankParameters) ).
   * - Min and max online banking password length, min TAN length, hint for login name (for all: if available)
   * - UPD (UserParameterDaten): username, UPD version.
   * - Customer system ID (Kundensystem-ID, see [KundensystemID]), TAN methods available for user and may user's TAN media.
   * - Which jobs the bank supports and which jobs need strong customer authorization (= require HKTAN segment).
   * - Which jobs the user is allowed to use.
   * - Which jobs can be called for a specific bank account.
   *
   * When implementing your own jobs, call this method first, then send an init dialog message and in next message your actual jobs.
   *
   * More or less implements everything of 02 FinTS_3.0_Formals.pdf so that you can start directly with the jobs from
   * 04 FinTS_3.0_Messages_Geschaeftsvorfaelle.pdf
   */
  open suspend fun getRequiredDataToSendUserJobs(param: FinTsClientParameter): net.dankito.banking.client.model.response.FinTsClientResponse {
    if (param.finTsModel != null) {
      return net.dankito.banking.client.model.response.FinTsClientResponse(null, null, emptyList(), param.finTsModel)
    }

    val finTsServerAddress = config.finTsServerAddressFinder.findFinTsServerAddress(param.bankCode)
    if (finTsServerAddress.isNullOrBlank()) {
      return net.dankito.banking.client.model.response.FinTsClientResponse(ErrorCode.BankDoesNotSupportFinTs3, "Either bank does not support FinTS 3.0 or we don't know its FinTS server address", emptyList(), null)
    }

    val bank = mapper.mapToBankData(param, finTsServerAddress)

    val getAccountInfoResponse = getAccountInfo(param, bank)

    return net.dankito.banking.client.model.response.FinTsClientResponse(mapper.mapErrorCode(getAccountInfoResponse), mapper.mapErrorMessages(getAccountInfoResponse),
      getAccountInfoResponse.messageLog, bank)
  }

  protected open suspend fun getAccountInfo(param: FinTsClientParameter, bank: BankData): GetAccountInfoResponse {
    param.finTsModel?.let {
      // TODO: implement
//      return GetAccountInfoResponse(it)
    }

    val context = JobContext(JobContextType.GetAccountInfo, this.callback, config, bank)

    /*      First dialog: Get user's basic data like BPD, customer system ID and her TAN methods     */

    val newUserInfoResponse = config.jobExecutor.retrieveBasicDataLikeUsersTanMethods(context, param.preferredTanMethods, param.preferredTanMedium)

    /*      Second dialog, executed in retrieveBasicDataLikeUsersTanMethods() if required: some banks require that in order to initialize a dialog with
    strong customer authorization TAN media is required       */

    if (newUserInfoResponse.successful == false) { // bank parameter (FinTS server address, ...) already seem to be wrong
      return GetAccountInfoResponse(context, newUserInfoResponse)
    }

    val getAccountsResponse = config.jobExecutor.getAccounts(context)

    return GetAccountInfoResponse(context, getAccountsResponse)
  }

}