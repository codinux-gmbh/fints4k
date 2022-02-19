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
                    presenter = Presenter()
                }
            }
        }
    }
}