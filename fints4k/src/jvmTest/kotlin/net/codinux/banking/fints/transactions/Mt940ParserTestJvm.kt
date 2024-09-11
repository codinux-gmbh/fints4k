package net.codinux.banking.fints.transactions

import net.codinux.banking.fints.FinTsTestBaseJvm
import net.codinux.banking.fints.test.assertSize
import net.codinux.banking.fints.transactions.mt940.Mt940Parser
import net.codinux.banking.fints.transactions.swift.MtParserBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class Mt940ParserTestJvm : FinTsTestBaseJvm() {

    private val underTest = Mt940Parser()


    @Test
    fun parseTransactions() {

        // given
        val transactionsString = loadTestFile(TransactionsMt940Filename)


        // when
        val result = underTest.parseMt940String(transactionsString)


        // then
        assertThat(result).hasSize(32)

        val transactions = result.flatMap { it.transactions }
        assertSize(55, transactions)
    }


    @Test
    fun parseTransactionsMtParserBase() {

        // given
        val transactionsString = loadTestFile(TransactionsMt940Filename)


        // when
        val result = MtParserBase().parseMtString(transactionsString)


        // then
        assertThat(result).hasSize(32)

        val references = result.flatMap { it.getMandatoryRepeatableField("86") }
        assertSize(55, references)
    }

}