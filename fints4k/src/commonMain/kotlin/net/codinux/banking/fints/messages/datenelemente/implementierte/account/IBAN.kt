package net.codinux.banking.fints.messages.datenelemente.implementierte.account

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.AlphanumerischesDatenelement


open class IBAN(iban: String, existenzstatus: Existenzstatus) : AlphanumerischesDatenelement(iban, existenzstatus, 34)