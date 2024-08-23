package net.codinux.banking.fints.messages.datenelemente.implementierte

import net.codinux.banking.fints.messages.Existenzstatus


open class BezugssegmentInKundennachricht(segmentNumber: Int) : Bezugssegment(segmentNumber, Existenzstatus.NotAllowed)