package net.dankito.banking.fints.messages.datenelemente.abgeleiteteformate

import net.dankito.banking.fints.messages.Existenzstatus
import net.dankito.banking.fints.messages.datenelemente.basisformate.NumerischesDatenelement
import net.dankito.utils.multiplatform.Date
import net.dankito.utils.multiplatform.DateFormatter
import net.dankito.utils.multiplatform.log.LoggerFactory


/**
 * Format: JJJJMMTT gemäß ISO 8601
 *
 * Erlaubt sind alle existenten Datumsangaben.
 */
open class Datum(date: Int?, existenzstatus: Existenzstatus) : NumerischesDatenelement(date, 8, existenzstatus) {

    companion object {
        const val HbciDateFormatString = "yyyyMMdd"

        val HbciDateFormat = DateFormatter(HbciDateFormatString)


        private val log = LoggerFactory.getLogger(Datum::class)


        fun format(date: Date): String {
            return HbciDateFormat.format(date) // TODO: is this correct?
        }

        fun parse(dateString: String): Date {
            // do not use DateFormatter as Java DateFormat is not thread safe, resulting in a lot of curious errors in parallel execution

            if (dateString.length == 8) {
                try {
                    val year = dateString.substring(0, 4)
                    val month = dateString.substring(4, 6)
                    val day = dateString.substring(6, 8)

                    return Date(year.toInt(), month.toInt(), day.toInt())
                } catch (e: Exception) {
                    log.error(e) { "Could not parse date string '$dateString' to HBCI date" }
                }
            }

            throw IllegalArgumentException("Cannot parse '$dateString' to HBCI Date. Only dates in format '$HbciDateFormatString' are allowed in HBCI / FinTS.")
        }
    }


    constructor(date: Date?, existenzstatus: Existenzstatus)
            : this(date?.let { format(it).toInt() }, existenzstatus)

}