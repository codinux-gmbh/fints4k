package net.codinux.banking.fints.model

import kotlinx.serialization.Serializable


@Serializable
open class DecoupledTanMethodParameters(
    open val manualConfirmationAllowed: Boolean,
    open val periodicStateRequestsAllowed: Boolean,
    open val maxNumberOfStateRequests: Int,
    open val initialDelayInSecondsForStateRequest: Int,
    open val delayInSecondsForNextStateRequest: Int
) {

    override fun toString(): String {
        return "DecoupledTanMethodParameters(manualConfirmationAllowed=$manualConfirmationAllowed, periodicStateRequestsAllowed=$periodicStateRequestsAllowed, maxNumberOfStateRequests=$maxNumberOfStateRequests, initialDelayInSecondsForStateRequests=$initialDelayInSecondsForStateRequest, delayInSecondsForNextStateRequests=$delayInSecondsForNextStateRequest)"
    }

}