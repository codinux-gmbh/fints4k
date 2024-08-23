package net.codinux.banking.fints.messages.segmente.implementierte.umsaetze

import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.account.Kontoverbindung
import net.codinux.banking.fints.model.AccountData


open class SaldenabfrageVersion6(
    segmentNumber: Int,
    account: AccountData,
    allAccounts: Boolean = false,
    maxAmountEntries: Int? = null,
    continuationId: String? = null
)
    : SaldenabfrageBase(segmentNumber, 6, Kontoverbindung(account), allAccounts, maxAmountEntries, continuationId)