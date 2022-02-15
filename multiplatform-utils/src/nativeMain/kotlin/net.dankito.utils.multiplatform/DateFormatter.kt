package net.dankito.utils.multiplatform

import kotlinx.datetime.*


actual class DateFormatter actual constructor(pattern: String) {

    actual constructor(dateStyle: DateFormatStyle) : this("")

    actual constructor(dateStyle: DateFormatStyle, timeStyle: DateFormatStyle) : this("")


    // TODO: implement for Logger, get current time formatted as string
    actual fun format(date: LocalDateTime): String {
        return "" // is only used in rare cases, don't implement right now
    }

    actual fun parseDate(dateString: String): LocalDate? {
        return null // is only used in rare cases, don't implement right now
    }

    actual fun parse(dateString: String): LocalDateTime? {
        return null // is only used in rare cases, don't implement right now
    }

}