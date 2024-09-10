package net.codinux.banking.fints.transactions.mt940

import net.codinux.banking.fints.log.IMessageLogAppender
import net.codinux.banking.fints.transactions.mt940.model.AmountAndCurrency
import net.codinux.banking.fints.transactions.mt940.model.InterimAccountStatement
import net.codinux.banking.fints.transactions.mt940.model.NumberOfPostingsAndAmount
import net.codinux.banking.fints.transactions.mt940.model.Transaction

open class Mt942Parser(
    logAppender: IMessageLogAppender? = null
) : Mt94xParserBase<InterimAccountStatement>(logAppender) {

    /**
     * Parses a whole MT 942 statements string, that is one that ends with a "-" line.
     */
    open fun parseMt942String(mt942String: String): List<InterimAccountStatement> =
        super.parseMt94xString(mt942String)

    /**
     * Parses incomplete MT 942 statements string, that is ones that not end with a "-" line,
     * as they are returned e.g. if a HKKAZ response is dispersed over multiple messages.
     *
     * Tries to parse all statements in the string except an incomplete last one and returns an
     * incomplete last MT 942 statement (if any) as remainder.
     *
     * So each single HKKAZ partial response can be parsed immediately, its statements/transactions
     * be displayed immediately to user and the remainder then be passed together with next partial
     * HKKAZ response to this method till this whole MT 942 statement is parsed.
     */
    open fun parseMt942Chunk(mt942Chunk: String): Pair<List<InterimAccountStatement>, String?> =
        super.parseMt94xChunk(mt942Chunk)


    override fun createAccountStatement(
        orderReferenceNumber: String,
        referenceNumber: String?,
        bankCodeBicOrIban: String,
        accountIdentifier: String?,
        statementNumber: Int,
        sheetNumber: Int?,
        transactions: List<Transaction>,
        fieldsByCode: List<Pair<String, String>>
    ): InterimAccountStatement {
        // also decided against parsing smallest amounts, i don't think they ever going to be used
//        val smallestAmounts = fieldsByCode.filter { it.first.startsWith(SmallestAmountCode) } // should we parse it? i see no use in it
//            .mapIndexed { index, field -> parseAmountAndCurrency(field.second, index == 0) }

        // decided against parsing creation time as there are so many non specification confirm time formats that parsing is likely to fail 'cause of this unused value
//        val creationTime = parseDateTime(fieldsByCode.first { it.first == CreationTimeCode || it.first.startsWith(CreationTimeStartCode) }.second)

        val numberAndTotalOfDebitPostings = fieldsByCode.firstOrNull { it.first.equals(AmountOfDebitPostingsCode) }
            ?.let { parseNumberAndTotalOfPostings(it.second) }
        val numberAndTotalOfCreditPostings = fieldsByCode.firstOrNull { it.first.equals(AmountOfCreditPostingsCode) }
            ?.let { parseNumberAndTotalOfPostings(it.second) }

        return InterimAccountStatement(
            orderReferenceNumber, referenceNumber,
            bankCodeBicOrIban, accountIdentifier,
            statementNumber, sheetNumber,
            transactions,
            numberAndTotalOfDebitPostings,
            numberAndTotalOfCreditPostings
        )
    }

    private fun parseAmountAndCurrency(fieldValue: String, isCreditCharOptional: Boolean = false): AmountAndCurrency {
        val currency = fieldValue.substring(0, 3)
        val hasCreditChar = isCreditCharOptional == false || fieldValue[3].isLetter()
        val isCredit = if (hasCreditChar) fieldValue[3] == 'C' else false
        val amount = fieldValue.substring(if (hasCreditChar) 4 else 3)

        return AmountAndCurrency(amount, currency, isCredit)
    }

    protected open fun parseNumberAndTotalOfPostings(fieldValue: String): NumberOfPostingsAndAmount {
        val currencyStartIndex = fieldValue.indexOfFirst { it.isLetter() }

        val numberOfPostings = fieldValue.substring(0, currencyStartIndex).toInt()
        val currency = fieldValue.substring(currencyStartIndex, currencyStartIndex + 3)
        val amount = fieldValue.substring(currencyStartIndex + 3)

        return NumberOfPostingsAndAmount(numberOfPostings, amount, currency)
    }

}