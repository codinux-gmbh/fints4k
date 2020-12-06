package net.dankito.banking.fints.model

import net.dankito.utils.multiplatform.Date
import net.dankito.utils.multiplatform.format


open class CreditCardTransaction(
    open val amount: Money,
    open val transactionDescriptionBase: String?,
    open val transactionDescriptionSupplement: String?,
    open val bookingDate: Date,
    open val valueDate: Date,
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
        return "${valueDate.format("dd.MM.yy")} $amount $description"
    }

}