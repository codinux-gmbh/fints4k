package net.codinux.banking.fints.messages.datenelemente.implementierte.encryption

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.BinaerDatenelement


/**
 * Verschlüsselter Nachrichtenschlüssel für den kryptographischen Algorithmusparameter.
 */
open class WertDesAlgorithmusparametersSchluessel(key: ByteArray) : BinaerDatenelement(key, Existenzstatus.Mandatory, 512) {

    companion object {
        val FinTsMock = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    }

}