package net.dankito.banking.fints.log

import net.dankito.banking.fints.model.BankData
import net.dankito.banking.fints.model.MessageLogEntry
import net.dankito.banking.fints.model.MessageLogEntryType
import net.dankito.utils.multiplatform.log.Logger
import net.dankito.utils.multiplatform.log.LoggerFactory
import net.dankito.utils.multiplatform.Date
import net.dankito.utils.multiplatform.StackTraceHelper
import net.dankito.utils.multiplatform.getInnerException


open class MessageLogCollector {

    companion object {
        val FindAccountTransactionsStartRegex = Regex("^HIKAZ:\\d:\\d:\\d\\+@\\d+@", RegexOption.MULTILINE)
        val FindAccountTransactionsEndRegex = Regex("^-'", RegexOption.MULTILINE)

        const val MaxCountStackTraceElements = 15

        private val log = LoggerFactory.getLogger(MessageLogCollector::class)
    }


    protected open val messageLog = ArrayList<MessageLogEntry>() // TODO: make thread safe like with CopyOnWriteArrayList

    // in either case remove sensitive data after response is parsed as otherwise some information like account holder name and accounts may is not set yet on BankData
    open val messageLogWithoutSensitiveData: List<MessageLogEntry>
        get() = messageLog.map { MessageLogEntry(safelyRemoveSensitiveDataFromMessage(it.message, it.bank), it.type, it.time, it.bank) }


    protected val stackTraceHelper = StackTraceHelper()


    open fun addMessageLog(message: String, type: MessageLogEntryType, bank: BankData) {
        val timeStamp = Date()
        val prettyPrintMessage = prettyPrintHbciMessage(message)

        log.debug { "${if (type == MessageLogEntryType.Sent) "[${bank.bankCode}-${bank.customerId}] Sending" else "Received"} message:\r\n" + prettyPrintMessage }

        messageLog.add(MessageLogEntry(prettyPrintMessage, type, timeStamp, bank))
    }

    protected open fun prettyPrintHbciMessage(message: String): String {
        return message.replace("'", "'\r\n")
    }


    open fun logError(message: String, e: Exception? = null, logger: Logger? = null, bank: BankData?) {
        val prettyPrintMessage = prettyPrintHbciMessage(message)

        val loggerToUse = logger ?: log
        if (e != null) {
            loggerToUse.error(e) { prettyPrintMessage }
        }
        else {
            loggerToUse.error(prettyPrintMessage)
        }

        val errorStackTrace = if (e != null) "\r\n" + getStackTrace(e) else ""

        // TODO: what to do when bank is not set?
        messageLog.add(MessageLogEntry(prettyPrintMessage + errorStackTrace, MessageLogEntryType.Error, Date(), bank))
    }


    protected open fun safelyRemoveSensitiveDataFromMessage(message: String, bank: BankData?): String {
        try {
            return removeSensitiveDataFromMessage(message, bank)
        } catch (e: Exception) {
            return "! WARNING !\r\nCould not remove sensitive data!\r\n$e\r\n${getStackTrace(e)}\r\n$message"
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

}