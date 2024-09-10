package net.codinux.banking.fints.transactions.mt940.model

class AmountAndCurrency(
    val amount: String,
    val currency: String,
    val isCredit: Boolean
) {
    internal constructor() : this("not an amount", "not a currency", false) // for object deserializers

    override fun toString() = "${if (isCredit == false) "-" else ""}$amount $currency"
}