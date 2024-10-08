package net.codinux.banking.fints.response.segments


open class ReceivedMessageHeader(
    val messageSize: Int,
    val finTsVersion: Int,
    val dialogId: String,
    val messageNumber: Int,
    segmentString: String)

    : ReceivedSegment(segmentString)