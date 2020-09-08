package net.dankito.banking.fints.model

import net.dankito.banking.fints.messages.MessageBuilderResult
import net.dankito.banking.fints.messages.datenelemente.implementierte.signatur.VersionDesSicherheitsverfahrens
import net.dankito.banking.fints.response.Response


open class DialogContext(
    bank: BankData,
    product: ProductData,
    var abortIfTanIsRequired: Boolean = false,
    var currentMessage: MessageBuilderResult? = null,
    var dialogId: String = InitialDialogId,
    var response: Response? = null,
    var didBankCloseDialog: Boolean = false,
    versionOfSecurityProcedure: VersionDesSicherheitsverfahrens = VersionDesSicherheitsverfahrens.Version_2,
    var previousMessageInDialog: MessageBuilderResult? = null, // for PinTan almost always the case except for getting a user's TAN procedures
    var chunkedResponseHandler: ((Response) -> Unit)? = null
) : MessageBaseData(bank, product, versionOfSecurityProcedure) {

    companion object {
        const val InitialDialogId = "0"

        const val InitialMessageNumber = 0
    }

    open var messageNumber: Int = InitialMessageNumber
        protected set

    open fun increaseMessageNumber() {
        messageNumber++
    }

}