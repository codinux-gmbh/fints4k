package net.dankito.banking.client.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import net.codinux.banking.fints.model.Amount
import net.codinux.banking.fints.model.Money
import net.codinux.banking.fints.extensions.UnixEpochStart


@Serializable
open class AccountTransaction(
    val amount: Money, // TODO: if we decide to stick with Money, create own type, don't use that one from fints.model (or move over from)
    val unparsedReference: String, // alternative names: purpose, reason

    val bookingDate: LocalDate,
    val valueDate: LocalDate,

    val otherPartyName: String?,
    val otherPartyBankCode: String?,
    val otherPartyAccountId: String?,

    val postingText: String?,
    val statementNumber: Int,
    val sheetNumber: Int?,

    val openingBalance: Money?,
    val closingBalance: Money?,

    val customerReference: String?,
    val bankReference: String?,
    val furtherInformation: String?,

    val endToEndReference: String?,
    val mandateReference: String?,
    val creditorIdentifier: String?,
    val originatorsIdentificationCode: String?,
    val compensationAmount: String?,
    val originalAmount: String?,
    val sepaReference: String?,
    val deviantOriginator: String?,
    val deviantRecipient: String?,
    val referenceWithNoSpecialType: String?,
    val primaNotaNumber: String?,
    val textKeySupplement: String?,

    val orderReferenceNumber: String?,
    val referenceNumber: String?
) {

    // for object deserializers
    internal constructor() : this(Money(Amount.Zero, ""), "", UnixEpochStart, UnixEpochStart, null, null, null, null)

    constructor(amount: Money, unparsedReference: String, bookingDate: LocalDate, valueDate: LocalDate, otherPartyName: String?, otherPartyBankCode: String?, otherPartyAccountId: String?, postingText: String?)
        : this(amount, unparsedReference, bookingDate, valueDate, otherPartyName, otherPartyBankCode, otherPartyAccountId, postingText,
        0, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null,  null, null,
        null, null, null, null)


    open val showOtherPartyName: Boolean
        get() = otherPartyName.isNullOrBlank() == false /* && type != "ENTGELTABSCHLUSS" && type != "AUSZAHLUNG" */ // TODO

    val reference: String
        get() = sepaReference ?: unparsedReference


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccountTransaction) return false

        if (amount != other.amount) return false
        if (unparsedReference != other.unparsedReference) return false
        if (bookingDate != other.bookingDate) return false
        if (otherPartyName != other.otherPartyName) return false
        if (otherPartyBankCode != other.otherPartyBankCode) return false
        if (otherPartyAccountId != other.otherPartyAccountId) return false
        if (postingText != other.postingText) return false
        if (valueDate != other.valueDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = amount.hashCode()
        result = 31 * result + unparsedReference.hashCode()
        result = 31 * result + bookingDate.hashCode()
        result = 31 * result + (otherPartyName?.hashCode() ?: 0)
        result = 31 * result + (otherPartyBankCode?.hashCode() ?: 0)
        result = 31 * result + (otherPartyAccountId?.hashCode() ?: 0)
        result = 31 * result + (postingText?.hashCode() ?: 0)
        result = 31 * result + valueDate.hashCode()
        return result
    }


    override fun toString(): String {
        return "$valueDate $amount $otherPartyName: $unparsedReference"
    }

}