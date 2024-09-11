package net.codinux.banking.fints.transactions.swift.model

enum class ContinuationIndicator(internal val mtValue: String) {
    /**
     * The only page
     */
    SinglePage("ONLY"),

    /**
     * Intermediate page, more pages follow
     */
    IntermediatePage("MORE"),

    /**
     * Last page
     */
    LastPage("LAST"),

    Unknown("NotAMtValue")

}