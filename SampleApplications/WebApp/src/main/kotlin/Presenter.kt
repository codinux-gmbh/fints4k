import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dankito.banking.client.model.parameter.GetAccountDataParameter
import net.dankito.banking.client.model.parameter.TransferMoneyParameter
import net.dankito.banking.client.model.response.GetAccountDataResponse
import net.dankito.banking.client.model.response.TransferMoneyResponse
import net.codinux.banking.fints.FinTsClient
import net.codinux.banking.fints.callback.SimpleFinTsClientCallback
import net.codinux.banking.fints.model.*
import net.codinux.banking.fints.webclient.KtorWebClient
import net.codinux.banking.fints.webclient.ProxyingWebClient
import net.dankito.utils.multiplatform.log.LoggerFactory

open class Presenter {

  companion object {
    private val log = LoggerFactory.getLogger(Presenter::class)
  }


  // to circumvent CORS we have to use a CORS proxy like the SampleApplications.CorsProxy Application.kt or
  // https://github.com/Rob--W/cors-anywhere. Set CORS proxy's URL here
  protected open val fintsClient = FinTsClient(SimpleFinTsClientCallback { challenge -> enterTan(challenge) },
    ProxyingWebClient("http://localhost:8082/", KtorWebClient()))

  open var enterTanCallback: ((TanChallenge) -> Unit)? = null

  open protected fun enterTan(tanChallenge: TanChallenge) {
    enterTanCallback?.invoke(tanChallenge) ?: run { tanChallenge.userDidNotEnterTan() }
  }


  open fun retrieveAccountData(bankCode: String, loginName: String, password: String, retrievedResult: (GetAccountDataResponse) -> Unit) {
    GlobalScope.launch(Dispatchers.Unconfined) {
      val response = fintsClient.getAccountDataAsync(GetAccountDataParameter(bankCode, loginName, password))

      log.info("Retrieved response from ${response.customerAccount?.bankName} for ${response.customerAccount?.customerName}")

      withContext(Dispatchers.Main) {
        retrievedResult(response)
      }
    }
  }

  open fun transferMoney(recipientName: String, recipientAccountIdentifier: String, recipientBankIdentifier: String?, reference: String?,
                    amount: String, instantPayment: Boolean = false, response: (TransferMoneyResponse) -> Unit) {
    GlobalScope.launch(Dispatchers.Unconfined) {
      // TODO: set your credentials here
      val transferMoneyResponse = fintsClient.transferMoneyAsync(TransferMoneyParameter("", "", "", null, recipientName,
        recipientAccountIdentifier, recipientBankIdentifier, Money(Amount(amount), Currency.DefaultCurrencyCode), reference, instantPayment))

      if (transferMoneyResponse.successful) {
        log.info("Successfully transferred $amount to $recipientName")
      } else {
        log.error("Could not transfer $amount to $recipientName: ${transferMoneyResponse.error} ${transferMoneyResponse.errorMessage}")
      }

      withContext(Dispatchers.Main) {
        response(transferMoneyResponse)
      }
    }
  }

}