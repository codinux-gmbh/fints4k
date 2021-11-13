package net.dankito.banking.fints.model

import net.dankito.utils.multiplatform.Date
import kotlin.jvm.JvmOverloads


open class GetAccountTransactionsParameter @JvmOverloads constructor(
    bank: BankData,
    account: AccountData,
    alsoRetrieveBalance: Boolean = true,
    fromDate: Date? = null,
    toDate: Date? = null,

    /**
     * Be aware this is by far not supported by all banks.
     * And it depends on the actual job if setting maxCountEntries is supported or not.
     */
    maxCountEntries: Int? = null,
    abortIfTanIsRequired: Boolean = false,
    retrievedChunkListener: ((Collection<AccountTransaction>) -> Unit)? = null
) : GetTransactionsParameter(bank, alsoRetrieveBalance, fromDate, toDate, maxCountEntries, abortIfTanIsRequired, retrievedChunkListener) {

    open var account: AccountData = account
        internal set


    constructor(parameter: GetTransactionsParameter, account: AccountData) : this(parameter.bank, account, parameter.alsoRetrieveBalance,
        parameter.fromDate, parameter.toDate, parameter.maxCountEntries, parameter.abortIfTanIsRequired, parameter.retrievedChunkListener)

}