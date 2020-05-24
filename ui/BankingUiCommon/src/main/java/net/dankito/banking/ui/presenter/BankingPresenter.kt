package net.dankito.banking.ui.presenter

import net.dankito.banking.persistence.IBankingPersistence
import net.dankito.banking.ui.BankingClientCallback
import net.dankito.banking.ui.IBankingClient
import net.dankito.banking.ui.IBankingClientCreator
import net.dankito.banking.ui.IRouter
import net.dankito.banking.ui.model.*
import net.dankito.banking.ui.model.parameters.TransferMoneyData
import net.dankito.banking.ui.model.responses.AddAccountResponse
import net.dankito.banking.ui.model.responses.BankingClientResponse
import net.dankito.banking.ui.model.responses.GetTransactionsResponse
import net.dankito.banking.ui.model.tan.EnterTanGeneratorAtcResult
import net.dankito.banking.ui.model.tan.EnterTanResult
import net.dankito.banking.ui.model.tan.TanChallenge
import net.dankito.banking.ui.model.tan.TanGeneratorTanMedium
import net.dankito.banking.util.IBankIconFinder
import net.dankito.banking.fints.banks.IBankFinder
import net.dankito.banking.fints.model.BankInfo
import net.dankito.banking.ui.model.settings.AppSettings
import net.dankito.utils.IThreadPool
import net.dankito.utils.ThreadPool
import net.dankito.utils.extensions.containsExactly
import net.dankito.utils.extensions.ofMaxLength
import net.dankito.utils.serialization.ISerializer
import net.dankito.utils.serialization.JacksonJsonSerializer
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


open class BankingPresenter(
    protected val bankingClientCreator: IBankingClientCreator,
    protected val bankFinder: IBankFinder,
    protected val databaseFolder: File,
    protected val dataFolder: File,
    protected val persister: IBankingPersistence,
    protected val bankIconFinder: IBankIconFinder,
    protected val router: IRouter,
    protected val serializer: ISerializer = JacksonJsonSerializer(),
    protected val threadPool: IThreadPool = ThreadPool()
) {

    companion object {
        protected const val OneDayMillis = 24 * 60 * 60 * 1000

        protected val MessageLogEntryDateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS")

        private val log = LoggerFactory.getLogger(BankingPresenter::class.java)
    }


    protected val clientsForAccounts = mutableMapOf<Account, IBankingClient>()

    protected var selectedBankAccountsField = mutableListOf<BankAccount>()

    protected var selectedAccountType = SelectedAccountType.AllAccounts

    protected var saveAccountOnNextEnterTanInvocation = false


    protected val accountsChangedListeners = mutableListOf<(List<Account>) -> Unit>()

    protected val retrievedAccountTransactionsResponseListeners = mutableListOf<(BankAccount, GetTransactionsResponse) -> Unit>()

    protected val selectedBankAccountsChangedListeners = mutableListOf<(List<BankAccount>) -> Unit>()


    protected val callback: BankingClientCallback = object : BankingClientCallback {

        override fun enterTan(account: Account, tanChallenge: TanChallenge): EnterTanResult {
            if (saveAccountOnNextEnterTanInvocation) {
                persistAccount(account)
                saveAccountOnNextEnterTanInvocation = false
            }

            val result = router.getTanFromUserFromNonUiThread(account, tanChallenge, this@BankingPresenter)

            if (result.changeTanProcedureTo != null || result.changeTanMediumTo != null) { // then either selected TAN medium or procedure will change -> save account on next call to enterTan() as then changes will be visible
                saveAccountOnNextEnterTanInvocation = true
            }

            return result
        }

        override fun enterTanGeneratorAtc(tanMedium: TanGeneratorTanMedium): EnterTanGeneratorAtcResult {
            return router.getAtcFromUserFromNonUiThread(tanMedium)
        }

    }


    init {
        threadPool.runAsync {
            readAppSettings()
            readPersistedAccounts()
        }

        // preloadBankList asynchronously; on Android it takes approximately 18 seconds till banks are indexed for first time -> do it as early as possible
        threadPool.runAsync {
            bankFinder.preloadBankList()
        }
    }


    protected open fun readPersistedAccounts() {
        try {
            databaseFolder.mkdirs()

            val deserializedAccounts = persister.readPersistedAccounts()

            deserializedAccounts.forEach { account ->
                val bank = account.bank
                val bankInfo = BankInfo(bank.name, bank.bankCode, bank.bic, "", "", "", bank.finTsServerAddress, "FinTS V3.0", null)

                val newClient = bankingClientCreator.createClient(bankInfo, account.customerId, account.password,
                    databaseFolder, threadPool, callback)

                try {
                    newClient.restoreData()
                } catch (e: Exception) {
                    log.error("Could not deserialize account data of $account", e)
                    // TODO: show error message to user
                }

                addClientForAccount(account, newClient)
            }

            callAccountsChangedListeners()

            selectedAllBankAccounts() // TODO: save last selected bank account(s)
        } catch (e: Exception) {
            log.error("Could not deserialize persisted accounts with persister $persister", e)
        }
    }

    protected open fun addClientForAccount(account: Account, client: IBankingClient) {
        clientsForAccounts.put(account, client)
    }


    // TODO: move BankInfo out of fints4k
    open fun addAccountAsync(bankInfo: BankInfo, customerId: String, pin: String, callback: (AddAccountResponse) -> Unit) {

        val newClient = bankingClientCreator.createClient(bankInfo, customerId, pin, databaseFolder, threadPool, this.callback)

        val startDate = Date()

        newClient.addAccountAsync { response ->
            val account = response.account

            if (response.isSuccessful) {
                addClientForAccount(account, newClient)

                selectedAccount(account)

                callAccountsChangedListeners()

                persistAccount(account)

                if (response.supportsRetrievingTransactionsOfLast90DaysWithoutTan) {
                    response.bookedTransactions.keys.forEach { bankAccount ->
                        retrievedAccountTransactions(bankAccount, startDate, response)
                    }
                }

                findIconForBankAsync(account)
            }

            callback(response)
        }
    }

    protected open fun findIconForBankAsync(account: Account) {
        threadPool.runAsync {
            findIconForBank(account)
        }
    }

    protected open fun findIconForBank(account: Account) {
        val bank = account.bank

        try {
            bankIconFinder.findIconForBank(bank.name)?.let { bankIconUrl ->
                val bankIconFile = saveBankIconToDisk(bank, bankIconUrl)

                bank.iconUrl = "file://" + bankIconFile.absolutePath // without 'file://' Android will not find it

                persistAccount(account)

                callAccountsChangedListeners()
            }
        } catch (e: Exception) {
            log.error("Could not get icon for bank $bank", e)
        }
    }

    protected open fun saveBankIconToDisk(bank: Bank, bankIconUrl: String): File {
        val bankIconsDir = File(dataFolder, "bank_icons")
        bankIconsDir.mkdirs()

        val extension = getIconFileExtension(bankIconUrl)
        val bankIconFile = File(bankIconsDir, bank.bankCode + if (extension != null) (".$extension") else "")

        URL(bankIconUrl).openConnection().getInputStream().buffered().use { iconInputStream ->
            FileOutputStream(bankIconFile).use { fileOutputStream ->
                iconInputStream.copyTo(fileOutputStream)
            }
        }

        return bankIconFile
    }

    protected open fun getIconFileExtension(bankIconUrl: String): String? {
        try {
            var iconFilename = File(bankIconUrl).name
            if (iconFilename.contains('?')) {
                iconFilename = iconFilename.substring(0, iconFilename.indexOf('?'))
            }

            val extension = File(iconFilename).extension
            if (extension.isNotBlank()) {
                return extension
            }
        } catch (e: Exception) {
            log.info("Could not get icon file extension from url '$bankIconUrl'", e)
        }

        return null
    }


    open fun deleteAccount(account: Account) {
        val wasSelected = isSingleSelectedAccount(account) or // either account or one of its bank accounts is currently selected
                (account.bankAccounts.firstOrNull { isSingleSelectedBankAccount(it) } != null)

        clientsForAccounts.remove(account)

        persister.deleteAccount(account, accounts)

        callAccountsChangedListeners()

        if (wasSelected || areAllAccountSelected) { // to update displayed account transactions as transactions of yet deleted accounts have to be removed
            selectedAllBankAccounts()
        }
    }


    open fun fetchAccountTransactionsAsync(account: Account,
                                           callback: (GetTransactionsResponse) -> Unit) {

        account.bankAccounts.forEach { bankAccount ->
            if (bankAccount.supportsRetrievingAccountTransactions) {
                fetchAccountTransactionsAsync(bankAccount, callback) // TODO: use a synchronous version of fetchAccountTransactions() so that all bank accounts get handled serially
            }
        }
    }

    open fun fetchAccountTransactionsAsync(bankAccount: BankAccount,
                                           callback: (GetTransactionsResponse) -> Unit) {

        fetchAccountTransactionsAsync(bankAccount, null, callback)
    }

    open fun fetchAccountTransactionsAsync(bankAccount: BankAccount, fromDate: Date?,
                                           callback: (GetTransactionsResponse) -> Unit) {

        getClientForAccount(bankAccount.account)?.let { client ->
            val startDate = Date()

            client.getTransactionsAsync(bankAccount, net.dankito.banking.ui.model.parameters.GetTransactionsParameter(true, fromDate, null, { receivedAccountsTransactionChunk(bankAccount, it) } )) { response ->

                retrievedAccountTransactions(bankAccount, startDate, response)

                callback(response)
            }
        }
    }

    open fun updateAccountsTransactionsAsync(callback: (GetTransactionsResponse) -> Unit) {
        clientsForAccounts.keys.forEach { account ->
            account.bankAccounts.forEach { bankAccount ->
                if (bankAccount.supportsRetrievingAccountTransactions) {
                    val fromDate = bankAccount.lastRetrievedTransactionsTimestamp?.let { Date(it.time - OneDayMillis) } // one day before last received transactions

                    fetchAccountTransactionsAsync(bankAccount, fromDate, callback)
                }
            }
        }
    }

    protected open fun retrievedAccountTransactions(bankAccount: BankAccount, startDate: Date, response: GetTransactionsResponse) {
        if (response.isSuccessful) {
            bankAccount.lastRetrievedTransactionsTimestamp = startDate

            updateAccountTransactionsAndBalances(bankAccount, response)
        }

        callRetrievedAccountTransactionsResponseListener(bankAccount, response)
    }

    protected open fun receivedAccountsTransactionChunk(bankAccount: BankAccount, accountTransactionsChunk: List<AccountTransaction>) {
        if (accountTransactionsChunk.isNotEmpty()) {
            bankAccount.addBookedTransactions(accountTransactionsChunk)

            callRetrievedAccountTransactionsResponseListener(bankAccount, GetTransactionsResponse(true, null, mapOf(bankAccount to accountTransactionsChunk)))
        }
    }

    protected open fun updateAccountTransactionsAndBalances(bankAccount: BankAccount, response: GetTransactionsResponse) {

        response.bookedTransactions.forEach { entry ->
            entry.key.addBookedTransactions(entry.value)
        }

        response.unbookedTransactions.forEach { entry ->
            entry.key.addUnbookedTransactions(entry.value)
        }

        response.balances.forEach { entry ->
            entry.key.balance = entry.value
        }

        persistAccount(bankAccount.account) // only needed because of balance
        persistAccountTransactions(response.bookedTransactions, response.unbookedTransactions)
    }

    open fun formatAmount(amount: BigDecimal): String {
        return String.format("%.02f", amount)
    }


    protected open fun persistAccount(account: Account) {
        persister.saveOrUpdateAccount(account, accounts)
    }

    protected open fun persistAccountTransactions(bookedTransactions: Map<BankAccount, List<AccountTransaction>>, unbookedTransactions: Map<BankAccount, List<Any>>) {
        bookedTransactions.forEach {
            persister.saveOrUpdateAccountTransactions(it.key, it.value)
        }

        // TODO: someday also persist unbooked transactions
    }


    open fun transferMoneyAsync(bankAccount: BankAccount, data: TransferMoneyData, callback: (BankingClientResponse) -> Unit) {
        getClientForAccount(bankAccount.account)?.let { client ->
            client.transferMoneyAsync(data, bankAccount, callback)
        }
    }


    open fun findUniqueBankForIbanAsync(iban: String, callback: (BankInfo?) -> Unit) {
        threadPool.runAsync {
            callback(findUniqueBankForIban(iban))
        }
    }

    open fun findUniqueBankForIban(iban: String): BankInfo? {
        val ibanWithoutWhiteSpaces = iban.replace(" ", "")

        // first two characters are country code, 3rd and 4th character are checksum, bank code has 8 digits in Germany and user
        // should enter at least five characters before we start searching (before there shouldn't be a chance of a unique result)
        if (ibanWithoutWhiteSpaces.length >= 9) {
            if (ibanWithoutWhiteSpaces.startsWith("DE")) {
                return findUniqueBankForBankCode(ibanWithoutWhiteSpaces.substring(4).ofMaxLength(8))
            }
        }

        return null
    }

    open fun findUniqueBankForBankCode(bankCode: String): BankInfo? {
        val searchResult = bankFinder.findBankByBankCode(bankCode)

        val groupedByBic = searchResult.groupBy { it.bic }

        return if (groupedByBic.size == 1) searchResult.first() else null
    }

    open fun searchBanksByNameBankCodeOrCity(query: String?): List<BankInfo> {
        return bankFinder.findBankByNameBankCodeOrCity(query)
    }


    open fun searchSelectedAccountTransactions(query: String): List<AccountTransaction> {
        val queryLowercase = query.trim().toLowerCase()

        if (queryLowercase.isEmpty()) {
            return selectedBankAccountsAccountTransactions
        }

        return selectedBankAccountsAccountTransactions.filter {
            it.otherPartyName?.toLowerCase()?.contains(queryLowercase) == true
                    || it.usage.toLowerCase().contains(queryLowercase)
                    || it.bookingText?.toLowerCase()?.contains(queryLowercase) == true
        }
    }


    open fun getMessageLogForAccounts(accounts: List<Account>): List<String> {
        val logEntries = accounts.flatMap {
            getClientForAccount(it)?.messageLogWithoutSensitiveData ?: listOf()
        }

        return logEntries.map { entry ->
            MessageLogEntryDateFormat.format(entry.time) + " " + entry.account.bank.bankCode + " " + entry.message
        }
    }


    open fun showAddAccountDialog() {
        router.showAddAccountDialog(this)
    }

    open fun showTransferMoneyDialog(preselectedBankAccount: BankAccount? = null, preselectedValues: TransferMoneyData? = null) {
        router.showTransferMoneyDialog(this, preselectedBankAccount, preselectedValues)
    }

    open fun showSendMessageLogDialog() {
        router.showSendMessageLogDialog(this)
    }


    protected open fun getClientForAccount(account: Account): IBankingClient? {
        return clientsForAccounts.get(account)
    }


    open val selectedBankAccounts: List<BankAccount>
        get() = ArrayList(selectedBankAccountsField)

    open val selectedBankAccountsAccountTransactions: List<AccountTransaction>
        get() = getAccountTransactionsForBankAccounts(selectedBankAccounts)

    open val balanceOfSelectedBankAccounts: BigDecimal
        get() = sumBalance(selectedBankAccounts.map { it.balance })


    open val areAllAccountSelected: Boolean
        get() = selectedAccountType == SelectedAccountType.AllAccounts

    open fun isSingleSelectedAccount(account: Account): Boolean {
        return selectedAccountType == SelectedAccountType.SingleAccount
                && selectedBankAccountsField.map { it.account }.toSet().containsExactly(account)
    }

    open fun isSingleSelectedBankAccount(bankAccount: BankAccount): Boolean {
        return selectedAccountType == SelectedAccountType.SingleBankAccount
                && selectedBankAccountsField.containsExactly(bankAccount)
    }

    open fun selectedAllBankAccounts() {
        selectedAccountType = SelectedAccountType.AllAccounts

        setSelectedBankAccounts(bankAccounts)
    }

    open fun selectedAccount(account: Account) {
        selectedAccountType = SelectedAccountType.SingleAccount

        setSelectedBankAccounts(account.bankAccounts)
    }

    open fun selectedBankAccount(bankAccount: BankAccount) {
        selectedAccountType = SelectedAccountType.SingleBankAccount

        setSelectedBankAccounts(listOf(bankAccount))
    }

    protected open fun setSelectedBankAccounts(bankAccounts: List<BankAccount>) {
        this.selectedBankAccountsField = ArrayList(bankAccounts) // make a copy

        callSelectedBankAccountsChangedListeners(selectedBankAccountsField)
    }


    open val accounts: List<Account>
        get() = clientsForAccounts.keys.toList()

    open val bankAccounts: List<BankAccount>
        get() = accounts.flatMap { it.bankAccounts }

    open val allTransactions: List<AccountTransaction>
        get() = getAccountTransactionsForBankAccounts(bankAccounts)

    open val balanceOfAllAccounts: BigDecimal
        get() = getBalanceForAccounts(accounts)


    open val bankAccountsSupportingRetrievingAccountTransactions: List<BankAccount>
        get() = bankAccounts.filter { it.supportsRetrievingAccountTransactions }

    open val hasBankAccountsSupportingRetrievingAccountTransactions: Boolean
        get() = doBankAccountsSupportRetrievingAccountTransactions(bankAccounts)

    open val doSelectedBankAccountsSupportRetrievingAccountTransactions: Boolean
        get() = doBankAccountsSupportRetrievingAccountTransactions(selectedBankAccounts)

    open fun doBankAccountsSupportRetrievingAccountTransactions(bankAccounts: List<BankAccount>): Boolean {
        return bankAccounts.firstOrNull { it.supportsRetrievingAccountTransactions } != null
    }


    open val bankAccountsSupportingRetrievingBalance: List<BankAccount>
        get() = bankAccounts.filter { it.supportsRetrievingBalance }

    open val hasBankAccountsSupportingRetrievingBalance: Boolean
        get() = doBankAccountsSupportRetrievingBalance(bankAccounts)

    open val doSelectedBankAccountsSupportRetrievingBalance: Boolean
        get() = doBankAccountsSupportRetrievingBalance(selectedBankAccounts)

    open fun doBankAccountsSupportRetrievingBalance(bankAccounts: List<BankAccount>): Boolean {
        return bankAccounts.firstOrNull { it.supportsRetrievingBalance } != null
    }


    open val bankAccountsSupportingTransferringMoney: List<BankAccount>
        get() = bankAccounts.filter { it.supportsTransferringMoney }

    open val hasBankAccountsSupportTransferringMoney: Boolean
        get() = doBankAccountsSupportTransferringMoney(bankAccounts)

    open val doSelectedBankAccountsSupportTransferringMoney: Boolean
        get() = doBankAccountsSupportTransferringMoney(selectedBankAccounts)

    open fun doBankAccountsSupportTransferringMoney(bankAccounts: List<BankAccount>): Boolean {
        return bankAccounts.firstOrNull { it.supportsTransferringMoney } != null
    }


    protected open fun getAccountTransactionsForBankAccounts(bankAccounts: Collection<BankAccount>): List<AccountTransaction> {
        return bankAccounts.flatMap { it.bookedTransactions }.sortedByDescending { it.bookingDate } // TODO: someday add unbooked transactions
    }

    protected open fun getBalanceForAccounts(accounts: Collection<Account>): BigDecimal {
        return accounts.map { it.balance }.fold(BigDecimal.ZERO) { acc, e -> acc + e }
    }

    protected open fun sumBalance(singleBalances: Collection<BigDecimal>): BigDecimal {
        return singleBalances.fold(BigDecimal.ZERO) { acc, e -> acc + e }
    }


    var appSettings: AppSettings = AppSettings()
        protected set

    open fun appSettingsChanged() {
        persistAppSettings()
    }

    protected open fun persistAppSettings() {
        try {
            serializer.serializeObject(appSettings, getAppSettingsFile())
        } catch (e: Exception) {
            log.error("Could not persist AppSettings to file ${getAppSettingsFile()}", e)
        }
    }

    protected open fun readAppSettings() {
        try {
            serializer.deserializeObject(getAppSettingsFile(), AppSettings::class.java)?.let {
                appSettings = it
            }
        } catch (e: Exception) {
            log.error("Could not read AppSettings from file ${getAppSettingsFile()}", e)
        }
    }

    protected open fun getAppSettingsFile(): File {
        return File(dataFolder, "app_settings.json")
    }


    open fun addAccountsChangedListener(listener: (List<Account>) -> Unit): Boolean {
        return accountsChangedListeners.add(listener)
    }

    open fun removeAccountsChangedListener(listener: (List<Account>) -> Unit): Boolean {
        return accountsChangedListeners.add(listener)
    }

    protected open fun callAccountsChangedListeners() {
        val accounts = this.accounts

        ArrayList(accountsChangedListeners).forEach {
            it(accounts) // TODO: use RxJava for this
        }
    }


    open fun addRetrievedAccountTransactionsResponseListener(listener: (BankAccount, GetTransactionsResponse) -> Unit): Boolean {
        return retrievedAccountTransactionsResponseListeners.add(listener)
    }

    open fun removeRetrievedAccountTransactionsResponseListener(listener: (BankAccount, GetTransactionsResponse) -> Unit): Boolean {
        return retrievedAccountTransactionsResponseListeners.add(listener)
    }

    protected open fun callRetrievedAccountTransactionsResponseListener(bankAccount: BankAccount, response: GetTransactionsResponse) {
        ArrayList(retrievedAccountTransactionsResponseListeners).forEach {
            it(bankAccount, response) // TODO: use RxJava for this
        }
    }


    open fun addSelectedBankAccountsChangedListener(listener: (List<BankAccount>) -> Unit): Boolean {
        return selectedBankAccountsChangedListeners.add(listener)
    }

    open fun removeSelectedBankAccountsChangedListener(listener: (List<BankAccount>) -> Unit): Boolean {
        return selectedBankAccountsChangedListeners.add(listener)
    }

    protected open fun callSelectedBankAccountsChangedListeners(selectedBankAccounts: List<BankAccount>) {
        val selectedBankAccounts = this.selectedBankAccounts

        ArrayList(selectedBankAccountsChangedListeners).forEach {
            it(selectedBankAccounts) // TODO: use RxJava for this
        }
    }

}