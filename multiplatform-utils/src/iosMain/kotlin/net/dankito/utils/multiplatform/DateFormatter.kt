package net.dankito.utils.multiplatform

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import platform.Foundation.*


fun DateFormatStyle.convert(): ULong {
    return when (this) {
        DateFormatStyle.Short -> NSDateFormatterShortStyle
        DateFormatStyle.Medium -> NSDateFormatterMediumStyle
        DateFormatStyle.Long -> NSDateFormatterLongStyle
        DateFormatStyle.Full -> NSDateFormatterFullStyle
    }
}



actual class DateFormatter actual constructor(val pattern: String): NSDateFormatter() {

    actual constructor(dateStyle: DateFormatStyle) : this(NSDateFormatter().apply {
        this.dateStyle = dateStyle.convert()
    }.dateFormat) // TODO: does this work?

    actual constructor(dateStyle: DateFormatStyle, timeStyle: DateFormatStyle) : this(NSDateFormatter().apply {
        this.dateStyle = dateStyle.convert()
        this.timeStyle = timeStyle.convert()
    }.dateFormat) // TODO: does this work?


    init {
        this.dateFormat = pattern

        this.timeZone = NSTimeZone.localTimeZone // TODO: needed?
    }


    actual fun format(date: LocalDateTime): String {
        return stringFromDate(date) // TODO: convert to NSDate when back on Mac
    }

    actual fun parseDate(dateString: String): LocalDate? {
        super.dateFromString(dateString)?.let { nsDate ->
            return LocalDate(nsDate) // TODO: convert to NSDate when back on Mac
        }

        return null
    }

    actual fun parse(dateString: String): LocalDateTime? {
        super.dateFromString(dateString)?.let { nsDate ->
            return LocalDateTime(nsDate) // TODO: convert to NSDate when back on Mac
        }

        return null
    }

}