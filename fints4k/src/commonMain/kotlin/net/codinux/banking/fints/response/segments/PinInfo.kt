package net.codinux.banking.fints.response.segments

import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.JobTanConfiguration


open class PinInfo(
    parameters: JobParameters,
    val minPinLength: Int?,
    val maxPinLength: Int?,
    val minTanLength: Int?,
    val userIdHint: String?,
    val customerIdHint: String?,
    val jobTanConfiguration: List<JobTanConfiguration>
)
    : JobParameters(parameters) {

    internal constructor() : this(JobParameters(), null, null, null, null, null, listOf()) // for object deserializers

}