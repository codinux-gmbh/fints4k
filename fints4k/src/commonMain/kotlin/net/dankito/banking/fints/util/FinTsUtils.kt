package net.dankito.banking.fints.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import net.dankito.banking.fints.extensions.nowAtEuropeBerlin
import net.dankito.banking.fints.extensions.todayAtEuropeBerlin
import net.dankito.banking.fints.log.MessageLogCollector
import net.dankito.banking.fints.messages.datenelemente.abgeleiteteformate.Datum
import net.dankito.banking.fints.messages.datenelemente.abgeleiteteformate.Uhrzeit


open class FinTsUtils {

    companion object {
        private val NewLine = MessageLogCollector.NewLine

        private val BreakableSegmentSeparatorsRegex = Regex("""'([A-Z])""")
    }


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
        return formatTime(LocalDateTime.nowAtEuropeBerlin().time)
    }

    open fun formatTime(time: LocalTime): String {
        return Uhrzeit.format(time)
    }

    open fun formatTimeNowAsInt(): Int {
        return convertToInt(formatTimeNow())
    }

    open fun formatTimeAsInt(time: LocalTime): Int {
        return convertToInt(formatTime(time))
    }


    open fun prettyPrintFinTsMessage(finTsMessage: String): String {
        return finTsMessage
            .replace(BreakableSegmentSeparatorsRegex, "'$NewLine$1")
            .replace("@HNSHK:", "@${NewLine}HNSHK:")
    }


    protected open fun convertToInt(string: String): Int {
        return string.toInt()
    }

}