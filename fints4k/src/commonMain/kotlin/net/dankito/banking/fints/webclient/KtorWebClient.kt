package net.dankito.banking.fints.webclient

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import net.dankito.utils.multiplatform.log.LoggerFactory


open class KtorWebClient : IWebClient {

    companion object {
        private val log = LoggerFactory.getLogger(KtorWebClient::class)
    }


    protected val client = HttpClient {

    }


    open fun close() {
        try {
            client.close()
        } catch (e: Exception) {
            log.error(e) { "Could not close client" }
            // Cancel after timeout
            client.cancel()
        }
    }


    override suspend fun post(url: String, body: String, contentType: String, userAgent: String): WebClientResponse {
        return postInCoroutine(url, body, contentType, userAgent)
    }

    protected open suspend fun postInCoroutine(url: String, body: String, contentType: String, userAgent: String): WebClientResponse {
        try {
            val clientResponse = client.post(url) {
                contentType(ContentType.Application.OctetStream)
                setBody(body)
            }

            val responseBody = clientResponse.bodyAsText()

            return WebClientResponse(clientResponse.status.value == 200, clientResponse.status.value, body = responseBody)
        } catch (e: Exception) {
            log.error(e) { "Could not send request to url '$url'" }

            return WebClientResponse(false, error = e)
        }
    }

}