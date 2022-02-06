package net.dankito.utils.multiplatform


fun Date.format(format: DateFormatter): String {
    return format.format(this)
}

fun Date.format(pattern: String): String {
    return this.format(DateFormatter(pattern))
}


expect class Date(millisSinceEpoch: Long) {

    companion object {

        val today: Date

        val nanoSecondsSinceEpoch: Long

    }


    constructor()

    constructor(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0)

    constructor(year: Int, month: Month, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0)


    val millisSinceEpoch: Long

    fun year(): Int

    fun month(): Month

    fun monthInt(): Int

    fun day(): Int


    fun compareTo(other: Date): Int

}