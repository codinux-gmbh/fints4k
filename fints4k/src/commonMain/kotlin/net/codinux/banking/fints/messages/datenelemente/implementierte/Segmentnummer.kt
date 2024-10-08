package net.codinux.banking.fints.messages.datenelemente.implementierte

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.NumerischesDatenelement


/**
 * Information zur eindeutigen Identifizierung eines Segments innerhalb einer Nachricht.
 * Die Segmente einer Nachricht werden in Einerschritten streng monoton aufsteigend nummeriert.
 * Die Nummerierung beginnt mit 1 im ersten Segment der Nachricht (Nachrichtenkopf).
 */
open class Segmentnummer(number: Int) : NumerischesDatenelement(number, 3, Existenzstatus.Mandatory) {


    override fun validate() {
        super.validate()

        number?.let {  // if number is null and number has to be written to output then validation already fails above
            if (number < 1) {
                throwValidationException("Segmentnummer '$number' muss größer oder gleich 1 sein.")
            }
        }
    }

}