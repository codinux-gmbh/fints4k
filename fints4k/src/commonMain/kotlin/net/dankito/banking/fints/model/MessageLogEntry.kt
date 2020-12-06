package net.dankito.banking.fints.model

import net.dankito.utils.multiplatform.Date


open class MessageLogEntry(
    open val message: String,
    open val type: MessageLogEntryType,
    open val time: Date,
    open val bank: BankData
) {

    override fun toString(): String {
        return "$type $message"
    }

}