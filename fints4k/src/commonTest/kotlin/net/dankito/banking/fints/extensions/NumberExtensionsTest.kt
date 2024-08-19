package net.dankito.banking.fints.extensions

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberExtensionsTest {

  @Test
  fun toStringWithMinDigits_0() {
    val result = 7.toStringWithMinDigits(0)

    assertEquals("7", result)
  }

  @Test
  fun toStringWithMinDigits_1() {
    val result = 7.toStringWithMinDigits(1)

    assertEquals("7", result)
  }

  @Test
  fun toStringWithMinDigits_2() {
    val result = 7.toStringWithMinDigits(2)

    assertEquals("07", result)
  }

  @Test
  fun toStringWithMinDigits_3() {
    val result = 7.toStringWithMinDigits(3)

    assertEquals("007", result)
  }

  @Test
  fun toStringWithMinDigits_5() {
    val result = 7.toStringWithMinDigits(5)

    assertEquals("00007", result)
  }

  @Test
  fun toStringWithMinDigits_10() {
    val result = 7.toStringWithMinDigits(10)

    assertEquals("0000000007", result)
  }


  @Test
  fun ensureMinStringLength() {
    val result = "123,45 EUR".padStart(12, ' ')

    assertEquals("  123,45 EUR", result)
  }

}