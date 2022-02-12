package net.dankito.banking.fints.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import net.dankito.utils.multiplatform.extensions.nowAtEuropeBerlin
import net.dankito.utils.multiplatform.extensions.todayAtEuropeBerlin
import net.dankito.banking.fints.messages.datenelemente.abgeleiteteformate.Datum
import net.dankito.banking.fints.messages.datenelemente.abgeleiteteformate.Uhrzeit


open class FinTsUtils {


    open fun formatDateToday(): String {
        return formatDate(LocalDate.todayAtEuropeBerlin())
    }

    open fun formatDate(date: LocalDate): String {
        return Datum.format(date)
    }

    open fun formatDateTodayAsInt(): Int {
        return convertToInt(formatDateToday())
    }

    open fun formatDateAsInt(date: LocalDate): Int {
        return convertToInt(formatDate(date))
    }


    open fun formatTimeNow(): String {
        return formatTime(LocalDateTime.nowAtEuropeBerlin())
    }

    open fun formatTime(time: LocalDateTime): String {
        return Uhrzeit.format(time)
    }

    open fun formatTimeNowAsInt(): Int {
        return convertToInt(formatTimeNow())
    }

    open fun formatTimeAsInt(time: LocalDateTime): Int {
        return convertToInt(formatTime(time))
    }


    protected open fun convertToInt(string: String): Int {
        return string.toInt()
    }

}