package net.codinux.banking.fints.model

import kotlinx.datetime.LocalDate
import kotlin.jvm.JvmOverloads


open class GetTransactionsParameter @JvmOverloads constructor(
    open val bank: BankData,
    open val alsoRetrieveBalance: Boolean = true,
    open val fromDate: LocalDate? = null,
    open val toDate: LocalDate? = null,

    /**
     * Be aware this is by far not supported by all banks.
     *
     * And it depends on the actual job if setting maxCountEntries is supported or not.
     */
    open val maxCountEntries: Int? = null,
    open val abortIfTanIsRequired: Boolean = false,
    open val retrievedChunkListener: ((Collection<AccountTransaction>) -> Unit)? = null
) {

    internal open var isSettingMaxCountEntriesAllowedByBank = false

    internal open val maxCountEntriesIfSettingItIsAllowed: Int?
        get() = if (isSettingMaxCountEntriesAllowedByBank) maxCountEntries else null

}