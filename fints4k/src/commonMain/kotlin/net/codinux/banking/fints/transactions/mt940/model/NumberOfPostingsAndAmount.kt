package net.codinux.banking.fints.transactions.mt940.model

class NumberOfPostingsAndAmount(
    val numberOfPostings: Int,
    val amount: String,
    val currency: String
) {
    private constructor() : this(-1, "not an amount", "not a currency") // for object deserializers

    override fun toString() = "$amount $currency, $numberOfPostings posting(s)"
}