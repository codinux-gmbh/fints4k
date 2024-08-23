package net.codinux.banking.fints.messages.datenelemente.implementierte

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.AlphanumerischesDatenelement


/**
 * Version des Kundenproduktes, mit dem kundenseitig die Nachrichten erzeugt wurden.
 *
 * Die Produktversion ist verpflichtend mit aussagekräftigen Informationen über das verwendete
 * Kundenprodukt, nicht eine ggf. verwendete interne FinTS-/HBCI-Bibliothek, zu füllen, um
 * Support-Anfragen leichter beantworten zu können.
 */
open class Produktversion(version: String, existenzstatus: Existenzstatus)
    : AlphanumerischesDatenelement(version, existenzstatus, 5)