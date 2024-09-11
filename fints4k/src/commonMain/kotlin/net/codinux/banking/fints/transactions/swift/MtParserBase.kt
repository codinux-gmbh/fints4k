package net.codinux.banking.fints.transactions.swift

import kotlinx.datetime.*
import net.codinux.banking.fints.extensions.EuropeBerlin
import net.codinux.banking.fints.log.IMessageLogAppender
import net.codinux.banking.fints.model.Amount
import net.codinux.banking.fints.transactions.mt940.Mt94xParserBase
import net.codinux.banking.fints.transactions.swift.model.SwiftMessageBlock
import net.codinux.log.logger

open class MtParserBase(
    open var logAppender: IMessageLogAppender? = null
) {

    protected val log by logger()


    fun parseMtString(mt: String, rememberOrderOfFields: Boolean = false): List<SwiftMessageBlock> {
        val lines = mt.lines().filterNot { it.isBlank() }

        return parseMtStringLines(lines, rememberOrderOfFields)
    }

    protected open fun parseMtStringLines(lines: List<String>, rememberOrderOfFields: Boolean = false): List<SwiftMessageBlock> {
        val messageBlocks = mutableListOf<SwiftMessageBlock>()
        var currentBlock = SwiftMessageBlock()

        var fieldCode = ""
        val fieldValueLines = mutableListOf<String>()

        lines.forEach { line ->
            // end of block
            if (line.trim() == "-") {
                if (fieldCode.isNotBlank()) {
                    currentBlock.addField(fieldCode, fieldValueLines, rememberOrderOfFields)
                }
                messageBlocks.add(currentBlock)

                currentBlock = SwiftMessageBlock()
                fieldCode = ""
                fieldValueLines.clear() // actually not necessary
            }
            // start of a new field
            else if (line.length > 5 && line[0] == ':' && line[1].isDigit() && line[2].isDigit() && (line[3] == ':' || line[3].isLetter() && line[4] == ':')) {
                if (fieldCode.isNotBlank()) {
                    currentBlock.addField(fieldCode, fieldValueLines, rememberOrderOfFields)
                }

                val fieldCodeContainsLetter = line[3].isLetter()
                fieldCode = line.substring(1, if (fieldCodeContainsLetter) 4 else 3)
                fieldValueLines.clear()
                fieldValueLines.add(if (fieldCodeContainsLetter) line.substring(5) else line.substring(4))
            }
            // a line that belongs to previous field value
            else {
                fieldValueLines.add(line)
            }
        }

        if (fieldCode.isNotBlank()) {
            currentBlock.addField(fieldCode, fieldValueLines, rememberOrderOfFields)
        }
        if (currentBlock.hasFields) {
            messageBlocks.add(currentBlock)
        }

        return messageBlocks
    }


    open fun parse4DigitYearDate(dateString: String): LocalDate {
        val year = dateString.substring(0, 4).toInt()
        val month = dateString.substring(4, 6).toInt()
        val day = dateString.substring(6, 8).toInt()

        return LocalDate(year , month, fixDay(year, month, day))
    }

    open fun parseDate(dateString: String): LocalDate {
        try {
            var year = dateString.substring(0, 2).toInt()
            val month = dateString.substring(2, 4).toInt()
            val day = dateString.substring(4, 6).toInt()

            /**
             * Bei 6-stelligen Datumsangaben (d.h. JJMMTT) wird gemäß SWIFT zwischen dem 20. und 21.
             * Jahrhundert wie folgt unterschieden:
             * - Ist das Jahr (d.h. JJ) größer als 79, bezieht sich das Datum auf das 20. Jahrhundert. Ist
             * das Jahr 79 oder kleiner, bezieht sich das Datum auf das 21. Jahrhundert.
             * - Ist JJ > 79:JJMMTT = 19JJMMTT
             * - sonst: JJMMTT = 20JJMMTT
             * - Damit reicht die Spanne des sechsstelligen Datums von 1980 bis 2079.
             */
            if (year > 79) {
                year += 1900
            } else {
                year += 2000
            }

            return LocalDate(year , month, fixDay(year, month, day))
        } catch (e: Throwable) {
            logError("Could not parse dateString '$dateString'", e)
            throw e
        }
    }

    private fun fixDay(year: Int, month: Int, day: Int): Int {
        // ah, here we go, banks (in Germany) calculate with 30 days each month, so yes, it can happen that dates
        // like 30th of February or 29th of February in non-leap years occur, see:
        // https://de.m.wikipedia.org/wiki/30._Februar#30._Februar_in_der_Zinsberechnung
        if (month == 2 && (day > 29 || (day > 28 && year % 4 != 0))) { // fix that for banks each month has 30 days
            return 28
        }

        return day
    }

    open fun parseTime(timeString: String): LocalTime {
        val hour = timeString.substring(0, 2).toInt()
        val minute = timeString.substring(2, 4).toInt()

        return LocalTime(hour, minute)
    }

    open fun parseDateTime(dateTimeString: String): Instant {
        val date = parseDate(dateTimeString.substring(0, 6))

        val time = parseTime(dateTimeString.substring(6, 10))

        val dateTime = LocalDateTime(date, time)

        return if (dateTimeString.length == 15) { // actually mandatory, but by far not always stated: the time zone
            val plus = dateTimeString[10] == '+'
            val timeDifference = parseTime(dateTimeString.substring(11))

            dateTime.toInstant(UtcOffset(if (plus) timeDifference.hour else timeDifference.hour * -1, timeDifference.minute))
        } else { // we then assume the server states the DateTime in FinTS's default time zone, Europe/Berlin
            dateTime.toInstant(TimeZone.EuropeBerlin)
        }
    }

    protected open fun parseAmount(amountString: String): Amount {
        return Amount(amountString)
    }


    protected open fun logError(message: String, e: Throwable?) {
        logAppender?.logError(Mt94xParserBase::class, message, e)
            ?: run {
                log.error(e) { message }
            }
    }

}