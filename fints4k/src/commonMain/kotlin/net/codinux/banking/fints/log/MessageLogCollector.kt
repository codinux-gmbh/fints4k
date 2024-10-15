package net.codinux.banking.fints.log

import net.codinux.log.LoggerFactory
import net.codinux.log.logger
import net.codinux.banking.fints.callback.FinTsClientCallback
import net.codinux.banking.fints.config.FinTsClientOptions
import net.codinux.banking.fints.model.BankData
import net.codinux.banking.fints.model.MessageLogEntry
import net.codinux.banking.fints.model.MessageLogEntryType
import net.codinux.banking.fints.extensions.getInnerException
import net.codinux.banking.fints.extensions.nthIndexOf
import net.codinux.banking.fints.extensions.toStringWithMinDigits
import net.codinux.banking.fints.response.segments.ReceivedSegment
import net.codinux.banking.fints.util.FinTsUtils
import kotlin.reflect.KClass


open class MessageLogCollector(
    private val callback: FinTsClientCallback,
    private val options: FinTsClientOptions = FinTsClientOptions(),
    private val finTsUtils: FinTsUtils = FinTsUtils()
) {

    companion object {
        val FindAccountTransactionsStartRegex = Regex("^HIKAZ:\\d:\\d:\\d\\+@\\d+@", RegexOption.MULTILINE)
        val FindAccountTransactionsEndRegex = Regex("^-'", RegexOption.MULTILINE)

        const val MaxCountStackTraceElements = 15

        internal const val NewLine = "\r\n"

        private val log by logger()
    }


    protected open val _messageLog = ArrayList<MessageLogEntry>() // TODO: make thread safe like with CopyOnWriteArrayList

    // in either case remove sensitive data after response is parsed as otherwise some information like account holder name and accounts may is not set yet on BankData
    open val messageLog: List<MessageLogEntry>
        // safe CPU cycles by only removing sensitive data if messageLog is really requested
        get() = _messageLog.map {
            val message = createMessageForLog(it)
            val messageWithoutSensitiveData = if (options.removeSensitiveDataFromMessageLog) {
                safelyRemoveSensitiveDataFromMessage(message, it.context.bank)
            } else {
                message
            }

            MessageLogEntry(it.type, it.context, it.messageTrace, message, messageWithoutSensitiveData, it.error, it.parsedSegments, it.time)
        }

    private fun createMessageForLog(logEntry: MessageLogEntry): String =
        if (logEntry.type == MessageLogEntryType.Error) {
            logEntry.message + (if (logEntry.error != null) NewLine + getStackTrace(logEntry.error!!) else "")
        } else {
            logEntry.message
        }


    open fun addMessageLog(type: MessageLogEntryType, message: String, context: MessageContext, parsedSegments: List<ReceivedSegment> = emptyList()) {
        val messageTrace = createMessageTraceString(type, context)
        val prettyPrintMessage = prettyPrintMessageIfRequired(message)

        log.debug { "$messageTrace\n$prettyPrintMessage" }

        addMessageLogEntry(type, context, messageTrace, prettyPrintMessage, null, parsedSegments)
    }

    open fun logError(loggingClass: KClass<*>, message: String, context: MessageContext, e: Throwable? = null) {
        val type = MessageLogEntryType.Error
        val messageTrace = createMessageTraceString(type, context)
        val prettyPrintMessage = prettyPrintFinTsMessage(message) // error messages almost always get logged / displayed -> pretty print

        LoggerFactory.getLogger(loggingClass).error(e) { "$messageTrace\n$prettyPrintMessage" }

        addMessageLogEntry(type, context, messageTrace, prettyPrintMessage, e)
    }

    protected open fun addMessageLogEntry(type: MessageLogEntryType, context: MessageContext, messageTrace: String, message: String, error: Throwable? = null, parsedSegments: List<ReceivedSegment> = emptyList()) {
        if (options.collectMessageLog || options.fireCallbackOnMessageLogs) {
            val newEntry = MessageLogEntry(type, context, messageTrace, message, null, error, parsedSegments)

            if (options.collectMessageLog) {
                _messageLog.add(newEntry)
            }

            if (options.fireCallbackOnMessageLogs) {
                callback.messageLogAdded(newEntry)
            }
        }
    }


    protected open fun createMessageTraceString(type: MessageLogEntryType, context: MessageContext): String {
        return "${twoDigits(context.jobNumber)}_${twoDigits(context.dialogNumber)}_${twoDigits(context.messageNumber)}_" +
                "${context.bank.bankCode}_${context.bank.customerId}" +
                "${ context.account?.let { "_${it.accountIdentifier}" } ?: "" }_" +
                "${context.jobType.name}_${context.messageType.name} " +
                "${getMessageTypeString(type)}:"
    }

    protected open fun twoDigits(number: Int): String {
        return number.toStringWithMinDigits(2)
    }

    protected open fun getMessageTypeString(type: MessageLogEntryType): String {
        return when (type) {
            MessageLogEntryType.Sent -> "01 Sending message"
            MessageLogEntryType.Received -> "02 Received message"
            MessageLogEntryType.Error -> "03 Error"
        }
    }

    protected open fun prettyPrintMessageIfRequired(message: String): String =
        if (options.collectMessageLog || options.fireCallbackOnMessageLogs || log.isDebugEnabled) { // only use CPU cycles if message will ever be used / displayed
            prettyPrintFinTsMessage(message)
        } else {
            message
        }

    protected open fun prettyPrintFinTsMessage(message: String): String =
        finTsUtils.prettyPrintFinTsMessage(message)


    protected open fun safelyRemoveSensitiveDataFromMessage(message: String, bank: BankData?): String {
        try {
            return removeSensitiveDataFromMessage(message, bank)
        } catch (e: Exception) {
            return "! WARNING !${NewLine}Could not remove sensitive data!$NewLine$e$NewLine${getStackTrace(e)}$NewLine$message"
        }
    }

    protected open fun removeSensitiveDataFromMessage(message: String, bank: BankData?): String {
        if (bank == null) {
            return message
        }

        var prettyPrintMessageWithoutSensitiveData = message
            .replace(bank.customerId, "<customer_id>")
            .replace("+" + bank.pin, "+<pin>")

        if (bank.userId != bank.customerId) {
            prettyPrintMessageWithoutSensitiveData = prettyPrintMessageWithoutSensitiveData
                .replace(bank.userId, "<user_id>")
        }

        if (bank.customerName.isNotBlank()) {
            prettyPrintMessageWithoutSensitiveData = prettyPrintMessageWithoutSensitiveData
                .replace(bank.customerName, "<customer_name>", true)
        }

        bank.accounts.forEach { account ->
            prettyPrintMessageWithoutSensitiveData = prettyPrintMessageWithoutSensitiveData
                .replace(account.accountIdentifier, "<account_identifier>")

            if (account.accountHolderName.isNotBlank()) {
                prettyPrintMessageWithoutSensitiveData = prettyPrintMessageWithoutSensitiveData
                    .replace(account.accountHolderName, "<account_holder>", true)
            }
        }

        return removeAccountTransactions(prettyPrintMessageWithoutSensitiveData)
    }

    protected open fun removeAccountTransactions(message: String): String {
        FindAccountTransactionsStartRegex.find(message)?.let { startMatchResult ->
            FindAccountTransactionsEndRegex.find(message, startMatchResult.range.endInclusive)?.let { endMatchResult ->
                return message.replaceRange(IntRange(startMatchResult.range.endInclusive, endMatchResult.range.start), "<account_transactions>")
            }
        }

        return message
    }


    protected open fun getStackTrace(e: Throwable): String {
        val innerException = e.getInnerException()

        val stackTraceString = innerException.stackTraceToString()
        val indexOf16thLine = stackTraceString.nthIndexOf("\n", MaxCountStackTraceElements)

        return if (indexOf16thLine < 0) stackTraceString else stackTraceString.substring(0, indexOf16thLine)
    }

}