package net.codinux.banking.fints.messages.segmente.implementierte.sepa

import net.codinux.banking.fints.test.assertContains
import net.codinux.banking.fints.messages.segmente.id.CustomerSegmentId
import net.codinux.banking.fints.model.*
import kotlin.test.Test


class SepaBankTransferBaseTest {

    companion object {

        val segmentNumber = 7
        val debitorName = "Nelson Mandela"
        val debitorIban = "ZA123456780987654321"
        val debitorBic = "ABCDZAEFXXX"
        val recipientName = "Mahatma Gandhi"
        val recipientIban = "IN123456780987654321"
        val recipientBic = "ABCDINEFXXX"
        val amount = Amount("1234,56")
        val reference = "What should Mahatma Gandhi want with money?"

    }


    @Test
    fun format_Pain001_001_03() {

        // given
        val underTest = SepaBankTransferBase(CustomerSegmentId.SepaBankTransfer, segmentNumber,
            "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03",
            debitorName,
            AccountData("", null, 0, "", debitorIban, "", null, null, "", null, null, listOf()),
            debitorBic,
            BankTransferData(recipientName, recipientIban, recipientBic, Money(amount, "EUR"), reference)
        )


        // when
        val result = underTest.format()


        // then
        assertContains(result, debitorName, debitorIban, debitorBic, recipientName, recipientIban, recipientBic,
            amount.toString().replace(',', '.'), reference, "urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.001.03")
    }

    @Test
    fun format_Pain001_003_03() {

        // given
        val underTest = SepaBankTransferBase(CustomerSegmentId.SepaBankTransfer, segmentNumber,
            "urn:iso:std:iso:20022:tech:xsd:pain.001.003.03",
            debitorName,
            AccountData("", null, 0, "", debitorIban, "", null, null, "", null, null, listOf()),
            debitorBic,
            BankTransferData(recipientName, recipientIban, recipientBic, Money(amount, "EUR"), reference)
        )


        // when
        val result = underTest.format()


        // then
        assertContains(result, debitorName, debitorIban, debitorBic, recipientName, recipientIban, recipientBic,
            amount.toString().replace(',', '.'), reference, "urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.003.03")
    }

}