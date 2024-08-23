package net.codinux.banking.fints.extensions

import kotlinx.datetime.*



fun LocalDateTime.Companion.nowAtUtc(): LocalDateTime {
  return nowAt(TimeZone.UTC)
}

fun LocalDateTime.Companion.nowAtEuropeBerlin(): LocalDateTime {
  return nowAt(TimeZone.europeBerlin)
}

fun LocalDateTime.Companion.nowAt(timeZone: TimeZone): LocalDateTime {
  return Clock.System.now().toLocalDateTime(timeZone)
}