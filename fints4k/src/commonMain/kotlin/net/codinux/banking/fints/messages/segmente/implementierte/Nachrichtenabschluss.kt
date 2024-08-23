package net.codinux.banking.fints.messages.segmente.implementierte

import net.codinux.banking.fints.messages.datenelemente.implementierte.Nachrichtennummer
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.Segmentkopf
import net.codinux.banking.fints.messages.segmente.Segment
import net.codinux.banking.fints.messages.segmente.id.MessageSegmentId
import net.codinux.banking.fints.model.DialogContext


/**
 * Dieses Segment beendet alle Kunden- und Kreditinstitutsnachrichten.
 */
open class Nachrichtenabschluss(
    segmentNumber: Int,
    dialogContext: DialogContext

) : Segment(listOf(
        Segmentkopf(MessageSegmentId.MessageEnding, 1, segmentNumber),
        Nachrichtennummer(dialogContext.messageNumber)
))