package net.codinux.banking.fints.response.client

import net.codinux.banking.fints.model.JobContext
import net.codinux.banking.fints.model.RetrievedAccountData
import net.codinux.banking.fints.response.BankResponse


open class GetAccountTransactionsResponse(
    context: JobContext,
    response: BankResponse,
    open val retrievedData: RetrievedAccountData?,
    /**
     * This value is only set if [GetTransactionsParameter.maxCountEntries] was set to tell caller if maxCountEntries parameter has been evaluated or not
     */
    open var isSettingMaxCountEntriesAllowedByBank: Boolean? = null
) : FinTsClientResponse(context, response) {

    override val successful: Boolean
        get() = super.successful
                && retrievedData?.successfullyRetrievedData == true

    override val internalError: String?
        get() = super.internalError
            ?: retrievedData?.errorMessage


    override fun toString(): String {
        return super.toString() + ": Retrieved data: $retrievedData"
    }

}