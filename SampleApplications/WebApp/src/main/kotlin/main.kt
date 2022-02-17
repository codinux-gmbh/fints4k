import kotlinx.browser.document
import kotlinx.browser.window
import net.dankito.banking.fints.FinTsClientDeprecated
import net.dankito.banking.fints.callback.SimpleFinTsClientCallback
import net.dankito.banking.fints.webclient.KtorWebClient
import net.dankito.banking.fints.webclient.ProxyingWebClient
import react.dom.render

fun main() {
    window.onload = {
        render(document.getElementById("root")!!) {
            child(AccountTransactionsView::class) {
                attrs {
                    // to circumvent CORS we have to use a CORS proxy like the SampleApplications.CorsProxy Application.kt or
                    // https://github.com/Rob--W/cors-anywhere. Set CORS proxy's URL here
                    client = FinTsClientDeprecated(SimpleFinTsClientCallback(), ProxyingWebClient("http://localhost:8082/", KtorWebClient()))
                }
            }
        }
    }
}