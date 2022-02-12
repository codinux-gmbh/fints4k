package net.dankito.banking.fints.messages.datenelemente.implementierte

import net.dankito.banking.fints.extensions.assertThrows
import net.dankito.banking.fints.messages.Existenzstatus
import kotlin.test.Test
import kotlin.test.assertEquals


class BPDVersionTest {

    @Test
    fun format() {

        // given
        val underTest = BPDVersion(3, Existenzstatus.Mandatory)

        // when
        val result = underTest.format()

        // then
        assertEquals("3", result)
    }


    @Test
    fun validate_0_Valid() {

        // given
        val underTest = BPDVersion(0, Existenzstatus.Mandatory)

        // when
        underTest.validate()

        // then
        // no exception
    }

    @Test
    fun validate_999_Valid() {

        // given
        val underTest = BPDVersion(999, Existenzstatus.Mandatory)

        // when
        underTest.validate()

        // then
        // no exception
    }

    @Test
    fun validate_TooLow() {

        // given
        val underTest = BPDVersion(-1, Existenzstatus.Mandatory)

        // when
        assertThrows<IllegalArgumentException> {
            underTest.validate()
        }
    }

    @Test
    fun validate_TooHigh() {

        // given
        val underTest = BPDVersion(1000, Existenzstatus.Mandatory)

        // when
        assertThrows<IllegalArgumentException> {
            underTest.validate()
        }
    }

}