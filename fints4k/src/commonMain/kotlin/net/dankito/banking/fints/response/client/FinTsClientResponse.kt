package net.dankito.banking.fints.response.client

import net.dankito.banking.fints.response.BankResponse
import net.dankito.banking.fints.response.segments.TanResponse


open class FinTsClientResponse(

    open val successful: Boolean,

    open val noTanMethodSelected: Boolean,

    open val isStrongAuthenticationRequired: Boolean,
    open val tanRequired: TanResponse? = null,

    open val errorsToShowToUser: List<String> = listOf(),

    /**
     * When a serious error occurred during web request or response parsing.
     */
    open val errorMessage: String? = null,

    open val wrongCredentialsEntered: Boolean = false,

    open val userCancelledAction: Boolean = false,

    open val tanRequiredButWeWereToldToAbortIfSo: Boolean = false,

    // i think they can be removed
    open val isJobAllowed: Boolean = true,
    open val isJobVersionSupported: Boolean = true,
    open val allowedVersions: List<Int> = listOf(),
    open val supportedVersions: List<Int> = listOf()
) {


    constructor(response: BankResponse) : this(response.successful, response.noTanMethodSelected,
        response.isStrongAuthenticationRequired, response.tanResponse, response.errorsToShowToUser,
        response.errorMessage, response.wrongCredentialsEntered,
        response.tanRequiredButUserDidNotEnterOne, response.tanRequiredButWeWereToldToAbortIfSo,
        response.messageThatCouldNotBeCreated?.isJobAllowed ?: true,
        response.messageThatCouldNotBeCreated?.isJobVersionSupported ?: true,
        response.messageThatCouldNotBeCreated?.allowedVersions ?: listOf(),
        response.messageThatCouldNotBeCreated?.supportedVersions ?: listOf())


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