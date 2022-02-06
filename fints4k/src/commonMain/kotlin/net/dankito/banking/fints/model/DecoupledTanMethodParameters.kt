package net.dankito.banking.fints.model


open class DecoupledTanMethodParameters(
    open val manualConfirmationAllowed: Boolean,
    open val periodicStateRequestsAllowed: Boolean,
    open val maxNumberOfStateRequests: Int,
    open val initialDelayInSecondsForStateRequests: Int,
    open val delayInSecondsForNextStateRequests: Int
) {

    override fun toString(): String {
        return "DecoupledTanMethodParameters(manualConfirmationAllowed=$manualConfirmationAllowed, periodicStateRequestsAllowed=$periodicStateRequestsAllowed, maxNumberOfStateRequests=$maxNumberOfStateRequests, initialDelayInSecondsForStateRequests=$initialDelayInSecondsForStateRequests, delayInSecondsForNextStateRequests=$delayInSecondsForNextStateRequests)"
    }

}