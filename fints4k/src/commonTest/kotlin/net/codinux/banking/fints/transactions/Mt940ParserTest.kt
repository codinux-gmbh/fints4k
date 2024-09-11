package net.codinux.banking.fints.transactions

import kotlinx.datetime.LocalDate
import net.codinux.banking.fints.model.Amount
import net.codinux.banking.fints.test.assertEquals
import net.codinux.banking.fints.test.assertNotNull
import net.codinux.banking.fints.test.assertNull
import net.codinux.banking.fints.test.assertSize
import net.codinux.banking.fints.transactions.mt940.Mt940Parser
import net.codinux.banking.fints.transactions.swift.MtParserTestBase
import kotlin.test.Test
import kotlin.test.assertContains


class Mt940ParserTest : MtParserTestBase() {

    private val underTest = object : Mt940Parser() {
        public override fun parseDate(dateString: String): LocalDate {
            return super.parseDate(dateString)
        }
    }


    @Test
    fun accountStatementWithSingleTransaction() {

        // when
        val result = underTest.parseMt940String(AccountStatementWithSingleTransaction)


        // then
        assertSize(1, result)

        val statement = result.first()

        assertEquals(BankCode, statement.bankCodeBicOrIban)
        assertEquals(CustomerId, statement.accountIdentifier)
        assertBalance(statement.openingBalance, true, AccountStatement1PreviousStatementBookingDate, AccountStatement1OpeningBalanceAmount)
        assertBalance(statement.closingBalance, true, AccountStatement1BookingDate, AccountStatement1ClosingBalanceAmount)

        assertSize(1, statement.transactions)

        val transaction = statement.transactions.first()
        assertTurnover(transaction.statementLine, AccountStatement1BookingDate, AccountStatement1Transaction1Amount)
        assertTransactionDetails(transaction.information, AccountStatement1Transaction1OtherPartyName,
            AccountStatement1Transaction1OtherPartyBankId, AccountStatement1Transaction1OtherPartyAccountId)
    }

    @Test
    fun accountStatementWithSingleTransaction_SheetNumberOmitted() {

        // given
        val amount = Amount("15,00")
        val isCredit = false
        val bookingDate = LocalDate(2020, 5, 11)

        // when
        val result = underTest.parseMt940String(AccountStatementWithSingleTransaction_SheetNumberOmitted)


        // then
        assertSize(1, result)

        val statement = result.first()

        assertEquals(BankCode, statement.bankCodeBicOrIban)
        assertEquals(CustomerId, statement.accountIdentifier)
        assertEquals(0, statement.statementNumber)
        assertNull(statement.sheetNumber)

        assertBalance(statement.openingBalance, true, bookingDate, Amount("0,00"))
        assertBalance(statement.closingBalance, isCredit, bookingDate, amount)

        assertSize(1, statement.transactions)

        val transaction = statement.transactions.first()
        assertTurnover(transaction.statementLine, bookingDate, amount, isCredit)
        assertTransactionDetails(transaction.information, "Ausgabe einer Debitkarte", BankCode, CustomerId)
    }

    @Test
    fun accountStatementWithTwoTransactions() {

        // when
        val (statements, remainder) = underTest.parseMt940Chunk(AccountStatementWithTwoTransactions)


        // then
        assertNull(remainder)

        assertSize(1, statements)

        val statement = statements.first()

        assertEquals(BankCode, statement.bankCodeBicOrIban)
        assertEquals(CustomerId, statement.accountIdentifier)
        assertBalance(statement.openingBalance, true, AccountStatement1PreviousStatementBookingDate, AccountStatement1OpeningBalanceAmount)
        assertBalance(statement.closingBalance, true, AccountStatement1BookingDate, AccountStatement1With2TransactionsClosingBalanceAmount)

        assertSize(2, statement.transactions)

        val firstTransaction = statement.transactions.first()
        assertTurnover(firstTransaction.statementLine, AccountStatement1BookingDate, AccountStatement1Transaction1Amount)
        assertTransactionDetails(firstTransaction.information, AccountStatement1Transaction1OtherPartyName,
            AccountStatement1Transaction1OtherPartyBankId, AccountStatement1Transaction1OtherPartyAccountId)

        val secondTransaction = statement.transactions[1]
        assertTurnover(secondTransaction.statementLine, AccountStatement1BookingDate, AccountStatement1Transaction2Amount, false)
        assertTransactionDetails(secondTransaction.information, AccountStatement1Transaction2OtherPartyName,
            AccountStatement1Transaction2OtherPartyBankId, AccountStatement1Transaction2OtherPartyAccountId)
    }

    @Test
    fun accountStatementWithPartialNextStatement() {

        // when
        val (statements, remainder) = underTest.parseMt940Chunk(AccountStatementWithSingleTransaction + "\r\n" + ":20:STARTUMSE")


        // then
        assertEquals(":20:STARTUMSE", remainder)

        assertSize(1, statements)

        val statement = statements.first()

        assertEquals(BankCode, statement.bankCodeBicOrIban)
        assertEquals(CustomerId, statement.accountIdentifier)
        assertBalance(statement.openingBalance, true, AccountStatement1PreviousStatementBookingDate, AccountStatement1OpeningBalanceAmount)
        assertBalance(statement.closingBalance, true, AccountStatement1BookingDate, AccountStatement1ClosingBalanceAmount)

        assertSize(1, statement.transactions)

        val transaction = statement.transactions.first()
        assertTurnover(transaction.statementLine, AccountStatement1BookingDate, AccountStatement1Transaction1Amount)
        assertTransactionDetails(transaction.information, AccountStatement1Transaction1OtherPartyName,
            AccountStatement1Transaction1OtherPartyBankId, AccountStatement1Transaction1OtherPartyAccountId)
    }

    @Test
    fun fixAnnualJumpFromBookingDateToValueDate() {

        val transactionsString = AccountStatementWithAnnualJumpFromBookingDateToValueDate


        // when
        val result = underTest.parseMt940String(transactionsString)


        // then
        assertSize(1, result)
        assertSize(2, result.first().transactions)

        result.first().transactions[0].statementLine.apply {
            assertEquals(LocalDate(2019, 12, 30), bookingDate)
            assertEquals(LocalDate(2020, 1, 1), valueDate)
        }
        result.first().transactions[1].statementLine.apply {
            assertEquals(LocalDate(2019, 12, 30), bookingDate)
            assertEquals(LocalDate(2020, 1, 1), valueDate)
        }
    }

    @Test
    fun fixLineStartsWithDashThatIsNotAStatementSeparator() {

        // given
        val transactionsString = AccountStatementWithLineStartsWithDashThatIsNotABlockSeparator


        // when
        val result = underTest.parseMt940String(transactionsString)


        // then
        assertSize(3, result)
        assertSize(7, result.flatMap { it.transactions })
    }

    @Test
    fun fixThatTimeGotDetectedAsFieldCode() {

        // given
        val transactionsString = AccountStatementWithTimeThatGotDetectedAsFieldCode


        // when
        val result = underTest.parseMt940String(transactionsString)


        // then
        assertSize(1, result)
        assertSize(3, result.flatMap { it.transactions })

        result.flatMap { it.transactions }.forEach { transaction ->
            assertNotNull(transaction.information)

            assertNotNull(transaction.information?.sepaReference)

            if (transaction.information?.unparsedReference?.contains("KREF+") == true) {
                assertNotNull(transaction.information?.customerReference)
            }
        }
    }

    @Test
    fun fixThat_QuestionMarkComma_GetsDetectedAsFieldCode() {
        val transactionsString = QuotationMarkCommaGetsDetectedAsFieldValue


        // when
        val result = underTest.parseMt940String(transactionsString)


        // then
        assertSize(1, result)
        assertSize(1, result.first().transactions)

        result.first().transactions[0].information?.apply {
            assertEquals("BASISLASTSCHRIFT", postingText)
            assertEquals("TUBDDEDD", otherPartyBankId)
            assertEquals("DE87300308801234567890", otherPartyAccountId)
            assertEquals("6MKL2OT30QENNLIU", endToEndReference)
            assertEquals("?,3SQNdUbxm9z7dB)+gKYDJAKzCM0G", mandateReference)
            assertContains(sepaReference ?: "", "IBAN: DE87300308801234567890 BIC: TUBDDEDD")
        }
    }


    @Test
    fun parseDate() {
        val result = underTest.parseDate("240507")

        assertEquals(LocalDate(2024, 5, 7), result)
    }

    @Test
    fun parseDateBeforeYear2000() {
        val result = underTest.parseDate("990507")

        assertEquals(LocalDate(1999, 5, 7), result)
    }

    @Test
    fun parseDate_FixSparkasse29thOFFebruaryInNonLeapYearBug() {
        val result = underTest.parseDate("230229")

        assertEquals(LocalDate(2023, 2, 28), result)
    }

    @Test
    fun parseDate_FixSparkasse30thOfFebruaryBug() {
        val result = underTest.parseDate("230229")

        assertEquals(LocalDate(2023, 2, 28), result)
    }

}