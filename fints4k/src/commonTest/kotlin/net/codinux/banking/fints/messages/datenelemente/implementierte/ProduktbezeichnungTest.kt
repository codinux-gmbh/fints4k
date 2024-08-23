package net.codinux.banking.fints.messages.datenelemente.implementierte

import net.codinux.banking.fints.test.assertThrows
import net.codinux.banking.fints.messages.Existenzstatus
import kotlin.test.Test


class ProduktbezeichnungTest {

    @Test
    fun validate_MaxLength() {

        // given
        val underTest = Produktbezeichnung("1234567890123456789012345", Existenzstatus.Mandatory)

        // when
        underTest.validate()

        // then
        // no exception
    }

    @Test
    fun validate_MaxLengthExceeded() {

        // given
        val underTest = Produktbezeichnung("12345678901234567890123456", Existenzstatus.Mandatory)

        // when
        assertThrows<IllegalArgumentException> {
            underTest.validate()
        }
    }

}