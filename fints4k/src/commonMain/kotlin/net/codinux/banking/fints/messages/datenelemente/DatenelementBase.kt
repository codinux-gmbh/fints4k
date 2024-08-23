package net.codinux.banking.fints.messages.datenelemente

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.Nachrichtenteil


abstract class DatenelementBase(val existenzstatus: Existenzstatus) : Nachrichtenteil()