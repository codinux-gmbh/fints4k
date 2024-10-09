package net.codinux.banking.fints.messages.datenelemente.implementierte.tan

import kotlinx.datetime.LocalDate


open class TanGeneratorTanMedium(
    mediumClass: TanMediumKlasse,
    status: TanMediumStatus,
    val cardNumber: String,
    val cardSequenceNumber: String?,
    val cardType: Int?,
    val validFrom: LocalDate?,
    val validTo: LocalDate?,
    mediumName: String?
) : TanMedium(mediumClass, status, mediumName) {


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as TanGeneratorTanMedium

        if (cardNumber != other.cardNumber) return false
        if (cardSequenceNumber != other.cardSequenceNumber) return false
        if (cardType != other.cardType) return false
        if (validFrom != other.validFrom) return false
        if (validTo != other.validTo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + cardNumber.hashCode()
        result = 31 * result + cardSequenceNumber.hashCode()
        result = 31 * result + cardType.hashCode()
        result = 31 * result + validFrom.hashCode()
        result = 31 * result + validTo.hashCode()
        return result
    }


    override fun toString(): String {
        return super.toString() + " $mediumName $cardNumber (card sequence number: ${cardSequenceNumber ?: "-"})"
    }

}