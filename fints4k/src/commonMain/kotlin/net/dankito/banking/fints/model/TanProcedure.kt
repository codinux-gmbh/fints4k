package net.dankito.banking.fints.model

import net.dankito.banking.fints.messages.datenelemente.implementierte.signatur.Sicherheitsfunktion
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.AllowedTanFormat


open class TanProcedure(
    open val displayName: String,
    open val securityFunction: Sicherheitsfunktion,
    open val type: TanProcedureType,
    open val hhdVersion: HHDVersion? = null,
    open val maxTanInputLength: Int? = null,
    open val allowedTanFormat: AllowedTanFormat = AllowedTanFormat.Alphanumeric,
    open val nameOfTanMediaRequired: Boolean = false
) {


    internal constructor() : this("", Sicherheitsfunktion.Einschritt_Verfahren, TanProcedureType.EnterTan) // for object deserializers



    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TanProcedure) return false

        if (displayName != other.displayName) return false
        if (securityFunction != other.securityFunction) return false
        if (type != other.type) return false
        if (nameOfTanMediaRequired != other.nameOfTanMediaRequired) return false

        return true
    }

    override fun hashCode(): Int {
        var result = displayName.hashCode()
        result = 31 * result + securityFunction.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + nameOfTanMediaRequired.hashCode()
        return result
    }


    override fun toString(): String {
        return "$displayName ($type, ${securityFunction.code})"
    }

}