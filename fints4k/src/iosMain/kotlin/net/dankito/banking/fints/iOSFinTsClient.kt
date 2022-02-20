package net.dankito.banking.fints

import kotlinx.coroutines.*
import net.dankito.banking.client.model.parameter.GetAccountDataParameter
import net.dankito.banking.client.model.response.GetAccountDataResponse
import net.dankito.banking.fints.callback.FinTsClientCallback
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


    open fun getAccountData(parameter: GetAccountDataParameter, callback: (GetAccountDataResponse) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) { // do not block UI thread as with runBlocking { } but stay on UI thread as passing mutable state between threads currently doesn't work in Kotlin/Native
            callback(fintsClient.getAccountData(parameter))
        }
    }

}