package net.codinux.banking.fints.model

import kotlinx.serialization.Serializable
import net.dankito.banking.client.model.serializer.AmountSerializer

/**
 * Be aware: The decimal separator is as specified by FinTS standard ',', not '.'.
 */
@Serializable(with = AmountSerializer::class)
open class Amount(
    val string: String
) {

    companion object {
        val ZeroString = "0,"

        val Zero = Amount(ZeroString)
    }


    internal constructor() : this(ZeroString) // for object deserializers


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Amount) return false

        if (string != other.string) return false

        return true
    }

    override fun hashCode(): Int {
        return string.hashCode()
    }


    override fun toString(): String {
        return string
    }

}