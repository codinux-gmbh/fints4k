package net.dankito.banking.fints.model

import kotlinx.datetime.LocalDate


open class CreditCardTransaction(
    open val amount: Money,
    open val transactionDescriptionBase: String?,
    open val transactionDescriptionSupplement: String?,
    open val bookingDate: LocalDate,
    open val valueDate: LocalDate,
    open val isCleared: Boolean
) {


    open val description: String
        get() {
            transactionDescriptionBase?.let { transactionDescriptionBase ->
                if (transactionDescriptionSupplement != null) {
                    return transactionDescriptionBase + " " + transactionDescriptionSupplement
                }

                return transactionDescriptionBase
            }

            return ""
        }


    override fun toString(): String {
        return "$valueDate $amount $description"
    }

}