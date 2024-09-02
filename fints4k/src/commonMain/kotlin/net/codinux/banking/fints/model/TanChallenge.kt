package net.codinux.banking.fints.model

import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMedium
import net.codinux.banking.fints.response.BankResponse
import net.codinux.banking.fints.response.client.FinTsClientResponse


open class TanChallenge(
    val forAction: ActionRequiringTan,
    val messageToShowToUser: String,
    val challenge: String,
    val tanMethod: TanMethod,
    val tanMediaIdentifier: String?,
    val bank: BankData,
    val account: AccountData? = null
) {

    var enterTanResult: EnterTanResult? = null
        private set

    open val isEnteringTanDone: Boolean
        get() = enterTanResult != null

    private val userApprovedDecoupledTanCallbacks = mutableListOf<() -> Unit>()


    fun userEnteredTan(enteredTan: String) {
        this.enterTanResult = EnterTanResult(enteredTan.replace(" ", ""))
    }

    internal fun userApprovedDecoupledTan(responseAfterApprovingDecoupledTan: BankResponse) {
        this.enterTanResult = EnterTanResult(null, true, responseAfterApprovingDecoupledTan)

        userApprovedDecoupledTanCallbacks.forEach { it.invoke() }
        userApprovedDecoupledTanCallbacks.clear()
    }

    fun userDidNotEnterTan() {
        this.enterTanResult = EnterTanResult(null)
    }

    fun userAsksToChangeTanMethod(changeTanMethodTo: TanMethod) {
        this.enterTanResult = EnterTanResult(null, changeTanMethodTo = changeTanMethodTo)
    }

    fun userAsksToChangeTanMedium(changeTanMediumTo: TanMedium, changeTanMediumResultCallback: ((FinTsClientResponse) -> Unit)?) {
        this.enterTanResult = EnterTanResult(null, changeTanMediumTo = changeTanMediumTo, changeTanMediumResultCallback = changeTanMediumResultCallback)
    }


    fun addUserApprovedDecoupledTanCallback(callback: () -> Unit) {
        if (isEnteringTanDone == false) {
            this.userApprovedDecoupledTanCallbacks.add(callback)
        } else if (enterTanResult != null && enterTanResult!!.userApprovedDecoupledTan == true) {
            callback()
        }
    }


    override fun toString(): String {
        return "$tanMethod (medium: $tanMediaIdentifier): $messageToShowToUser"
    }

}