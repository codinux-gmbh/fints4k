package net.codinux.banking.fints.messages.segmente.implementierte

import net.codinux.banking.fints.messages.datenelemente.implementierte.*
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.Segmentkopf
import net.codinux.banking.fints.messages.segmente.Segment
import net.codinux.banking.fints.messages.segmente.id.MessageSegmentId
import net.codinux.banking.fints.model.DialogContext


open class Nachrichtenkopf(
    segmentNumber: Int,
    messageSize: Int,
    dialogContext: DialogContext

) : Segment(listOf(
        Segmentkopf(MessageSegmentId.MessageHeader, 3, segmentNumber),
        Nachrichtengroesse(messageSize),
        HbciVersionDatenelement(HbciVersion.FinTs_3_0_0),
        DialogId(dialogContext.dialogId),
        Nachrichtennummer(dialogContext.messageNumber)
))