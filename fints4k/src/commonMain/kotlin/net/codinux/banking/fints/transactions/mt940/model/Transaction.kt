package net.codinux.banking.fints.transactions.mt940.model


open class Transaction(

    val statementLine: StatementLine,
    val information: RemittanceInformationField? = null

) {

    override fun toString(): String {
        return "$statementLine ($information)"
    }

}