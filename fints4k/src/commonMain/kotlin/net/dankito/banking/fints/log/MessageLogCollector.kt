package net.dankito.banking.fints.log

import net.dankito.banking.fints.model.BankData
import net.dankito.banking.fints.model.MessageLogEntry
import net.dankito.banking.fints.model.MessageLogEntryType
import net.dankito.utils.multiplatform.log.Logger
import net.dankito.utils.multiplatform.log.LoggerFactory
import net.dankito.utils.multiplatform.Date
import net.dankito.utils.multiplatform.StackTraceHelper
import net.dankito.utils.multiplatform.StringHelper
import net.dankito.utils.multiplatform.getInnerException
import kotlin.reflect.KClass


open class MessageLogCollector {

    companion object {
        val FindAccountTransactionsStartRegex = Regex("^HIKAZ:\\d:\\d:\\d\\+@\\d+@", RegexOption.MULTILINE)
        val FindAccountTransactionsEndRegex = Regex("^-'", RegexOption.MULTILINE)

        const val MaxCountStackTraceElements = 15

        private const val NewLine = "\r\n"

        private val log = LoggerFactory.getLogger(MessageLogCollector::class)
    }


    protected open val messageLog = ArrayList<MessageLogEntry>() // TODO: make thread safe like with CopyOnWriteArrayList

    // in either case remove sensitive data after response is parsed as otherwise some information like account holder name and accounts may is not set yet on BankData
    open val messageLogWithoutSensitiveData: List<MessageLogEntry>
        get() = messageLog.map { MessageLogEntry(it.type, safelyRemoveSensitiveDataFromMessage(it.message, it.context.bank), it.context, it.time) }


    protected open val stackTraceHelper = StackTraceHelper()


    open fun addMessageLog(type: MessageLogEntryType, message: String, context: MessageContext) {
        val messageToLog = createMessage(type, prettyPrintHbciMessage(message), context, true)

        messageLog.add(MessageLogEntry(type, messageToLog, context))

        log.debug { messageToLog }
    }

    open fun logError(loggingClass: KClass<*>, message: String, context: MessageContext, e: Exception? = null) {
        val type = MessageLogEntryType.Error
        val messageToLog = createMessage(type, message, context, false)

        if (e != null) {
            getLogger(loggingClass).error(e) { messageToLog }
        } else {
            getLogger(loggingClass).error(messageToLog)
        }

        val errorStackTrace = if (e != null) NewLine + getStackTrace(e) else ""

        messageLog.add(MessageLogEntry(type, messageToLog + errorStackTrace, context))
    }


    protected open fun createMessage(type: MessageLogEntryType, message: String, context: MessageContext, separateWithNewLine: Boolean): String {
        val messageTrace = createMessageTraceString(type, context)

        return "$messageTrace${ if (separateWithNewLine) NewLine else " " }$message"
    }

    protected open fun createMessageTraceString(type: MessageLogEntryType, context: MessageContext): String {
        return "${twoDigits(context.jobNumber)}_${twoDigits(context.dialogNumber)}_${twoDigits(context.messageNumber)}_" +
                "${context.bank.bankCode}_${context.bank.customerId}" +
                "${ context.account?.let { "_${it.accountIdentifier}" } ?: "" }_" +
                "${context.jobType.name}_${context.dialogType.name} " +
                "${getMessageTypeString(type)}:"
    }

    protected open fun twoDigits(number: Int): String {
        return StringHelper.format("%02d", number)
    }

    protected open fun getMessageTypeString(type: MessageLogEntryType): String {
        return when (type) {
            MessageLogEntryType.Sent -> "Sending message"
            MessageLogEntryType.Received -> "Received message"
            MessageLogEntryType.Error -> "Error"
        }
    }

    protected open fun prettyPrintHbciMessage(message: String): String {
        return message.replace("'", "'$NewLine")
    }


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


    protected open fun getStackTrace(e: Exception): String {
        val innerException = e.getInnerException()

        return stackTraceHelper.getStackTrace(innerException, MaxCountStackTraceElements)
    }

    protected open fun getLogger(loggingClass: KClass<*>): Logger {
        return LoggerFactory.getLogger(loggingClass)
    }

}