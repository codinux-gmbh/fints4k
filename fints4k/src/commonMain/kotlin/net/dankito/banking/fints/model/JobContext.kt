package net.dankito.banking.fints.model

import net.dankito.banking.fints.callback.FinTsClientCallback


class JobContext(
    val type: JobContextType,
    val callback: FinTsClientCallback,
    val bank: BankData,
    /**
     * Only set if the current context is for a specific account (like get account's transactions).
     */
    val account: AccountData? = null
) {


    lateinit var dialog: DialogContext


    fun startNewDialog(dialog: DialogContext) {
        this.dialog = dialog
    }

}