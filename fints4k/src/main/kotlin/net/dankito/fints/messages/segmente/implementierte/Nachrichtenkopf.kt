package net.dankito.fints.messages.segmente.implementierte

import net.dankito.fints.messages.datenelemente.implementierte.*
import net.dankito.fints.messages.datenelementgruppen.implementierte.Segmentkopf
import net.dankito.fints.messages.segmente.Segment
import net.dankito.fints.messages.segmente.id.MessageSegmentId
import net.dankito.fints.model.DialogContext


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