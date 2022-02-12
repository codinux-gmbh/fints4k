package net.dankito.utils.multiplatform.extensions


fun Int.toStringWithTwoDigits(): String {
  return toStringWithMinDigits(2)
}

fun Int.toStringWithMinDigits(minimumCountDigits: Int): String {
  val countDigitsToAdd = minimumCountDigits - this.numberOfDigits
  val prefix = if (countDigitsToAdd > 0) "0".repeat(countDigitsToAdd) else ""

  return prefix + this.toString()
}

val Int.numberOfDigits: Int
  get() {
    var number = this
    var count = 0

    while (number != 0) {
      number /= 10
      ++count
    }

    return count
  }