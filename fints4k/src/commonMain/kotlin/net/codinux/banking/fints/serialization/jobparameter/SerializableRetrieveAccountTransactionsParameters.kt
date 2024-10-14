package net.codinux.banking.fints.serialization.jobparameter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("RetrieveAccountTransactionsParameters")
class SerializableRetrieveAccountTransactionsParameters(
    override val jobParameters: SerializableJobParameters,

    val serverTransactionsRetentionDays: Int,
    val settingCountEntriesAllowed: Boolean,
    val settingAllAccountAllowed: Boolean,
    val supportedCamtDataFormats: List<String> = emptyList()
) : DetailedSerializableJobParameters() {
    override fun toString() = "${super.toString()}, serverTransactionsRetentionDays = $serverTransactionsRetentionDays, supportedCamtDataFormats = $supportedCamtDataFormats"
}