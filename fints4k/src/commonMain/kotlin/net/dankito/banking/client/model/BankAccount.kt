package net.dankito.banking.client.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import net.codinux.banking.fints.model.Currency
import net.codinux.banking.fints.model.Money
import net.codinux.banking.fints.transactions.swift.model.StatementOfHoldings


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

    open val serverTransactionsRetentionDays: Int? = null,
    open val isAccountTypeSupportedByApplication: Boolean = false,
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

    /**
     * Gibt wider, wann zuletzt aktuelle Kontoumsätze, d.h. [net.dankito.banking.client.model.parameter.GetAccountDataParameter.retrieveTransactionsTo]
     * war nicht gesetzt, oder aktuelle [StatementOfHoldings] empfangen wurden.
     */
    open var lastAccountUpdateTime: Instant? = null

    open var bookedTransactions: List<AccountTransaction> = listOf()

    open var statementOfHoldings: List<StatementOfHoldings> = emptyList()


    override fun toString(): String {
        return "$productName ($identifier)"
    }

}