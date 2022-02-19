package net.dankito.banking.fints


import kotlinx.coroutines.*
import net.dankito.banking.fints.callback.FinTsClientCallback
import net.dankito.banking.fints.model.AddAccountParameter
import net.dankito.banking.fints.response.client.AddAccountResponse
import net.dankito.banking.fints.webclient.IWebClient

open class iOSFinTsClient(
    callback: FinTsClientCallback,
    webClient: IWebClient
) {

    protected open val fintsClient = FinTsClientDeprecated(callback, FinTsJobExecutor(RequestExecutor(webClient = webClient)))

    open var callback: FinTsClientCallback
        get() = fintsClient.callback
        set(value) {
            fintsClient.callback = value
        }


    open fun addAccountAsync(parameter: AddAccountParameter, callback: (AddAccountResponse) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) { // do not block UI thread as with runBlocking { } but stay on UI thread as passing mutable state between threads currently doesn't work in Kotlin/Native
            callback(fintsClient.addAccountAsync(parameter))
        }
    }

}