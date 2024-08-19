package net.dankito.banking.fints.model

import kotlinx.datetime.LocalDate
import net.dankito.utils.multiplatform.extensions.UnixEpochStart


open class AccountTransaction(
    val account: AccountData,
    val amount: Money,
    val isReversal: Boolean,
    val unparsedReference: String,
    val bookingDate: LocalDate,
    val otherPartyName: String?,
    val otherPartyBankCode: String?,
    val otherPartyAccountId: String?,
    val bookingText: String?,
    val valueDate: LocalDate,
    val statementNumber: Int,
    val sequenceNumber: Int?,
    val openingBalance: Money?,
    val closingBalance: Money?,

    val endToEndReference: String?,
    val customerReference: String?,
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

    val currencyType: String?,
    val bookingKey: String,
    val referenceForTheAccountOwner: String,
    val referenceOfTheAccountServicingInstitution: String?,
    val supplementaryDetails: String?,

    val transactionReferenceNumber: String,
    val relatedReferenceNumber: String?
) {

    // for object deserializers
    internal constructor() : this(AccountData(), Money(Amount.Zero, ""), "", UnixEpochStart, null, null, null, null, UnixEpochStart)

    constructor(account: AccountData, amount: Money, unparsedReference: String, bookingDate: LocalDate, otherPartyName: String?, otherPartyBankCode: String?, otherPartyAccountId: String?, bookingText: String?, valueDate: LocalDate)
        : this(account, amount, false, unparsedReference, bookingDate, otherPartyName, otherPartyBankCode, otherPartyAccountId, bookingText, valueDate,
        0, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null,  null, null,
        null, "", "", null, null, "", null)


    open val showOtherPartyName: Boolean
        get() = otherPartyName.isNullOrBlank() == false /* && type != "ENTGELTABSCHLUSS" && type != "AUSZAHLUNG" */ // TODO

    val reference: String
        get() = sepaReference ?: unparsedReference


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccountTransaction) return false

        if (account != other.account) return false
        if (amount != other.amount) return false
        if (unparsedReference != other.unparsedReference) return false
        if (bookingDate != other.bookingDate) return false
        if (otherPartyName != other.otherPartyName) return false
        if (otherPartyBankCode != other.otherPartyBankCode) return false
        if (otherPartyAccountId != other.otherPartyAccountId) return false
        if (bookingText != other.bookingText) return false
        if (valueDate != other.valueDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = account.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + unparsedReference.hashCode()
        result = 31 * result + bookingDate.hashCode()
        result = 31 * result + (otherPartyName?.hashCode() ?: 0)
        result = 31 * result + (otherPartyBankCode?.hashCode() ?: 0)
        result = 31 * result + (otherPartyAccountId?.hashCode() ?: 0)
        result = 31 * result + (bookingText?.hashCode() ?: 0)
        result = 31 * result + valueDate.hashCode()
        return result
    }


    override fun toString(): String {
        return "$valueDate $amount $otherPartyName: $unparsedReference"
    }

}