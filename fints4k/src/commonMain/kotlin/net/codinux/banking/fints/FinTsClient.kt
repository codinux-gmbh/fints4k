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
import net.codinux.banking.fints.model.*
import net.codinux.banking.fints.response.client.FinTsClientResponse
import net.codinux.banking.fints.response.client.GetAccountInfoResponse
import net.codinux.banking.fints.response.client.GetAccountTransactionsResponse
import net.codinux.banking.fints.response.segments.AccountType
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
    val finTsServerAddress = config.finTsServerAddressFinder.findFinTsServerAddress(param.bankCode)
    if (finTsServerAddress.isNullOrBlank()) {
      return GetAccountDataResponse(ErrorCode.BankDoesNotSupportFinTs3, "Either bank does not support FinTS 3.0 or we don't know its FinTS server address", null, listOf())
    }

    val bank = mapper.mapToBankData(param, finTsServerAddress)
    val accounts = param.accounts

    if (accounts.isNullOrEmpty() || param.retrieveOnlyAccountInfo) { // then first retrieve customer's bank accounts
      val getAccountInfoResponse = getAccountInfo(param, bank)

      if (getAccountInfoResponse.successful == false || param.retrieveOnlyAccountInfo) {
        return GetAccountDataResponse(mapper.mapErrorCode(getAccountInfoResponse), mapper.mapErrorMessages(getAccountInfoResponse), null,
          getAccountInfoResponse.messageLog, bank)
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
      return GetAccountDataResponse(ErrorCode.NoneOfTheAccountsSupportsRetrievingData, errorMessage, mapper.map(bank), previousJobResponse?.messageLog ?: listOf(), bank)
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

  protected open suspend fun getAccountInfo(param: FinTsClientParameter, bank: BankData): GetAccountInfoResponse {
    param.finTsModel?.let {
      // TODO: implement
//      return GetAccountInfoResponse(it)
    }

    val context = JobContext(JobContextType.GetAccountInfo, this.callback, config, bank)

    /*      First dialog: Get user's basic data like BPD, customer system ID and her TAN methods     */

    val newUserInfoResponse = config.jobExecutor.retrieveBasicDataLikeUsersTanMethods(context, param.preferredTanMethods, param.preferredTanMedium)

    if (newUserInfoResponse.successful == false) { // bank parameter (FinTS server address, ...) already seem to be wrong
      return GetAccountInfoResponse(context, newUserInfoResponse)
    }

    /*      Second dialog, executed in retrieveBasicDataLikeUsersTanMethods() if required: some banks require that in order to initialize a dialog with
    strong customer authorization TAN media is required       */

    val getAccountsResponse = config.jobExecutor.getAccounts(context)

    return GetAccountInfoResponse(context, getAccountsResponse)
  }

}