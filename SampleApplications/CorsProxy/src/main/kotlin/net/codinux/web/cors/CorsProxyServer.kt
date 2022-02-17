package net.codinux.web.cors

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress


class CorsProxyServer {

  private val client = OkHttpClient()

  private val log = LoggerFactory.getLogger(CorsProxyServer::class.java)


  fun start(port: Int = 8082) {
    val server = HttpServer.create(InetSocketAddress("0.0.0.0", port), 0)
    server.createContext("/") { httpExchange -> handleRequest(httpExchange) }
    server.start()

    log.info("Started CORS proxy at port $port")
  }


  private fun handleRequest(exchange: HttpExchange) {
    try {
      setCorsHeaders(exchange)

      // Pre-flight request. Reply successfully:
      if (exchange.requestMethod == "OPTIONS") {
        exchange.sendResponseHeaders(200, -1)
        return
      }

      proxyCall(exchange)
    } catch (e: Exception) {
      log.error("Could not proxy call to ${exchange.requestURI}", e)

      exchange.sendResponseHeaders(500, -1)
    }
  }

  private fun proxyCall(exchange: HttpExchange) {
    log.info("Proxying call to ${exchange.requestURI}")

    var url = exchange.requestURI.toString()
    if (url.startsWith("/")) {
      url = url.substring(1)
    }

    // filter out host header as otherwise OkHttp is trying to connect to this header
    val headers = exchange.requestHeaders.mapNotNull { if (it.key.lowercase() == "host") null else it.key to it.value.first() }.toMap()

    // OkHttp throws an exception if for a GET request a request body gets set
    val requestBody = if ("GET" == exchange.requestMethod.uppercase()) null else exchange.requestBody.readAllBytes()?.toRequestBody()

    val request = Request.Builder()
      .url(url)
      .method(exchange.requestMethod, requestBody)
      .headers(headers.toHeaders())
      .build()

    client.newCall(request).execute().use { response ->
      response.headers.forEach { header -> exchange.responseHeaders.add(header.first, header.second) }

      exchange.sendResponseHeaders(response.code, response.body?.contentLength() ?: -1)

      exchange.responseBody.buffered().use { responseBodyStream -> // we need to close exchange.responseBody otherwise response doesn't get send
        response.body?.byteStream()?.buffered()?.copyTo(responseBodyStream)
      }
    }
  }

  private fun setCorsHeaders(exchange: HttpExchange) {
    exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")

    exchange.responseHeaders.add("Access-Control-Allow-Credentials", "true")

    exchange.responseHeaders.add("Access-Control-Allow-Methods", exchange.requestHeaders["access-control-request-method"]?.first() ?: "OPTIONS,HEAD,GET,POST,PUT,PATCH,DELETE")

    exchange.responseHeaders.add("Access-Control-Allow-Headers", exchange.requestHeaders["access-control-request-headers"]?.first() ?: "*")

//    exchange.responseHeaders.add("access-control-expose-headers", exchange.requestHeaders.map { it.key }.joinToString(",")) // TODO: needed?
  }

}