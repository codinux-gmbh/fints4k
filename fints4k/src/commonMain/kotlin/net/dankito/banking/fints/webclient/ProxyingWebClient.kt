package net.dankito.banking.fints.webclient


/**
 * In browsers we cannot request bank servers directly due to CORS. So this 'WebClient' prepends each server url with CORS proxy's url and delegates then the
 * call to a real IWebClient implementation.
 */
class ProxyingWebClient(proxyUrl: String, private val delegate: IWebClient) : IWebClient {

  private val proxyUrl = if (proxyUrl.endsWith("/")) proxyUrl else proxyUrl + "/"


  override fun post(url: String, body: String, contentType: String, userAgent: String, callback: (WebClientResponse) -> Unit) {
    delegate.post(proxyUrl + url, body, contentType, userAgent, callback)
  }

}