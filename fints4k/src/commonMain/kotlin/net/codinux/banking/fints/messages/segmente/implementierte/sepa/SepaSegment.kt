package net.codinux.banking.fints.messages.segmente.implementierte.sepa

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.AlphanumerischesDatenelement
import net.codinux.banking.fints.messages.datenelemente.implementierte.sepa.SepaMessage
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.Segmentkopf
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.account.KontoverbindungInternational
import net.codinux.banking.fints.messages.segmente.Segment
import net.codinux.banking.fints.messages.segmente.id.ISegmentId
import net.codinux.banking.fints.model.AccountData


open class SepaSegment(
    segmentNumber: Int,
    segmentId: ISegmentId,
    segmentVersion: Int,
    sepaDescriptorUrn: String,
    messageTemplate: PaymentInformationMessages,
    account: AccountData,
    bic: String,
    replacementStrings: Map<String, String>,
    messageCreator: ISepaMessageCreator = SepaMessageCreator()
)
    : Segment(listOf(
        Segmentkopf(segmentId, segmentVersion, segmentNumber),
        KontoverbindungInternational(account, bic),
        object : AlphanumerischesDatenelement(sepaDescriptorUrn, Existenzstatus.Mandatory, 256) { },
        SepaMessage(messageTemplate, replacementStrings, messageCreator)
))