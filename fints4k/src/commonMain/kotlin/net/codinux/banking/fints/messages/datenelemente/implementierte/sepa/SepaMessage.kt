package net.codinux.banking.fints.messages.datenelemente.implementierte.sepa

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.BinaerDatenelement
import net.codinux.banking.fints.messages.segmente.implementierte.sepa.ISepaMessageCreator
import net.codinux.banking.fints.messages.segmente.implementierte.sepa.PaymentInformationMessages


open class SepaMessage(
    messageTemplate: PaymentInformationMessages,
    replacementStrings: Map<String, String>,
    messageCreator: ISepaMessageCreator
)
    : BinaerDatenelement(messageCreator.createXmlFile(messageTemplate, replacementStrings), Existenzstatus.Mandatory) {

}


