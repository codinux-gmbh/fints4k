package net.codinux.banking.fints.model

import kotlinx.serialization.Serializable
import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.Sicherheitsfunktion
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.AllowedTanFormat


@Serializable
open class TanMethod(
    open val displayName: String,
    open val securityFunction: Sicherheitsfunktion,
    open val type: TanMethodType,
    open val hhdVersion: HHDVersion? = null,
    open val maxTanInputLength: Int? = null,
    open val allowedTanFormat: AllowedTanFormat? = null,
    open val nameOfTanMediumRequired: Boolean = false,
    open val hktanVersion: Int = 6,
    open val decoupledParameters: DecoupledTanMethodParameters? = null
) {


    internal constructor() : this("", Sicherheitsfunktion.Einschritt_Verfahren, TanMethodType.EnterTan) // for object deserializers



    open val isNumericTan: Boolean
        get() = allowedTanFormat == AllowedTanFormat.Numeric


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TanMethod) return false

        if (displayName != other.displayName) return false
        if (securityFunction != other.securityFunction) return false
        if (type != other.type) return false
        if (nameOfTanMediumRequired != other.nameOfTanMediumRequired) return false

        return true
    }

    override fun hashCode(): Int {
        var result = displayName.hashCode()
        result = 31 * result + securityFunction.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + nameOfTanMediumRequired.hashCode()
        return result
    }


    override fun toString(): String {
        return "$displayName ($type, ${securityFunction.code})"
    }

}