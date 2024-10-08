package net.codinux.banking.fints.messages.datenelementgruppen.implementierte.account

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.implementierte.account.KontoDepotnummer
import net.codinux.banking.fints.messages.datenelemente.implementierte.account.Unterkontomerkmal
import net.codinux.banking.fints.messages.datenelementgruppen.Datenelementgruppe
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.Kreditinstitutskennung
import net.codinux.banking.fints.model.AccountData


/**
 * Anhand dieses Formats können sowohl deutsche als auch internationale Bankverbindungen beschrieben
 * werden. Die Belegung für wichtige europäische Länder ist dem Kapitel E.5 zu entnehmen.
 *
 * Falls bei einem Institut unter einer Kontonummer verschiedene Konten mit unterschiedlichen Merkmalen
 * geführt werden (z.B. verschiedene Währungen oder Festgeldanlagen), wird von diesem Institut in den
 * UPD zu jeder dieser Kontonummern zur Unterscheidung zusätzlich ein „Unterkontomerkmal“ angegeben.
 * Dieser ist dann bei jeder Auftraggeberkontoverbindung anzugeben.
 */
open class Kontoverbindung(
    bankCountryCode: Int,
    bankCode: String,
    accountNumber: String,
    subAccountAttribute: String? = null

) : Datenelementgruppe(listOf(
    KontoDepotnummer(accountNumber, Existenzstatus.Mandatory),
    Unterkontomerkmal(subAccountAttribute ?: "", Existenzstatus.Optional),
    Kreditinstitutskennung(bankCountryCode, bankCode)
), Existenzstatus.Mandatory) {

    constructor(account: AccountData)
            : this(account.bankCountryCode, account.bankCode, account.accountIdentifier, account.subAccountAttribute)

}