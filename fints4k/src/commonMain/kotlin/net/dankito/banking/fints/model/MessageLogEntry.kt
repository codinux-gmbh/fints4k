package net.dankito.banking.fints.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.dankito.banking.fints.log.MessageContext


open class MessageLogEntry(
    open val type: MessageLogEntryType,
    open val context: MessageContext,
    open val messageTrace: String,
    open val message: String,
    open val error: Throwable? = null,
    open val time: Instant = Clock.System.now()
) {

    val messageIncludingMessageTrace: String
        get() = messageTrace + "\n" + message

    override fun toString(): String {
        return "$type $message"
    }

}