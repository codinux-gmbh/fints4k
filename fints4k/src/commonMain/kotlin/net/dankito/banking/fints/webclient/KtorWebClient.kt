package net.dankito.banking.fints.webclient

import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.cancel
import net.codinux.log.logger


open class KtorWebClient(
    connectTimeoutMillis: Long = 10_000,
    requestTimeoutMillis: Long = 60_000
) : IWebClient {

    private val log by logger()


    protected val client = HttpClient {
        install(HttpTimeout) {
            this.connectTimeoutMillis = connectTimeoutMillis
            this.requestTimeoutMillis = requestTimeoutMillis
        }
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