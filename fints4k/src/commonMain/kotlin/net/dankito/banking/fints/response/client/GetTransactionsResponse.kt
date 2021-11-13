package net.dankito.banking.fints.response.client

import net.dankito.banking.fints.model.RetrievedAccountData


open class GetTransactionsResponse(
    open val retrievedResponses: List<GetAccountTransactionsResponse>,
    errorMessage: String? = null
) : FinTsClientResponse(isSuccessful(retrievedResponses), retrievedResponses.any { it.noTanMethodSelected },
    retrievedResponses.any { it.isStrongAuthenticationRequired }, retrievedResponses.map { it.tanRequired }.firstOrNull(),
    retrievedResponses.flatMap { it.messageLogWithoutSensitiveData },
    errorMessage ?: retrievedResponses.mapNotNull { it.internalError }.joinToString("\r\n"),
    retrievedResponses.flatMap { it.errorMessagesFromBank }, retrievedResponses.any { it.isPinLocked },
    retrievedResponses.any { it.wrongCredentialsEntered }, retrievedResponses.any { it.userCancelledAction },
    retrievedResponses.any { it.tanRequiredButWeWereToldToAbortIfSo },
    retrievedResponses.any { it.isJobAllowed }, retrievedResponses.any { it.isJobAllowed },
    retrievedResponses.flatMap { it.allowedVersions }.toSet().toList(),
    retrievedResponses.flatMap { it.supportedVersions }.toSet().toList()
) {

    companion object {

        fun isSuccessful(retrievedResponses: List<GetAccountTransactionsResponse>): Boolean {
            return retrievedResponses.isNotEmpty() &&
                    retrievedResponses.none { it.retrievedData?.account?.supportsRetrievingAccountTransactions == true && it.retrievedData?.successfullyRetrievedData == false }
        }

    }


    open val retrievedData: List<RetrievedAccountData>
        get() = retrievedResponses.mapNotNull { it.retrievedData }


    override fun toString(): String {
        return super.toString() + ": Retrieved data: $retrievedData"
    }

}