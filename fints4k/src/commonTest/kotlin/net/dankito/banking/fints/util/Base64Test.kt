package net.dankito.banking.fints.util

import net.dankito.utils.multiplatform.extensions.randomWithSeed
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals


class Base64Test {

    private val underTest = Base64()


    @Test
    fun encode() {
        testEncode("Kotlin is awesome", "S290bGluIGlzIGF3ZXNvbWU=")
    }

    @Test
    fun testEncodingPadding() {
        testEncode("", "")
        testEncode("1", "MQ==")
        testEncode("22", "MjI=")
        testEncode("333", "MzMz")
        testEncode("4444", "NDQ0NA==")
    }

    private fun testEncode(input: String, expectedOutput: String) {
        val actualOutput = underTest.encode(input)

        assertEquals(expectedOutput, actualOutput)
    }


    @Test
    fun decode() {
        testDecode("S290bGluIGlzIGF3ZXNvbWU=", "Kotlin is awesome")
    }

    @Test
    fun testDecodingPadding() {
        testDecode("", "")
        testDecode("MQ==", "1")
        testDecode("MjI=", "22")
        testDecode("MzMz", "333")
        testDecode("NDQ0NA==", "4444")
    }

    private fun testDecode(input: String, expectedOutput: String) {
        val actualOutput = underTest.decode(input)

        assertEquals(expectedOutput, actualOutput)
    }


    @Test
    fun testRandomStrings() {
        val steps = 10_000 // 1_000_000 have been too much for JavaScript
        val random = randomWithSeed()

        for (count in 0 until steps) {
            // given
            val original = createRandomString(random)


            // when
            val encoded: String = underTest.encode(original)

            val decoded = underTest.decode(encoded)


            // then
            assertEquals(original, decoded, "Decoded string '$decoded' should actually be '$original'")
        }
    }

    private fun createRandomString(random: Random): String {
        val len: Int = random.nextInt(1, 1000)
        val original = CharArray(len)

        for (i in 0 until len) {
            original[i] = random.nextInt(0xFF).toChar()
        }

        return original.toString()
    }

}