package net.dankito.banking.mapper

import net.dankito.banking.ui.model.*
import net.dankito.banking.ui.model.responses.AddAccountResponse
import net.dankito.banking.ui.model.responses.BankingClientResponse
import net.dankito.banking.ui.model.responses.GetTransactionsResponse
import net.dankito.banking.ui.model.tan.*
import net.dankito.banking.fints.messages.datenelemente.implementierte.signatur.Sicherheitsfunktion
import net.dankito.banking.fints.model.AccountData
import net.dankito.banking.fints.model.AccountFeature
import net.dankito.banking.fints.model.BankData
import net.dankito.banking.fints.model.CustomerData
import net.dankito.banking.fints.response.client.FinTsClientResponse
import net.dankito.banking.fints.response.segments.AccountType
import net.dankito.utils.exception.ExceptionHelper
import java.math.BigDecimal


open class fints4kModelMapper {


    private val exceptionHelper = ExceptionHelper()


    open fun mapResponse(response: FinTsClientResponse): BankingClientResponse {
        return BankingClientResponse(response.isSuccessful, mapErrorToShowToUser(response), response.exception, response.userCancelledAction)
    }

    open fun mapResponse(account: Account, response: net.dankito.banking.fints.response.client.AddAccountResponse): AddAccountResponse {
        val balances = response.balances.mapKeys { findMatchingBankAccount(account, it.key) }.filter { it.key != null } as Map<BankAccount, BigDecimal>

        val bookedTransactions = response.bookedTransactions.associateBy { it.account }
        val mappedBookedTransactions = mutableMapOf<BankAccount, List<AccountTransaction>>()

        bookedTransactions.keys.forEach { accountData ->
            findMatchingBankAccount(account, accountData)?.let { bankAccount ->
                mappedBookedTransactions.put(bankAccount, mapTransactions(bankAccount, response.bookedTransactions))
            }
        }

        return AddAccountResponse(response.isSuccessful, mapErrorToShowToUser(response),
            account, response.supportsRetrievingTransactionsOfLast90DaysWithoutTan,
            mappedBookedTransactions,
            mapOf(), // TODO: map unbooked transactions
            balances,
            response.exception,
            response.userCancelledAction)
    }

    open fun mapResponse(bankAccount: BankAccount, response: net.dankito.banking.fints.response.client.GetTransactionsResponse): GetTransactionsResponse {

        return GetTransactionsResponse(response.isSuccessful, mapErrorToShowToUser(response),
            mapOf(bankAccount to mapTransactions(bankAccount, response.bookedTransactions)),
            mapOf(), // TODO: map unbooked transactions
            response.balance?.let { mapOf(bankAccount to it) } ?: mapOf(),
            response.exception, response.userCancelledAction)
    }

    open fun mapErrorToShowToUser(response: FinTsClientResponse): String? {
        val innerException = response.exception?.let { exception -> exceptionHelper.getInnerException(exception) }

        return innerException?.localizedMessage ?: response.errorsToShowToUser.joinToString("\n")
    }


    open fun mapBank(bank: BankData): Bank {
        return Bank(bank.name, bank.bankCode, bank.bic, bank.finTs3ServerAddress)
    }

    open fun mapAccount(customer: CustomerData, bank: BankData): Account {
        val mappedBank = mapBank(bank)

        val account = Account(mappedBank, customer.customerId, customer.pin, customer.name, customer.userId)

        account.bankAccounts = mapBankAccounts(account, customer.accounts)

        updateTanMediaAndProcedures(account, customer)

        return account
    }

    // TODO: move to a fints4k internal mapper
    open fun updateCustomer(customer: CustomerData, updatedCustomer: CustomerData) {
        customer.pin = updatedCustomer.pin
        customer.name = updatedCustomer.name

        customer.supportedTanProcedures = updatedCustomer.supportedTanProcedures
        customer.selectedTanProcedure = updatedCustomer.selectedTanProcedure
        customer.tanMedia = updatedCustomer.tanMedia

        customer.updVersion = updatedCustomer.updVersion
        customer.selectedLanguage = updatedCustomer.selectedLanguage
        customer.customerSystemId = updatedCustomer.customerSystemId
        customer.customerSystemStatus = updatedCustomer.customerSystemStatus

        updateBankAccounts(customer, updatedCustomer.accounts)
    }


    open fun mapBankAccounts(account: Account, accountData: List<AccountData>): List<BankAccount> {
        return accountData.map { mapBankAccount(account, it) }
    }

    open fun mapBankAccount(account: Account, accountData: AccountData): BankAccount {
        return BankAccount(account, accountData.accountIdentifier, accountData.accountHolderName, accountData.iban,
            accountData.subAccountAttribute, BigDecimal.ZERO, accountData.currency ?: "EUR",
            mapBankAccountType(accountData.accountType), null, accountData.supportsFeature(AccountFeature.RetrieveAccountTransactions),
            accountData.supportsFeature(AccountFeature.RetrieveBalance), accountData.supportsFeature(AccountFeature.TransferMoney),
            accountData.supportsFeature(AccountFeature.InstantPayment))
    }

    open fun mapBankAccountType(type: AccountType?): BankAccountType {
        return when (type) {
            AccountType.Girokonto -> BankAccountType.Girokonto
            AccountType.Sparkonto -> BankAccountType.Sparkonto
            AccountType.Festgeldkonto -> BankAccountType.Festgeldkonto
            AccountType.Wertpapierdepot -> BankAccountType.Wertpapierdepot
            AccountType.Darlehenskonto -> BankAccountType.Darlehenskonto
            AccountType.Kreditkartenkonto -> BankAccountType.Kreditkartenkonto
            AccountType.FondsDepot -> BankAccountType.FondsDepot
            AccountType.Bausparvertrag -> BankAccountType.Bausparvertrag
            AccountType.Versicherungsvertrag -> BankAccountType.Versicherungsvertrag
            else -> BankAccountType.Sonstige
        }
    }

    // TODO: move to a fints4k internal mapper
    open fun updateBankAccounts(customer: CustomerData, updatedAccounts: List<AccountData>) {
        val accounts = customer.accounts

        updatedAccounts.forEach { updatedAccount ->
            val matchingExistingAccount = findMatchingBankAccount(accounts, updatedAccount)

            if (matchingExistingAccount == null) {
                customer.addAccount(updatedAccount)
            }
            else {
                updateBankAccount(matchingExistingAccount, updatedAccount)
            }
        }

        customer.accounts.forEach { account ->
            val updatedAccount = findMatchingBankAccount(updatedAccounts, account)

            if (updatedAccount == null) {
                customer.removeAccount(account)
            }
        }
    }

    open fun updateBankAccount(account: AccountData, updatedAccount: AccountData) {
        account.allowedJobs = updatedAccount.allowedJobs

        AccountFeature.values().forEach { feature ->
            account.setSupportsFeature(feature, updatedAccount.supportsFeature(feature))
        }
    }

    open fun findAccountForBankAccount(customer: CustomerData, bankAccount: BankAccount): AccountData? {
        return customer.accounts.firstOrNull { bankAccount.identifier == it.accountIdentifier }
    }

    open fun findMatchingBankAccount(account: Account, accountData: AccountData): BankAccount? {
        return account.bankAccounts.firstOrNull { it.identifier == accountData.accountIdentifier }
    }

    open fun findMatchingBankAccount(accounts: List<AccountData>, accountData: AccountData): AccountData? {
        return accounts.firstOrNull { it.accountIdentifier == accountData.accountIdentifier }
    }


    open fun mapTransactions(bankAccount: BankAccount, transactions: List<net.dankito.banking.fints.model.AccountTransaction>): List<AccountTransaction> {
        return transactions.map { mapTransaction(bankAccount, it) }
    }

    open fun mapTransaction(bankAccount: BankAccount, transaction: net.dankito.banking.fints.model.AccountTransaction): AccountTransaction {
        return AccountTransaction(
            transaction.amount,
            transaction.bookingDate,
            transaction.usage,
            transaction.otherPartyName,
            transaction.otherPartyBankCode,
            transaction.otherPartyAccountId,
            transaction.bookingText,
            transaction.closingBalance,
            transaction.currency,
            bankAccount
        )
    }


    open fun updateTanMediaAndProcedures(account: Account, customer: CustomerData) {
        account.supportedTanProcedures = mapTanProcedures(customer.supportedTanProcedures)

        if (customer.isTanProcedureSelected) {
            account.selectedTanProcedure = findMappedTanProcedure(account, customer.selectedTanProcedure)
        }
        else {
            account.selectedTanProcedure = null
        }

        account.tanMedia = mapTanMediums(customer.tanMedia)
    }


    open fun mapTanProcedures(tanProcedures: List<net.dankito.banking.fints.model.TanProcedure>): List<TanProcedure> {
        return tanProcedures.map { mapTanProcedure(it) }
    }

    open fun mapTanProcedure(tanProcedure: net.dankito.banking.fints.model.TanProcedure): TanProcedure {
        return TanProcedure(
            tanProcedure.displayName,
            mapTanProcedureType(tanProcedure.type),
            tanProcedure.securityFunction.code
        )
    }

    open fun mapTanProcedureType(type: net.dankito.banking.fints.model.TanProcedureType): TanProcedureType {
        return when (type) {
            net.dankito.banking.fints.model.TanProcedureType.EnterTan -> TanProcedureType.EnterTan
            net.dankito.banking.fints.model.TanProcedureType.ChipTanManuell -> TanProcedureType.ChipTanManuell
            net.dankito.banking.fints.model.TanProcedureType.ChipTanFlickercode -> TanProcedureType.ChipTanFlickercode
            net.dankito.banking.fints.model.TanProcedureType.ChipTanUsb -> TanProcedureType.ChipTanUsb
            net.dankito.banking.fints.model.TanProcedureType.ChipTanQrCode -> TanProcedureType.ChipTanQrCode
            net.dankito.banking.fints.model.TanProcedureType.ChipTanPhotoTanMatrixCode -> TanProcedureType.ChipTanPhotoTanMatrixCode
            net.dankito.banking.fints.model.TanProcedureType.SmsTan -> TanProcedureType.SmsTan
            net.dankito.banking.fints.model.TanProcedureType.AppTan -> TanProcedureType.AppTan
            net.dankito.banking.fints.model.TanProcedureType.photoTan -> TanProcedureType.photoTan
            net.dankito.banking.fints.model.TanProcedureType.QrCode -> TanProcedureType.QrCode
        }
    }

    protected open fun findMappedTanProcedure(account: Account, tanProcedure: net.dankito.banking.fints.model.TanProcedure): TanProcedure? {
        return account.supportedTanProcedures.firstOrNull { it.bankInternalProcedureCode == tanProcedure.securityFunction.code }
    }


    open fun mapTanMediums(tanMediums: List<net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMedium>): List<TanMedium> {
        return tanMediums.map { mapTanMedium(it) }
    }

    open fun mapTanMedium(tanMedium: net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMedium): TanMedium {
        if (tanMedium is net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanGeneratorTanMedium) {
            return mapTanMedium(tanMedium)
        }

        if (tanMedium is net.dankito.banking.fints.messages.datenelemente.implementierte.tan.MobilePhoneTanMedium) {
            return mapTanMedium(tanMedium)
        }

        return TanMedium(
            getDisplayNameForTanMedium(tanMedium),
            mapTanMediumStatus(tanMedium)
        )
    }

    open fun mapTanMedium(tanMedium: net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanGeneratorTanMedium): TanGeneratorTanMedium {
        return TanGeneratorTanMedium(
            getDisplayNameForTanMedium(tanMedium),
            mapTanMediumStatus(tanMedium),
            tanMedium.cardNumber
        )
    }

    open fun mapTanMedium(tanMedium: net.dankito.banking.fints.messages.datenelemente.implementierte.tan.MobilePhoneTanMedium): MobilePhoneTanMedium {
        return MobilePhoneTanMedium(
            getDisplayNameForTanMedium(tanMedium),
            mapTanMediumStatus(tanMedium),
            tanMedium.phoneNumber ?: tanMedium.concealedPhoneNumber
        )
    }

    protected open fun getDisplayNameForTanMedium(tanMedium: net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMedium): String {
        if (tanMedium is net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanGeneratorTanMedium) {
            var cardNumber = tanMedium.cardNumber
            tanMedium.cardSequenceNumber?.let {
                cardNumber += " (Kartenfolgenummer $it)" // TODO: translate
            }

            tanMedium.mediumName?.let { mediaName ->
                return "$mediaName $cardNumber"
            }

            return "Karte $cardNumber" // TODO: translate
        }
        else if (tanMedium is net.dankito.banking.fints.messages.datenelemente.implementierte.tan.MobilePhoneTanMedium) {
            val mediumName =  tanMedium.mediumName

            (tanMedium.phoneNumber ?: tanMedium.concealedPhoneNumber)?.let { phoneNumber ->
                return "$mediumName ($phoneNumber)"
            }

            return mediumName
        }

        return tanMedium.mediumClass.name
    }

    open fun mapTanMediumStatus(tanMedium: net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMedium): TanMediumStatus {
        return if (tanMedium.status.name.contains("aktiv", true)) TanMediumStatus.Used else TanMediumStatus.Available
    }

    open fun mapTanMedium(tanMedium: TanMedium, customer: CustomerData): net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMedium {
        if (tanMedium is TanGeneratorTanMedium) {
            return mapTanMedium(tanMedium, customer)
        }

        val statusToHave = if (tanMedium.status == TanMediumStatus.Used) listOf(net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMediumStatus.Aktiv, net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMediumStatus.AktivFolgekarte)
        else listOf(net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMediumStatus.Verfuegbar, net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMediumStatus.VerfuegbarFolgekarte)

        return customer.tanMedia.first { tanMedium.displayName == it.mediumClass.name && statusToHave.contains(it.status) }
    }

    open fun mapTanMedium(tanMedium: TanGeneratorTanMedium, customer: CustomerData): net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanGeneratorTanMedium {
        return customer.tanMedia.mapNotNull { it as? net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanGeneratorTanMedium }
            .first { it.cardNumber == tanMedium.cardNumber
                    && (it.cardSequenceNumber == null || tanMedium.displayName.contains(it.cardSequenceNumber!!)) }
    }


    open fun mapTanProcedure(tanProcedure: TanProcedure): net.dankito.banking.fints.model.TanProcedure {
        return net.dankito.banking.fints.model.TanProcedure(
            tanProcedure.displayName,
            Sicherheitsfunktion.values().first { it.code == tanProcedure.bankInternalProcedureCode },
            mapTanProcedureType(tanProcedure.type)
        )
    }

    open fun mapTanProcedureType(type: TanProcedureType): net.dankito.banking.fints.model.TanProcedureType {
        return when (type) {
            TanProcedureType.EnterTan -> net.dankito.banking.fints.model.TanProcedureType.EnterTan
            TanProcedureType.ChipTanManuell -> net.dankito.banking.fints.model.TanProcedureType.ChipTanManuell
            TanProcedureType.ChipTanFlickercode -> net.dankito.banking.fints.model.TanProcedureType.ChipTanFlickercode
            TanProcedureType.ChipTanUsb -> net.dankito.banking.fints.model.TanProcedureType.ChipTanUsb
            TanProcedureType.ChipTanQrCode -> net.dankito.banking.fints.model.TanProcedureType.ChipTanQrCode
            TanProcedureType.ChipTanPhotoTanMatrixCode -> net.dankito.banking.fints.model.TanProcedureType.ChipTanPhotoTanMatrixCode
            TanProcedureType.SmsTan -> net.dankito.banking.fints.model.TanProcedureType.SmsTan
            TanProcedureType.AppTan -> net.dankito.banking.fints.model.TanProcedureType.AppTan
            TanProcedureType.photoTan -> net.dankito.banking.fints.model.TanProcedureType.photoTan
            TanProcedureType.QrCode -> net.dankito.banking.fints.model.TanProcedureType.QrCode
        }
    }

    open fun mapEnterTanResult(result: EnterTanResult, customer: CustomerData): net.dankito.banking.fints.model.EnterTanResult {
        result.changeTanProcedureTo?.let { changeTanProcedureTo ->
            return net.dankito.banking.fints.model.EnterTanResult.userAsksToChangeTanProcedure(mapTanProcedure(changeTanProcedureTo))
        }

        result.changeTanMediumTo?.let { changeTanMediumTo ->
            val callback: ((FinTsClientResponse) -> Unit)? = if (result.changeTanMediumResultCallback == null) null
            else { response -> result.changeTanMediumResultCallback?.invoke(mapResponse(response)) }
            return net.dankito.banking.fints.model.EnterTanResult.userAsksToChangeTanMedium(mapTanMedium(changeTanMediumTo, customer), callback)
        }

        result.enteredTan?.let { enteredTan ->
            return net.dankito.banking.fints.model.EnterTanResult.userEnteredTan(enteredTan)
        }

        return net.dankito.banking.fints.model.EnterTanResult.userDidNotEnterTan()
    }

    open fun mapEnterTanGeneratorAtcResult(result: EnterTanGeneratorAtcResult): net.dankito.banking.fints.model.EnterTanGeneratorAtcResult {
        if (result.hasAtcBeenEntered) {
            return net.dankito.banking.fints.model.EnterTanGeneratorAtcResult.userEnteredAtc(result.tan!!, result.atc!!)
        }

        return net.dankito.banking.fints.model.EnterTanGeneratorAtcResult.userDidNotEnterAtc()
    }

    open fun mapTanChallenge(tanChallenge: net.dankito.banking.fints.model.TanChallenge): TanChallenge {
        if (tanChallenge is net.dankito.banking.fints.model.FlickerCodeTanChallenge) {
            return mapTanChallenge(tanChallenge)
        }

        if (tanChallenge is net.dankito.banking.fints.model.ImageTanChallenge) {
            return mapTanChallenge(tanChallenge)
        }

        return TanChallenge(tanChallenge.messageToShowToUser,
            mapTanProcedure(tanChallenge.tanProcedure)
        )
    }

    open fun mapTanChallenge(tanChallenge: net.dankito.banking.fints.model.FlickerCodeTanChallenge): FlickerCodeTanChallenge {
        return FlickerCodeTanChallenge(mapFlickerCode(tanChallenge.flickerCode), tanChallenge.messageToShowToUser,
            mapTanProcedure(tanChallenge.tanProcedure)
        )
    }

    open fun mapFlickerCode(flickerCode: net.dankito.banking.fints.tan.FlickerCode): FlickerCode {
        return FlickerCode(flickerCode.challengeHHD_UC, flickerCode.parsedDataSet, flickerCode.decodingError)
    }

    open fun mapTanChallenge(tanChallenge: net.dankito.banking.fints.model.ImageTanChallenge): ImageTanChallenge {
        return ImageTanChallenge(mapTanImage(tanChallenge.image), tanChallenge.messageToShowToUser,
            mapTanProcedure(tanChallenge.tanProcedure)
        )
    }

    open fun mapTanImage(image: net.dankito.banking.fints.tan.TanImage): TanImage {
        return TanImage(image.mimeType, image.imageBytes, image.decodingError)
    }

}