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

    fun parseDate(dateString: String): LocalDate?

    fun parse(dateString: String): LocalDateTime?

}