package net.dankito.banking.fints.response.segments

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import net.dankito.banking.fints.model.Amount


open class Balance(
    val amount: Amount,
    val currency: String?,
    val date: LocalDate,
    val time: LocalDateTime?
) {

    override fun toString(): String {
        return "$amount ($date)"
    }

}