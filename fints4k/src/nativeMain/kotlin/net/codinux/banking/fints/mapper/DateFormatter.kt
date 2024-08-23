package net.codinux.banking.fints.mapper

import kotlinx.datetime.LocalDate

actual class DateFormatter actual constructor(pattern: String) {

    actual fun parseDate(dateString: String): LocalDate? {
        return null // is only used in rare cases, don't implement right now
    }

}