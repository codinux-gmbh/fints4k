package net.dankito.banking.fints.model

import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMedium
import net.dankito.banking.fints.response.client.FinTsClientResponse


open class TanChallenge(
    val forAction: ActionRequiringTan,
    val bank: BankData,
    val messageToShowToUser: String,
    val challenge: String,
    val tanMethod: TanMethod,
    val tanMediaIdentifier: String?
) {

    var enterTanResult: EnterTanResult? = null
        private set

    open val isEnteringTanDone: Boolean
        get() = enterTanResult != null


    fun userEnteredTan(enteredTan: String) {
        this.enterTanResult = EnterTanResult(enteredTan.replace(" ", ""))
    }

    fun userDidNotEnterTan() {
        this.enterTanResult = EnterTanResult(null)
    }

    fun userAsksToChangeTanMethod(changeTanMethodTo: TanMethod) {
        this.enterTanResult = EnterTanResult(null, changeTanMethodTo)
    }

    fun userAsksToChangeTanMedium(changeTanMediumTo: TanMedium, changeTanMediumResultCallback: ((FinTsClientResponse) -> Unit)?) {
        this.enterTanResult = EnterTanResult(null, null, changeTanMediumTo, changeTanMediumResultCallback)
    }


    override fun toString(): String {
        return "$tanMethod (medium: $tanMediaIdentifier): $messageToShowToUser"
    }

}