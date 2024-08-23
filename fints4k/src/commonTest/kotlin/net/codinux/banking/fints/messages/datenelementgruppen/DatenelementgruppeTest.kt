package net.codinux.banking.fints.messages.datenelementgruppen

import net.codinux.banking.fints.test.assertEmpty
import net.codinux.banking.fints.messages.Separators
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.signatur.BenutzerdefinierteSignatur
import kotlin.test.Test
import kotlin.test.assertEquals


class DatenelementgruppeTest {

    @Test
    fun format_LastDataElementIsSet_GetsWrittenToOutput() {

        // given
        val pin = "12345"
        val tan = "654321"
        val dataElementGroup = BenutzerdefinierteSignatur(pin, tan)

        // when
        val result = dataElementGroup.format()

        // then
        assertEquals(pin + Separators.DataElementsSeparator + tan, result) // ":" does not get written to output
    }

    @Test
    fun format_LastDataElementIsEmpty_EmptyLastDataElementDoesNotGetWrittenToOutput() {

        // given
        val pin = "12345"
        val tan = null
        val dataElementGroup = BenutzerdefinierteSignatur(pin, tan)

        // when
        val result = dataElementGroup.format()

        // then
        assertEquals(pin, result) // ":" does not get written to output
    }

    @Test
    fun format_AllDataElementsAreEmpty_NothingGetsWrittenToOutput() {

        // given
        val pin = ""
        val tan = null
        val dataElementGroup = BenutzerdefinierteSignatur(pin, tan)

        // when
        val result = dataElementGroup.format()

        // then
        assertEmpty(result) // ":" does not get written to output
    }

}