package net.codinux.banking.fints.transactions

import net.codinux.banking.fints.FinTsTestBaseJvm
import net.codinux.banking.fints.model.AccountData
import net.codinux.banking.fints.model.BankData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class Mt940AccountTransactionsParserTest : FinTsTestBaseJvm() {


    private val underTest = Mt940AccountTransactionsParser()


    @Test
    fun parseTransactions() {

        // given
        val transactionsString = loadTestFile(TransactionsMt940Filename)


        // when
        val result = underTest.parseTransactions(transactionsString, BankData(), AccountData())


        // then
        assertThat(result).hasSize(55)
    }

}