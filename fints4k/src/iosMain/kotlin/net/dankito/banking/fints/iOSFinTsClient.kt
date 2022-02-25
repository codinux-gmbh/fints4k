package net.dankito.banking.fints

import kotlinx.coroutines.*
import net.dankito.banking.client.model.parameter.GetAccountDataParameter
import net.dankito.banking.client.model.parameter.TransferMoneyParameter
import net.dankito.banking.client.model.response.GetAccountDataResponse
import net.dankito.banking.client.model.response.TransferMoneyResponse
import net.dankito.banking.fints.callback.FinTsClientCallback
import net.dankito.banking.fints.model.Money
import net.dankito.banking.fints.webclient.IWebClient

open class iOSFinTsClient(
    callback: FinTsClientCallback,
    webClient: IWebClient
) {

    protected open val fintsClient = FinTsClient(callback, FinTsJobExecutor(RequestExecutor(webClient = webClient)))

    open var callback: FinTsClientCallback
        get() = fintsClient.callback
        set(value) {
            fintsClient.callback = value
        }


    open fun getAccountDataAsync(bankCode: String, loginName: String, password: String, callback: (GetAccountDataResponse) -> Unit) {
        dispatchToCoroutine {
            callback(fintsClient.getAccountDataAsync(bankCode, loginName, password))
        }
    }

    open fun getAccountDataAsync(param: GetAccountDataParameter, callback: (GetAccountDataResponse) -> Unit) {
        dispatchToCoroutine {
            callback(fintsClient.getAccountDataAsync(param))
        }
    }


    open suspend fun transferMoneyAsync(bankCode: String, loginName: String, password: String, recipientName: String, recipientAccountIdentifier: String,
                                        amount: Money, reference: String? = null, callback: (TransferMoneyResponse) -> Unit) {
        dispatchToCoroutine {
            callback(fintsClient.transferMoneyAsync(bankCode, loginName, password, recipientName, recipientAccountIdentifier, amount))
        }
    }

    open fun transferMoneyAsync(param: TransferMoneyParameter, callback: (TransferMoneyResponse) -> Unit) {
        dispatchToCoroutine {
            callback(fintsClient.transferMoneyAsync(param))
        }
    }


    protected open fun dispatchToCoroutine(action: suspend () -> Unit) {
        GlobalScope.launch(Dispatchers.Main) { // do not block UI thread as with runBlocking { } but stay on UI thread as passing mutable state between threads currently doesn't work in Kotlin/Native
            action()
        }
    }

}