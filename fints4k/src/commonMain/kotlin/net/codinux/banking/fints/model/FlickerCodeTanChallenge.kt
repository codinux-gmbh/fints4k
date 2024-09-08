package net.codinux.banking.fints.model

import kotlinx.datetime.LocalDateTime
import net.codinux.banking.fints.tan.FlickerCode


open class FlickerCodeTanChallenge(
    val flickerCode: FlickerCode,
    forAction: ActionRequiringTan,
    messageToShowToUser: String,
    challenge: String,
    tanMethod: TanMethod,
    tanMediaIdentifier: String?,
    bank: BankData,
    account: AccountData? = null,
    tanExpirationTime: LocalDateTime? = null
) : TanChallenge(forAction, messageToShowToUser, challenge, tanMethod, tanMediaIdentifier, bank, account, tanExpirationTime) {

    override fun toString(): String {
        return "$tanMethod (medium: $tanMediaIdentifier) $flickerCode: $messageToShowToUser"
    }

}