package net.dankito.fints.messages.segmente.implementierte.sepa

import net.dankito.fints.messages.segmente.id.CustomerSegmentId
import net.dankito.fints.model.BankTransferData
import net.dankito.fints.model.CustomerData


open class SepaEinzelueberweisung(
    segmentNumber: Int,
    debitor: CustomerData,
    debitorBic: String,
    data: BankTransferData,
    messageCreator: ISepaMessageCreator = SepaMessageCreator()
)
    : SepaSegment(
    segmentNumber,
    CustomerSegmentId.SepaBankTransfer,
    1,
    "urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.001.03", // TODO: read from HISPAS
    "pain.001.001.03.xml",
    data.creditorIban,
    data.creditorBic,
    mapOf(
        SepaMessageCreator.NumberOfTransactionsKey to "1", // TODO: may someday support more then one transaction per file
        "DebitorName" to debitor.name,
        "DebitorIban" to debitor.iban!!,
        "DebitorBic" to debitorBic,
        "CreditorName" to data.creditorName,
        "CreditorIban" to data.creditorIban,
        "CreditorBic" to data.creditorBic,
        "Amount" to data.amount.toString(),
        "Usage" to data.usage,
        "RequestedExecutionDate" to RequestedExecutionDateValueForNotScheduledTransfers
    ),
    messageCreator
) {

    companion object {
        /**
         * In das Mussfeld RequestedExecutionDate <ReqdExctnDt> ist der 1999-01-01 einzustellen.
         */
        const val RequestedExecutionDateValueForNotScheduledTransfers = "1999-01-01"
    }
}