package net.codinux.banking.fints.model

import kotlinx.datetime.Instant
import net.codinux.banking.fints.extensions.nowExt
import net.codinux.banking.fints.log.MessageContext
import net.codinux.banking.fints.response.segments.ReceivedSegment


open class MessageLogEntry(
    open val type: MessageLogEntryType,
    open val context: MessageContext,
    open val messageTrace: String,
    open val message: String,
    open val messageWithoutSensitiveData: String? = null,
    open val error: Throwable? = null,
    /**
     * Parsed received segments.
     *
     * Is only set if [type] is set to [MessageLogEntryType.Received] and response parsing was successful.
     */
    open val parsedSegments: List<ReceivedSegment> = emptyList(),
    open val time: Instant = Instant.nowExt()
) {

    val messageIncludingMessageTrace: String
        get() = messageTrace + "\n" + message

    override fun toString(): String {
        return "$context $type $message"
    }

}