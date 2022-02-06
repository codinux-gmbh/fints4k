package net.dankito.banking.fints.transactions

import net.dankito.banking.fints.log.IMessageLogAppender
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.transactions.mt940.IMt940Parser
import net.dankito.banking.fints.transactions.mt940.Mt940Parser
import net.dankito.banking.fints.transactions.mt940.model.AccountStatement
import net.dankito.banking.fints.transactions.mt940.model.Balance
import net.dankito.banking.fints.transactions.mt940.model.Transaction
import net.dankito.banking.fints.transactions.mt940.model.StatementLine
import net.dankito.utils.multiplatform.log.LoggerFactory
import kotlin.reflect.KClass


open class Mt940AccountTransactionsParser(
    protected val mt940Parser: IMt940Parser = Mt940Parser(),
    override var logAppender: IMessageLogAppender? = null
) : IAccountTransactionsParser {

    companion object {
        private val log = LoggerFactory.getLogger(Mt940AccountTransactionsParser::class)
    }


    override fun parseTransactions(transactionsString: String, bank: BankData, account: AccountData): List<AccountTransaction> {
        val accountStatements = mt940Parser.parseMt940String(transactionsString)

        return accountStatements.flatMap { mapToAccountTransactions(it, bank, account) }
    }

    override fun parseTransactionsChunk(transactionsChunk: String, bank: BankData, account: AccountData): Pair<List<AccountTransaction>, String> {
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

        return AccountTransaction(
            account,
            Money(mapAmount(transaction.statementLine), currency),
            transaction.statementLine.isReversal,
            transaction.information?.unparsedReference ?: "",
            transaction.statementLine.bookingDate ?: statement.closingBalance.bookingDate,
            transaction.information?.otherPartyName,
            transaction.information?.otherPartyBankCode,
            transaction.information?.otherPartyAccountId,
            transaction.information?.bookingText,
            transaction.statementLine.valueDate,
            statement.statementNumber,
            statement.sequenceNumber,
            Money(mapAmount(statement.openingBalance), currency), // TODO: that's not true, these are the opening and closing balance of
            Money(mapAmount(statement.closingBalance), currency), // all transactions of this day, not this specific transaction's ones

            transaction.information?.endToEndReference,
            transaction.information?.customerReference,
            transaction.information?.mandateReference,
            transaction.information?.creditorIdentifier,
            transaction.information?.originatorsIdentificationCode,
            transaction.information?.compensationAmount,
            transaction.information?.originalAmount,
            transaction.information?.sepaReference,
            transaction.information?.deviantOriginator,
            transaction.information?.deviantRecipient,
            transaction.information?.referenceWithNoSpecialType,
            transaction.information?.primaNotaNumber,
            transaction.information?.textKeySupplement,

            transaction.statementLine.currencyType,
            transaction.statementLine.bookingKey,
            transaction.statementLine.referenceForTheAccountOwner,
            transaction.statementLine.referenceOfTheAccountServicingInstitution,
            transaction.statementLine.supplementaryDetails,

            statement.transactionReferenceNumber,
            statement.relatedReferenceNumber
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