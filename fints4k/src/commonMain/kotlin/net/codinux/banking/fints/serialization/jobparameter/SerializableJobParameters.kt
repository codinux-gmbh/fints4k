package net.codinux.banking.fints.serialization.jobparameter

import kotlinx.serialization.Serializable

@Serializable
class SerializableJobParameters(
    val jobName: String,
    val maxCountJobs: Int,
    val minimumCountSignatures: Int,
    val securityClass: Int?,

    val segmentId: String,
    val segmentNumber: Int,
    val segmentVersion: Int,

    val segmentString: String
) {
    override fun toString() = "$jobName $segmentVersion"
}