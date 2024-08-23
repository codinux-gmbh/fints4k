package net.codinux.banking.fints.messages.segmente.implementierte

import net.codinux.banking.fints.FinTsTestBase
import net.codinux.banking.fints.model.MessageBaseData
import kotlin.test.Test
import kotlin.test.assertEquals


class VerschluesselungskopfTest : FinTsTestBase() {

    @Test
    fun format() {

        // given

        val underTest = PinTanVerschluesselungskopf(MessageBaseData(Bank, Product), Date, Time)

        // when
        val result = underTest.format()

        // then
        assertEquals(normalizeBinaryData(result), "HNVSK:998:3+PIN:2+998+1+1::0+1:$Date:$Time+2:2:13:@8@        :5:1+280:$BankCode:$CustomerId:V:0:0+0")
    }

}