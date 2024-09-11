package net.codinux.banking.fints.transactions.swift

import net.codinux.banking.fints.test.assertContains
import net.codinux.banking.fints.test.assertEquals
import net.codinux.banking.fints.test.assertNotNull
import net.codinux.banking.fints.test.assertSize
import net.codinux.banking.fints.transactions.swift.model.SwiftMessageBlock
import kotlin.test.Test

class MtParserBaseTest : MtParserTestBase() {

    private val underTest = MtParserBase()


    @Test
    fun accountStatementWithSingleTransaction() {

        // when
        val result = underTest.parseMtString(AccountStatementWithSingleTransaction)


        // then
        assertSize(1, result)

        val block = result.first()

        assertAccountStatementWithSingleTransaction(block)
    }

    @Test
    fun accountStatementWithTwoTransactions() {

        // when
        val result = underTest.parseMtString(AccountStatementWithTwoTransactions)


        // then
        assertSize(1, result)

        val block = result.first()

        assertEquals("$BankCode/$CustomerId", block.getMandatoryField("25"))
        assertEquals("00000/001", block.getMandatoryField("28C"))
        assertEquals("C${convertMt940Date(AccountStatement1PreviousStatementBookingDate)}EUR${AccountStatement1OpeningBalanceAmount.string}", block.getMandatoryField("60F"))

        assertEquals("C${convertMt940Date(AccountStatement1BookingDate)}EUR${AccountStatement1With2TransactionsClosingBalanceAmount.string}", block.getMandatoryField("62F"))

        assertNotNull(block.getOptionalRepeatableField("61"))
        assertSize(2, block.getOptionalRepeatableField("61")!!)

        assertNotNull(block.getOptionalRepeatableField("86"))
        assertSize(2, block.getOptionalRepeatableField("86")!!)

        assertEquals("8802270227CR1234,56N062NONREF", block.getOptionalRepeatableField("61")?.get(0))
        assertEquals("166?00GUTSCHR. UEBERWEISUNG?109249?20EREF+674?21SVWZ+1908301/\n" +
                "EUR 1234,56/20?2219-10-02/...?30AAAADE12?31DE99876543210987654321\n" +
                "?32Sender1", block.getOptionalRepeatableField("86")?.get(0))

        assertEquals("8802270227DR432,10N062NONREF", block.getOptionalRepeatableField("61")?.get(1))
        assertEquals("166?00ONLINE-UEBERWEISUNG?109249?20EREF+674?21SVWZ+1908301/\n" +
                "EUR 432,10/20?2219-10-02/...?30BBBBDE56?31DE77987654321234567890\n" +
                "?32Receiver2", block.getOptionalRepeatableField("86")?.get(1))
    }

    @Test
    fun accountStatementWithPartialNextStatement() {

        // when
        val result = underTest.parseMtString(AccountStatementWithSingleTransaction + "\r\n" + ":20:STARTUMSE")


        // then
        assertSize(2, result)

        val remainder = result.get(1)
        assertSize(1, remainder.fieldCodes)
        assertEquals("STARTUMSE", remainder.getMandatoryField("20"))

        assertAccountStatementWithSingleTransaction(result.first())
    }

    @Test
    fun fixLineStartsWithDashThatIsNotABlockSeparator() {

        // when
        val result = underTest.parseMtString(AccountStatementWithLineStartsWithDashThatIsNotABlockSeparator)


        // then
        assertSize(3, result)

        // the references field (86) contains a line that starts with "-End-Ref..."
        val references = result.flatMap { it.getMandatoryRepeatableField("86") }
        assertSize(7, references)
        assertContains(references, "820?20ÜBERTRAG / ÜBERWEISUNG?21ECHTZEITUEBERWEISUNGSTEST?22END-TO\n" +
        "-END-REF.:?23NICHT ANGEGEBEN?24Ref. 402C0YTD0GLPFDFV/1?32DANKI\n" +
                "TO")
    }

    @Test
    fun fixThatTimeGotDetectedAsFieldCode() {

        // when
        val result = underTest.parseMtString(AccountStatementWithTimeThatGotDetectedAsFieldCode)


        // then
        assertSize(1, result)
        assertSize(3, result.flatMap { it.getMandatoryRepeatableField("86") })
    }

    @Test
    fun fixThat_QuestionMarkComma_GetsDetectedAsFieldCode() {

        // when
        val result = underTest.parseMtString(QuotationMarkCommaGetsDetectedAsFieldValue)


        // then
        assertSize(1, result)

        val references = result.flatMap { it.getMandatoryRepeatableField("86") }
        assertSize(1, references)

        assertContains(references.first(),
            "BASISLASTSCHRIFT",
            "TUBDDEDD",
            "DE87300308801234567890",
            "6MKL2OT30QENNLIU",
            "?,3SQNdUbxm9z7dB)+gKYDJA?28KzCM0G",
            "IBAN: DE87300308801234?30TUBDDEDD"
        )
    }


    private fun assertAccountStatementWithSingleTransaction(block: SwiftMessageBlock) {
        assertEquals("$BankCode/$CustomerId", block.getMandatoryField("25"))
        assertEquals("00000/001", block.getMandatoryField("28C"))
        assertEquals(
            "C${convertMt940Date(AccountStatement1PreviousStatementBookingDate)}EUR${AccountStatement1OpeningBalanceAmount.string}",
            block.getMandatoryField("60F")
        )

        assertEquals(
            "C${convertMt940Date(AccountStatement1BookingDate)}EUR${AccountStatement1ClosingBalanceAmount.string}",
            block.getMandatoryField("62F")
        )

        assertNotNull(block.getOptionalRepeatableField("61"))
        assertSize(1, block.getOptionalRepeatableField("61")!!)

        assertNotNull(block.getOptionalRepeatableField("86"))
        assertSize(1, block.getOptionalRepeatableField("86")!!)

        assertEquals("8802270227CR1234,56N062NONREF", block.getOptionalRepeatableField("61")?.first())
        assertEquals(
            "166?00GUTSCHR. UEBERWEISUNG?109249?20EREF+674?21SVWZ+1908301/\n" +
                    "EUR 1234,56/20?2219-10-02/...?30AAAADE12?31DE99876543210987654321\n" +
                    "?32Sender1", block.getOptionalRepeatableField("86")?.first()
        )
    }

}