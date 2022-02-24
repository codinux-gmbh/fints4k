package net.dankito.banking.client.model

import kotlinx.serialization.Serializable


@Serializable
open class BankAccountIdentifierImpl(
    override val identifier: String,
    override val subAccountNumber: String?,
    override val iban: String?,
) : BankAccountIdentifier