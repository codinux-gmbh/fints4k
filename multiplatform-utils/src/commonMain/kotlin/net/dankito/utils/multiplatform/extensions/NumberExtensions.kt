package net.dankito.utils.multiplatform.extensions


fun Int.toStringWithTwoDigits(): String {
  return toStringWithMinDigits(2)
}

fun Int.toStringWithMinDigits(minimumCountDigits: Int): String {
  return toStringWithMinDigits(minimumCountDigits, "0")
}

fun Int.toStringWithMinDigits(minimumCountDigits: Int, fillerString: String): String {
  return this.toString().ensureMinStringLength(minimumCountDigits, fillerString)
}

fun String.ensureMinStringLength(minimumStringLength: Int, fillerString: String): String {
  val countDigitsToAdd = minimumStringLength - this.length
  val prefix = if (countDigitsToAdd > 0) fillerString.repeat(countDigitsToAdd) else ""

  return prefix + this
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