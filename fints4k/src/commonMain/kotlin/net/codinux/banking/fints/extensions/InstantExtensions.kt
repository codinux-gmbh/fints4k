package net.codinux.banking.fints.extensions

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus


// should actually be named `now()`, but that name is already shadowed by deprecated Instant.Companion.now() method
fun Instant.Companion.nowExt(): Instant = Clock.System.now()

fun Instant.plusMinutes(minutes: Int) = this.plus(minutes, DateTimeUnit.MINUTE)