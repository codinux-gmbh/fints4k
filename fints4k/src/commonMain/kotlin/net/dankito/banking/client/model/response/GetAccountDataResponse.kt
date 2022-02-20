package net.dankito.banking.client.model.response

import net.dankito.banking.client.model.AccountTransaction
import net.dankito.banking.client.model.CustomerAccount
import net.dankito.banking.fints.model.*


open class GetAccountDataResponse(
    error: ErrorCode?,
    errorMessage: String?,
    open val customerAccount: CustomerAccount?,
    messageLogWithoutSensitiveData: List<MessageLogEntry>,
    finTsModel: BankData? = null
) : FinTsClientResponse(error, errorMessage, messageLogWithoutSensitiveData, finTsModel) {

    open val retrievedTransactions: List<AccountTransaction>
        get() = customerAccount?.accounts?.flatMap { it.bookedTransactions } ?: listOf()

}