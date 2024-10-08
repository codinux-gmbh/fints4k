package net.codinux.banking.fints.messages.datenelementgruppen.implementierte.signatur

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.BezeichnerFuerHashalgorithmusparameter
import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.Hashalgorithmus
import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.HashalgorithmusKodiert
import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.VerwendungDesHashalgorithmusKodiert
import net.codinux.banking.fints.messages.datenelementgruppen.Datenelementgruppe


/**
 * Angaben zu einem kryptographischen Algorithmus, seinen Operationsmodus, sowie dessen Einsatz.
 *
 *
 * Abweichende Belegung für PIN/TAN Verfahren (Dokument Sicherheitsverfahren PIN/TAN, B.9.5 DEG „Hashalgorithmus“, S. 58):
 *
 * Wert des Hashalgorithmusparameters
 *      Dieses Feld darf nicht belegt werden.
 */
open class HashalgorithmusDatenelementgruppe
    : Datenelementgruppe(listOf(
        VerwendungDesHashalgorithmusKodiert(),
        HashalgorithmusKodiert(Hashalgorithmus.Gegenseitig_vereinbart), // allowed: 3, 4, 5, 6
        BezeichnerFuerHashalgorithmusparameter()
), Existenzstatus.Mandatory)