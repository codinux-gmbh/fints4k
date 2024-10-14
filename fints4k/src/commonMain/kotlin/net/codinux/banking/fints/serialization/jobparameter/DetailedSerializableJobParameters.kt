package net.codinux.banking.fints.serialization.jobparameter

import kotlinx.serialization.Serializable

@Serializable
sealed class DetailedSerializableJobParameters {

    abstract val jobParameters: SerializableJobParameters


    override fun toString() = jobParameters.toString()

}