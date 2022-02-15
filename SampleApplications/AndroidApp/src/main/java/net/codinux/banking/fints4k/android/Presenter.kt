package net.codinux.banking.fints4k.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import net.dankito.banking.fints.FinTsClientDeprecated
import net.dankito.banking.fints.callback.SimpleFinTsClientCallback
import net.dankito.banking.fints.model.AddAccountParameter
import net.dankito.banking.fints.response.client.AddAccountResponse
import net.dankito.utils.multiplatform.extensions.millisSinceEpochAtSystemDefaultTimeZone
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.text.DateFormat
import java.util.*

class Presenter {

    companion object {
        val ValueDateFormat = DateFormat.getDateInstance(DateFormat.SHORT)

        private val log = LoggerFactory.getLogger(Presenter::class.java)
    }

    private val fintsClient = FinTsClientDeprecated(SimpleFinTsClientCallback())



    fun retrieveAccountData(bankCode: String, customerId: String, pin: String, finTs3ServerAddress: String, retrievedResult: (AddAccountResponse) -> Unit) {
        fintsClient.addAccountAsync(AddAccountParameter(bankCode, customerId, pin, finTs3ServerAddress)) { response ->
            log.info("Retrieved response from ${response.bank.bankName} for ${response.bank.customerName}")

            GlobalScope.launch(Dispatchers.Main) {
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