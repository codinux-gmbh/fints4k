package net.dankito.banking.fints.model

import net.dankito.utils.multiplatform.Date


open class GetTransactionsParameter(
    val alsoRetrieveBalance: Boolean = true,
    val fromDate: Date? = null,
    val toDate: Date? = null,

    /**
     * Be aware this is by far not supported by all banks.
     *
     * And it depends on the actual job if setting maxCountEntries is supported or not.
     *
     * // TODO: set a parameter in response if maxCountEntries is set but bank doesn't support it.
     */
    val maxCountEntries: Int? = null,
    val abortIfTanIsRequired: Boolean = false,
    val retrievedChunkListener: ((Collection<AccountTransaction>) -> Unit)? = null
) {

    internal var isSettingMaxCountEntriesAllowedByBank = false

    internal val maxCountEntriesIfSettingItIsAllowed: Int?
        get() = if (isSettingMaxCountEntriesAllowedByBank) maxCountEntries else null

}