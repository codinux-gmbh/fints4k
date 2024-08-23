package net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.AlphanumerischesDatenelement


/**
 * Format: J bzw. N (in Großbuchstaben)
 *
 * Hat das DE den Status „Kann“, so gilt bei Auslassung der Standardwert „N“.
 */
open class JaNein(yes: Boolean?, existenzstatus: Existenzstatus)
    : AlphanumerischesDatenelement(yes?.let { if (yes == true) "J" else "N" }, existenzstatus, 1)