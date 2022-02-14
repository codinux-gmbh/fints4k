package net.dankito.utils.multiplatform.extensions


/**
 * Returns the n-th occurrence of [string] or -1 if [string] isn't contained n-times.
 */
fun String.nthIndexOf(string: String, nthOccurrence: Int, startIndex: Int = 0, ignoreCase: Boolean = false): Int {
  var currentIndex = startIndex
  var countOccurrence = 0

  while (countOccurrence < nthOccurrence) {
    val index = this.indexOf(string, currentIndex, ignoreCase)

    if (index < 0) {
      return -1 // not found
    }

    if (++countOccurrence == nthOccurrence) {
      return index
    }

    currentIndex = index + 1
  }

  return -1
}

/**
 * Finds all occurrences of [string] in String and returns their indices.
 */
fun String.indicesOf(string: String, startIndex: Int = 0, ignoreCase: Boolean = false): List<Int> {
  val indices = mutableListOf<Int>()
  var currentIndex = startIndex

  while (currentIndex >= 0) {
    currentIndex = this.indexOf(string, currentIndex, ignoreCase)

    if (currentIndex < 0) {
      break
    } else {
      indices.add(currentIndex++)
    }
  }

  return indices
}

/**
 * Finds all occurrences of [string] in String.
 */
fun String.countOccurrences(string: String, startIndex: Int = 0, ignoreCase: Boolean = false): Int {
  return this.indicesOf(string, startIndex, ignoreCase).size
}