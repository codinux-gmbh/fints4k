package net.codinux.banking.fints.mapper

import kotlinx.datetime.LocalDate

/**
 * Be aware that Java DateFormat is not thread safe!
 */
expect class DateFormatter constructor(pattern: String) {

    fun parseDate(dateString: String): LocalDate?

}