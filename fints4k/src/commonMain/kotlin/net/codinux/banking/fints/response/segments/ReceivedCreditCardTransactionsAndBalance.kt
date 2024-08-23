package net.codinux.banking.fints.response.segments

import net.codinux.banking.fints.model.CreditCardTransaction


open class ReceivedCreditCardTransactionsAndBalance(
    val balance: Balance,
    val transactions: List<CreditCardTransaction>,
    segmentString: String

) : ReceivedSegment(segmentString)