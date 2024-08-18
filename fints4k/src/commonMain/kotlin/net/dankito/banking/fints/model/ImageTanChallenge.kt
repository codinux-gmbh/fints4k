package net.dankito.banking.fints.model

import net.dankito.banking.fints.tan.TanImage


open class ImageTanChallenge(
    val image: TanImage,
    forAction: ActionRequiringTan,
    messageToShowToUser: String,
    challenge: String,
    tanMethod: TanMethod,
    tanMediaIdentifier: String?,
    bank: BankData,
    account: AccountData? = null
) : TanChallenge(forAction, messageToShowToUser, challenge, tanMethod, tanMediaIdentifier, bank, account) {

    override fun toString(): String {
        return "$tanMethod (medium: $tanMediaIdentifier) $image: $messageToShowToUser"
    }

}