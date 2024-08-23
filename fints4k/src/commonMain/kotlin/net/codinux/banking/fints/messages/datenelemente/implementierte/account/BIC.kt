package net.codinux.banking.fints.messages.datenelemente.implementierte.account

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.AlphanumerischesDatenelement


open class BIC(bic: String, existenzstatus: Existenzstatus) : AlphanumerischesDatenelement(bic, existenzstatus, 11)