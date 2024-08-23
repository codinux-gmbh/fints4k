package net.codinux.banking.fints.messages.segmente.implementierte

import net.codinux.banking.fints.messages.datenelemente.implementierte.DialogId
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.Segmentkopf
import net.codinux.banking.fints.messages.segmente.Segment
import net.codinux.banking.fints.messages.segmente.id.CustomerSegmentId
import net.codinux.banking.fints.model.DialogContext


class Dialogende(
    segmentNumber: Int,
    dialogContext: DialogContext

) : Segment(listOf(
        Segmentkopf(CustomerSegmentId.DialogEnd, 1, segmentNumber),
        DialogId(dialogContext.dialogId)
))