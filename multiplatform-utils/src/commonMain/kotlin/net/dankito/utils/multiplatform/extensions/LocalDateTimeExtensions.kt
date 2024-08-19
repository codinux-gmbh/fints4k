package net.dankito.utils.multiplatform.extensions

import kotlinx.datetime.*
import kotlin.js.JsName


fun LocalDateTime.Companion.of(hour: Int, minute: Int, second: Int = 0, nanosecond: Int = 0): LocalDateTime {
  return LocalDateTime(0, 1, 1, hour, minute, second, nanosecond) // minimum values for month and day are 1
}

fun LocalDateTime.Companion.fromEpochMillisecondsAtUtc(epochMilliseconds: Long): LocalDateTime {
  return fromEpochMilliseconds(epochMilliseconds, TimeZone.UTC)
}

fun LocalDateTime.Companion.fromEpochMillisecondsAtSystemDefaultTimeZone(epochMilliseconds: Long): LocalDateTime {
  return fromEpochMilliseconds(epochMilliseconds, TimeZone.currentSystemDefault())
}

fun LocalDateTime.Companion.fromEpochMillisecondsAtEuropeBerlin(epochMilliseconds: Long): LocalDateTime {
  return fromEpochMilliseconds(epochMilliseconds, TimeZone.europeBerlin)
}

fun LocalDateTime.Companion.fromEpochMilliseconds(epochMilliseconds: Long, timeZone: TimeZone): LocalDateTime {
  return Instant.fromEpochMilliseconds(epochMilliseconds).toLocalDateTime(timeZone)
}


fun LocalDateTime.Companion.nowAtUtc(): LocalDateTime {
  return nowAt(TimeZone.UTC)
}

fun LocalDateTime.Companion.nowAtSystemDefaultTimeZone(): LocalDateTime {
  return nowAt(TimeZone.currentSystemDefault())
}

fun LocalDateTime.Companion.nowAtEuropeBerlin(): LocalDateTime {
  return nowAt(TimeZone.europeBerlin)
}

@JsName("nowAtForTimeZoneString")
fun LocalDateTime.Companion.nowAt(timeZone: String): LocalDateTime {
  return nowAt(TimeZone.of(timeZone))
}

fun LocalDateTime.Companion.nowAt(timeZone: TimeZone): LocalDateTime {
  return Clock.System.now().toLocalDateTime(timeZone)
}


//val LocalDateTime.millisSinceEpochAtUtc: Long
//  get() = this.toEpochMillisecondsAt(TimeZone.UTC)
//
//val LocalDateTime.millisSinceEpochAtSystemDefaultTimeZone: Long
//  get() = this.toEpochMillisecondsAt(TimeZone.currentSystemDefault())
//
//val LocalDateTime.millisSinceEpochAtEuropeBerlin: Long
//  get() = this.toEpochMillisecondsAt(TimeZone.europeBerlin)

fun LocalDateTime.toEpochMillisecondsAt(timeZone: TimeZone): Long {
  return this.toInstant(timeZone).toEpochMilliseconds()
}