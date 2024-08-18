package net.dankito.banking.fints.model

import net.dankito.banking.fints.tan.FlickerCode


open class FlickerCodeTanChallenge(
    val flickerCode: FlickerCode,
    forAction: ActionRequiringTan,
    messageToShowToUser: String,
    challenge: String,
    tanMethod: TanMethod,
    tanMediaIdentifier: String?,
    bank: BankData,
    account: AccountData? = null
) : TanChallenge(forAction, messageToShowToUser, challenge, tanMethod, tanMediaIdentifier, bank, account) {

    override fun toString(): String {
        return "$tanMethod (medium: $tanMediaIdentifier) $flickerCode: $messageToShowToUser"
    }

}