package net.dankito.banking.ui.model

import net.dankito.utils.multiplatform.BigDecimal
import net.dankito.utils.multiplatform.Date
import net.dankito.utils.multiplatform.DateFormatStyle
import net.dankito.utils.multiplatform.DateFormatter


interface IAccountTransaction {

    companion object {
        val IdDateFormat = DateFormatter("yyyy.MM.dd")
    }


    val bankAccount: IBankAccount<*>
    val amount: BigDecimal
    val currency: String
    val unparsedUsage: String
    val bookingDate: Date
    val otherPartyName: String?
    val otherPartyBankCode: String?
    val otherPartyAccountId: String?
    val bookingText: String?
    val valueDate: Date
    val statementNumber: Int
    val sequenceNumber: Int?
    val openingBalance: BigDecimal?
    val closingBalance: BigDecimal?

    val endToEndReference: String?
    val customerReference: String?
    val mandateReference: String?
    val creditorIdentifier: String?
    val originatorsIdentificationCode: String?
    val compensationAmount: String?
    val originalAmount: String?
    val sepaUsage: String?
    val deviantOriginator: String?
    val deviantRecipient: String?
    val usageWithNoSpecialType: String?
    val primaNotaNumber: String?
    val textKeySupplement: String?

    val currencyType: String?
    val bookingKey: String
    val referenceForTheAccountOwner: String
    val referenceOfTheAccountServicingInstitution: String?
    val supplementaryDetails: String?

    val transactionReferenceNumber: String
    val relatedReferenceNumber: String?


    var technicalId: String

    open val transactionIdentifier: String
        get() = buildTransactionIdentifier()


    val showOtherPartyName: Boolean
        get() = otherPartyName.isNullOrBlank() == false /* && type != "ENTGELTABSCHLUSS" && type != "AUSZAHLUNG" */ // TODO

    val canCreateMoneyTransferFrom: Boolean
        get() = otherPartyAccountId != null && bankAccount.supportsTransferringMoney

    val usage: String
        get() = sepaUsage ?: unparsedUsage


    fun buildTransactionIdentifier() : String {
        if (bankAccount != null) {
            return "${bankAccount.technicalId} ${IdDateFormat.format(bookingDate)} ${IdDateFormat.format(valueDate)} $amount $currency $unparsedUsage $otherPartyName $otherPartyBankCode $otherPartyAccountId"
        }
        else { // happens for derived classes during initialization. These have to set technicalId after initialization by themselves
            return "<uninitialized_bank_acccount> ${IdDateFormat.format(bookingDate)} ${IdDateFormat.format(valueDate)} $amount $currency $unparsedUsage $otherPartyName $otherPartyBankCode $otherPartyAccountId"
        }
    }



    fun doesEqual(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IAccountTransaction) return false

        if (bankAccount != other.bankAccount) return false
        if (amount != other.amount) return false
        if (currency != other.currency) return false
        if (unparsedUsage != other.unparsedUsage) return false
        if (bookingDate != other.bookingDate) return false
        if (otherPartyName != other.otherPartyName) return false
        if (otherPartyBankCode != other.otherPartyBankCode) return false
        if (otherPartyAccountId != other.otherPartyAccountId) return false
        if (bookingText != other.bookingText) return false
        if (valueDate != other.valueDate) return false

        return true
    }

    fun calculateHashCode(): Int {
        var result = bankAccount.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + currency.hashCode()
        result = 31 * result + unparsedUsage.hashCode()
        result = 31 * result + bookingDate.hashCode()
        result = 31 * result + (otherPartyName?.hashCode() ?: 0)
        result = 31 * result + (otherPartyBankCode?.hashCode() ?: 0)
        result = 31 * result + (otherPartyAccountId?.hashCode() ?: 0)
        result = 31 * result + (bookingText?.hashCode() ?: 0)
        result = 31 * result + valueDate.hashCode()
        return result
    }

    val stringRepresentation: String
        get() = "${DateFormatter(DateFormatStyle.Medium).format(valueDate)} $amount $otherPartyName: $usage"
    
}