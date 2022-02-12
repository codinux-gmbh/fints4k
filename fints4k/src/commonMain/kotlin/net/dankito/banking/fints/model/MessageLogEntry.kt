package net.dankito.banking.fints.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.dankito.banking.fints.log.MessageContext


open class MessageLogEntry(
    open val type: MessageLogEntryType,
    open val message: String,
    open val context: MessageContext,
    open val time: Instant = Clock.System.now()
) {

    override fun toString(): String {
        return "$type $message"
    }

}