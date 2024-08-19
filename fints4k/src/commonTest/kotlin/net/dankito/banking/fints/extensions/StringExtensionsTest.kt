package net.dankito.banking.fints.extensions

import kotlin.test.Test
import kotlin.test.assertEquals

internal class StringExtensionsTest {

  @Test
  fun nthIndexOf() {
    val input = "java.lang.Exception: A severe error occurred" + IntRange(1, 30).map { "\r\n\tStack trace element $it" }

    val result = input.nthIndexOf("\n", 15)

    assertEquals(415, result)
  }

  @Test
  fun indicesOf() {
    val expectedOccurrences = 30
    val stringToFind = "\r\n"
    val input = "java.lang.Exception: A severe error occurred" + IntRange(1, expectedOccurrences).map { "$stringToFind\tStack trace element $it" }

    val result = input.indicesOf(stringToFind)

    assertEquals(expectedOccurrences, result.size)

    result.forEach { index ->
      assertEquals(input.substring(index, index + stringToFind.length), stringToFind)
    }
  }

}