package net.codinux.banking.fints.messages.segmente.implementierte.umsaetze

import net.codinux.banking.fints.FinTsTestBase
import kotlin.test.Test
import kotlin.test.assertEquals


class SaldenabfrageVersion5Test : FinTsTestBase() {

    @Test
    fun format_NotAllAccounts() {

        // given
        val underTest = SaldenabfrageVersion5(3, Account, false)

        // when
        val result = underTest.format()

        // then
        assertEquals(result, "HKSAL:3:5+$CustomerId::280:$BankCode+N")
    }

    @Test
    fun format_AllAccounts() {

        // given
        val underTest = SaldenabfrageVersion5(3, Account, true)

        // when
        val result = underTest.format()

        // then
        assertEquals(result, "HKSAL:3:5+$CustomerId::280:$BankCode+J")
    }

}