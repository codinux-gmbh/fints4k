package net.codinux.banking.fints.messages.segmente.implementierte

import kotlin.test.Test
import kotlin.test.assertEquals

class SignaturabschlussTest {

    @Test
    fun format() {

        // given
        val segmentNumber = 7
        val controlReference = "1902675680"
        val pin = "MyPin"

        val underTest = Signaturabschluss(segmentNumber, controlReference, pin)

        // when
        val result = underTest.format()

        // then
        assertEquals(result, "HNSHA:$segmentNumber:2+$controlReference++$pin")
    }

}