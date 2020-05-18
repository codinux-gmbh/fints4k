package net.dankito.banking.fints.messages.datenelemente.implementierte.account

import net.dankito.banking.fints.messages.Existenzstatus
import net.dankito.banking.fints.messages.datenelemente.basisformate.AlphanumerischesDatenelement


open class IBAN(iban: String, existenzstatus: Existenzstatus) : AlphanumerischesDatenelement(iban, existenzstatus, 34)