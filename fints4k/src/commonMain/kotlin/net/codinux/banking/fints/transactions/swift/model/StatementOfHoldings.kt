package net.codinux.banking.fints.transactions.swift.model

import kotlinx.datetime.LocalDate
import net.codinux.banking.fints.model.Amount

/**
 * 4.3 MT 535 Depotaufstellung
 * „Statement of Holdings“; basiert auf SWIFT „Standards Release Guide“
 * (letzte berücksichtigte Änderung SRG 1998)
 */
data class StatementOfHoldings(
    val bankCode: String,
    val accountIdentifier: String,

    val holdings: List<Holding>,

    val totalBalance: Amount? = null,
    val totalBalanceCurrency: String? = null,

    /**
     * The page number is actually mandatory, but to be prepared for surprises like for [statementDate] i added error
     * handling and made it optional.
     */
    val pageNumber: Int? = null,
    val continuationIndicator: ContinuationIndicator = ContinuationIndicator.Unknown,

    /**
     * The statement date is actually mandatory, but not all banks actually set it.
     */
    val statementDate: LocalDate? = null,
    val preparationDate: LocalDate? = null
) {
    override fun toString() = "$bankCode ${holdings.size} holdings: ${holdings.joinToString { "{${it.name} ${it.totalBalance}" }}"
}