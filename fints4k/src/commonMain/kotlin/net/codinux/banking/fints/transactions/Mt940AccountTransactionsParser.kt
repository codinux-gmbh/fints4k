package net.codinux.banking.fints.transactions

import net.codinux.log.logger
import net.codinux.banking.fints.log.IMessageLogAppender
import net.codinux.banking.fints.model.*
import net.codinux.banking.fints.transactions.mt940.IMt940Parser
import net.codinux.banking.fints.transactions.mt940.Mt940Parser
import net.codinux.banking.fints.transactions.mt940.model.AccountStatement
import net.codinux.banking.fints.transactions.mt940.model.Balance
import net.codinux.banking.fints.transactions.mt940.model.Transaction
import net.codinux.banking.fints.transactions.mt940.model.StatementLine


open class Mt940AccountTransactionsParser(
    protected val mt940Parser: IMt940Parser = Mt940Parser(),
    override var logAppender: IMessageLogAppender? = null
) : IAccountTransactionsParser {

    private val log by logger()


    override fun parseTransactions(transactionsString: String, bank: BankData, account: AccountData): List<AccountTransaction> {
        val accountStatements = mt940Parser.parseMt940String(transactionsString)

        return accountStatements.flatMap { mapToAccountTransactions(it, bank, account) }
    }

    override fun parseTransactionsChunk(transactionsChunk: String, bank: BankData, account: AccountData): Pair<List<AccountTransaction>, String?> {
        val (accountStatements, remainder) = mt940Parser.parseMt940Chunk(transactionsChunk)

        return Pair(accountStatements.flatMap { mapToAccountTransactions(it, bank, account) }, remainder)
    }

    protected open fun mapToAccountTransactions(statement: AccountStatement, bank: BankData, account: AccountData): List<AccountTransaction> {
        try {
            return statement.transactions.map { mapToAccountTransaction(statement, it, account) }
        } catch (e: Exception) {
            logError("Could not map AccountStatement '$statement' to AccountTransactions", e, bank)
        }

        return listOf()
    }

    protected open fun mapToAccountTransaction(statement: AccountStatement, transaction: Transaction, account: AccountData): AccountTransaction {
        val currency = statement.closingBalance.currency

        // may parse postingKey to postingText (Buchungstext) according to table in 8.2.3 Buchungsschlüssel (Feld 61), S. 654 ff.

        return AccountTransaction(
            account,
            Money(mapAmount(transaction.statementLine), currency),
            // either field :86: contains structured information, then sepaReference is a mandatory field, or :86: is unstructured, then the whole field value is the reference
            transaction.information?.sepaReference ?: transaction.information?.unparsedReference ?: "",

            transaction.statementLine.bookingDate ?: statement.closingBalance.bookingDate,
            transaction.statementLine.valueDate,

            transaction.information?.otherPartyName,
            transaction.information?.otherPartyBankId,
            transaction.information?.otherPartyAccountId,

            transaction.information?.postingText,

            Money(mapAmount(statement.openingBalance), currency),
            Money(mapAmount(statement.closingBalance), currency),

            statement.statementNumber,
            statement.sheetNumber,

            // :60: customer reference: Wenn „KREF+“ eingestellt ist, dann erfolgt die Angabe der Referenznummer in Tag :86: .
            transaction.information?.customerReference ?: transaction.statementLine.customerReference,
            transaction.statementLine.bankReference,
            transaction.statementLine.furtherInformationOriginalAmountAndCharges,

            transaction.information?.endToEndReference,
            transaction.information?.mandateReference,
            transaction.information?.creditorIdentifier,
            transaction.information?.originatorsIdentificationCode,
            transaction.information?.compensationAmount,
            transaction.information?.originalAmount,
            transaction.information?.deviantOriginator,
            transaction.information?.deviantRecipient,
            transaction.information?.referenceWithNoSpecialType,
            transaction.information?.journalNumber,
            transaction.information?.textKeyAddition,

            statement.orderReferenceNumber,
            statement.referenceNumber,

            transaction.statementLine.isReversal,
        )
    }

    /**
     * In MT940 amounts are always stated as a positive number and flag isCredit decides if it's a credit or debit.
     */
    protected open fun mapAmount(balance: Balance): Amount {
        return mapAmount(balance.amount, balance.isCredit)
    }

    /**
     * In MT940 amounts are always stated as a positive number and flag isCredit decides if it's a credit or debit.
     */
    protected open fun mapAmount(statementLine: StatementLine): Amount {
        return mapAmount(statementLine.amount, statementLine.isCredit)
    }

    /**
     * In MT940 amounts are always stated as a positive number and flag isCredit decides if it's a credit or debit.
     */
    protected open fun mapAmount(positiveAmount: Amount, isCredit: Boolean): Amount {
        if (isCredit == false && positiveAmount.string.startsWith('-') == false) {
            return Amount("-" + positiveAmount)
        }

        return positiveAmount
    }


    protected open fun logError(message: String, e: Exception?, bank: BankData) {
        logAppender?.let { logAppender ->
            logAppender.logError(Mt940AccountTransactionsParser::class, message, e)
        }
        ?: run {
            log.error(e) { message }
        }
    }

}