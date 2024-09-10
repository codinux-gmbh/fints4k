package net.codinux.banking.fints.transactions

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.codinux.banking.fints.extensions.EuropeBerlin
import net.codinux.banking.fints.test.assertEquals
import net.codinux.banking.fints.transactions.mt940.Mt94xParserBase
import net.codinux.banking.fints.transactions.mt940.model.AccountStatement
import net.codinux.banking.fints.transactions.mt940.model.Transaction
import kotlin.test.Test

class Mt94xParserBaseTest {

    private val underTest = object : Mt94xParserBase<AccountStatement>() {
        override fun createAccountStatement(orderReferenceNumber: String, referenceNumber: String?, bankCodeBicOrIban: String, accountIdentifier: String?, statementNumber: Int, sheetNumber: Int?, transactions: List<Transaction>, fieldsByCode: List<Pair<String, String>>): AccountStatement {
            throw IllegalStateException("We are testing base functionality, not parsing (Interim)AccountStatements")
        }
    }


    @Test
    fun parseDateTimeWithTimeZoneUtc() {
        val result = underTest.parseDateTime("1311130945+0000")

        val resultAtEuropeBerlin = result.toLocalDateTime(TimeZone.EuropeBerlin)

        assertEquals(LocalDateTime(2013, 11, 13, 10, 45), resultAtEuropeBerlin)
    }

    @Test
    fun parseDateTimeWithTimeZoneEuropeBerlin() {
        val result = underTest.parseDateTime("2408210742+0200")

        val resultAtEuropeBerlin = result.toLocalDateTime(TimeZone.EuropeBerlin)

        assertEquals(LocalDateTime(2024, 8, 21, 7, 42), resultAtEuropeBerlin)
    }

    @Test
    fun parseDateTimeWithoutTimeZone() { // actually the time zone is mandatory, but by far not all banks add it
        val result = underTest.parseDateTime("2408232156")

        val resultAtEuropeBerlin = result.toLocalDateTime(TimeZone.EuropeBerlin)

        assertEquals(LocalDateTime(2024, 8, 23, 21, 56), resultAtEuropeBerlin)
    }

//    @Test
//    fun parseDateTimeStartingWithCharacter() {
//        // really don't know where's the 'C' at the start is coming from, but this is an example from DFÜ-Abkommen PDF, p. 674
//        val result = underTest.parseDateTime("C1311130945+0000")
//
//        val resultAtEuropeBerlin = result.toLocalDateTime(TimeZone.EuropeBerlin)
//
//        assertEquals(LocalDateTime(2024, 8, 23, 21, 56), resultAtEuropeBerlin)
//    }

}