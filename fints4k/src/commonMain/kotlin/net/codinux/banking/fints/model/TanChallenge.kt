package net.codinux.banking.fints.model

import kotlinx.datetime.Instant
import net.codinux.banking.fints.extensions.nowExt
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMedium
import net.codinux.banking.fints.response.BankResponse
import net.codinux.banking.fints.response.client.FinTsClientResponse
import net.codinux.log.Log


open class TanChallenge(
    val forAction: ActionRequiringTan,
    val messageToShowToUser: String,
    val challenge: String,
    val tanMethod: TanMethod,
    val tanMediaIdentifier: String?,
    val bank: BankData,
    val account: AccountData? = null,
    /**
     * Datum und Uhrzeit, bis zu welchem Zeitpunkt eine TAN auf Basis der gesendeten Challenge gültig ist. Nach Ablauf der Gültigkeitsdauer wird die entsprechende TAN entwertet.
     *
     * In server's time zone, that is Europe/Berlin.
     */
    val tanExpirationTime: Instant? = null,
    val challengeCreationTimestamp: Instant = Instant.nowExt()
) {

    var enterTanResult: EnterTanResult? = null
        private set

    open val isEnteringTanDone: Boolean
        get() = enterTanResult != null

    private val tanExpiredCallbacks = mutableListOf<() -> Unit>()

    private val userApprovedDecoupledTanCallbacks = mutableListOf<() -> Unit>()


    fun userEnteredTan(enteredTan: String) {
        this.enterTanResult = EnterTanResult(enteredTan.replace(" ", ""))
    }

    internal fun userApprovedDecoupledTan(responseAfterApprovingDecoupledTan: BankResponse) {
        this.enterTanResult = EnterTanResult(null, true, responseAfterApprovingDecoupledTan)

        userApprovedDecoupledTanCallbacks.toTypedArray().forEach { // copy to avoid ConcurrentModificationException
            try {
                it.invoke()
            } catch (e: Throwable) {
                Log.error(e) { "Could not call userApprovedDecoupledTanCallback" }
            }
        }
        clearUserApprovedDecoupledTanCallbacks()
    }

    fun userDidNotEnterTan() {
        clearUserApprovedDecoupledTanCallbacks()

        this.enterTanResult = EnterTanResult(null)
    }

    internal fun tanExpired() {
        tanExpiredCallbacks.toTypedArray().forEach {
            try {
                it.invoke()
            } catch (e: Throwable) {
                Log.error(e) { "Could not call tanExpiredCallback" }
            }
        }

        clearTanExpiredCallbacks()

        userDidNotEnterTan()
    }

    fun userAsksToChangeTanMethod(changeTanMethodTo: TanMethod) {
        clearUserApprovedDecoupledTanCallbacks()

        this.enterTanResult = EnterTanResult(null, changeTanMethodTo = changeTanMethodTo)
    }

    fun userAsksToChangeTanMedium(changeTanMediumTo: TanMedium, changeTanMediumResultCallback: ((FinTsClientResponse) -> Unit)?) {
        clearUserApprovedDecoupledTanCallbacks()

        this.enterTanResult = EnterTanResult(null, changeTanMediumTo = changeTanMediumTo, changeTanMediumResultCallback = changeTanMediumResultCallback)
    }


    fun addTanExpiredCallback(callback: () -> Unit) {
        if (isEnteringTanDone == false) {
            this.tanExpiredCallbacks.add(callback)
        }
    }

    protected open fun clearTanExpiredCallbacks() {
        tanExpiredCallbacks.clear()
    }

    fun addUserApprovedDecoupledTanCallback(callback: () -> Unit) {
        if (isEnteringTanDone == false) {
            this.userApprovedDecoupledTanCallbacks.add(callback)
        } else if (enterTanResult != null && enterTanResult!!.userApprovedDecoupledTan == true) {
            callback()
        }
    }

    protected open fun clearUserApprovedDecoupledTanCallbacks() {
        userApprovedDecoupledTanCallbacks.clear()
    }


    override fun toString(): String {
        return "$tanMethod (medium: $tanMediaIdentifier): $messageToShowToUser"
    }

}