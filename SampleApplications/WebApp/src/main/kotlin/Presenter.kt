import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dankito.banking.fints.FinTsClientDeprecated
import net.dankito.banking.fints.callback.SimpleFinTsClientCallback
import net.dankito.banking.fints.model.AddAccountParameter
import net.dankito.banking.fints.model.TanChallenge
import net.dankito.banking.fints.response.client.AddAccountResponse
import net.dankito.banking.fints.webclient.KtorWebClient
import net.dankito.banking.fints.webclient.ProxyingWebClient
import net.dankito.utils.multiplatform.log.LoggerFactory

open class Presenter {

  companion object {
    private val log = LoggerFactory.getLogger(Presenter::class)
  }


  // to circumvent CORS we have to use a CORS proxy like the SampleApplications.CorsProxy Application.kt or
  // https://github.com/Rob--W/cors-anywhere. Set CORS proxy's URL here
  protected open val fintsClient = FinTsClientDeprecated(SimpleFinTsClientCallback { challenge -> enterTan(challenge) },
    ProxyingWebClient("http://localhost:8082/", KtorWebClient()))

  open var enterTanCallback: ((TanChallenge) -> Unit)? = null

  open protected fun enterTan(tanChallenge: TanChallenge) {
    enterTanCallback?.invoke(tanChallenge) ?: run { tanChallenge.userDidNotEnterTan() }
  }


  open fun retrieveAccountData(bankCode: String, customerId: String, pin: String, finTs3ServerAddress: String, retrievedResult: (AddAccountResponse) -> Unit) {
    GlobalScope.launch(Dispatchers.Unconfined) {
      val response = fintsClient.addAccountAsync(AddAccountParameter(bankCode, customerId, pin, finTs3ServerAddress))

      log.info("Retrieved response from ${response.bank.bankName} for ${response.bank.customerName}")

      withContext(Dispatchers.Main) {
        retrievedResult(response)
      }
    }
  }

}