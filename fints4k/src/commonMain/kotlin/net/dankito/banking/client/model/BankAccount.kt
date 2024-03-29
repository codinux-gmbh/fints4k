package net.dankito.banking.client.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import net.dankito.banking.fints.model.Currency
import net.dankito.banking.fints.model.Money


@Serializable
open class BankAccount(
    override val identifier: String,
    override val subAccountNumber: String?,
    override val iban: String?,
    open val accountHolderName: String,
    open val type: BankAccountType = BankAccountType.CheckingAccount,
    open val productName: String? = null,
    open val currency: String = Currency.DefaultCurrencyCode, // TODO: may parse to a value object
    open val accountLimit: String? = null,
    // TODO: create an enum AccountCapabilities [ RetrieveBalance, RetrieveTransactions, TransferMoney / MoneyTransfer(?), InstantPayment ]
    open val supportsRetrievingTransactions: Boolean = false,
    open val supportsRetrievingBalance: Boolean = false,
    open val supportsTransferringMoney: Boolean = false,
    open val supportsInstantPayment: Boolean = false
) : BankAccountIdentifier {

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