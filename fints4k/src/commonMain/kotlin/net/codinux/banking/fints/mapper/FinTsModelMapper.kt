package net.codinux.banking.fints.mapper

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import net.codinux.banking.fints.extensions.EuropeBerlin
import net.dankito.banking.client.model.*
import net.dankito.banking.client.model.AccountTransaction
import net.dankito.banking.client.model.parameter.FinTsClientParameter
import net.dankito.banking.client.model.parameter.GetAccountDataParameter
import net.dankito.banking.client.model.parameter.RetrieveTransactions
import net.dankito.banking.client.model.response.ErrorCode
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Laenderkennzeichen
import net.codinux.banking.fints.model.*
import net.codinux.banking.fints.response.client.FinTsClientResponse
import net.codinux.banking.fints.response.client.GetAccountTransactionsResponse
import net.codinux.banking.fints.response.segments.AccountType
import net.codinux.banking.fints.util.BicFinder
import net.codinux.banking.fints.extensions.minusDays
import net.codinux.banking.fints.extensions.todayAtEuropeBerlin


open class FinTsModelMapper {

  protected open val bicFinder = BicFinder()


  open fun mapToBankData(param: FinTsClientParameter, finTsServerAddress: String): BankData {
    return BankData(param.bankCode, param.loginName, param.password, finTsServerAddress, bicFinder.findBic(param.bankCode) ?: "")
  }

  open fun mapToAccountData(credentials: BankAccountIdentifier, param: FinTsClientParameter): AccountData {
    val accountData = AccountData(credentials.identifier, credentials.subAccountNumber, Laenderkennzeichen.Germany, param.bankCode,
      credentials.iban, param.loginName, null, null, "", null, null, listOf(), listOf())

    // TODO: where to know from if account supports retrieving balance and transactions?
    accountData.setSupportsFeature(AccountFeature.RetrieveBalance, true)
    accountData.setSupportsFeature(AccountFeature.RetrieveAccountTransactions, true)
    accountData.setSupportsFeature(AccountFeature.TransferMoney, true)
    accountData.setSupportsFeature(AccountFeature.RealTimeTransfer, true)

    return accountData
  }


  open fun map(bank: BankData): CustomerAccount {
    return CustomerAccount(bank.bankCode, bank.customerId, bank.pin, bank.finTs3ServerAddress, bank.bankName, bank.bic, bank.customerName, bank.userId,
      map(bank.accounts), bank.tanMethodsAvailableForUser, bank.selectedTanMethod, bank.tanMedia, bank.selectedTanMedium)
  }

  open fun map(accounts: List<AccountData>): List<BankAccount> {
    return accounts.map { map(it) }
  }

  open fun map(account: AccountData): BankAccount {
    return BankAccount(account.accountIdentifier, account.subAccountAttribute, account.iban, account.accountHolderName, map(account.accountType), account.productName,
      account.currency ?: Currency.DefaultCurrencyCode, account.accountLimit, account.countDaysForWhichTransactionsAreKept, account.isAccountTypeSupportedByApplication,
      account.supportsRetrievingAccountTransactions, account.supportsRetrievingBalance, account.supportsTransferringMoney, account.supportsRealTimeTransfer)
  }

  open fun map(accountType: AccountType?): BankAccountType {
    return when (accountType) {
      AccountType.Girokonto -> BankAccountType.CheckingAccount
      AccountType.Sparkonto -> BankAccountType.SavingsAccount
      AccountType.Festgeldkonto -> BankAccountType.FixedTermDepositAccount
      AccountType.Wertpapierdepot -> BankAccountType.SecuritiesAccount
      AccountType.Darlehenskonto -> BankAccountType.LoanAccount
      AccountType.Kreditkartenkonto -> BankAccountType.CreditCardAccount
      AccountType.FondsDepot -> BankAccountType.FundDeposit
      AccountType.Bausparvertrag -> BankAccountType.BuildingLoanContract
      AccountType.Versicherungsvertrag -> BankAccountType.InsuranceContract
      else -> BankAccountType.Other
    }
  }

  open fun map(bank: BankData, retrievedTransactionsResponses: List<GetAccountTransactionsResponse>, retrieveTransactionsTo: LocalDate? = null): CustomerAccount {
    val customerAccount = map(bank)
    val retrievedData = retrievedTransactionsResponses.mapNotNull { it.retrievedData }

    customerAccount.accounts.forEach { bankAccount ->
      retrievedData.firstOrNull { it.account.accountIdentifier == bankAccount.identifier }?.let { accountTransactionsResponse ->
        accountTransactionsResponse.balance?.let { balance ->
          bankAccount.balance = balance
        }

        if (accountTransactionsResponse.retrievedTransactionsFrom != null && (bankAccount.retrievedTransactionsFrom == null ||
                  accountTransactionsResponse.retrievedTransactionsFrom!! < bankAccount.retrievedTransactionsFrom!!)) {
          bankAccount.retrievedTransactionsFrom = accountTransactionsResponse.retrievedTransactionsFrom
        }

        val retrievalTime = if (retrieveTransactionsTo == null) accountTransactionsResponse.retrievalTime
                            else retrieveTransactionsTo.atTime(0, 0).toInstant(TimeZone.EuropeBerlin)
        if (bankAccount.lastTransactionsRetrievalTime == null || bankAccount.lastTransactionsRetrievalTime!! <= retrievalTime) { // if retrieveTransactionsTo is set it may is older than current account's lastTransactionsRetrievalTime
          bankAccount.lastTransactionsRetrievalTime = retrievalTime
        }

        if (accountTransactionsResponse.bookedTransactions.isNotEmpty()) {
          bankAccount.bookedTransactions = bankAccount.bookedTransactions.toMutableList().apply {
            addAll(map(accountTransactionsResponse))
          }
        }
      }
    }

    return customerAccount
  }

  open fun map(data: RetrievedAccountData): List<AccountTransaction> {
    return data.bookedTransactions.map { map(it) }
  }

  open fun map(transaction: net.codinux.banking.fints.model.AccountTransaction): AccountTransaction {
    return AccountTransaction(
      transaction.amount, transaction.reference,
      transaction.bookingDate, transaction.valueDate,
      transaction.otherPartyName, transaction.otherPartyBankCode, transaction.otherPartyAccountId,

      transaction.postingText, transaction.statementNumber, transaction.sheetNumber,
      transaction.openingBalance, transaction.closingBalance,

      transaction.customerReference, transaction.bankReference, transaction.furtherInformation,

      transaction.endToEndReference, transaction.mandateReference, transaction.creditorIdentifier, transaction.originatorsIdentificationCode,
      transaction.compensationAmount, transaction.originalAmount, transaction.deviantOriginator, transaction.deviantRecipient,
      transaction.referenceWithNoSpecialType,

      transaction.journalNumber, transaction.textKeyAddition,
      transaction.orderReferenceNumber, transaction.referenceNumber
    )
  }


  open fun toGetAccountTransactionsParameter(param: GetAccountDataParameter, bank: BankData, account: AccountData): GetAccountTransactionsParameter {
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

    return GetAccountTransactionsParameter(bank, account, param.retrieveBalance, retrieveTransactionsFrom,
      retrieveTransactionsTo, abortIfTanIsRequired = param.abortIfTanIsRequired)
  }

  open fun calculate90DaysAgo(): LocalDate {
    // Europe/Berlin: we're communicating with German bank servers, so we have to use their time zone
    return LocalDate.todayAtEuropeBerlin().minusDays(90)
  }


  open fun mapErrorCode(response: FinTsClientResponse): ErrorCode? {
    return when {
      response.didReceiveResponse == false -> ErrorCode.NetworkError
      response.internalError != null -> ErrorCode.InternalError
      response.errorMessagesFromBank.isNotEmpty() -> ErrorCode.BankReturnedError
      response.isPinLocked -> ErrorCode.AccountLocked
      response.wrongCredentialsEntered -> ErrorCode.WrongCredentials
      response.isJobAllowed == false || response.isJobVersionSupported == false -> ErrorCode.JobNotSupported
      response.tanRequiredButWeWereToldToAbortIfSo -> ErrorCode.TanRequiredButShouldAbortIfRequiresTan
      response.userCancelledAction || response.noTanMethodSelected || // either the user really has the choice to select one, then the errorCode would be UserCancelledAction,
        // or if it gets selected automatically, that means there aren't any TanMethods which should only be the case if before another error occurred
        // if isStrongAuthenticationRequired is set but tanRequiredButWeWereToldToAbortIfSo then user cancelled entering TAN
        response.isStrongAuthenticationRequired -> ErrorCode.UserCancelledAction
      else -> null
    }
  }

  open fun mapErrorMessages(response: FinTsClientResponse?): String? {
    if (response == null) {
      return null
    }

    val errorMessages = response.errorMessagesFromBank.toMutableList()

    response.internalError?.let {
      errorMessages.add(it)
    }

    return if (errorMessages.isEmpty()) null
    else errorMessages.joinToString("\r\n")
  }

  open fun mergeMessageLog(vararg messageLogs: List<MessageLogEntry>?): List<MessageLogEntry> {
    return messageLogs.filterNotNull().flatten()
  }

  open fun mergeMessageLog(vararg responses: FinTsClientResponse?): List<MessageLogEntry> {
    return responses.filterNotNull().flatMap { it.messageLog }
  }

}