package net.dankito.banking.client.model

interface BankAccountIdentifier {

    val identifier: String

    val subAccountNumber: String?

    val iban: String?

}