package net.codinux.banking.fints.messages.segmente.implementierte

import net.codinux.banking.fints.FinTsTestBase
import kotlin.test.Test
import kotlin.test.assertEquals


class IdentifikationsSegmentTest : FinTsTestBase() {

    @Test
    fun format() {

        // given
        val underTest = IdentifikationsSegment(2, Bank)

        // when
        val result = underTest.format()

        // then
        assertEquals(result, "HKIDN:2:2+280:12345678+0987654321+0+0")
    }

}