package net.dankito.banking.ui.model.responses

import net.dankito.banking.ui.model.*


open class AddAccountResponse(
    open val bank: TypedBankData,
    retrievedData: List<RetrievedAccountData> = listOf(),
    errorToShowToUser: String? = null,
    didBankReturnError: Boolean = false,
    wrongCredentialsEntered: Boolean = false,
    userCancelledAction: Boolean = false
) : GetTransactionsResponse(retrievedData, errorToShowToUser, didBankReturnError, wrongCredentialsEntered, userCancelledAction) {

    constructor(bank: TypedBankData, errorToShowToUser: String?) : this(bank, listOf(), errorToShowToUser)


    override val successful: Boolean
        get() = noErrorOccurred
                && bank.accounts.isNotEmpty()

    open val supportsRetrievingTransactionsOfLast90DaysWithoutTan: Boolean
        get() = retrievedData.isNotEmpty() && retrievedData.any { it.successfullyRetrievedData }


    override fun toString(): String {
        return bank.toString() + " " + super.toString()
    }

}