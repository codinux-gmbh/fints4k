package net.dankito.banking.fints.model

import co.touchlab.stately.concurrency.AtomicInt
import net.dankito.banking.fints.callback.FinTsClientCallback
import net.dankito.banking.fints.log.IMessageLogAppender
import net.dankito.banking.fints.log.MessageContext
import net.dankito.banking.fints.log.MessageLogCollector
import net.dankito.banking.fints.messages.datenelemente.implementierte.signatur.VersionDesSicherheitsverfahrens
import net.dankito.banking.fints.response.BankResponse
import net.dankito.banking.fints.response.ResponseParser
import net.dankito.banking.fints.transactions.IAccountTransactionsParser
import net.dankito.banking.fints.transactions.Mt940AccountTransactionsParser
import net.dankito.banking.fints.transactions.mt940.Mt940Parser
import kotlin.reflect.KClass


open class JobContext(
    open val type: JobContextType,
    open val callback: FinTsClientCallback,
    product: ProductData,
    bank: BankData,
    /**
     * Only set if the current context is for a specific account (like get account's transactions).
     */
    open val account: AccountData? = null,
    protected open val messageLogCollector: MessageLogCollector = MessageLogCollector()
) : MessageBaseData(bank, product), IMessageLogAppender {

    companion object {
        private var JobCount = AtomicInt(0) // this variable is accessed from multiple threads, so make it thread safe
    }


    protected open val _dialogs = mutableListOf<DialogContext>()

    open val mt940Parser: IAccountTransactionsParser = Mt940AccountTransactionsParser(Mt940Parser(this), this)

    open val responseParser: ResponseParser = ResponseParser(logAppender = this)

    open val messageLogWithoutSensitiveData: List<MessageLogEntry>
        get() = messageLogCollector.messageLogWithoutSensitiveData


    open var dialog: DialogContext = DialogContext() // create null value so that variable is not null
        protected set

    open val dialogs: List<DialogContext>
        get() = ArrayList(_dialogs) // create a copy


    protected open val jobNumber: Int = JobCount.incrementAndGet()

    protected open var dialogNumber: Int = 0


    open fun startNewDialog(closeDialog: Boolean = true, dialogId: String = DialogContext.InitialDialogId,
                       versionOfSecurityProcedure: VersionDesSicherheitsverfahrens = VersionDesSicherheitsverfahrens.Version_2,
                       chunkedResponseHandler: ((BankResponse) -> Unit)? = dialog.chunkedResponseHandler) : DialogContext {

        val newDialogContext = DialogContext(closeDialog, dialogId, chunkedResponseHandler)

        this.versionOfSecurityProcedure = versionOfSecurityProcedure

        this.dialog = newDialogContext

        this._dialogs.add(newDialogContext)

        this.dialogNumber++

        return newDialogContext
    }


    open fun addMessageLog(type: MessageLogEntryType, message: String) {
        messageLogCollector.addMessageLog(type, message, createMessageContext())
    }

    override fun logError(loggingClass: KClass<*>, message: String, e: Exception?) {
        messageLogCollector.logError(loggingClass, message, createMessageContext(), e)
    }

    protected open fun createMessageContext(): MessageContext {
        return MessageContext(type, dialog.messageType, jobNumber, dialogNumber, dialog.messageNumber, bank, account)
    }

}