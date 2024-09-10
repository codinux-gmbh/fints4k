package net.codinux.banking.fints.transactions.mt940

import net.codinux.banking.fints.log.IMessageLogAppender
import net.codinux.banking.fints.transactions.mt940.model.*

open class Mt940Parser(
    override var logAppender: IMessageLogAppender? = null
) : Mt94xParserBase<AccountStatement>(logAppender), IMt940Parser {

    /**
     * Parses a whole MT 940 statements string, that is one that ends with a "-" line.
     */
    override fun parseMt940String(mt940String: String): List<AccountStatement> =
        super.parseMt94xString(mt940String)

    /**
     * Parses incomplete MT 940 statements string, that is ones that not end with a "-" line,
     * as they are returned e.g. if a HKKAZ response is dispersed over multiple messages.
     *
     * Tries to parse all statements in the string except an incomplete last one and returns an
     * incomplete last MT 940 statement (if any) as remainder.
     *
     * So each single HKKAZ partial response can be parsed immediately, its statements/transactions
     * be displayed immediately to user and the remainder then be passed together with next partial
     * HKKAZ response to this method till this whole MT 940 statement is parsed.
     */
    override fun parseMt940Chunk(mt940Chunk: String): Pair<List<AccountStatement>, String?> =
        super.parseMt94xChunk(mt940Chunk)


    override fun createAccountStatement(
        orderReferenceNumber: String,
        referenceNumber: String?,
        bankCodeBicOrIban: String,
        accountIdentifier: String?,
        statementNumber: Int,
        sheetNumber: Int?,
        transactions: List<Transaction>,
        fieldsByCode: List<Pair<String, String>>
    ): AccountStatement {
        val openingBalancePair = fieldsByCode.first { it.first.startsWith(OpeningBalanceCode) }
        val closingBalancePair = fieldsByCode.first { it.first.startsWith(ClosingBalanceCode) }

        return AccountStatement(
            orderReferenceNumber, referenceNumber,
            bankCodeBicOrIban, accountIdentifier,
            statementNumber, sheetNumber,
            parseBalance(openingBalancePair.first, openingBalancePair.second),
            transactions,
            parseBalance(closingBalancePair.first, closingBalancePair.second)
        )
    }

}