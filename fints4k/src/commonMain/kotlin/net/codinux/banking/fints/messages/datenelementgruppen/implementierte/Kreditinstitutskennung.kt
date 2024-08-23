package net.codinux.banking.fints.messages.datenelementgruppen.implementierte

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Laenderkennzeichen
import net.codinux.banking.fints.messages.datenelemente.implementierte.Kreditinstitutscode
import net.codinux.banking.fints.messages.datenelementgruppen.Datenelementgruppe


open class Kreditinstitutskennung(
    val bankCountryCode: Int,
    val bankCode: String,
    existenzstatus: Existenzstatus = Existenzstatus.Mandatory
)
    : Datenelementgruppe(listOf(
        Laenderkennzeichen(bankCountryCode, Existenzstatus.Mandatory),
        Kreditinstitutscode(bankCode, Existenzstatus.Mandatory)
    ), existenzstatus)