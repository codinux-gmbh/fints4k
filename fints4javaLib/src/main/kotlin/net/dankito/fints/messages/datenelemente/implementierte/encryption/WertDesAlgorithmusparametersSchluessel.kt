package net.dankito.fints.messages.datenelemente.implementierte.encryption

import net.dankito.fints.messages.Existenzstatus
import net.dankito.fints.messages.datenelemente.basisformate.BinaerDatenelement


/**
 * Verschlüsselter Nachrichtenschlüssel für den kryptographischen Algorithmusparameter.
 */
open class WertDesAlgorithmusparametersSchluessel(key: ByteArray) : BinaerDatenelement(key, Existenzstatus.Mandatory, 512) {

    companion object {
        val FinTsMock = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    }

}