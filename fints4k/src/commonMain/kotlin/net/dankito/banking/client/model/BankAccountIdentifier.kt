package net.dankito.banking.client.model


open class BankAccountIdentifier(
    open val identifier: String,
    open val subAccountNumber: String?,
    open val iban: String?,
)