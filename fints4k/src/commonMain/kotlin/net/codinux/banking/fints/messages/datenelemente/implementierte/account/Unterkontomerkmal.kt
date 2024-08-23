package net.codinux.banking.fints.messages.datenelemente.implementierte.account

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Identifikation


/**
 * Unterkontonummer, falls unter einer Kontonummer verschiedene Unterkonten (z.B. Währungskonten)
 * geführt werden. Anstatt einer Kontonummer kann auch ein anderes identifizierendes Merkmal
 * angegeben werden (z.B. der entsprechende ISO-Währungscode bei Währungskonten).
 */
open class Unterkontomerkmal(attribute: String?, existenzstatus: Existenzstatus) : Identifikation(attribute, existenzstatus)