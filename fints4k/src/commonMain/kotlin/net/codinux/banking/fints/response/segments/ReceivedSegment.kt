package net.codinux.banking.fints.response.segments

import net.codinux.banking.fints.messages.Separators


open class ReceivedSegment(
    open val segmentId: String,
    open val segmentNumber: Int,
    open val segmentVersion: Int,
    open val referenceSegmentNumber: Int? = null,
    open val segmentString: String
) {


    /**
     * Convenience constructor for setting [segmentId], [segmentNumber] and [segmentVersion].
     *
     * [segmentHeader] has to have three or four elements like [net.dankito.fints.messages.datenelementgruppen.implementierte.Segmentkopf] has.
     */
    constructor(segmentHeader: List<String>, segmentString: String) :
            this(segmentHeader[0], segmentHeader[1].toInt(), segmentHeader[2].toInt(),
                if (segmentHeader.size >= 4 && segmentHeader[3].isNotBlank()) segmentHeader[3].toInt() else null, segmentString)

    constructor(segmentString: String) : this(
        segmentString.split(Separators.DataElementGroupsSeparator).first().split(Separators.DataElementsSeparator),
        segmentString
    )


    override fun toString(): String {
        return "$segmentId: $segmentString"
    }

}