package net.dankito.banking.fints.model

import kotlinx.datetime.LocalDate
import net.dankito.utils.multiplatform.extensions.format


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
        return "${valueDate.format("dd.MM.yy")} $amount $description"
    }

}