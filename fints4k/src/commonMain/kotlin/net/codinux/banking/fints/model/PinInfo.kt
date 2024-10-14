package net.codinux.banking.fints.model

import kotlinx.serialization.Serializable

@Serializable
open class PinInfo(
    val minPinLength: Int?,
    val maxPinLength: Int?,
    val minTanLength: Int?,
    val userIdHint: String?,
    val customerIdHint: String?
)