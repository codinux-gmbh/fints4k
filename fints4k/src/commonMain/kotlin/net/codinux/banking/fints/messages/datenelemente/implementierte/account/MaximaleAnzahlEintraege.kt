package net.codinux.banking.fints.messages.datenelemente.implementierte.account

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.NumerischesDatenelement
import net.codinux.banking.fints.model.GetAccountTransactionsParameter


/**
 * Maximale Anzahl rückzumeldender Einträge bei Abholaufträgen, Kreditinstitutsangeboten
 * oder –informationen (vgl. [Formals], Kap. B.6.3).
 */
open class MaximaleAnzahlEintraege(maxAmount: Int?, existenzstatus: Existenzstatus) : NumerischesDatenelement(maxAmount, 4, existenzstatus) {

    constructor(parameter: GetAccountTransactionsParameter) : this(parameter.maxCountEntriesIfSettingItIsAllowed, if (parameter.isSettingMaxCountEntriesAllowedByBank) Existenzstatus.Optional else Existenzstatus.NotAllowed) // > 0. O: „Eingabe Anzahl Einträge erlaubt“ (BPD) = „J“. N: sonst

}