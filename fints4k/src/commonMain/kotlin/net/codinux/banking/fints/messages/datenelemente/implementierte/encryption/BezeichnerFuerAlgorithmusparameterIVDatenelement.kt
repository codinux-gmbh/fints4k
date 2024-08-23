package net.codinux.banking.fints.messages.datenelemente.implementierte.encryption

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Code
import net.codinux.banking.fints.messages.datenelemente.implementierte.allCodes


/**
 * Eigenschaft betreffend den Initialisierungswert für die RAH-Verfahren (Die
 * Steuerung erfolgt in den BPD, vgl. [Formals]).
 *
 * Codierung:
 * 1: Initialization value, clear text (IVC)
 */
open class BezeichnerFuerAlgorithmusparameterIVDatenelement(parameter: BezeichnerFuerAlgorithmusparameterIV, existenzstatus: Existenzstatus)
    : Code(parameter.code, AllowedValues, existenzstatus) {

    companion object {
        val AllowedValues = allCodes<BezeichnerFuerAlgorithmusparameterIV>()
    }

}