package net.codinux.banking.fints.messages.segmente.implementierte.umsaetze

import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.account.KontoverbindungInternational
import net.codinux.banking.fints.model.AccountData
import net.codinux.banking.fints.model.BankData


open class SaldenabfrageVersion7(
    segmentNumber: Int,
    account: AccountData,
    bank: BankData,
    allAccounts: Boolean = false,
    maxAmountEntries: Int? = null,
    continuationId: String? = null
)
    : SaldenabfrageBase(segmentNumber, 7, KontoverbindungInternational(account, bank), allAccounts, maxAmountEntries, continuationId)