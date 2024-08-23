package net.codinux.banking.fints.messages.segmente

import net.codinux.banking.fints.messages.datenelemente.implementierte.Synchronisierungsmodus
import net.codinux.banking.fints.messages.datenelemente.implementierte.SynchronisierungsmodusDatenelement
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.Segmentkopf
import net.codinux.banking.fints.messages.segmente.id.CustomerSegmentId


open class Synchronisierung(
    segmentNumber: Int,
    mode: Synchronisierungsmodus

) : Segment(listOf(
    Segmentkopf(CustomerSegmentId.Synchronization, 3, segmentNumber),
    SynchronisierungsmodusDatenelement(mode)
))