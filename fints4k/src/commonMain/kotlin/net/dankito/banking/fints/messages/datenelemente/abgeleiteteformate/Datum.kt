package net.dankito.banking.fints.messages.datenelemente.abgeleiteteformate

import kotlinx.datetime.LocalDate
import net.dankito.banking.fints.messages.Existenzstatus
import net.dankito.banking.fints.messages.datenelemente.basisformate.NumerischesDatenelement
import net.dankito.utils.multiplatform.extensions.toStringWithMinDigits
import net.dankito.utils.multiplatform.extensions.toStringWithTwoDigits
import net.dankito.utils.multiplatform.log.LoggerFactory


/**
 * Format: JJJJMMTT gemäß ISO 8601
 *
 * Erlaubt sind alle existenten Datumsangaben.
 */
open class Datum(date: Int?, existenzstatus: Existenzstatus) : NumerischesDatenelement(date, 8, existenzstatus) {

    companion object {
        const val HbciDateFormatString = "yyyyMMdd"


        private val log = LoggerFactory.getLogger(Datum::class)


        fun format(date: LocalDate): String { // create HbciDateFormatString
            return date.year.toStringWithMinDigits(4) + date.monthNumber.toStringWithTwoDigits() + date.dayOfMonth.toStringWithTwoDigits() // TODO: is this correct?
        }

        fun parse(dateString: String): LocalDate {
            // do not use DateFormatter as Java DateFormat is not thread safe, resulting in a lot of curious errors in parallel execution

            if (dateString.length == 8) {
                try {
                    val year = dateString.substring(0, 4)
                    val month = dateString.substring(4, 6)
                    val day = dateString.substring(6, 8)

                    return LocalDate(year.toInt(), month.toInt(), day.toInt())
                } catch (e: Exception) {
                    log.error(e) { "Could not parse date string '$dateString' to HBCI date" }
                }
            }

            throw IllegalArgumentException("Cannot parse '$dateString' to HBCI Date. Only dates in format '$HbciDateFormatString' are allowed in HBCI / FinTS.")
        }
    }


    constructor(date: LocalDate?, existenzstatus: Existenzstatus)
            : this(date?.let { format(it).toInt() }, existenzstatus)

}