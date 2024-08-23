package net.codinux.banking.fints.messages.datenelemente.implementierte

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.ZiffernDatenelement


/**
 * Größe der Nachricht (nach Verschlüsselung und Komprimierung) in Byte.
 * Das DE ist mit führenden Nullen auf die vorgegebene feste Länge aufzufüllen.
 * Dies ist erforderlich, damit die Nachrichtenlänge nicht mit der Länge des DE variiert.
 */
open class Nachrichtengroesse(messageSize: Int) : ZiffernDatenelement(messageSize, 12, Existenzstatus.Mandatory)