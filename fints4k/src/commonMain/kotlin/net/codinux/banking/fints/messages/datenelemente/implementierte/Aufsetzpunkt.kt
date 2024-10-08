package net.codinux.banking.fints.messages.datenelemente.implementierte

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.AlphanumerischesDatenelement


/**
 * Information darüber, wie die Beantwortung des Kundenauftrags an einem bestimmten Punkt kontrolliert
 * beendet und aufgesetzt werden kann, falls die Rückmeldung des Kreditinstituts nicht in einem
 * einzigen Auftragssegment erfolgen kann (s. [Formals]).
 */
open class Aufsetzpunkt(continuationId: String?, existenzstatus: Existenzstatus)
    : AlphanumerischesDatenelement(continuationId, existenzstatus, 35) {

    open fun resetContinuationId(continuationId: String?) {
        value = continuationId
    }

}