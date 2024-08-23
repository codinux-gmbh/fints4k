package net.codinux.banking.fints.mapper

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.format.DateTimeFormatter

actual class DateFormatter actual constructor(pattern: String) {

    private val formatter = DateTimeFormatter.ofPattern(pattern)


    actual fun parseDate(dateString: String): LocalDate? {
        return java.time.LocalDate.parse(dateString, formatter)?.toKotlinLocalDate()
    }

}