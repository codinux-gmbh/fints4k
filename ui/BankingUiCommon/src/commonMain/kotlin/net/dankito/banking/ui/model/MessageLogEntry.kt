package net.dankito.banking.ui.model

import net.dankito.utils.multiplatform.Date


open class MessageLogEntry(
    open val message: String,
    open val type: MessageLogEntryType,
    open val time: Date,
    open val bank: TypedBankData,
    open val account: TypedBankAccount?
) {

    override fun toString(): String {
        return message
    }

}