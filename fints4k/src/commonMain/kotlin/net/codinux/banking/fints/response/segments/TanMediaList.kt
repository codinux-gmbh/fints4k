package net.codinux.banking.fints.response.segments

import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanEinsatzOption
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMedium


open class TanMediaList(
    val usageOption: TanEinsatzOption,
    val tanMedia: List<TanMedium>,
    segmentString: String
)
    : ReceivedSegment(segmentString)