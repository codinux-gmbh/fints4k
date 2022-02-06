package net.dankito.utils.multiplatform

import platform.CoreFoundation.*
import platform.Foundation.*


fun NSTimeInterval.toMillis(): Long {
    return this.toLong() * 1000
}

fun NSDate?.toDate(): Date? {
    return this?.toDate()
}

fun NSDate.toDate(): Date {
    return Date(this)
}


actual class Date(val date: NSDate) { // cannot subclass NSDate as it's a class cluster

    actual companion object {

        val DiffBetweenEpochTimeAndReferenceDate = (NSDate.timeIntervalSinceReferenceDate - NSTimeIntervalSince1970).toMillis()

        fun from(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0): NSDate {
            val dateComponents = NSDateComponents()

            dateComponents.year = year.toLong()
            dateComponents.month = month.toLong()
            dateComponents.day = day.toLong()

            dateComponents.hour = hour.toLong()
            dateComponents.minute = minute.toLong()
            dateComponents.second = second.toLong()

            val calendar = NSCalendar.currentCalendar
            val todayInUtc = calendar.dateFromComponents(dateComponents) !!

            return calendar.dateByAddingUnit(NSCalendarUnitSecond, NSTimeZone.defaultTimeZone.secondsFromGMT, todayInUtc, 0)!!
        }


        actual val today: Date
            get() {
                val now = Date()

                return Date(from(now.year(), now.monthInt(), now.day()))
            }

        actual val nanoSecondsSinceEpoch: Long
            get() = CFAbsoluteTimeGetCurrent().toLong() * 1000

    }


    actual constructor(millisSinceEpoch: Long) : this(NSDate(timeIntervalSinceReferenceDate = ((millisSinceEpoch / 1000).toDouble() - NSTimeIntervalSince1970)))

    actual constructor() : this(NSDate())

    actual constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int) : this(from(year, month, day, hour, minute, second))

    actual constructor(year: Int, month: Month, day: Int, hour: Int, minute: Int, second: Int) : this(year, month.month, day, hour, minute, second)


    actual val millisSinceEpoch: Long
        get() = date.timeIntervalSince1970.toMillis()


    actual fun year(): Int {
        val components = NSCalendar.currentCalendar.components(NSCalendarUnitYear, date)
        return components.year.toInt()
    }

    actual fun month(): Month {
        return Month.fromInt(monthInt())
    }

    actual fun monthInt(): Int {
        val components = NSCalendar.currentCalendar.components(NSCalendarUnitMonth, date)
        return components.month.toInt()
    }

    actual fun day(): Int {
        val components = NSCalendar.currentCalendar.components(NSCalendarUnitDay, date)
        return components.day.toInt()
    }



    actual fun compareTo(other: Date): Int {
        return date.compare(other.date).toCompareToResult()
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Date) return false

        if (date != other.date) return false

        return true
    }

    override fun hashCode(): Int {
        return date.hashCode()
    }

    override fun toString(): String {
        return date.description ?: "Date(date=$date)"
    }


}