package net.codinux.banking.fints.response.segments

import net.codinux.banking.fints.transactions.swift.model.StatementOfHoldings

class SecuritiesAccountBalanceSegment(
    val statementOfHoldings: List<StatementOfHoldings>,
    segmentString: String
)
    : ReceivedSegment(segmentString)