package net.dankito.banking.fints.model

import kotlinx.serialization.Serializable
import net.dankito.banking.client.model.serializer.CurrencySerializer


@Serializable(with = CurrencySerializer::class)
open class Currency(
    val code: String
) {

    companion object {
        const val DefaultCurrencyCode = "EUR"
    }


    internal constructor() : this("") // for object deserializers


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Currency) return false

        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }


    override fun toString(): String {
        return code
    }

}