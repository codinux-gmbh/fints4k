package net.dankito.banking.client.model.response

import net.dankito.banking.client.model.AccountTransaction
import net.dankito.banking.client.model.CustomerAccount
import net.codinux.banking.fints.model.*


open class GetAccountDataResponse(
    error: ErrorCode?,
    errorMessage: String?,
    open val customerAccount: CustomerAccount?,
    messageLog: List<MessageLogEntry>,
    finTsModel: BankData? = null
) : FinTsClientResponse(error, errorMessage, messageLog, finTsModel) {

    internal constructor() : this(null, null, null, listOf()) // for object deserializers


    open val retrievedTransactions: List<AccountTransaction>
        get() = customerAccount?.accounts?.flatMap { it.bookedTransactions } ?: listOf()

}