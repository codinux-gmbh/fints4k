package net.dankito.banking.fints.response.segments

import kotlinx.datetime.LocalDate
import net.dankito.banking.fints.model.Amount


open class BalanceSegment(
    val balance: Amount,
    val currency: String,
    val date: LocalDate,
    val accountProductName: String,
    val balanceOfPreBookedTransactions: Amount?,
    segmentString: String
)
    : ReceivedSegment(segmentString)