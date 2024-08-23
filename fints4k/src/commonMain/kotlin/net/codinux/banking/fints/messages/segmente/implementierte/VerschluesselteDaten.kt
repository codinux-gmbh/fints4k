package net.codinux.banking.fints.messages.segmente.implementierte

import net.codinux.banking.fints.messages.datenelemente.implementierte.encryption.PinTanVerschluesselteDatenDatenelement
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.Segmentkopf
import net.codinux.banking.fints.messages.segmente.Segment
import net.codinux.banking.fints.messages.segmente.id.MessageSegmentId


/**
 * Dieses Segment enthält die verschlüsselten (und komprimierten) Daten.
 */
open class VerschluesselteDaten(
    payload: String

) : Segment(listOf(
    Segmentkopf(MessageSegmentId.EncryptionData, 1, 999),
    PinTanVerschluesselteDatenDatenelement(payload)
))