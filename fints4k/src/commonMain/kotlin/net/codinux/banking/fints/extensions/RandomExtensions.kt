package net.codinux.banking.fints.extensions

import kotlinx.datetime.Instant
import kotlin.random.Random


fun randomWithSeed(): Random = Random(randomSeed())

fun randomSeed(): Long {
  return Instant.nowExt().nanosecondsOfSecond.toLong() + Instant.nowExt().toEpochMilliseconds()
}