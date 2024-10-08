package net.codinux.banking.fints.messages.datenelementgruppen.implementierte

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.implementierte.Segmentkennung
import net.codinux.banking.fints.messages.datenelemente.implementierte.Segmentnummer
import net.codinux.banking.fints.messages.datenelemente.implementierte.Segmentversion
import net.codinux.banking.fints.messages.datenelementgruppen.Datenelementgruppe
import net.codinux.banking.fints.messages.segmente.id.ISegmentId


open class Segmentkopf(
    val identifier: String,
    val segmentVersion: Int,
    val segmentNumber: Int = 0,
    bezugssegment: Int? = null

) : Datenelementgruppe(listOf(
    Segmentkennung(identifier),
    Segmentnummer(segmentNumber),
    Segmentversion(segmentVersion) // TODO: how to conditionally add Bezugsegment?
), Existenzstatus.Mandatory) {

    constructor(id: ISegmentId, segmentVersion: Int, segmentNumber: Int) : this(id.id, segmentVersion, segmentNumber)

    override fun toString() = "$identifier:$segmentNumber:$segmentVersion"

}