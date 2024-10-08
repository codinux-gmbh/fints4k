package net.codinux.banking.fints.transactions.mt940.model

open class InterimAccountStatement(
    orderReferenceNumber: String,
    referenceNumber: String?,

    bankCodeBicOrIban: String,
    accountIdentifier: String?,

    statementNumber: Int,
    sheetNumber: Int?,

    // decided against parsing them, see Mt942Parser
//    val smallestAmountOfReportedTransactions: AmountAndCurrency,
//
//    val smallestAmountOfReportedCreditTransactions: AmountAndCurrency? = null,
//
//    val creationTime: Instant,

    transactions: List<Transaction>,

    val amountAndTotalOfDebitPostings: NumberOfPostingsAndAmount? = null,

    val amountAndTotalOfCreditPostings: NumberOfPostingsAndAmount? = null,

) : AccountStatementCommon(orderReferenceNumber, referenceNumber, bankCodeBicOrIban, accountIdentifier, statementNumber, sheetNumber, transactions) {

    // for object deserializers
    private constructor() : this("", "", "", null, 0, null, listOf())


    override fun toString(): String {
        return "${amountAndTotalOfDebitPostings?.amount} ${super.toString()}"
    }

}