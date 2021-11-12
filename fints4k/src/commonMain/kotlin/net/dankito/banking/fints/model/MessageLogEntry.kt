package net.dankito.banking.fints.model

import net.dankito.banking.fints.log.MessageContext
import net.dankito.utils.multiplatform.Date


open class MessageLogEntry(
    open val type: MessageLogEntryType,
    open val message: String,
    open val time: Date,
    open val context: MessageContext
) {

    override fun toString(): String {
        return "$type $message"
    }

}