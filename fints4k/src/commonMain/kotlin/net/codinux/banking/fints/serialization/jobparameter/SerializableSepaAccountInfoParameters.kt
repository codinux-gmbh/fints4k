package net.codinux.banking.fints.serialization.jobparameter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("SepaAccountInfoParameters")
class SerializableSepaAccountInfoParameters(
    override val jobParameters: SerializableJobParameters,

    val retrieveSingleAccountAllowed: Boolean,
    val nationalAccountRelationshipAllowed: Boolean,
    val structuredReferenceAllowed: Boolean,
    val settingMaxAllowedEntriesAllowed: Boolean,
    val countReservedReferenceLength: Int,
    val supportedSepaFormats: List<String>
) : DetailedSerializableJobParameters() {
    override fun toString() = "${super.toString()}, supportedSepaFormats = $supportedSepaFormats"
}