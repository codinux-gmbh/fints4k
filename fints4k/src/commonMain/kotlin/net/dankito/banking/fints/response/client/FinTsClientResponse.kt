package net.dankito.banking.fints.response.client

import net.dankito.banking.fints.model.JobContext
import net.dankito.banking.fints.model.MessageLogEntry
import net.dankito.banking.fints.response.BankResponse
import net.dankito.banking.fints.response.segments.TanResponse


open class FinTsClientResponse(

    open val successful: Boolean,

    open val didReceiveResponse: Boolean,

    open val noTanMethodSelected: Boolean,

    open val isStrongAuthenticationRequired: Boolean,
    open val tanRequired: TanResponse? = null,

    open val messageLogWithoutSensitiveData: List<MessageLogEntry>,

    /**
     * A fints4k internal error like an error occurred during web request or response parsing.
     */
    open val internalError: String? = null,

    open val errorMessagesFromBank: List<String> = listOf(),

    open val isPinLocked: Boolean = false,

    open val wrongCredentialsEntered: Boolean = false,

    open val userCancelledAction: Boolean = false,

    open val tanRequiredButWeWereToldToAbortIfSo: Boolean = false,

    // i think they can be removed
    open val isJobAllowed: Boolean = true,
    open val isJobVersionSupported: Boolean = true,
    open val allowedVersions: List<Int> = listOf(),
    open val supportedVersions: List<Int> = listOf()
) {


    constructor(context: JobContext, response: BankResponse) : this(response.successful, response.didReceiveResponse, response.noTanMethodSelected,
        response.isStrongAuthenticationRequired, response.tanResponse, context.messageLogWithoutSensitiveData,
        response.internalError, response.errorsToShowToUser, response.isPinLocked, response.wrongCredentialsEntered,
        response.tanRequiredButUserDidNotEnterOne, response.tanRequiredButWeWereToldToAbortIfSo,
        response.messageThatCouldNotBeCreated?.isJobAllowed ?: true,
        response.messageThatCouldNotBeCreated?.isJobVersionSupported ?: true,
        response.messageThatCouldNotBeCreated?.allowedVersions ?: listOf(),
        response.messageThatCouldNotBeCreated?.supportedVersions ?: listOf())


    open val errorMessage: String?
        get() = internalError
            ?: if (errorMessagesFromBank.isNotEmpty()) errorMessagesFromBank.joinToString("\n")
                else null

    open val didBankReturnError: Boolean
        get() = internalError == null && errorMessagesFromBank.isNotEmpty()


    override fun toString(): String {
        if (noTanMethodSelected) {
            return "Error: No TAN method selected"
        }

        if (isJobAllowed == false) {
            return "Error: Job is not allowed"
        }

        if (isJobVersionSupported == false) {
            return "Error: Job version is not supported. Supported versions are $supportedVersions"
        }

        return "successful = $successful"
    }

}