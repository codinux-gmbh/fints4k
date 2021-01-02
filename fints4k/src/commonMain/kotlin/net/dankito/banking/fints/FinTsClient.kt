package net.dankito.banking.fints

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dankito.banking.fints.callback.FinTsClientCallback
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.*
import net.dankito.banking.fints.messages.segmente.id.CustomerSegmentId
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.BankResponse
import net.dankito.banking.fints.response.client.*
import net.dankito.banking.fints.response.segments.*
import net.dankito.utils.multiplatform.Date


/**
 * This is the high level FinTS client that groups single low level jobs of [FinTsJobExecutor] to senseful units e.g.
 * [addAccountAsync] gets user's TAN methods, user's TAN media, user's bank accounts and may even current balance and account transactions of last 90 days.
 */
open class FinTsClient(
    protected open val jobExecutor: FinTsJobExecutor
) {

    companion object {
        val SupportedAccountTypes = listOf(AccountType.Girokonto, AccountType.Festgeldkonto, AccountType.Kreditkartenkonto)

        const val OneDayMillis = 24 * 60 * 60 * 1000L
        const val NinetyDaysMillis = 90 * OneDayMillis
    }


    constructor(callback: FinTsClientCallback) : this(FinTsJobExecutor(callback))


    open val messageLogWithoutSensitiveData: List<MessageLogEntry>
        get() = jobExecutor.messageLogWithoutSensitiveData


    /**
     * Retrieves information about bank (e.g. supported HBCI versions, FinTS server address,
     * supported jobs, ...).
     *
     * On success [bank] parameter is updated afterwards.
     */
    open fun getAnonymousBankInfoAsync(bank: BankData, callback: (FinTsClientResponse) -> Unit) {

        GlobalScope.launch {
            getAnonymousBankInfo(bank, callback)
        }
    }

    /**
     * Retrieves information about bank (e.g. supported HBCI versions, FinTS server address,
     * supported jobs, ...).
     *
     * On success [bank] parameter is updated afterwards.
     */
    open fun getAnonymousBankInfo(bank: BankData, callback: (FinTsClientResponse) -> Unit) {
        jobExecutor.getAnonymousBankInfo(bank) { response ->
            callback(FinTsClientResponse(response))
        }
    }


    open fun addAccountAsync(parameter: AddAccountParameter, callback: (AddAccountResponse) -> Unit) {
        val bank = parameter.bank

        /*      First dialog: Get user's basic data like BPD, customer system ID and her TAN methods     */

        jobExecutor.retrieveBasicDataLikeUsersTanMethods(bank) { newUserInfoResponse ->

            if (newUserInfoResponse.successful == false) { // bank parameter (FinTS server address, ...) already seem to be wrong
                callback(AddAccountResponse(newUserInfoResponse, bank))
                return@retrieveBasicDataLikeUsersTanMethods
            }

            jobExecutor.getUsersTanMethod(bank) { didSelectTanMethod ->

                if (didSelectTanMethod == false) {
                    callback(AddAccountResponse(BankResponse(false), bank))
                    return@getUsersTanMethod
                }

                /*      Second dialog: some banks require that in order to initialize a dialog with strong customer authorization TAN media is required       */

                if (jobExecutor.isJobSupported(bank, CustomerSegmentId.TanMediaList)) {
                    getTanMediaList(bank, TanMedienArtVersion.Alle, TanMediumKlasse.AlleMedien) {
                        addAccountGetAccountsAndTransactions(parameter, bank, callback)
                    }
                } else {
                    addAccountGetAccountsAndTransactions(parameter, bank, callback)
                }
            }
        }
    }

    protected open fun addAccountGetAccountsAndTransactions(parameter: AddAccountParameter, bank: BankData,
                                                            callback: (AddAccountResponse) -> Unit) {

        /*      Third dialog: Now we can initialize our first dialog with strong customer authorization. Use it to get UPD and customer's accounts        */

        jobExecutor.getAccounts(bank) { getAccountsResponse ->

            if (getAccountsResponse.successful == false) {
                callback(AddAccountResponse(getAccountsResponse, bank))
                return@getAccounts
            }

            /*      Fourth dialog (if requested): Try to retrieve account balances and transactions of last 90 days without TAN     */

            if (parameter.fetchBalanceAndTransactions) {
                addAccountGetAccountBalancesAndTransactions(bank, getAccountsResponse, callback)
            }
            else {
                addAccountDone(bank, getAccountsResponse, mapOf(), callback)
            }
        }
    }

    protected open fun addAccountGetAccountBalancesAndTransactions(bank: BankData, getAccountsResponse: BankResponse,
                                                                   callback: (AddAccountResponse) -> Unit) {

        val retrievedAccountData = bank.accounts.associateBy( { it }, { RetrievedAccountData.unsuccessful(it) } ).toMutableMap()

        val accountsSupportingRetrievingTransactions = bank.accounts.filter { it.supportsRetrievingBalance || it.supportsRetrievingAccountTransactions }
        val countAccountsSupportingRetrievingTransactions = accountsSupportingRetrievingTransactions.size
        var countRetrievedAccounts = 0

        if (countAccountsSupportingRetrievingTransactions == 0) {
            addAccountDone(bank, getAccountsResponse, retrievedAccountData, callback)
            return // no necessary just to make it clearer that code below doesn't get called
        }

        accountsSupportingRetrievingTransactions.forEach { account ->
            tryGetTransactionsOfLast90DaysWithoutTan(bank, account) { response ->
                retrievedAccountData.put(account, response.retrievedData.first())

                countRetrievedAccounts++
                if (countRetrievedAccounts == countAccountsSupportingRetrievingTransactions) {
                    addAccountDone(bank, getAccountsResponse, retrievedAccountData, callback)
                }
            }
        }
    }

    protected open fun addAccountDone(bank: BankData, getAccountsResponse: BankResponse,
                                      retrievedAccountData: Map<AccountData, RetrievedAccountData>,
                                      callback: (AddAccountResponse) -> Unit) {

        callback(AddAccountResponse(getAccountsResponse, bank, retrievedAccountData.values.toList()))
    }


    /**
     * Some banks support that according to PSD2 account transactions may be retrieved without
     * a TAN (= no strong customer authorization needed).
     *
     * Check if bank supports this.
     */
    open fun tryGetTransactionsOfLast90DaysWithoutTan(bank: BankData, account: AccountData, callback: (GetTransactionsResponse) -> Unit) {

        val ninetyDaysAgo = Date(Date.today.millisSinceEpoch - NinetyDaysMillis)

        getTransactionsAsync(GetTransactionsParameter(account, account.supportsRetrievingBalance, ninetyDaysAgo, abortIfTanIsRequired = true), bank) { response ->
            callback(response)
        }
    }

    open fun getTransactionsAsync(parameter: GetTransactionsParameter, bank: BankData, callback: (GetTransactionsResponse) -> Unit) {

        jobExecutor.getTransactionsAsync(parameter, bank, callback)
    }


    open fun getTanMediaList(bank: BankData, tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle,
                             tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien, callback: (GetTanMediaListResponse) -> Unit) {

        jobExecutor.getTanMediaList(bank, tanMediaKind, tanMediumClass, callback)
    }


    open fun changeTanMedium(newActiveTanMedium: TanGeneratorTanMedium, bank: BankData, callback: (FinTsClientResponse) -> Unit) {
        jobExecutor.changeTanMedium(newActiveTanMedium, bank) { response ->
            callback(FinTsClientResponse(response))
        }
    }


    open fun doBankTransferAsync(bankTransferData: BankTransferData, bank: BankData, account: AccountData, callback: (FinTsClientResponse) -> Unit) {
        jobExecutor.doBankTransferAsync(bankTransferData, bank, account, callback)
    }

}