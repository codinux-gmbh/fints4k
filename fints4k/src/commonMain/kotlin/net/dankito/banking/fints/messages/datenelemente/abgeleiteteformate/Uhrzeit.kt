package net.dankito.banking.fints.messages.datenelemente.abgeleiteteformate

import net.dankito.banking.fints.messages.Existenzstatus
import net.dankito.banking.fints.messages.datenelemente.basisformate.ZiffernDatenelement
import net.dankito.utils.multiplatform.Date
import net.dankito.utils.multiplatform.DateFormatter
import net.dankito.utils.multiplatform.log.LoggerFactory


/**
 * Format: hhmmss gemäß ISO 8601
 *
 * Gültige Uhrzeit. Es ist immer Ortszeit des sendenden Systems einzustellen.
 * Unterschiedliche Zeitzonen werden nicht unterstützt
 */
open class Uhrzeit(time: Int?, existenzstatus: Existenzstatus) : ZiffernDatenelement(time, 6, existenzstatus) {

    companion object {
        const val HbciTimeFormatString = "HHmmss"

        val HbciTimeFormat = DateFormatter(HbciTimeFormatString)


        private val log = LoggerFactory.getLogger(Uhrzeit::class)


        fun format(time: Date): String {
            return HbciTimeFormat.format(time) // TODO: is this correct?
        }

        fun parse(timeString: String): Date {
            // do not use DateFormatter as Java DateFormat is not thread safe, resulting in a lot of curious errors in parallel execution

            if (timeString.length == 6) {
                try {
                    val hour = timeString.substring(0, 2)
                    val minute = timeString.substring(2, 4)
                    val second = timeString.substring(4, 6)

                    return Date(0, 0, 0, hour.toInt(), minute.toInt(), second.toInt())
                } catch (e: Exception) {
                    log.error(e) { "Could not parse time string '$timeString' to HBCI time" }
                }
            }

            throw IllegalArgumentException("Cannot parse '$timeString' to HBCI Time. Only times in format '${Uhrzeit.HbciTimeFormatString}' are allowed in HBCI / FinTS.")
        }
    }


    constructor(time: Date?, existenzstatus: Existenzstatus)
            : this(time?.let { format(time).toInt() }, existenzstatus)

}