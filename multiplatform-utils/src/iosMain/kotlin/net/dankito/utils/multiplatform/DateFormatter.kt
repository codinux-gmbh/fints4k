package net.dankito.utils.multiplatform

import kotlinx.datetime.*
import net.dankito.utils.multiplatform.extensions.toLocalDateTime
import net.dankito.utils.multiplatform.extensions.toNSDate
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


    actual fun format(date: LocalDate): String {
        return format(date.toLocalDateTime())
    }

    actual fun format(date: LocalDateTime): String {
        val nsDate = date.toNSDate()

        return this.stringFromDate(nsDate)
    }

    actual fun parseDate(dateString: String): LocalDate? {
        return parse(dateString)?.date
    }

    actual fun parse(dateString: String): LocalDateTime? {
        this.dateFromString(dateString)?.let { nsDate ->
            return nsDate.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault())
        }

        return null
    }

}