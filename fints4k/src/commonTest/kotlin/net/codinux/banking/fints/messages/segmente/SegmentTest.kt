package net.codinux.banking.fints.messages.segmente

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.DatenelementBase
import net.codinux.banking.fints.messages.datenelemente.basisformate.TextDatenelement
import kotlin.test.Test
import kotlin.test.assertEquals

class SegmentTest {

    @Test
    fun format_CutEmptyDataElementGroupsAtSegmentEnd() {

        // given
        val underTest = object : Segment(listOf(
            createTextDataElement("DE1"),
            createTextDataElement("DE2"),
            createEmptyTextDataElement(),
            createEmptyTextDataElement(),
            createEmptyTextDataElement(),
            createTextDataElement("DE6"),
            createEmptyTextDataElement(),
            createTextDataElement("DE8"),
            createEmptyTextDataElement(),
            createEmptyTextDataElement()
        )) { }


        // when
        val result = underTest.format()

        // then
        // assert that empty data elements at end get cut but that the empty ones in the middle remain
        assertEquals("DE1+DE2++++DE6++DE8", result)
    }


    private fun createEmptyTextDataElement(): DatenelementBase {
        return createTextDataElement("")
    }

    private fun createTextDataElement(text: String): DatenelementBase {
        return object : TextDatenelement(text, Existenzstatus.Optional) { }
    }

}