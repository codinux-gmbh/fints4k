package net.codinux.banking.fints.messages.datenelemente.implementierte

import net.codinux.banking.fints.messages.Existenzstatus


open class BezugssegmentInKreditinstitutsnachricht(segmentNumber: Int) : Bezugssegment(segmentNumber, Existenzstatus.Optional)