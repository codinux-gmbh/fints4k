package net.codinux.banking.fints.extensions

import kotlinx.datetime.*
import kotlin.js.JsName


val UnixEpochStart: LocalDate = LocalDate.parse("1970-01-01")


fun LocalDate.Companion.todayAtSystemDefaultTimeZone(): LocalDate {
  return nowAt(TimeZone.currentSystemDefault())
}

fun LocalDate.Companion.todayAtEuropeBerlin(): LocalDate {
  return nowAt(TimeZone.europeBerlin)
}

@JsName("nowAtForDate")
fun LocalDate.Companion.nowAt(timeZone: TimeZone): LocalDate {
  return Clock.System.todayIn(timeZone)
}


fun LocalDate.minusDays(days: Int): LocalDate {
  return this.minus(days, DateTimeUnit.DAY)
}