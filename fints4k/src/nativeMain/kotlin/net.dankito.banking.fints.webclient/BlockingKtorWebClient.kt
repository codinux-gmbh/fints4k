package net.dankito.banking.fints.webclient
import kotlinx.coroutines.runBlocking


open class BlockingKtorWebClient : KtorWebClient() {


    override fun post(url: String, body: String, contentType: String, userAgent: String, callback: (WebClientResponse) -> Unit) {
        runBlocking {
            postInCoroutine(url, body, contentType, userAgent, callback)
        }
    }

}