package net.dankito.utils.multiplatform

/**
 * Be aware that Java DateFormat is not thread safe!
 */
expect class DateFormatter constructor(pattern: String) {

    constructor(dateStyle: DateFormatStyle)

    constructor(dateStyle: DateFormatStyle, timeStyle: DateFormatStyle)


    fun format(date: Date): String

    fun parse(dateString: String): Date?

}