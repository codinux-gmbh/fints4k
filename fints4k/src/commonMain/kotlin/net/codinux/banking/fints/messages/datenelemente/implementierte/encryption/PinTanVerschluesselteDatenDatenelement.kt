package net.codinux.banking.fints.messages.datenelemente.implementierte.encryption

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.basisformate.BinaerDatenelement


/**
 * For PIN/TAN data doesn't get encrypted at all.
 *
 * It simply gets, prefixed by '@<payload_length>@', appended to VerschluesselteDaten segment header
 */
open class PinTanVerschluesselteDatenDatenelement(payload: String)
    : BinaerDatenelement(payload, Existenzstatus.Mandatory)