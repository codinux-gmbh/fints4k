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
    fun containsOnlyAllowedCharacters_ColonIsALegalCharacter() {

        // when
        val result = underTest.containsOnlyAllowedCharacters(":")

        // then
        assertTrue(result)
    }

    @Test
    fun containsOnlyAllowedCharacters_ExclamationMarkIsAnIllegalCharacter() {

        // when
        val result = underTest.containsOnlyAllowedCharacters("!")

        // then
        assertFalse(result)
    }

    @Test
    fun containsOnlyAllowedCharacters_EuroSignIsAnIllegalCharacter() {

        // when
        val result = underTest.containsOnlyAllowedCharacters("€")

        // then
        assertFalse(result)
    }

    @Test
    fun containsOnlyAllowedCharacters_AtSignIsAnIllegalCharacter() {

        // when
        val result = underTest.containsOnlyAllowedCharacters("@")

        // then
        assertFalse(result)
    }

    @Test
    fun containsOnlyAllowedCharacters_ÖIsAnIllegalCharacter() {

        // when
        val result = underTest.containsOnlyAllowedCharacters("ö")

        // then
        assertFalse(result)
    }

}