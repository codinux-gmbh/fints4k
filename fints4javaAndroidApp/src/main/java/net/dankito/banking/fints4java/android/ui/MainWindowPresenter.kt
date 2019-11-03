package net.dankito.banking.fints4java.android.ui

import net.dankito.banking.ui.model.Account
import net.dankito.banking.ui.model.AccountTransaction
import net.dankito.banking.ui.model.BankAccount
import net.dankito.banking.ui.model.responses.AddAccountResponse
import net.dankito.banking.ui.model.responses.GetTransactionsResponse
import net.dankito.fints.FinTsClientCallback
import net.dankito.fints.FinTsClientForCustomer
import net.dankito.fints.banks.BankFinder
import net.dankito.fints.model.BankInfo
import net.dankito.fints.model.BankTransferData
import net.dankito.fints.model.CustomerData
import net.dankito.fints.model.GetTransactionsParameter
import net.dankito.fints.model.mapper.BankDataMapper
import net.dankito.fints.response.client.FinTsClientResponse
import net.dankito.fints.util.IBase64Service
import net.dankito.utils.IThreadPool
import net.dankito.utils.ThreadPool
import java.math.BigDecimal
import java.util.*
import kotlin.collections.ArrayList


open class MainWindowPresenter(protected val base64Service: IBase64Service,
                               protected val callback: FinTsClientCallback
) {

    protected val bankFinder: BankFinder = BankFinder()

    protected val threadPool: IThreadPool = ThreadPool()

    protected val bankDataMapper = BankDataMapper()

    protected val fints4javaModelMapper = net.dankito.banking.fints4java.android.mapper.fints4javaModelMapper()


    protected val accounts = mutableMapOf<Account, FinTsClientForCustomer>()

    protected val accountAddedListeners = mutableListOf<(Account) -> Unit>()

    protected val retrievedAccountTransactionsResponseListeners = mutableListOf<(BankAccount, GetTransactionsResponse) -> Unit>()


    open fun addAccountAsync(bankInfo: BankInfo, customerId: String, pin: String,
                             callback: (AddAccountResponse) -> Unit) {

        val bank = bankDataMapper.mapFromBankInfo(bankInfo)
        val customer = CustomerData(customerId, pin)
        val newClient = FinTsClientForCustomer(bank, customer, this.callback, base64Service)

        newClient.addAccountAsync { response ->
            val account = fints4javaModelMapper.mapAccount(customer, bank)
            val mappedResponse = fints4javaModelMapper.mapResponse(account, response)

            if (response.isSuccessful) {
                accounts.put(account, newClient)

                callAccountAddedListeners(account)

                if (response.supportsRetrievingTransactionsOfLast90DaysWithoutTan) {
                    account.bankAccounts.forEach { bankAccount ->
                        retrievedAccountTransactions(bankAccount, mappedResponse)
                    }
                }
            }

            callback(mappedResponse)
        }
    }


    open fun getAccountTransactionsAsync(account: Account,
                                         callback: (GetTransactionsResponse) -> Unit) {

        account.bankAccounts.forEach { bankAccount ->
            getAccountTransactionsAsync(bankAccount, callback) // TODO: use a synchronous version of getAccountTransactions() so that all bank accounts get handled serially
        }
    }

    open fun getAccountTransactionsAsync(bankAccount: BankAccount,
                                         callback: (GetTransactionsResponse) -> Unit) {

        getAccountTransactionsAsync(bankAccount, null, callback)
    }

    open fun getAccountTransactionsAsync(bankAccount: BankAccount, fromDate: Date?,
                                         callback: (GetTransactionsResponse) -> Unit) {

        getClientForAccount(bankAccount.account)?.let { client ->
            client.getTransactionsAsync(GetTransactionsParameter(true, fromDate)) { response ->

                val mappedResponse = fints4javaModelMapper.mapResponse(bankAccount.account, response)

                retrievedAccountTransactions(bankAccount, mappedResponse)

                callback(mappedResponse)
            }
        }
    }

    open fun updateAccountsTransactionsAsync(callback: (GetTransactionsResponse) -> Unit) {
        accounts.keys.forEach { account ->
            account.bankAccounts.forEach { bankAccount ->
                val today = Date() // TODO: still don't know where this bug is coming from that bank returns a transaction dated at end of year
                val lastRetrievedTransactionDate = bankAccount.bookedTransactions.firstOrNull { it.bookingDate <= today }?.bookingDate
                val fromDate = lastRetrievedTransactionDate?.let { Date(it.time - 24 * 60 * 60 * 1000) } // on day before last received transaction

                getAccountTransactionsAsync(bankAccount, fromDate, callback)
            }
        }
    }

    protected open fun retrievedAccountTransactions(bankAccount: BankAccount, response: GetTransactionsResponse) {
        if (response.isSuccessful) {
            updateAccountTransactionsAndBalances(bankAccount, response)
        }

        callRetrievedAccountTransactionsResponseListener(bankAccount, response)
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
    }


    open fun transferMoneyAsync(bankAccount: BankAccount, bankTransferData: BankTransferData, callback: (FinTsClientResponse) -> Unit) {
        getClientForAccount(bankAccount.account)?.let { client ->
            client.doBankTransferAsync(bankTransferData, callback)
        }
    }


    open fun searchForBankAsync(enteredBankCode: String, callback: (List<BankInfo>) -> Unit) {
        threadPool.runAsync {
            callback(searchForBank(enteredBankCode))
        }
    }

    open fun searchForBank(enteredBankCode: String): List<BankInfo> {
        return bankFinder.findBankByBankCode(enteredBankCode)
    }


    open fun searchAccountTransactions(query: String): List<AccountTransaction> {
        val queryLowercase = query.trim().toLowerCase()

        if (queryLowercase.isEmpty()) {
            return allTransactions
        }

        return allTransactions.filter {
            it.otherPartyName?.toLowerCase()?.contains(queryLowercase) == true
                    || it.usage.toLowerCase().contains(queryLowercase)
                    || it.bookingText?.toLowerCase()?.contains(queryLowercase) == true
        }
    }


    protected open fun getClientForAccount(account: Account): FinTsClientForCustomer? {
        accounts.get(account)?.let { client ->
            account.selectedTanProcedure?.let { selectedTanProcedure ->
                client.customer.selectedTanProcedure = fints4javaModelMapper.mapTanProcedureBack(selectedTanProcedure)
            }

            return client // TODO: return IBankingClient
        }

        return null
    }


    open val allTransactions: List<AccountTransaction>
        get() = accounts.keys.flatMap { it.transactions }.sortedByDescending { it.bookingDate } // TODO: someday add unbooked transactions

    open val balanceOfAllAccounts: BigDecimal
        get() = accounts.keys.map { it.balance }.fold(BigDecimal.ZERO) { acc, e -> acc + e }


    open fun addAccountAddedListener(listener: (Account) -> Unit) {
        accountAddedListeners.add(listener)
    }

    protected open fun callAccountAddedListeners(account: Account) {
        ArrayList(accountAddedListeners).forEach {
            it(account) // TODO: use RxJava for this
        }
    }

    open fun addRetrievedAccountTransactionsResponseListener(listener: (BankAccount, GetTransactionsResponse) -> Unit) {
        retrievedAccountTransactionsResponseListeners.add(listener)
    }

    protected open fun callRetrievedAccountTransactionsResponseListener(bankAccount: BankAccount, response: GetTransactionsResponse) {
        ArrayList(retrievedAccountTransactionsResponseListeners).forEach {
            it(bankAccount, response) // TODO: use RxJava for this
        }
    }

}