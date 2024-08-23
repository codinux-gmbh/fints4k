package net.codinux.banking.fints.messages.segmente.implementierte.umsaetze

import net.codinux.banking.fints.FinTsTestBase
import kotlin.test.Test
import kotlin.test.assertEquals


class SaldenabfrageVersion7Test : FinTsTestBase() {

    @Test
    fun format_NotAllAccounts() {

        // given
        val underTest = SaldenabfrageVersion7(3, Account, Bank, false)

        // when
        val result = underTest.format()

        // then
        assertEquals(result, "HKSAL:3:7+$Iban:$Bic:$CustomerId::280:$BankCode+N")
    }

    @Test
    fun format_AllAccounts() {

        // given
        val underTest = SaldenabfrageVersion7(3, Account, Bank, true)

        // when
        val result = underTest.format()

        // then
        assertEquals(result, "HKSAL:3:7+$Iban:$Bic:$CustomerId::280:$BankCode+J")
    }

}