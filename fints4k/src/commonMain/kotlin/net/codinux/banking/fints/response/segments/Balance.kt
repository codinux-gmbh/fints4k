package net.codinux.banking.fints.response.segments

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.codinux.banking.fints.model.Amount


open class Balance(
    val amount: Amount,
    val currency: String?,
    val date: LocalDate,
    val time: LocalTime?
) {

    override fun toString(): String {
        return "$amount ($date)"
    }

}