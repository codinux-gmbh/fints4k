package net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.AlphanumerischesDatenelement
import net.codinux.banking.fints.messages.datenelemente.implementierte.ICodeEnum


/**
 * Es sind nur die jeweils aufgeführten Werte zulässig.
 */
open class Code(code: String?, val allowedValues: List<String>, existenzstatus: Existenzstatus)
    : AlphanumerischesDatenelement(code, existenzstatus) {


    constructor(code: ICodeEnum?, allowedValues: List<String>, existenzstatus: Existenzstatus)
            : this(code?.code, allowedValues, existenzstatus)


    override fun validate() {
        super.validate()

        if (allowedValues.contains(value) == false) {
            throwValidationException("'$value' ist kein Wert aus der Liste der zulässigen Werte: " +
                    allowedValues.joinToString(", ")
            )
        }
    }

}