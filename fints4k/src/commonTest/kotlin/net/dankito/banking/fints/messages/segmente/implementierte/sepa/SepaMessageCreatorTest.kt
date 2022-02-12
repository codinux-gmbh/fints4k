package net.dankito.banking.fints.messages.segmente.implementierte.sepa

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class SepaMessageCreatorTest {

    private val underTest = SepaMessageCreator()


    @Test
    fun containsOnlyAllowedCharacters_SimpleName() {

        // when
        val result = underTest.containsOnlyAllowedCharacters("Marieke Musterfrau")

        // then
        assertTrue(result)
    }

    @Test
    fun containsOnlyAllowedCharacters_WithAllAllowedCharacters() {

        // when
        val result = underTest.containsOnlyAllowedCharacters("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789,.?+-/()")

        // then
        assertTrue(result)
    }

    @Test
    fun `containsOnlyAllowedCharacters colon is a legal character`() {

        // when
        val result = underTest.containsOnlyAllowedCharacters(":")

        // then
        assertTrue(result)
    }

    @Test
    fun `containsOnlyAllowedCharacters ! is an illegal character`() {

        // when
        val result = underTest.containsOnlyAllowedCharacters("!")

        // then
        assertFalse(result)
    }

    @Test
    fun `containsOnlyAllowedCharacters € is an illegal character`() {

        // when
        val result = underTest.containsOnlyAllowedCharacters("€")

        // then
        assertFalse(result)
    }

    @Test
    fun `containsOnlyAllowedCharacters at sign is an illegal character`() {

        // when
        val result = underTest.containsOnlyAllowedCharacters("@")

        // then
        assertFalse(result)
    }

    @Test
    fun `containsOnlyAllowedCharacters ö is an illegal character`() {

        // when
        val result = underTest.containsOnlyAllowedCharacters("ö")

        // then
        assertFalse(result)
    }

}