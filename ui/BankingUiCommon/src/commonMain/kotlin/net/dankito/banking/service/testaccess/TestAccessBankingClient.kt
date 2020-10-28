package net.dankito.banking.service.testaccess

import kotlin.random.Random
import net.dankito.banking.ui.BankingClientCallback
import net.dankito.banking.ui.IBankingClient
import net.dankito.banking.ui.model.*
import net.dankito.banking.ui.model.mapper.IModelCreator
import net.dankito.banking.ui.model.parameters.GetTransactionsParameter
import net.dankito.banking.ui.model.parameters.TransferMoneyData
import net.dankito.banking.ui.model.responses.AddAccountResponse
import net.dankito.banking.ui.model.responses.BankingClientResponse
import net.dankito.banking.ui.model.responses.GetTransactionsResponse
import net.dankito.banking.util.IAsyncRunner
import net.dankito.utils.multiplatform.BigDecimal
import net.dankito.utils.multiplatform.Date


/**
 * Apple requires a test access. So this class implements a banking client that just returns fake data
 */
open class TestAccessBankingClient(
    protected val bank: TypedBankData,
    protected val modelCreator: IModelCreator,
    protected val asyncRunner: IAsyncRunner,
    callback: BankingClientCallback
) : IBankingClient {

    companion object {
        const val MillisecondsOfADay = 24 * 60 * 60 * 1000L
    }


    override val messageLogWithoutSensitiveData: List<MessageLogEntry> = listOf()


    override fun addAccountAsync(callback: (AddAccountResponse) -> Unit) {
        asyncRunner.runAsync { // for Android it's essential to get off UI thread
            bank.customerName = "Horst"
            bank.supportedTanMethods = listOf()
            bank.tanMedia = listOf()

            bank.accounts = createAccounts(bank)

            callback(AddAccountResponse(bank, createRetrievedAccountData(bank)))
        }
    }

    override fun getTransactionsAsync(parameter: GetTransactionsParameter, callback: (GetTransactionsResponse) -> Unit) {
        asyncRunner.runAsync {
            callback(GetTransactionsResponse(createRetrievedAccountData(parameter.account)))
        }
    }


    override fun transferMoneyAsync(data: TransferMoneyData, callback: (BankingClientResponse) -> Unit) {
        asyncRunner.runAsync {
            callback(BankingClientResponse(true, null))
        }
    }


    override fun dataChanged(bank: TypedBankData) {
        // nothing to do
    }

    override fun deletedBank(bank: TypedBankData, wasLastAccountWithThisCredentials: Boolean) {
        // nothing to do
    }


    protected open fun createAccounts(bank: TypedBankData): List<TypedBankAccount> {
        val checkingAccount = createAccount(bank, "Girokonto Deluxe", "DELiebe", BankAccountType.CheckingAccount)
        val fixedTermDepositAccount = createAccount(bank, "Tagesgeld Minus", "DEKuscheln", BankAccountType.FixedTermDepositAccount)
        val creditCardAccount = createAccount(bank, "Credit card golden super plus", "12345678", BankAccountType.CreditCardAccount)

        setAccountFeatures(checkingAccount)
        setAccountFeatures(fixedTermDepositAccount, false, false)
        creditCardAccount.supportsRetrievingAccountTransactions = true

        return listOf(
            checkingAccount,
            fixedTermDepositAccount,
            creditCardAccount
        )
    }

    protected open fun createAccount(bank: TypedBankData, productName: String, identifier: String, type: BankAccountType) : TypedBankAccount {
        val account = modelCreator.createAccount(bank, productName, identifier)

        account.isAccountTypeSupportedByApplication = true
        account.accountHolderName = bank.customerName
        account.type = type

        account.countDaysForWhichTransactionsAreKept = 90

        return account
    }

    protected open fun setAccountFeatures(account: TypedBankAccount, supportsRealTimeTransfer: Boolean = true, supportsTransferringMoney: Boolean = true) {
        account.supportsRetrievingBalance = true
        account.supportsRetrievingAccountTransactions = true
        account.supportsTransferringMoney = supportsTransferringMoney
        account.supportsRealTimeTransfer = supportsRealTimeTransfer
    }


    protected open fun createRetrievedAccountData(bank: TypedBankData): List<RetrievedAccountData> {
        return bank.accounts.map { createRetrievedAccountData(it) }
    }

    protected open fun createRetrievedAccountData(account: TypedBankAccount): RetrievedAccountData {
        val balance = createAmount()
        val transactionsStartDate = account.retrievedTransactionsUpTo ?: Date(Date.today.millisSinceEpoch - 90 * MillisecondsOfADay)
        val transactionsEndDate = Date()

        return RetrievedAccountData(account, true, balance, createBookedTransactions(account, transactionsStartDate, transactionsEndDate),
            listOf(), transactionsStartDate, transactionsEndDate)
    }

    protected open fun createBookedTransactions(account: TypedBankAccount, transactionsStartDate: Date, transactionsEndDate: Date): List<IAccountTransaction> {
        val countDays = ((transactionsEndDate.millisSinceEpoch - transactionsStartDate.millisSinceEpoch) / MillisecondsOfADay).toInt()

        return IntRange(1, countDays).flatMap { dayIndex ->
            val valueDate = Date(transactionsEndDate.millisSinceEpoch - (countDays - dayIndex) * MillisecondsOfADay)

            createAccountTransactionForDay(account, valueDate)
        }
    }

    protected open fun createAccountTransactionForDay(account: TypedBankAccount, valueDate: Date): List<IAccountTransaction> {
        val random = defaultRandom
        val countTransactionsForDay = random.nextInt(0, 4)

        return IntRange(0, countTransactionsForDay - 1).map {
            createAccountTransaction(account, valueDate)
        }
    }

    protected open fun createAccountTransaction(account: TypedBankAccount, valueDate: Date): IAccountTransaction {
        val random = defaultRandom

        val amount = createAmount(random)
        val otherPartyName = getOtherPartyName(random)
        val otherPartyBankCode = null
        val otherPartyAccountId = "DEMirHerzlichEgal"
        val bookingText = "Überweisung"

        return modelCreator.createTransaction(account, amount, "EUR", "Reference", valueDate, otherPartyName, otherPartyBankCode, otherPartyAccountId,
            bookingText, valueDate)
    }

    protected open fun getOtherPartyName(random: Random): String? {
        val otherPartyNames = listOf("Mahatma Gandhi", "Mutter Theresa", "Nelson Mandela", "Schnappi das Krokodil", "Winnie Puh", "Albert Einstein", "Heinrich VIII.", "Andreas Scheuer")
        val otherPartyNameIndex = random.nextInt(0, otherPartyNames.size)

        return otherPartyNames[otherPartyNameIndex]
    }

    protected open fun createAmount(): BigDecimal {
        return createAmount(defaultRandom)
    }

    protected open fun createAmount(random: Random): BigDecimal {
        val amountAsDouble = random.nextDouble(-10_000.01, 10_000.01)

        return BigDecimal(amountAsDouble)
    }

    protected open val defaultRandom: Random = Random.Default

}