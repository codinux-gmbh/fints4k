package net.codinux.banking.fints4k.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import net.dankito.banking.client.model.parameter.GetAccountDataParameter
import net.dankito.banking.client.model.response.GetAccountDataResponse
import net.dankito.banking.fints.FinTsClient
import net.dankito.banking.fints.callback.SimpleFinTsClientCallback
import net.dankito.banking.fints.model.TanChallenge
import net.dankito.utils.multiplatform.extensions.millisSinceEpochAtSystemDefaultTimeZone
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.text.DateFormat
import java.util.*

open class Presenter {

    companion object {
        val ValueDateFormat = DateFormat.getDateInstance(DateFormat.SHORT)

        private val log = LoggerFactory.getLogger(Presenter::class.java)
    }

    private val fintsClient = FinTsClient(SimpleFinTsClientCallback { challenge -> enterTan(challenge) })

    open var enterTanCallback: ((TanChallenge) -> Unit)? = null

    open protected fun enterTan(tanChallenge: TanChallenge) {
        enterTanCallback?.invoke(tanChallenge) ?: run { tanChallenge.userDidNotEnterTan() }
    }



    open fun retrieveAccountData(bankCode: String, loginName: String, password: String, retrievedResult: (GetAccountDataResponse) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = fintsClient.getAccountDataAsync(GetAccountDataParameter(bankCode, loginName, password))
            log.info("Retrieved response from ${response.customerAccount?.bankName} for ${response.customerAccount?.customerName}")

            withContext(Dispatchers.Main) {
                retrievedResult(response)
            }
        }
    }


    fun formatDate(date: LocalDate): String {
        try {
            return ValueDateFormat.format(Date(date.millisSinceEpochAtSystemDefaultTimeZone))
        } catch (e: Exception) {
            log.error("Could not format date $date", e)
        }

        return date.toString()
    }

    fun formatAmount(amount: BigDecimal): String {
        return String.format("%.02f", amount)
    }

}