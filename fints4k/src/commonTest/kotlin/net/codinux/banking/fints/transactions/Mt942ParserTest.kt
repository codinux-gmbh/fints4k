package net.codinux.banking.fints.transactions

import kotlinx.datetime.LocalDate
import net.codinux.banking.fints.test.assertEquals
import net.codinux.banking.fints.test.assertNull
import net.codinux.banking.fints.test.assertSize
import net.codinux.banking.fints.transactions.mt940.Mt942Parser
import net.codinux.banking.fints.transactions.mt940.model.InterimAccountStatement
import net.codinux.banking.fints.transactions.mt940.model.Transaction
import kotlin.test.Test

class Mt942ParserTest {

    private val underTest = Mt942Parser()


    @Test
    fun parseNullValuesMt942String() {
        // speciality of Deutsche Bank, it adds a MT942 if there are prebookings or not, so in most cases contains simply empty values
        val mt942String = """
            :20:DEUTDEFFXXXX
            :25:70070024/01234560000
            :28C:00000/001
            :34F:EUR0,
            :13:2408212359
            :90D:0EUR0,
            :90C:0EUR0,
            -
            :20:DEUTDEFFXXXX
            :25:00000000/DE08700700240012345600
            :28C:00000/001
            :34F:EURC0,
            :13D:2408210442+0200
            :90D:0EUR0,
            :90C:0EUR0,
            -
        """.trimIndent()


        val result = underTest.parseMt942String(mt942String)


        assertSize(2, result)


        val firstStatement = result.first()

        assertNullValuesStatement(firstStatement)
        assertEquals("70070024", firstStatement.bankCodeBicOrIban)
        assertEquals("01234560000", firstStatement.accountIdentifier)


        val secondStatement = result[1]

        assertNullValuesStatement(secondStatement)
        assertEquals("00000000", secondStatement.bankCodeBicOrIban)
        assertEquals("DE08700700240012345600", secondStatement.accountIdentifier)
    }


    @Test
    fun parseDkExampleMt942String() {
        // see
        val mt942String = """
            :20:1234567
            :21:9876543210
            :25:10020030/1234567
            :28C:5/1
            :34F:EURD20,50
            :34F:EURC155,34
            :13D:C1311130945+0000
            :61:1311131113CR155,34NTRFNONREF//55555
            :86:166?00SEPA-UEBERWEISUNG?109315
            ?20EREF+987654123456?21SVWZ+Invoice no.
            123455056?22734 und 123455056735
            ?30COLSDE33XXX?31DE91370501980100558000
            ?32Max Mustermann
            :61:1311131113DR20,50NDDTNONREF//55555
            :86:105?00SEPA-BASIS-LASTSCHRIFT?109316
            ?20EREF+987654123497?21MREF+10023?22CRED+DE5
            4ZZZ09999999999?23SVWZ+Insurance premium 2
            ?24013?30WELADED1MST?31DE87240501501234567890
            ?32XYZ Insurance limited?34991
            :90D:1EUR20,50
            :90C:1EUR155,34
            -
        """.trimIndent()


        val result = underTest.parseMt942String(mt942String)


        assertSize(1, result)

        val statement = result.first()

        assertEquals("1234567", statement.orderReferenceNumber)
        assertEquals("9876543210", statement.referenceNumber)

        assertEquals("10020030", statement.bankCodeBicOrIban)
        assertEquals("1234567", statement.accountIdentifier)

        assertEquals(5, statement.statementNumber)
        assertEquals(1, statement.sheetNumber)

        assertEquals("20,50", statement.smallestAmountOfReportedTransactions.amount)
        assertEquals("EUR", statement.smallestAmountOfReportedTransactions.currency)
        assertEquals(false, statement.smallestAmountOfReportedTransactions.isCredit)

        assertEquals("155,34", statement.smallestAmountOfReportedCreditTransactions?.amount)
        assertEquals("EUR", statement.smallestAmountOfReportedCreditTransactions?.currency)
        assertEquals(true, statement.smallestAmountOfReportedCreditTransactions?.isCredit)

        assertEquals(1, statement.amountAndTotalOfDebitPostings?.numberOfPostings)
        assertEquals("20,50", statement.amountAndTotalOfDebitPostings?.amount)
        assertEquals("EUR", statement.amountAndTotalOfDebitPostings?.currency)

        assertEquals(1, statement.amountAndTotalOfCreditPostings?.numberOfPostings)
        assertEquals("155,34", statement.amountAndTotalOfCreditPostings?.amount)
        assertEquals("EUR", statement.amountAndTotalOfCreditPostings?.currency)


        assertSize(2, statement.transactions)

        val firstTransaction = statement.transactions.first()
        assertTransactionStatementLine(firstTransaction, LocalDate(2013, 11, 13), LocalDate(2013, 11, 13), "155,34", true)
        assertTransactionReference(firstTransaction, "SEPA-UEBERWEISUNG", "Max Mustermann", "COLSDE33XXX", "DE91370501980100558000", "Invoice no.123455056734 und 123455056735", "987654123456 ", null, null)

        val secondTransaction = statement.transactions[1]
        assertTransactionStatementLine(secondTransaction, LocalDate(2013, 11, 13), LocalDate(2013, 11, 13), "20,50", false)
        assertTransactionReference(secondTransaction, "SEPA-BASIS-LASTSCHRIFT", "XYZ Insurance limited", "WELADED1MST", "DE87240501501234567890", "Insurance premium 2013", "987654123497 ", "10023 ", "DE54ZZZ09999999999 ")
    }


    @Test
    fun parseSparkasseMt942String() {
        val mt942String = """
            :20:STARTDISPE
            :25:70050000/0123456789
            :28C:00000/001
            :34F:EURD60,77
            :13:2408232156
            :61:2408260823DR60,77NDDTNONREF
            :86:105?00FOLGELASTSCHRIFT?109248?20EREF+R0012345678?21MREF+M-K12
            34567890-0001?22CRED+DE63ZZZ00000012345?23SVWZ+Rechnungsnr.. R001
            2345?246789 - Kundennr.. K123456789?251?30HYVEDEMM406?31DE80765200
            710123456789?32Dein Cloud Provider?34992
            :90D:1EUR60,77
            :90C:0EUR0,00
            -
        """.trimIndent()


        val result = underTest.parseMt942String(mt942String)


        assertSize(1, result)

        val statement = result.first()

        assertEquals("STARTDISPE", statement.orderReferenceNumber)
        assertNull(statement.referenceNumber)

        assertEquals("70050000", statement.bankCodeBicOrIban)
        assertEquals("0123456789", statement.accountIdentifier)

        assertEquals(0, statement.statementNumber)
        assertEquals(1, statement.sheetNumber)

        assertEquals("60,77", statement.smallestAmountOfReportedTransactions.amount)
        assertEquals("EUR", statement.smallestAmountOfReportedTransactions.currency)
        assertEquals(false, statement.smallestAmountOfReportedTransactions.isCredit)
        assertNull(statement.smallestAmountOfReportedCreditTransactions)

        assertEquals(1, statement.amountAndTotalOfDebitPostings?.numberOfPostings)
        assertEquals("60,77", statement.amountAndTotalOfDebitPostings?.amount)
        assertEquals("EUR", statement.amountAndTotalOfDebitPostings?.currency)

        assertEquals(0, statement.amountAndTotalOfCreditPostings?.numberOfPostings)
        assertEquals("0,00", statement.amountAndTotalOfCreditPostings?.amount)
        assertEquals("EUR", statement.amountAndTotalOfCreditPostings?.currency)


        assertSize(1, statement.transactions)

        val transaction = statement.transactions.first()

        assertTransactionStatementLine(transaction, LocalDate(2024, 8, 23), LocalDate(2024, 8, 26), "60,77", false)

        assertTransactionReference(transaction, "FOLGELASTSCHRIFT", "Dein Cloud Provider", "HYVEDEMM406", "DE80765200710123456789",
            "Rechnungsnr.. R00123456789 - Kundennr.. K1234567891", "R0012345678 ", "M-K1234567890-0001 ", "DE63ZZZ00000012345 ")
    }


    private fun assertNullValuesStatement(statement: InterimAccountStatement) {
        assertEquals("DEUTDEFFXXXX", statement.orderReferenceNumber)
        assertNull(statement.referenceNumber)

        assertEquals(0, statement.statementNumber)
        assertEquals(1, statement.sheetNumber)

        assertEquals("0,", statement.smallestAmountOfReportedTransactions.amount)
        assertEquals("EUR", statement.smallestAmountOfReportedTransactions.currency)

        assertSize(0, statement.transactions)

        assertEquals(0, statement.amountAndTotalOfDebitPostings?.numberOfPostings)
        assertEquals("0,", statement.amountAndTotalOfDebitPostings?.amount)
        assertEquals("EUR", statement.amountAndTotalOfDebitPostings?.currency)

        assertEquals(0, statement.amountAndTotalOfCreditPostings?.numberOfPostings)
        assertEquals("0,", statement.amountAndTotalOfCreditPostings?.amount)
        assertEquals("EUR", statement.amountAndTotalOfCreditPostings?.currency)
    }

    private fun assertTransactionStatementLine(transaction: Transaction, bookingDate: LocalDate, valueDate: LocalDate, amount: String, isCredit: Boolean, isReversal: Boolean = false) {
        assertEquals(bookingDate, transaction.statementLine.bookingDate)
        assertEquals(valueDate, transaction.statementLine.valueDate)
        assertEquals(amount, transaction.statementLine.amount.string)
        assertEquals(isCredit, transaction.statementLine.isCredit)
        assertEquals(isReversal, transaction.statementLine.isReversal)
    }

    private fun assertTransactionReference(transaction: Transaction,
                                           postingText: String, otherPartyName: String?, otherPartyBankId: String?, otherPartyAccountId: String?,
                                           sepaReference: String, endToEndReference: String? = null, mandateReference: String? = null, creditorIdentifier: String? = null
    ) {
        assertEquals(postingText, transaction.information?.postingText)
        assertEquals(otherPartyName, transaction.information?.otherPartyName)
        assertEquals(otherPartyBankId, transaction.information?.otherPartyBankId)
        assertEquals(otherPartyAccountId, transaction.information?.otherPartyAccountId)

        assertEquals(sepaReference, transaction.information?.sepaReference)
        assertEquals(endToEndReference, transaction.information?.endToEndReference)
        assertEquals(mandateReference, transaction.information?.mandateReference)
        assertEquals(creditorIdentifier, transaction.information?.creditorIdentifier)
    }

}