package net.dankito.utils.multiplatform

import kotlinx.datetime.*
import net.dankito.utils.multiplatform.extensions.toLocalDateTime
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter


fun DateFormatStyle.convert(): Int {
    return when (this) {
        DateFormatStyle.Short -> DateFormat.SHORT
        DateFormatStyle.Medium -> DateFormat.MEDIUM
        DateFormatStyle.Long -> DateFormat.LONG
        DateFormatStyle.Full -> DateFormat.FULL
    }
}


actual class DateFormatter actual constructor(pattern: String) {

    private val formatter = DateTimeFormatter.ofPattern(pattern)


    actual constructor(dateStyle: DateFormatStyle)
            : this((DateFormat.getDateInstance(dateStyle.convert()) as? SimpleDateFormat)?.toPattern() ?: "")

    actual constructor(dateStyle: DateFormatStyle, timeStyle: DateFormatStyle)
            : this((DateFormat.getDateTimeInstance(dateStyle.convert(), timeStyle.convert()) as? SimpleDateFormat)?.toPattern() ?: "")


    actual fun format(date: LocalDate): String {
        return format(date.toLocalDateTime())
    }

    actual fun format(date: LocalDateTime): String {
        return formatter.format(date.toJavaLocalDateTime())
    }

    actual fun parseDate(dateString: String): LocalDate? {
        return java.time.LocalDate.parse(dateString, formatter)?.toKotlinLocalDate()
    }

    actual fun parse(dateString: String): LocalDateTime? {
        return java.time.LocalDateTime.parse(dateString, formatter)?.toKotlinLocalDateTime()
    }

}