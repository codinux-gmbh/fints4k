package net.codinux.banking.fints.serialization.jobparameter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ChangeTanMediaParameters")
class SerializableChangeTanMediaParameters(
    override val jobParameters: SerializableJobParameters,

    val enteringTanListNumberRequired: Boolean,
    val enteringCardSequenceNumberRequired: Boolean,
    val enteringAtcAndTanRequired: Boolean,
    val enteringCardTypeAllowed: Boolean,
    val accountInfoRequired: Boolean,
    val allowedCardTypes: List<Int>
) : DetailedSerializableJobParameters()