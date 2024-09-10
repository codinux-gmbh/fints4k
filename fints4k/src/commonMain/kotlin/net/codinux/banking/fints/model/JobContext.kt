package net.codinux.banking.fints.model

import kotlinx.atomicfu.atomic
import net.codinux.banking.fints.callback.FinTsClientCallback
import net.codinux.banking.fints.config.FinTsClientConfiguration
import net.codinux.banking.fints.log.IMessageLogAppender
import net.codinux.banking.fints.log.MessageContext
import net.codinux.banking.fints.log.MessageLogCollector
import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.VersionDesSicherheitsverfahrens
import net.codinux.banking.fints.response.BankResponse
import net.codinux.banking.fints.response.ResponseParser
import net.codinux.banking.fints.response.segments.ReceivedSegment
import net.codinux.banking.fints.transactions.IAccountTransactionsParser
import net.codinux.banking.fints.transactions.Mt940AccountTransactionsParser
import net.codinux.banking.fints.transactions.mt940.Mt940Parser
import kotlin.reflect.KClass


open class JobContext(
    open val type: JobContextType,
    open val callback: FinTsClientCallback,
    open val config: FinTsClientConfiguration,
    bank: BankData,
    /**
     * Only set if the current context is for a specific account (like get account's transactions).
     */
    open val account: AccountData? = null,
    open val preferredTanMethods: List<TanMethodType>? = null,
    tanMethodsNotSupportedByApplication: List<TanMethodType>? = null,
    open val preferredTanMedium: String? = null,
    protected open val messageLogCollector: MessageLogCollector = MessageLogCollector(callback, config.options)
) : MessageBaseData(bank, config.options.product), IMessageLogAppender {

    companion object {
        private val JobCount = atomic(0) // this variable is accessed from multiple threads, so make it thread safe
    }


    protected open val _dialogs = mutableListOf<DialogContext>()

    open val tanMethodsNotSupportedByApplication: List<TanMethodType> = tanMethodsNotSupportedByApplication ?: emptyList()

    open val mt940Parser: IAccountTransactionsParser = Mt940AccountTransactionsParser(Mt940Parser(this), this)

    open val responseParser: ResponseParser = ResponseParser(logAppender = this)

    open val messageLog: List<MessageLogEntry>
        get() = messageLogCollector.messageLog


    open var dialog: DialogContext = DialogContext() // create null value so that variable is not null
        protected set

    open val dialogs: List<DialogContext>
        get() = ArrayList(_dialogs) // create a copy


    protected open val jobNumber: Int = JobCount.incrementAndGet()

    protected open var dialogNumber: Int = 0


    open fun startNewDialog(closeDialog: Boolean = config.options.closeDialogs, dialogId: String = DialogContext.InitialDialogId,
                       versionOfSecurityProcedure: VersionDesSicherheitsverfahrens = VersionDesSicherheitsverfahrens.Version_2,
                       chunkedResponseHandler: ((BankResponse) -> Unit)? = dialog.chunkedResponseHandler) : DialogContext {

        val newDialogContext = DialogContext(closeDialog, dialogId, chunkedResponseHandler)

        this.versionOfSecurityProcedure = versionOfSecurityProcedure

        this.dialog = newDialogContext

        this._dialogs.add(newDialogContext)

        this.dialogNumber++

        return newDialogContext
    }


    open fun addMessageLog(type: MessageLogEntryType, message: String, parsedSegments: List<ReceivedSegment> = emptyList()) {
        messageLogCollector.addMessageLog(type, message, createMessageContext(), parsedSegments)
    }

    override fun logError(loggingClass: KClass<*>, message: String, e: Throwable?) {
        messageLogCollector.logError(loggingClass, message, createMessageContext(), e)
    }

    protected open fun createMessageContext(): MessageContext {
        return MessageContext(type, dialog.messageType, jobNumber, dialogNumber, dialog.messageNumber, bank, account)
    }

}