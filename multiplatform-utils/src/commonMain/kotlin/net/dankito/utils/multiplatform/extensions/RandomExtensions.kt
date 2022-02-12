package net.dankito.utils.multiplatform.extensions

import kotlinx.datetime.Clock
import kotlin.random.Random


fun randomWithSeed(): Random = Random(randomSeed())

fun randomSeed(): Long {
  return Clock.System.now().nanosecondsOfSecond.toLong() + Clock.System.now().toEpochMilliseconds()
}