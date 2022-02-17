package net.dankito.banking.fints.model


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