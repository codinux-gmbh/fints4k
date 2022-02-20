package net.dankito.banking.client.model

import kotlinx.datetime.LocalDate
import net.dankito.banking.fints.model.Money


open class BankAccount(
    identifier: String,
    subAccountNumber: String?,
    iban: String?,
    val accountHolderName: String,
    val type: BankAccountType = BankAccountType.CheckingAccount,
    val productName: String? = null,
    val currency: String = "EUR", // TODO: may parse to a value object
    val accountLimit: String? = null,
    // TODO: create an enum AccountCapabilities [ RetrieveBalance, RetrieveTransactions, TransferMoney / MoneyTransfer(?), InstantPayment ]
    val supportsRetrievingTransactions: Boolean = false,
    val supportsRetrievingBalance: Boolean = false,
    val supportsTransferringMoney: Boolean = false,
    val supportsInstantPayment: Boolean = false
) : BankAccountIdentifier(identifier, subAccountNumber, iban) {

    internal constructor() : this("", null, null, "") // for object deserializers

    constructor(identifier: BankAccountIdentifier) : this(identifier.identifier, identifier.subAccountNumber, identifier.iban, "")


    open var balance: Money = Money.Zero

    open var retrievedTransactionsFrom: LocalDate? = null

    open var retrievedTransactionsTo: LocalDate? = null

    open var bookedTransactions: List<AccountTransaction> = listOf()


    override fun toString(): String {
        return "$productName ($identifier)"
    }

}