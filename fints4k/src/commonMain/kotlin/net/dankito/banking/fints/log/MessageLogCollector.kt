package net.dankito.banking.fints.log

import net.dankito.banking.fints.model.BankData
import net.dankito.banking.fints.model.MessageLogEntry
import net.dankito.banking.fints.model.MessageLogEntryType
import net.dankito.utils.multiplatform.log.LoggerFactory
import net.dankito.utils.multiplatform.Date
import net.dankito.utils.multiplatform.getInnerExceptionMessage


open class MessageLogCollector {

    companion object {
        val FindAccountTransactionsStartRegex = Regex("^HIKAZ:\\d:\\d:\\d\\+@\\d+@", RegexOption.MULTILINE)
        val FindAccountTransactionsEndRegex = Regex("^-'", RegexOption.MULTILINE)

        private val log = LoggerFactory.getLogger(MessageLogCollector::class)
    }


    protected open val messageLog = ArrayList<MessageLogEntry>() // TODO: make thread safe like with CopyOnWriteArrayList

    // in either case remove sensitive data after response is parsed as otherwise some information like account holder name and accounts may is not set yet on BankData
    open val messageLogWithoutSensitiveData: List<MessageLogEntry>
        get() = messageLog.map { MessageLogEntry(removeSensitiveDataFromMessage(it.message, it.bank), it.time, it.bank) }


    open fun addMessageLog(message: String, type: MessageLogEntryType, bank: BankData) {
        val timeStamp = Date()
        val messagePrefix = "${if (type == MessageLogEntryType.Sent) "Sending" else "Received"} message:\r\n" // currently no need to translate
        val prettyPrintMessage = prettyPrintHbciMessage(message)
        val prettyPrintMessageWithPrefix = "$messagePrefix$prettyPrintMessage"

        log.debug { prettyPrintMessageWithPrefix }

        messageLog.add(MessageLogEntry(prettyPrintMessageWithPrefix, timeStamp, bank))
    }

    protected open fun prettyPrintHbciMessage(message: String): String {
        return message.replace("'", "'\r\n")
    }


    protected open fun removeSensitiveDataFromMessage(message: String, bank: BankData): String {
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

}