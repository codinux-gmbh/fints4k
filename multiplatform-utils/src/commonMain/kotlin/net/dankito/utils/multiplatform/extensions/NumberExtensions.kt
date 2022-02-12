package net.dankito.utils.multiplatform.extensions

import net.dankito.utils.multiplatform.StringHelper


fun Int.toStringWithTwoDigits(): String {
  return toStringWithMinDigits(2)
}

fun Int.toStringWithMinDigits(countDigits: Int): String {
  return format("%0${countDigits}d")
}

fun Int.format(format: String): String {
  return StringHelper.format(format, this)
}