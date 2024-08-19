package net.dankito.banking.fints.mapper

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

actual class DateFormatter actual constructor(val pattern: String): NSDateFormatter() {


    actual fun parseDate(dateString: String): LocalDate? {
        this.dateFromString(dateString)?.let { nsDate ->
            return nsDate.toKotlinInstant().toLocalDateTime(TimeZone.Companion.currentSystemDefault())?.date
        }

        return null
    }

}