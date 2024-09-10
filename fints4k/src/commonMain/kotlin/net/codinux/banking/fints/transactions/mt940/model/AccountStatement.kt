package net.codinux.banking.fints.transactions.mt940.model

open class AccountStatement(
    orderReferenceNumber: String,
    referenceNumber: String?,

    bankCodeBicOrIban: String,
    accountIdentifier: String?,

    statementNumber: Int,
    sheetNumber: Int?,

    val openingBalance: Balance,

    transactions: List<Transaction>,

    val closingBalance: Balance,

    val currentValueBalance: Balance? = null,

    val futureValueBalance: Balance? = null,

    /**
     * Mehrzweckfeld
     *
     * Es dürfen nur unstrukturierte Informationen eingestellt werden. Es dürfen keine Informationen,
     * die auf einzelne Umsätze bezogen sind, eingestellt werden.
     *
     * Die Zeilen werden mit <CR><LF> getrennt.
     *
     * Max length = 65
     */
    val remittanceInformationField: String? = null

) : AccountStatementCommon(orderReferenceNumber, referenceNumber, bankCodeBicOrIban, accountIdentifier, statementNumber, sheetNumber, transactions) {

    // for object deserializers
    private constructor() : this("", "", "", null, 0, null, Balance(), listOf(), Balance())


    override fun toString(): String {
        return "$closingBalance ${super.toString()}"
    }

}