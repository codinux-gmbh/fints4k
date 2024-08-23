package net.codinux.banking.fints.messages.datenelemente.implementierte.signatur

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Identifikation


/**
 * Code, welcher die (Kommunikations-)Partei identifiziert. Bei Verwendung des
 * RAH-Verfahrens ist die Kundensystem-ID einzustellen.
 */
open class IdentifizierungDerPartei(identification: String) : Identifikation(identification, Existenzstatus.Optional)