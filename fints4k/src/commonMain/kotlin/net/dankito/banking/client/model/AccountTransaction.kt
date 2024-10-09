package net.dankito.banking.client.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import net.codinux.banking.fints.model.Amount
import net.codinux.banking.fints.model.Money
import net.codinux.banking.fints.extensions.UnixEpochStart


@Serializable
open class AccountTransaction(
    val amount: Money, // TODO: if we decide to stick with Money, create own type, don't use that one from fints.model (or move over from)
    val reference: String?, // alternative names: purpose, reason

    val bookingDate: LocalDate,
    val valueDate: LocalDate,

    val otherPartyName: String?,
    val otherPartyBankId: String?,
    val otherPartyAccountId: String?,

    val postingText: String?,

    val openingBalance: Money?,
    val closingBalance: Money?,

    val statementNumber: Int,
    val sheetNumber: Int?,

    val customerReference: String?,
    val bankReference: String?,
    val furtherInformation: String?,

    val endToEndReference: String?,
    val mandateReference: String?,
    val creditorIdentifier: String?,
    val originatorsIdentificationCode: String?,

    val compensationAmount: String?,
    val originalAmount: String?,
    val deviantOriginator: String?,
    val deviantRecipient: String?,
    val referenceWithNoSpecialType: String?,

    val journalNumber: String?,
    val textKeyAddition: String?,

    val orderReferenceNumber: String?,
    val referenceNumber: String?,

    val isReversal: Boolean
) {

    // for object deserializers
    internal constructor() : this(Money(Amount.Zero, ""), "", UnixEpochStart, UnixEpochStart, null, null, null, null)

    constructor(amount: Money, unparsedReference: String, bookingDate: LocalDate, valueDate: LocalDate, otherPartyName: String?, otherPartyBankId: String?, otherPartyAccountId: String?, postingText: String?)
        : this(amount, unparsedReference, bookingDate, valueDate, otherPartyName, otherPartyBankId, otherPartyAccountId, postingText,
        null, null, 0, null,
        null, null, null, null, null, null, null, null, null, null, null,  null,
        null, null, null, null, false)


    open val showOtherPartyName: Boolean
        get() = otherPartyName.isNullOrBlank() == false /* && type != "ENTGELTABSCHLUSS" && type != "AUSZAHLUNG" */ // TODO


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccountTransaction) return false

        if (amount != other.amount) return false
        if (reference != other.reference) return false
        if (bookingDate != other.bookingDate) return false
        if (otherPartyName != other.otherPartyName) return false
        if (otherPartyBankId != other.otherPartyBankId) return false
        if (otherPartyAccountId != other.otherPartyAccountId) return false
        if (postingText != other.postingText) return false
        if (valueDate != other.valueDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = amount.hashCode()
        result = 31 * result + reference.hashCode()
        result = 31 * result + bookingDate.hashCode()
        result = 31 * result + otherPartyName.hashCode()
        result = 31 * result + otherPartyBankId.hashCode()
        result = 31 * result + otherPartyAccountId.hashCode()
        result = 31 * result + postingText.hashCode()
        result = 31 * result + valueDate.hashCode()
        return result
    }


    override fun toString(): String {
        return "$valueDate $amount $otherPartyName: $reference"
    }

}