package net.dankito.utils.multiplatform

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Be aware that Java DateFormat is not thread safe!
 */
expect class DateFormatter constructor(pattern: String) {

    constructor(dateStyle: DateFormatStyle)

    constructor(dateStyle: DateFormatStyle, timeStyle: DateFormatStyle)


    fun format(date: LocalDateTime): String

    // cannot be named format() due to JavaScript name conflicts
    fun formatDate(date: LocalDate): String

    fun parseDate(dateString: String): LocalDate?

    fun parse(dateString: String): LocalDateTime?

}