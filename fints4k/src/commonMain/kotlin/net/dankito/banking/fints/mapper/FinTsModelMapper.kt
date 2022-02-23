package net.dankito.banking.fints.mapper

import net.dankito.banking.client.model.*
import net.dankito.banking.client.model.AccountTransaction
import net.dankito.banking.client.model.parameter.FinTsClientParameter
import net.dankito.banking.client.model.parameter.GetAccountDataParameter
import net.dankito.banking.client.model.response.ErrorCode
import net.dankito.banking.fints.messages.datenelemente.abgeleiteteformate.Laenderkennzeichen
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.client.FinTsClientResponse
import net.dankito.banking.fints.response.client.GetAccountTransactionsResponse
import net.dankito.banking.fints.response.segments.AccountType


open class FinTsModelMapper {

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
      account.currency ?: Currency.DefaultCurrencyCode, account.accountLimit, account.supportsRetrievingAccountTransactions, account.supportsRetrievingBalance, account.supportsTransferringMoney, account.supportsRealTimeTransfer)
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

  open fun map(bank: BankData, retrievedTransactionsResponses: List<GetAccountTransactionsResponse>): CustomerAccount {
    val customerAccount = map(bank)
    val retrievedData = retrievedTransactionsResponses.mapNotNull { it.retrievedData }

    customerAccount.accounts.forEach { bankAccount ->
      retrievedData.firstOrNull { it.account.accountIdentifier == bankAccount.identifier }?.let { accountTransactionsResponse ->
        bankAccount.balance = accountTransactionsResponse.balance ?: Money.Zero
        bankAccount.retrievedTransactionsFrom = accountTransactionsResponse.retrievedTransactionsFrom
        bankAccount.retrievedTransactionsTo = accountTransactionsResponse.retrievedTransactionsTo
        bankAccount.bookedTransactions = map(accountTransactionsResponse)
      }
    }

    return customerAccount
  }

  open fun map(data: RetrievedAccountData): List<AccountTransaction> {
    return data.bookedTransactions.map { map(it) }
  }

  open fun map(transaction: net.dankito.banking.fints.model.AccountTransaction): AccountTransaction {
    return AccountTransaction(transaction.amount, transaction.unparsedReference, transaction.bookingDate,
      transaction.otherPartyName, transaction.otherPartyBankCode, transaction.otherPartyAccountId, transaction.bookingText, transaction.valueDate,
      transaction.statementNumber, transaction.sequenceNumber, transaction.openingBalance, transaction.closingBalance,
      transaction.endToEndReference, transaction.customerReference, transaction.mandateReference, transaction.creditorIdentifier, transaction.originatorsIdentificationCode,
      transaction.compensationAmount, transaction.originalAmount, transaction.sepaReference, transaction.deviantOriginator, transaction.deviantRecipient,
      transaction.referenceWithNoSpecialType, transaction.primaNotaNumber, transaction.textKeySupplement,
      transaction.currencyType, transaction.bookingKey, transaction.referenceForTheAccountOwner, transaction.referenceOfTheAccountServicingInstitution, transaction.supplementaryDetails,
      transaction.transactionReferenceNumber, transaction.relatedReferenceNumber)
  }


  open fun mapErrorCode(response: FinTsClientResponse): ErrorCode? {
    return when {
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

  open fun mergeMessageLog(vararg responses: FinTsClientResponse?): List<MessageLogEntry> {
    return responses.filterNotNull().flatMap { it.messageLogWithoutSensitiveData }
  }

}