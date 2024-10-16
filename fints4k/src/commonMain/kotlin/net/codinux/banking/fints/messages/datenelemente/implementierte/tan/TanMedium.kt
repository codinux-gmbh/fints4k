package net.codinux.banking.fints.messages.datenelemente.implementierte.tan

import kotlinx.serialization.Serializable


/**
 * Informationen zu Art und Parametrisierung von TAN-Medien. Als TAN-Medien werden sowohl
 * TAN-Listen als auch DK-TAN-Generatoren / Karten oder Mobiltelefone sowie bilateral
 * vereinbarte Medien bezeichnet.
 *
 * Wird das Datenelement „TAN-Medium-Klasse“ mit „B“ (bilateral vereinbart) belegt, so muss im Element „Sicherheitsfunktion, kodiert“ die entsprechende Sicherheitsfunktion in der DEG „Verfahrensparameter Zwei-Schritt-Verfahren“ referenziert werden.
 */
@Serializable
open class TanMedium(
    open val mediumClass: TanMediumKlasse,
    open val status: TanMediumStatus,
    open val mediumName: String?,
    open val tanGenerator: TanGeneratorTanMedium? = null,
    open val mobilePhone: MobilePhoneTanMedium? = null
) {


    internal constructor() : this(TanMediumKlasse.AlleMedien, TanMediumStatus.Verfuegbar, null) // for object deserializers


    val identifier: String by lazy {
        "$mediumClass $mediumName $status ${tanGenerator?.cardNumber} ${mobilePhone?.concealedPhoneNumber ?: mobilePhone?.concealedPhoneNumber}"
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TanMedium

        if (mediumClass != other.mediumClass) return false
        if (status != other.status) return false
        if (mediumName != other.mediumName) return false
        if (tanGenerator != other.tanGenerator) return false
        if (mobilePhone != other.mobilePhone) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mediumClass.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + mediumName.hashCode()
        result = 31 * result + tanGenerator.hashCode()
        result = 31 * result + mobilePhone.hashCode()
        return result
    }


    override fun toString(): String {
        return "$mediumClass $mediumName $status"
    }

}