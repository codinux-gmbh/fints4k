package net.dankito.utils.multiplatform.extensions

import kotlinx.datetime.*
import net.dankito.utils.multiplatform.DateFormatter


val LocalDate.Companion.atUnixEpochStart: LocalDate
  get() = fromEpochMillisecondsAtUtc(0)

fun LocalDate.Companion.fromEpochMillisecondsAtUtc(epochMilliseconds: Long): LocalDate {
  return fromEpochMilliseconds(epochMilliseconds, TimeZone.UTC)
}

fun LocalDate.Companion.fromEpochMillisecondsAtSystemDefaultTimeZone(epochMilliseconds: Long): LocalDate {
  return fromEpochMilliseconds(epochMilliseconds, TimeZone.currentSystemDefault())
}

fun LocalDate.Companion.fromEpochMillisecondsAtEuropeBerlin(epochMilliseconds: Long): LocalDate {
  return fromEpochMilliseconds(epochMilliseconds, TimeZone.europeBerlin)
}

fun LocalDate.Companion.fromEpochMilliseconds(epochMilliseconds: Long, timeZone: TimeZone): LocalDate {
  return Instant.fromEpochMilliseconds(epochMilliseconds).toLocalDateTime(timeZone).date
}


fun LocalDate.Companion.todayAtUtc(): LocalDate {
  return nowAt(TimeZone.UTC)
}

fun LocalDate.Companion.todayAtSystemDefaultTimeZone(): LocalDate {
  return nowAt(TimeZone.currentSystemDefault())
}

fun LocalDate.Companion.todayAtEuropeBerlin(): LocalDate {
  return nowAt(TimeZone.europeBerlin)
}

fun LocalDate.Companion.nowAt(timeZone: String): LocalDate {
  return nowAt(TimeZone.of(timeZone))
}

fun LocalDate.Companion.nowAt(timeZone: TimeZone): LocalDate {
  return Clock.System.todayAt(timeZone)
}


val LocalDate.millisSinceEpochAtUtc: Long
  get() = this.toEpochMillisecondsAt(TimeZone.UTC)

val LocalDate.millisSinceEpochAtSystemDefaultTimeZone: Long
  get() = this.toEpochMillisecondsAt(TimeZone.currentSystemDefault())

val LocalDate.millisSinceEpochAtEuropeBerlin: Long
  get() = this.toEpochMillisecondsAt(TimeZone.europeBerlin)

fun LocalDate.toEpochMillisecondsAt(timeZone: TimeZone): Long {
  return this.atTime(0, 0).toInstant(timeZone).toEpochMilliseconds()
}


fun LocalDate.addDays(days: Int): LocalDate {
  return this.plus(days, DateTimeUnit.DAY)
}

fun LocalDate.minusDays(days: Int): LocalDate {
  return this.minus(days, DateTimeUnit.DAY)
}


fun LocalDate.format(formatter: DateFormatter): String {
  return this.atTime(0, 0).format(formatter)
}

fun LocalDate.format(pattern: String): String {
  return this.format(DateFormatter(pattern))
}