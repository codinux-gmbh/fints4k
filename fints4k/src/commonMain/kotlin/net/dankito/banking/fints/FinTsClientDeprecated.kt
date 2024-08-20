package net.dankito.banking.fints

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import net.dankito.banking.fints.callback.FinTsClientCallback
import net.dankito.banking.fints.config.FinTsClientConfiguration
import net.dankito.banking.fints.extensions.minusDays
import net.dankito.banking.fints.extensions.todayAtEuropeBerlin
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.*
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.BankResponse
import net.dankito.banking.fints.response.client.*
import net.dankito.banking.fints.webclient.IWebClient


/**
 * This is the high level FinTS client that groups single low level jobs of [FinTsJobExecutor] to senseful units e.g.
 * [addAccountAsync] gets user's TAN methods, user's TAN media, user's bank accounts and may even current balance and account transactions of last 90 days.
 */
open class FinTsClientDeprecated(
    protected val config: FinTsClientConfiguration,
    open val callback: FinTsClientCallback
) {

    constructor(callback: FinTsClientCallback) : this(FinTsClientConfiguration(), callback)


    /**
     * Retrieves information about bank (e.g. supported HBCI versions, FinTS server address,
     * supported jobs, ...).
     *
     * On success [bank] parameter is updated afterwards.
     */
    open fun getAnonymousBankInfoAsync(bank: BankData, callback: (FinTsClientResponse) -> Unit) {

        GlobalScope.launch {
            callback(getAnonymousBankInfo(bank))
        }
    }

    /**
     * Retrieves information about bank (e.g. supported HBCI versions, FinTS server address,
     * supported jobs, ...).
     *
     * On success [bank] parameter is updated afterwards.
     */
    open suspend fun getAnonymousBankInfo(bank: BankData): FinTsClientResponse {
        val context = JobContext(JobContextType.AnonymousBankInfo, this.callback, config, bank)

        val response =  config.jobExecutor.getAnonymousBankInfo(context)
        return FinTsClientResponse(context, response)
    }


    open suspend fun addAccountAsync(parameter: AddAccountParameter): AddAccountResponse {
        val bank = parameter.bank
        val context = JobContext(JobContextType.AddAccount, this.callback, config, bank)

        /*      First dialog: Get user's basic data like BPD, customer system ID and her TAN methods     */

        val newUserInfoResponse = config.jobExecutor.retrieveBasicDataLikeUsersTanMethods(context, parameter.preferredTanMethods, parameter.preferredTanMedium)

        if (newUserInfoResponse.successful == false) { // bank parameter (FinTS server address, ...) already seem to be wrong
            return AddAccountResponse(context, newUserInfoResponse)
        }

        /*      Second dialog, executed in retrieveBasicDataLikeUsersTanMethods() if required: some banks require that in order to initialize a dialog with
        strong customer authorization TAN media is required       */

        return addAccountGetAccountsAndTransactions(context, parameter)
    }

    protected open suspend fun addAccountGetAccountsAndTransactions(context: JobContext, parameter: AddAccountParameter): AddAccountResponse {

        /*      Third dialog: Now we can initialize our first dialog with strong customer authorization. Use it to get UPD and customer's accounts        */

        val getAccountsResponse = config.jobExecutor.getAccounts(context)

        if (getAccountsResponse.successful == false) {
            return AddAccountResponse(context, getAccountsResponse)
        }

        /*      Fourth dialog (if requested): Try to retrieve account balances and transactions of last 90 days without TAN     */

        if (parameter.fetchBalanceAndTransactions) {
            return addAccountGetAccountBalancesAndTransactions(context, getAccountsResponse)
        }
        else {
            return addAccountDone(context, getAccountsResponse, listOf())
        }
    }

    protected open suspend fun addAccountGetAccountBalancesAndTransactions(context: JobContext, getAccountsResponse: BankResponse): AddAccountResponse {

        val bank = context.bank
        val retrievedTransactionsResponses = mutableListOf<GetAccountTransactionsResponse>()

        val accountsSupportingRetrievingTransactions = bank.accounts.filter { it.supportsRetrievingBalance || it.supportsRetrievingAccountTransactions }

        if (accountsSupportingRetrievingTransactions.isEmpty()) {
            return addAccountDone(context, getAccountsResponse, retrievedTransactionsResponses)
        }

        accountsSupportingRetrievingTransactions.forEach { account ->
            retrievedTransactionsResponses.add(tryGetAccountTransactionsOfLast90DaysWithoutTan(bank, account))
        }

        return addAccountDone(context, getAccountsResponse, retrievedTransactionsResponses)
    }

    protected open fun addAccountDone(context: JobContext, getAccountsResponse: BankResponse,
                                      retrievedTransactionsResponses: List<GetAccountTransactionsResponse>): AddAccountResponse {

        return AddAccountResponse(context, getAccountsResponse, retrievedTransactionsResponses)
    }


    /**
     * Some banks support that according to PSD2 account transactions may be retrieved without
     * a TAN (= no strong customer authorization needed).
     *
     * Check if bank supports this.
     */
    open suspend fun tryGetAccountTransactionsOfLast90DaysWithoutTan(bank: BankData, account: AccountData): GetAccountTransactionsResponse {

        return getAccountTransactionsAsync(createGetAccountTransactionsOfLast90DaysParameter(bank, account))
    }

    protected open fun createGetAccountTransactionsOfLast90DaysParameter(bank: BankData, account: AccountData): GetAccountTransactionsParameter {
        // Europe/Berlin: we're communicating with German bank servers, so we have to use their time zone
        val ninetyDaysAgo = LocalDate.todayAtEuropeBerlin().minusDays(90)

        return GetAccountTransactionsParameter(bank, account, account.supportsRetrievingBalance, ninetyDaysAgo, abortIfTanIsRequired = true)
    }

    open suspend fun getAccountTransactionsAsync(parameter: GetAccountTransactionsParameter): GetAccountTransactionsResponse {

        val context = JobContext(JobContextType.GetTransactions, this.callback, config, parameter.bank, parameter.account)

        return config.jobExecutor.getTransactionsAsync(context, parameter)
    }


    open suspend fun getTanMediaList(bank: BankData, tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle,
                             tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien): GetTanMediaListResponse {

        val context = JobContext(JobContextType.GetTanMedia, this.callback, config, bank)

        return config.jobExecutor.getTanMediaList(context, tanMediaKind, tanMediumClass)
    }


    open suspend fun changeTanMedium(newActiveTanMedium: TanGeneratorTanMedium, bank: BankData): FinTsClientResponse {
        val context = JobContext(JobContextType.ChangeTanMedium, this.callback, config, bank)

        val response = config.jobExecutor.changeTanMedium(context, newActiveTanMedium)
        return FinTsClientResponse(context, response)
    }


    open suspend fun doBankTransferAsync(bankTransferData: BankTransferData, bank: BankData, account: AccountData): FinTsClientResponse {
        val context = JobContext(JobContextType.TransferMoney, this.callback, config, bank, account)

        return config.jobExecutor.transferMoneyAsync(context, bankTransferData)
    }

}