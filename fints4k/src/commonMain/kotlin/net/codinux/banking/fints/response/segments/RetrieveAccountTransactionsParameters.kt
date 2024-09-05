package net.codinux.banking.fints.response.segments


open class RetrieveAccountTransactionsParameters(
    parameters: JobParameters,
    open val transactionsRetentionDays: Int,
    open val settingCountEntriesAllowed: Boolean,
    open val settingAllAccountAllowed: Boolean
) : JobParameters(parameters) {

    internal constructor() : this(JobParameters(), -1, false, false) // for object deserializers

}