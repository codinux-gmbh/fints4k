package net.dankito.banking.fints

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import net.dankito.banking.fints.callback.FinTsClientCallback
import net.dankito.utils.multiplatform.extensions.minusDays
import net.dankito.utils.multiplatform.extensions.todayAtEuropeBerlin
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.*
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.BankResponse
import net.dankito.banking.fints.response.client.*
import net.dankito.banking.fints.response.segments.*
import kotlin.jvm.JvmOverloads


/**
 * This is the high level FinTS client that groups single low level jobs of [FinTsJobExecutor] to senseful units e.g.
 * [addAccountAsync] gets user's TAN methods, user's TAN media, user's bank accounts and may even current balance and account transactions of last 90 days.
 */
open class FinTsClient @JvmOverloads constructor(
    open var callback: FinTsClientCallback,
    protected open val jobExecutor: FinTsJobExecutor = FinTsJobExecutor(),
    protected open val product: ProductData = ProductData("15E53C26816138699C7B6A3E8", "1.0.0") // TODO: get version dynamically
) {

    companion object {
        val SupportedAccountTypes = listOf(AccountType.Girokonto, AccountType.Festgeldkonto, AccountType.Kreditkartenkonto)
    }


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
        val context = JobContext(JobContextType.AnonymousBankInfo, this.callback, product, bank)

        jobExecutor.getAnonymousBankInfo(context) { response ->
            callback(FinTsClientResponse(context, response))
        }
    }


    open fun addAccountAsync(parameter: AddAccountParameter, callback: (AddAccountResponse) -> Unit) {
        val bank = parameter.bank
        val context = JobContext(JobContextType.AddAccount, this.callback, product, bank)

        /*      First dialog: Get user's basic data like BPD, customer system ID and her TAN methods     */

        jobExecutor.retrieveBasicDataLikeUsersTanMethods(context, parameter.preferredTanMethods, parameter.preferredTanMedium) { newUserInfoResponse ->

            if (newUserInfoResponse.successful == false) { // bank parameter (FinTS server address, ...) already seem to be wrong
                callback(AddAccountResponse(context, newUserInfoResponse))
                return@retrieveBasicDataLikeUsersTanMethods
            }

            /*      Second dialog: some banks require that in order to initialize a dialog with strong customer authorization TAN media is required       */

            addAccountGetAccountsAndTransactions(context, parameter, callback)
        }
    }

    protected open fun addAccountGetAccountsAndTransactions(context: JobContext, parameter: AddAccountParameter,
                                                            callback: (AddAccountResponse) -> Unit) {

        /*      Third dialog: Now we can initialize our first dialog with strong customer authorization. Use it to get UPD and customer's accounts        */

        jobExecutor.getAccounts(context) { getAccountsResponse ->

            if (getAccountsResponse.successful == false) {
                callback(AddAccountResponse(context, getAccountsResponse))
                return@getAccounts
            }

            /*      Fourth dialog (if requested): Try to retrieve account balances and transactions of last 90 days without TAN     */

            if (parameter.fetchBalanceAndTransactions) {
                addAccountGetAccountBalancesAndTransactions(context, getAccountsResponse, callback)
            }
            else {
                addAccountDone(context, getAccountsResponse, listOf(), callback)
            }
        }
    }

    protected open fun addAccountGetAccountBalancesAndTransactions(context: JobContext, getAccountsResponse: BankResponse,
                                                                   callback: (AddAccountResponse) -> Unit) {

        val bank = context.bank
        val retrievedTransactionsResponses = mutableListOf<GetAccountTransactionsResponse>()

        val accountsSupportingRetrievingTransactions = bank.accounts.filter { it.supportsRetrievingBalance || it.supportsRetrievingAccountTransactions }
        val countAccountsSupportingRetrievingTransactions = accountsSupportingRetrievingTransactions.size
        var countRetrievedAccounts = 0

        if (countAccountsSupportingRetrievingTransactions == 0) {
            addAccountDone(context, getAccountsResponse, retrievedTransactionsResponses, callback)
            return // not necessary just to make it clearer that code below doesn't get called
        }

        accountsSupportingRetrievingTransactions.forEach { account ->
            tryGetAccountTransactionsOfLast90DaysWithoutTan(bank, account) { response ->
                retrievedTransactionsResponses.add(response)

                countRetrievedAccounts++
                if (countRetrievedAccounts == countAccountsSupportingRetrievingTransactions) {
                    addAccountDone(context, getAccountsResponse, retrievedTransactionsResponses, callback)
                }
            }
        }
    }

    protected open fun addAccountDone(context: JobContext, getAccountsResponse: BankResponse,
                                      retrievedTransactionsResponses: List<GetAccountTransactionsResponse>,
                                      callback: (AddAccountResponse) -> Unit) {

        callback(AddAccountResponse(context, getAccountsResponse, retrievedTransactionsResponses))
    }


    /**
     * Some banks support that according to PSD2 account transactions may be retrieved without
     * a TAN (= no strong customer authorization needed).
     *
     * Check if bank supports this.
     */
    open fun tryGetAccountTransactionsOfLast90DaysWithoutTan(bank: BankData, account: AccountData, callback: (GetAccountTransactionsResponse) -> Unit) {

        getAccountTransactionsAsync(createGetAccountTransactionsOfLast90DaysParameter(bank, account), callback)
    }

    protected open fun createGetAccountTransactionsOfLast90DaysParameter(bank: BankData, account: AccountData): GetAccountTransactionsParameter {
        // Europe/Berlin: we're communicating with German bank servers, so we have to use their time zone
        val ninetyDaysAgo = LocalDate.todayAtEuropeBerlin().minusDays(90)

        return GetAccountTransactionsParameter(bank, account, account.supportsRetrievingBalance, ninetyDaysAgo, abortIfTanIsRequired = true)
    }

    open fun getAccountTransactionsAsync(parameter: GetAccountTransactionsParameter, callback: (GetAccountTransactionsResponse) -> Unit) {

        val context = JobContext(JobContextType.GetTransactions, this.callback, product, parameter.bank, parameter.account)

        jobExecutor.getTransactionsAsync(context, parameter, callback)
    }


    open fun getTanMediaList(bank: BankData, tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle,
                             tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien, callback: (GetTanMediaListResponse) -> Unit) {

        val context = JobContext(JobContextType.GetTanMedia, this.callback, product, bank)

        jobExecutor.getTanMediaList(context, tanMediaKind, tanMediumClass, callback)
    }


    open fun changeTanMedium(newActiveTanMedium: TanGeneratorTanMedium, bank: BankData, callback: (FinTsClientResponse) -> Unit) {
        val context = JobContext(JobContextType.ChangeTanMedium, this.callback, product, bank)

        jobExecutor.changeTanMedium(context, newActiveTanMedium) { response ->
            callback(FinTsClientResponse(context, response))
        }
    }


    open fun doBankTransferAsync(bankTransferData: BankTransferData, bank: BankData, account: AccountData, callback: (FinTsClientResponse) -> Unit) {
        val context = JobContext(JobContextType.TransferMoney, this.callback, product, bank, account)

        jobExecutor.doBankTransferAsync(context, bankTransferData, callback)
    }

}