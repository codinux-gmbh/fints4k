package net.dankito.banking.fints.util

import net.dankito.banking.fints.model.Amount
import net.dankito.banking.fints.model.Money
import java.math.BigDecimal


fun Amount.toBigDecimal(): BigDecimal {
    return BigDecimal(this.string.replace(',', '.'))
}

fun Money.toBigDecimal(): BigDecimal {
    return this.amount.toBigDecimal()
}