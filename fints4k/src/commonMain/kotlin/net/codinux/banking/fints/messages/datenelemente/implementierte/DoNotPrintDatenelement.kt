package net.codinux.banking.fints.messages.datenelemente.implementierte

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.TextDatenelement


/**
 * A dummy data element for conditional data elements building to tell formatter not to print this data element
 */
open class DoNotPrintDatenelement : TextDatenelement("", Existenzstatus.NotAllowed)