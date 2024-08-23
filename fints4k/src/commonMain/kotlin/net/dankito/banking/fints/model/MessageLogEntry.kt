package net.dankito.banking.fints.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.dankito.banking.fints.log.MessageContext
import net.dankito.banking.fints.response.segments.ReceivedSegment


open class MessageLogEntry(
    open val type: MessageLogEntryType,
    open val context: MessageContext,
    open val messageTrace: String,
    open val message: String,
    open val error: Throwable? = null,
    /**
     * Parsed received segments.
     *
     * Is only set if [type] is set to [MessageLogEntryType.Received] and response parsing was successful.
     */
    open val parsedSegments: List<ReceivedSegment> = emptyList(),
    open val time: Instant = Clock.System.now()
) {

    val messageIncludingMessageTrace: String
        get() = messageTrace + "\n" + message

    override fun toString(): String {
        return "$type $message"
    }

}