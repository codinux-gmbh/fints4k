package net.dankito.banking.fints.messages.datenelemente.abgeleiteteformate

import kotlinx.datetime.LocalTime
import net.codinux.log.logger
import net.dankito.banking.fints.extensions.toStringWithMinDigits
import net.dankito.banking.fints.messages.Existenzstatus
import net.dankito.banking.fints.messages.datenelemente.basisformate.ZiffernDatenelement


/**
 * Format: hhmmss gemäß ISO 8601
 *
 * Gültige Uhrzeit. Es ist immer Ortszeit des sendenden Systems einzustellen.
 * Unterschiedliche Zeitzonen werden nicht unterstützt
 */
open class Uhrzeit(time: Int?, existenzstatus: Existenzstatus) : ZiffernDatenelement(time, 6, existenzstatus) {

    companion object {
        const val HbciTimeFormatString = "HHmmss"


        private val log by logger()


        fun format(time: LocalTime): String { // parse to HbciTimeFormatString
            return time.hour.toStringWithMinDigits(2) + time.minute.toStringWithMinDigits(2) + time.second.toStringWithMinDigits(2) // TODO: is this correct?
        }

        fun parse(timeString: String): LocalTime {
            // do not use DateFormatter as Java DateFormat is not thread safe, resulting in a lot of curious errors in parallel execution

            if (timeString.length == 6) {
                try {
                    val hour = timeString.substring(0, 2)
                    val minute = timeString.substring(2, 4)
                    val second = timeString.substring(4, 6)

                    return LocalTime(hour.toInt(), minute.toInt(), second.toInt())
                } catch (e: Exception) {
                    log.error(e) { "Could not parse time string '$timeString' to HBCI time" }
                }
            }

            throw IllegalArgumentException("Cannot parse '$timeString' to HBCI Time. Only times in format '${HbciTimeFormatString}' are allowed in HBCI / FinTS.")
        }
    }

}