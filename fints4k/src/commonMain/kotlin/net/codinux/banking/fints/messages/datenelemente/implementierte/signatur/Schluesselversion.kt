package net.codinux.banking.fints.messages.datenelemente.implementierte.signatur

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.NumerischesDatenelement


/**
 * Versionsnummer des entsprechenden Schlüssels.
 */
open class Schluesselversion(version: Int) : NumerischesDatenelement(version, 3, Existenzstatus.Mandatory) {

    companion object {
        const val PinTanDefaultValue = 0
    }

}