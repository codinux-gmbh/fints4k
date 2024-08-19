package net.dankito.utils.multiplatform

import kotlinx.datetime.*
import net.dankito.utils.multiplatform.extensions.toLocalDateTime
import platform.Foundation.*



actual class DateFormatter actual constructor(val pattern: String): NSDateFormatter() {


    actual fun parseDate(dateString: String): LocalDate? {
        this.dateFromString(dateString)?.let { nsDate ->
            return nsDate.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault())?.date
        }

        return null
    }

}