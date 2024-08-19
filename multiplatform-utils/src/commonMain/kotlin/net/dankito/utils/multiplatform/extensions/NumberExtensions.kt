package net.dankito.utils.multiplatform.extensions


fun Int.toStringWithMinDigits(minimumCountDigits: Int, fillerString: Char = '0'): String {
  return this.toString().padStart(minimumCountDigits, fillerString)
}