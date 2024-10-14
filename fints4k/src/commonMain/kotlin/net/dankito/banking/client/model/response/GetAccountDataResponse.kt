package net.dankito.banking.client.model.response

import net.dankito.banking.client.model.AccountTransaction
import net.dankito.banking.client.model.CustomerAccount
import net.codinux.banking.fints.model.*


open class GetAccountDataResponse(
    error: ErrorCode?,
    errorMessage: String?,
    open val customerAccount: CustomerAccount?,
    messageLogWithoutSensitiveData: List<MessageLogEntry>,
    finTsModel: BankData? = null,
    serializedFinTsModel: String? = null
) : FinTsClientResponse(error, errorMessage, messageLogWithoutSensitiveData, finTsModel, serializedFinTsModel) {

    internal constructor() : this(null, null, null, listOf()) // for object deserializers


    open val retrievedTransactions: List<AccountTransaction>
        get() = customerAccount?.accounts?.flatMap { it.bookedTransactions } ?: listOf()

}