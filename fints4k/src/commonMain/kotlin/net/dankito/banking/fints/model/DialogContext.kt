package net.dankito.banking.fints.model

import net.dankito.banking.fints.messages.MessageBuilderResult
import net.dankito.banking.fints.response.BankResponse


open class DialogContext(
    open val closeDialog: Boolean = true,
    open var dialogId: String = InitialDialogId,
    open var chunkedResponseHandler: ((BankResponse) -> Unit)? = null
) {

    companion object {
        const val InitialDialogId = "0"

        const val InitialMessageNumber = 0
    }


    open var messageType: MessageType = MessageType.AnonymousDialogInit
        protected set

    open var currentMessage: MessageBuilderResult? = null
        protected set

    open var previousMessageInDialog: MessageBuilderResult? = null
        protected set

    open var messageNumber: Int = InitialMessageNumber
        protected set

    open var abortIfTanIsRequired: Boolean = false

    open var response: BankResponse? = null

    open var didBankCloseDialog: Boolean = false


    open fun increaseMessageNumber() {
        messageNumber++
    }

    open fun setNextMessage(type: MessageType, message: MessageBuilderResult) {
        this.messageType = type

        this.previousMessageInDialog = this.currentMessage

        this.currentMessage = message
    }

}