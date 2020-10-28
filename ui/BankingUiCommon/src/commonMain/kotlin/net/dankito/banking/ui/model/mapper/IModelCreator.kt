package net.dankito.banking.ui.model.mapper

import net.dankito.banking.ui.model.*
import net.dankito.banking.ui.model.tan.*
import net.dankito.utils.multiplatform.BigDecimal
import net.dankito.utils.multiplatform.Date


interface IModelCreator {
    
    fun createBank(bankCode: String, userName: String, password: String, finTsServerAddress: String, bankName: String, bic: String,
        customerName: String = "", userId: String = userName, iconData: ByteArray? = null): TypedBankData


    fun createAccount(bank: TypedBankData, productName: String?, identifier: String) : TypedBankAccount


    fun createTransaction(
        account: TypedBankAccount,
        amount: BigDecimal,
        currency: String,
        unparsedReference: String,
        bookingDate: Date,
        otherPartyName: String?,
        otherPartyBankCode: String?,
        otherPartyAccountId: String?,
        bookingText: String?,
        valueDate: Date,
        statementNumber: Int = 1,
        sequenceNumber: Int? = null,
        openingBalance: BigDecimal? = null,
        closingBalance: BigDecimal? = null,

        endToEndReference: String? = null,
        customerReference: String? = null,
        mandateReference: String? = null,
        creditorIdentifier: String? = null,
        originatorsIdentificationCode: String? = null,
        compensationAmount: String? = null,
        originalAmount: String? = null,
        sepaReference: String? = null,
        deviantOriginator: String? = null,
        deviantRecipient: String? = null,
        referenceWithNoSpecialType: String? = null,
        primaNotaNumber: String? = null,
        textKeySupplement: String? = null,

        currencyType: String? = null,
        bookingKey: String = "",
        referenceForTheAccountOwner: String = "",
        referenceOfTheAccountServicingInstitution: String? = null,
        supplementaryDetails: String? = null,

        transactionReferenceNumber: String = "",
        relatedReferenceNumber: String? = null
    ) : IAccountTransaction


    fun createTanMethod(displayName: String, type: TanMethodType, bankInternalMethodCode: String,
                        maxTanInputLength: Int? = null, allowedTanFormat: AllowedTanFormat = AllowedTanFormat.Alphanumeric): TanMethod {
        return TanMethod(displayName, type, bankInternalMethodCode, maxTanInputLength)
    }

    fun createTanMedium(displayName: String, status: TanMediumStatus): TanMedium {
        return TanMedium(displayName, status)
    }

    fun createTanGeneratorTanMedium(displayName: String, status: TanMediumStatus, cardNumber: String): TanGeneratorTanMedium {
        return TanGeneratorTanMedium(displayName, status, cardNumber)
    }

    fun createMobilePhoneTanMedium(displayName: String, status: TanMediumStatus, phoneNumber: String?): MobilePhoneTanMedium {
        return MobilePhoneTanMedium(displayName, status, phoneNumber)
    }

}