package net.codinux.banking.fints.response.segments


open class RetrieveAccountTransactionsParameters(
    parameters: JobParameters,
    open val serverTransactionsRetentionDays: Int,
    open val settingCountEntriesAllowed: Boolean,
    open val settingAllAccountAllowed: Boolean,
    open val supportedCamtDataFormats: List<String> = emptyList()
) : JobParameters(parameters) {

    internal constructor() : this(JobParameters(), -1, false, false) // for object deserializers

    // for languages not supporting default parameters
    constructor(parameters: JobParameters, serverTransactionsRetentionDays: Int, settingCountEntriesAllowed: Boolean, settingAllAccountAllowed: Boolean) :
            this(parameters, serverTransactionsRetentionDays, settingCountEntriesAllowed, settingAllAccountAllowed, emptyList())

}