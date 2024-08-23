package net.codinux.banking.fints

import kotlinx.datetime.LocalDate
import net.codinux.banking.fints.callback.SimpleFinTsClientCallback
import net.codinux.banking.fints.config.FinTsClientConfiguration
import net.codinux.banking.fints.config.FinTsClientOptions
import net.codinux.banking.fints.extensions.randomWithSeed
import net.codinux.banking.fints.messages.MessageBuilder
import net.codinux.banking.fints.messages.MessageBuilderResult
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Datum
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Laenderkennzeichen
import net.codinux.banking.fints.messages.datenelemente.implementierte.Dialogsprache
import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.Sicherheitsfunktion
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.*
import net.codinux.banking.fints.messages.segmente.id.CustomerSegmentId
import net.codinux.banking.fints.model.*
import net.codinux.banking.fints.response.segments.*


abstract class FinTsTestBase {

    companion object {

        const val BankCode = "12345678"

        const val BankCountryCode = Laenderkennzeichen.Germany

        const val BankFinTsServerAddress = "banking.supi-dupi-bank.de/fints30"

        const val CustomerId = "0987654321"

        const val Pin = "12345"

        const val Iban = "DE11$BankCode$CustomerId"

        const val Bic = "ABCDDEMM123"

        val Language = Dialogsprache.German

        val SecurityFunction = Sicherheitsfunktion.PIN_TAN_910

        const val ControlReference = "4477"

        val Bank = createTestBank()

        val Currency = "EUR"

        val AccountHolderName = "Martina Musterfrau"

        val Account = createTestAccount()

        const val ProductName = "FinTS-TestClient25Stellen"

        const val ProductVersion = "1"

        val Product = ProductData(ProductName, ProductVersion)

        const val Date = 19880327

        const val Time = 182752

        val ClientConfig = FinTsClientConfiguration(FinTsClientOptions(version = ProductVersion, productName = ProductName))


        init {
            Bank.changeTanMediumParameters = ChangeTanMediaParameters(JobParameters("", 1, 1, 1, ":0:0"), false, false, false, false, false, listOf())
        }


        fun createTestBank(): BankData {
            return BankData(BankCode, CustomerId, Pin, BankFinTsServerAddress, Bic, "", BankCountryCode, selectedTanMethod = TanMethod("chipTAN-optisch", SecurityFunction, TanMethodType.ChipTanFlickercode), selectedLanguage = Language)
        }

        fun createTestAccount(): AccountData {
            return AccountData(CustomerId, null, BankCountryCode, BankCode, Iban, CustomerId, AccountType.Girokonto, Currency, AccountHolderName, null, null, listOf(), listOf())
        }
    }


    protected open fun createContext(bank: BankData = Bank, dialogId: String = DialogContext.InitialDialogId): JobContext {
        val context = JobContext(JobContextType.AnonymousBankInfo, SimpleFinTsClientCallback(), ClientConfig, bank)
        context.startNewDialog(dialogId = dialogId)

        return context
    }

    protected open fun createDialogId(): String {
        return randomWithSeed().nextInt(1000000, 9999999).toString()
    }

    protected open fun createAllowedJob(segmentId: CustomerSegmentId, version: Int): JobParameters = JobParameters(
        segmentId.id, 1, 1, null, "${segmentId.id.replace("HK", "HI")}S:1:$version"
    )

    protected open fun convertDate(date: LocalDate): String {
        return Datum.format(date)
    }

    protected open fun unmaskString(string: String): String {
        return string.replace("?'", "'").replace("?+", "+").replace("?:", ":")
    }

    protected open fun normalizeBinaryData(message: String): String {
        return message.replace(0.toChar(), ' ')
    }


    protected open fun createEmptyJobParameters(): JobParameters {
        return JobParameters("", 1, 1, 1, ":0:0")
    }


    protected fun createBankWithAllFeatures(): BankData {
        val bank = createTestBank()

        val getTransactionsJob = RetrieveAccountTransactionsParameters(JobParameters(CustomerSegmentId.AccountTransactionsMt940.id, 1, 1, null, "HIKAZS:73:5"), 180, true, false)
        val changeTanMediumJob = createAllowedJob(CustomerSegmentId.ChangeTanMedium, 3)
        bank.supportedJobs = listOf(
            getTransactionsJob,
            createAllowedJob(CustomerSegmentId.TanMediaList, 5), changeTanMediumJob,
            createAllowedJob(CustomerSegmentId.Balance, 7),
            createAllowedJob(CustomerSegmentId.CreditCardTransactions, 2),
            SepaAccountInfoParameters(createAllowedJob(CustomerSegmentId.SepaBankTransfer, 1), true, true, true, true, 35, listOf("pain.001.001.03")),
            SepaAccountInfoParameters(createAllowedJob(CustomerSegmentId.SepaRealTimeTransfer, 1), true, true, true, true, 35, listOf("pain.001.001.03")),
        )
        bank.pinInfo = PinInfo(getTransactionsJob, null, null, null, null, null, listOf(
            JobTanConfiguration(CustomerSegmentId.Balance.id, true),
            JobTanConfiguration(CustomerSegmentId.AccountTransactionsMt940.id, true),
            JobTanConfiguration(CustomerSegmentId.CreditCardTransactions.id, true),
            JobTanConfiguration(CustomerSegmentId.SepaBankTransfer.id, true),
            JobTanConfiguration(CustomerSegmentId.SepaRealTimeTransfer.id, true)
        ))
        bank.changeTanMediumParameters = ChangeTanMediaParameters(changeTanMediumJob, false, false, false, false, false, listOf())

        val checkingAccount = AccountData(CustomerId, null, BankCountryCode, BankCode, "ABCDDEBBXXX", CustomerId, AccountType.Girokonto, "EUR", "", null, null, bank.supportedJobs.map { it.jobName }, bank.supportedJobs)
        bank.addAccount(checkingAccount)

        val creditCardAccountJobs = bank.supportedJobs.filterNot { it.jobName == CustomerSegmentId.AccountTransactionsMt940.id }
        val creditCardAccount = AccountData(CustomerId + "_CreditCard", null, BankCountryCode, BankCode, "ABCDDEBBXXX", CustomerId, AccountType.Kreditkartenkonto, "EUR", "", null, null, creditCardAccountJobs.map { it.jobName }, creditCardAccountJobs)
        bank.addAccount(creditCardAccount)

        return bank
    }


    protected fun createRandomMessage(index: Int, context: JobContext, messageBuilder: MessageBuilder = MessageBuilder(), bank: BankData = context.bank, account: AccountData = bank.accounts.first()): MessageBuilderResult = when (index % 14) {
        0 -> messageBuilder.createAnonymousDialogInitMessage(context)
        2 -> messageBuilder.createInitDialogMessage(context)
        3 -> messageBuilder.createInitDialogMessageWithoutStrongCustomerAuthentication(context, null)
        4 -> messageBuilder.createSynchronizeCustomerSystemIdMessage(context)
        5 -> messageBuilder.createGetTanMediaListMessage(context)
        6 -> messageBuilder.createChangeTanMediumMessage(context, TanGeneratorTanMedium(TanMediumKlasse.TanGenerator, TanMediumStatus.Aktiv, "", null, null, null, null, null), null, null)
        7 -> messageBuilder.createGetBalanceMessage(context, account)
        8 -> messageBuilder.createGetTransactionsMessage(context, GetAccountTransactionsParameter(bank, account, true))
        9 -> messageBuilder.createGetTransactionsMessage(context, GetAccountTransactionsParameter(bank, bank.accounts[1], true))
        10 -> messageBuilder.createBankTransferMessage(context, BankTransferData("", "", "", Money.Zero, null), account)
        11 -> messageBuilder.createBankTransferMessage(context, BankTransferData("", "", "", Money.Zero, null, true), account)
        12 -> messageBuilder.createSendEnteredTanMessage(context, "", TanResponse(TanProcess.TanProcess2, null, null, null, null, null, null, "HITAN:5:6:4+4++4937-10-13-02.30.03.700259+Sie möchten eine \"Umsatzabfrage\" freigeben?: Bitte bestätigen Sie den \"Startcode 80085335\" mit der Taste \"OK\".+@12@100880085335++Kartennummer ******0892"))
        13 -> messageBuilder.createDialogEndMessage(context)
        else -> messageBuilder.createAnonymousDialogEndMessage(context)
    }

}