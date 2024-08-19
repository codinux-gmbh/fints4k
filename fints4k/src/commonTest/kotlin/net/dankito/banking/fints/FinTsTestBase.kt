package net.dankito.banking.fints

import kotlinx.datetime.LocalDate
import net.dankito.banking.fints.callback.SimpleFinTsClientCallback
import net.dankito.banking.fints.extensions.randomWithSeed
import net.dankito.banking.fints.messages.datenelemente.abgeleiteteformate.Datum
import net.dankito.banking.fints.messages.datenelemente.abgeleiteteformate.Laenderkennzeichen
import net.dankito.banking.fints.messages.datenelemente.implementierte.Dialogsprache
import net.dankito.banking.fints.messages.datenelemente.implementierte.signatur.Sicherheitsfunktion
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.segments.AccountType
import net.dankito.banking.fints.response.segments.ChangeTanMediaParameters
import net.dankito.banking.fints.response.segments.JobParameters


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
        val context = JobContext(JobContextType.AnonymousBankInfo, SimpleFinTsClientCallback(), Product, bank)
        context.startNewDialog(dialogId = dialogId)

        return context
    }

    protected open fun createDialogId(): String {
        return randomWithSeed().nextInt(1000000, 9999999).toString()
    }

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

}