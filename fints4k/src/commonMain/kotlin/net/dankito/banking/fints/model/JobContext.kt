package net.dankito.banking.fints.model

import net.dankito.banking.fints.callback.FinTsClientCallback
import net.dankito.banking.fints.log.IMessageLogAppender
import net.dankito.banking.fints.log.MessageLogCollector
import net.dankito.banking.fints.messages.datenelemente.implementierte.signatur.VersionDesSicherheitsverfahrens
import net.dankito.banking.fints.response.BankResponse
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


    protected open val _dialogs = mutableListOf<DialogContext>()

    open val mt940Parser: IAccountTransactionsParser = Mt940AccountTransactionsParser(Mt940Parser(this), this)

    open val messageLogWithoutSensitiveData: List<MessageLogEntry>
        get() = messageLogCollector.messageLogWithoutSensitiveData


    open var dialog: DialogContext = DialogContext() // create null value so that variable is not null
        protected set

    open val dialogs: List<DialogContext>
        get() = ArrayList(_dialogs) // create a copy


    open fun startNewDialog(closeDialog: Boolean = true, dialogId: String = DialogContext.InitialDialogId,
                       versionOfSecurityProcedure: VersionDesSicherheitsverfahrens = VersionDesSicherheitsverfahrens.Version_2,
                       chunkedResponseHandler: ((BankResponse) -> Unit)? = null) : DialogContext {
        val newDialogContext = DialogContext(closeDialog, dialogId = dialogId, chunkedResponseHandler = chunkedResponseHandler)

        this.versionOfSecurityProcedure = versionOfSecurityProcedure

        this.dialog = newDialogContext

        this._dialogs.add(newDialogContext)

        return newDialogContext
    }


    open fun addMessageLog(type: MessageLogEntryType, message: String) {
        messageLogCollector.addMessageLog(bank, type, message)
    }

    override fun logError(loggingClass: KClass<*>, message: String, e: Exception?) {
        messageLogCollector.logError(loggingClass, bank, message, e)
    }

}