package net.dankito.banking.fints.model

import net.dankito.banking.fints.callback.FinTsClientCallback
import net.dankito.banking.fints.messages.datenelemente.implementierte.signatur.VersionDesSicherheitsverfahrens
import net.dankito.banking.fints.response.BankResponse


open class JobContext(
    open val type: JobContextType,
    open val callback: FinTsClientCallback,
    product: ProductData,
    bank: BankData,
    /**
     * Only set if the current context is for a specific account (like get account's transactions).
     */
    open val account: AccountData? = null
) : MessageBaseData(bank, product) {


    protected open val _dialogs = mutableListOf<DialogContext>()


    open var dialog: DialogContext = DialogContext() // create null value so that variable is not null
        protected set

    open val dialogs: List<DialogContext>
        get() = ArrayList(_dialogs) // create a copy


    fun startNewDialog(closeDialog: Boolean = true, dialogId: String = DialogContext.InitialDialogId,
                       versionOfSecurityProcedure: VersionDesSicherheitsverfahrens = VersionDesSicherheitsverfahrens.Version_2,
                       chunkedResponseHandler: ((BankResponse) -> Unit)? = null) : DialogContext {
        val newDialogContext = DialogContext(closeDialog, dialogId = dialogId, chunkedResponseHandler = chunkedResponseHandler)

        this.versionOfSecurityProcedure = versionOfSecurityProcedure

        this.dialog = newDialogContext

        this._dialogs.add(newDialogContext)

        return newDialogContext
    }

}