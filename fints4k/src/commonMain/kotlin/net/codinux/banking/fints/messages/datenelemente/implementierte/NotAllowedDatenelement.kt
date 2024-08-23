package net.codinux.banking.fints.messages.datenelemente.implementierte

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.TextDatenelement


open class NotAllowedDatenelement : TextDatenelement("", Existenzstatus.NotAllowed)