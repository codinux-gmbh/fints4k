package net.codinux.banking.fints.transactions.swift.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import net.codinux.banking.fints.model.Amount

@Serializable
data class Holding(
    val name: String,
    val isin: String?,
    val wkn: String?,
    val buyingDate: LocalDate?,
    val quantity: Int?,
    /**
     * (Durchschnittlicher) Einstandspreis/-kurs einer Einheit des Wertpapiers
     */
    val averageCostPrice: Amount?,
    /**
     * Gesamter Kurswert aller Einheiten des Wertpapiers
     */
    val totalBalance: Amount?,
    val currency: String? = null,

    /**
     * Aktueller Kurswert einer einzelnen Einheit des Wertpapiers
     */
    val marketValue: Amount? = null,
    /**
     * Zeitpunkt zu dem der Kurswert bestimmt wurde
     */
    val pricingTime: Instant? = null,
    /**
     * Gesamter Einstandspreis
     */
    val totalCostPrice: Amount? = null
)