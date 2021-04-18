package net.dankito.banking.fints.rest.service

import net.dankito.banking.bankfinder.InMemoryBankFinder
import net.dankito.banking.fints.FinTsClientForCustomer
import net.dankito.banking.fints.callback.FinTsClientCallback
import net.dankito.banking.fints.callback.SimpleFinTsClientCallback
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.client.AddAccountResponse
import net.dankito.banking.fints.response.client.GetTransactionsResponse
import net.dankito.banking.fints.rest.model.BankAccessData
import net.dankito.banking.fints.rest.model.EnterTanContext
import net.dankito.banking.fints.rest.model.EnteringTanRequested
import net.dankito.banking.fints.rest.model.ResponseHolder
import net.dankito.banking.fints.rest.model.dto.request.GetAccountsTransactionsRequestDto
import net.dankito.banking.fints.rest.model.dto.request.TanResponseDto
import net.dankito.banking.fints.rest.service.model.GetAccountsTransactionsResponse
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import javax.enterprise.context.ApplicationScoped


@ApplicationScoped
class fints4kService {

    protected val bankFinder = InMemoryBankFinder()

    protected val clientCache = ConcurrentHashMap<String, FinTsClientForCustomer>()

    // TODO: create clean up job for timed out TAN requests
    protected val tanRequests = ConcurrentHashMap<String, EnterTanContext>()


    fun getAddAccountResponse(accessData: BankAccessData): ResponseHolder<AddAccountResponse> {
        val (bank, errorMessage) = mapToBankData(accessData)

        if (errorMessage != null) {
            return ResponseHolder(errorMessage)
        }

        return getAccountData(bank)
    }

    protected fun getAccountData(bank: BankData): ResponseHolder<AddAccountResponse> {
        return getAsyncResponse(bank) { client, responseRetrieved ->
            client.addAccountAsync(AddAccountParameter(bank)) { response ->
                responseRetrieved(response)
            }
        }
    }


    fun getAccountTransactions(dto: GetAccountsTransactionsRequestDto): GetAccountsTransactionsResponse {
        val (bank, errorMessage) = mapToBankData(dto.credentials)

        if (errorMessage != null) {
            return GetAccountsTransactionsResponse(listOf(ResponseHolder(errorMessage)))
        }

        val retrievedAccounts = getAccounts(bank)

        val transactionsPerAccount = dto.accounts.map { accountDto ->
            val account = retrievedAccounts?.firstOrNull { it.accountIdentifier == accountDto.identifier }

            return@map if (account != null) {
                val parameter = GetTransactionsParameter(account, dto.alsoRetrieveBalance, dto.fromDate, dto.toDate, abortIfTanIsRequired = dto.abortIfTanIsRequired)
                getAccountTransactions(bank, parameter)
            }
            else {
                ResponseHolder("Account with identifier '${accountDto.identifier}' not found. Available accounts: " +
                        "${retrievedAccounts?.joinToString(", ") { it.accountIdentifier }}")
            }
        }

        return GetAccountsTransactionsResponse(transactionsPerAccount)
    }

    fun getAccountTransactions(bank: BankData, parameter: GetTransactionsParameter): ResponseHolder<GetTransactionsResponse> {
        return getAsyncResponse(bank) { client, responseRetrieved ->
            client.getTransactionsAsync(parameter) { response ->
                responseRetrieved(response)
            }
        }
    }


    fun handleTanResponse(dto: TanResponseDto): ResponseHolder<*> {
        tanRequests.remove(dto.tanRequestId)?.let { enterTanContext ->
            val responseHolder = enterTanContext.responseHolder
            responseHolder.resetAfterEnteringTan()

            enterTanContext.enterTanResult.set(dto.enterTanResult)
            enterTanContext.countDownLatch.countDown()

            responseHolder.waitForResponse()

            return responseHolder
        }

        return ResponseHolder<Any>("No TAN request found for TAN Request ID '${dto.tanRequestId}'")
    }


    protected fun <T> getAsyncResponse(bank: BankData, executeRequest: (FinTsClientForCustomer, ((T) -> Unit)) -> Unit): ResponseHolder<T> {
        val responseHolder = ResponseHolder<T>()

        val client = getClient(bank, responseHolder)

        executeRequest(client) { response ->
            responseHolder.setResponse(response)
        }

        responseHolder.waitForResponse()

        return responseHolder
    }

    private fun <T> getClient(bank: BankData, responseHolder: ResponseHolder<T>): FinTsClientForCustomer {
        val cacheKey = getCacheKey(bank.bankCode, bank.customerId)

        clientCache[cacheKey]?.let {
            // TODO: this will not work for two parallel calls for the same account if both calls require entering a TAN as second one overwrites callback and ResponseHolder of first one -> first one blocks forever
            it.setCallback(createFinTsClientCallback(responseHolder)) // we have to newly create callback otherwise ResponseHolder instance of when client was created is used -> its CountDownLatch would never signal
            return it
        }

        val client = FinTsClientForCustomer(bank, createFinTsClientCallback(responseHolder))

        clientCache[cacheKey] = client

        return client
    }

    private fun <T> createFinTsClientCallback(responseHolder: ResponseHolder<T>): FinTsClientCallback {
        return SimpleFinTsClientCallback({ bank, tanChallenge -> handleEnterTan(bank, tanChallenge, responseHolder) }) { supportedTanMethods, suggestedTanMethod ->
            suggestedTanMethod
        }
    }

    protected fun <T> handleEnterTan(bank: BankData, tanChallenge: TanChallenge, responseHolder: ResponseHolder<T>): EnterTanResult {
        val enterTanResult = AtomicReference<EnterTanResult>()
        val enterTanLatch = CountDownLatch(1)

        val tanRequestId = UUID.randomUUID().toString()

        tanRequests.put(tanRequestId, EnterTanContext(enterTanResult, responseHolder, enterTanLatch))

        responseHolder.setEnterTanRequest(EnteringTanRequested(tanRequestId, tanChallenge))

        enterTanLatch.await()

        return enterTanResult.get()
    }


    protected fun mapToBankData(accessData: BankAccessData): Pair<BankData, String?> {
        val bankSearchResult = bankFinder.findBankByBankCode(accessData.bankCode)
        val fintsServerAddress = accessData.finTsServerAddress ?: bankSearchResult.firstOrNull { it.pinTanAddress != null }?.pinTanAddress
        val potentialBankInfo = bankSearchResult.firstOrNull()

        val bank = BankData(accessData.bankCode, accessData.loginName, accessData.password, fintsServerAddress ?: "",
            potentialBankInfo?.bic ?: "", potentialBankInfo?.name ?: "")

        if (fintsServerAddress == null) {
            val errorMessage = if (bankSearchResult.isEmpty()) "No bank found for bank code '${accessData.bankCode}'" else "Bank '${bankSearchResult.firstOrNull()?.name} does not support FinTS 3.0"

            return Pair(bank, errorMessage)
        }

        return Pair(bank, null)
    }


    protected fun getAccounts(bank: BankData): List<AccountData>? {
        getCachedClient(bank)?.bank?.accounts?.let {
            return it
        }

        val addAccountResponse = getAccountData(bank)

        return addAccountResponse.response?.bank?.accounts
    }


    private fun getCachedClient(bank: BankData): FinTsClientForCustomer? {
        val cacheKey = getCacheKey(bank.bankCode, bank.customerId)

        return clientCache[cacheKey]
    }

    private fun getCachedClient(credentials: BankAccessData): FinTsClientForCustomer? {
        val cacheKey = getCacheKey(credentials.bankCode, credentials.loginName)

        return clientCache[cacheKey]
    }

    private fun getCacheKey(bankCode: String, loginName: String): String {
        return bankCode + "_" + loginName
    }

}