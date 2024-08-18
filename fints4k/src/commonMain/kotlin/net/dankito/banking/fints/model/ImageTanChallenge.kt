package net.dankito.banking.fints.model

import net.dankito.banking.fints.tan.TanImage


open class ImageTanChallenge(
    val image: TanImage,
    forAction: ActionRequiringTan,
    bank: BankData,
    messageToShowToUser: String,
    challenge: String,
    tanMethod: TanMethod,
    tanMediaIdentifier: String?
) : TanChallenge(forAction, bank, messageToShowToUser, challenge, tanMethod, tanMediaIdentifier) {

    override fun toString(): String {
        return "$tanMethod (medium: $tanMediaIdentifier) $image: $messageToShowToUser"
    }

}