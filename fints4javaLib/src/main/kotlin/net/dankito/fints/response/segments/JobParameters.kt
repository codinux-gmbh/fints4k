package net.dankito.fints.response.segments


open class JobParameters(
    val jobName: String,
    val maxCountJobs: Int,
    val minimumCountSignatures: Int,
    val securityClass: Int?,
    segmentString: String
)
    : ReceivedSegment(segmentString) {


    override fun toString(): String {
        return "$jobName $segmentVersion"
    }

}