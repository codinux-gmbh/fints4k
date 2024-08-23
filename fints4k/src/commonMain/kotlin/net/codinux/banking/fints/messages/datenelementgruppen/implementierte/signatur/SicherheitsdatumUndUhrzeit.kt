package net.codinux.banking.fints.messages.datenelementgruppen.implementierte.signatur

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Datum
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Uhrzeit
import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.DatumUndZeitbezeichner
import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.DatumUndZeitbezeichnerKodiert
import net.codinux.banking.fints.messages.datenelementgruppen.Datenelementgruppe


open class SicherheitsdatumUndUhrzeit(date: Int, time: Int)
    : Datenelementgruppe(listOf(
        DatumUndZeitbezeichnerKodiert(DatumUndZeitbezeichner.Sicherheitszeitstempel), // Als Bezeichner wird „1“ eingestellt, da es sich um einen Sicherheitszeitstempel handelt.
        Datum(date, Existenzstatus.Optional),
        Uhrzeit(time, Existenzstatus.Optional)
), Existenzstatus.Mandatory)