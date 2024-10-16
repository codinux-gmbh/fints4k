package net.codinux.banking.fints.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.codinux.banking.fints.FinTsClient
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Laenderkennzeichen
import net.codinux.banking.fints.messages.segmente.id.CustomerSegmentId
import net.codinux.banking.fints.response.segments.AccountType
import net.codinux.banking.fints.response.segments.JobParameters

@Serializable
open class AccountData(
    open val accountIdentifier: String,
    open val subAccountAttribute: String?,
    open val bankCountryCode: Int,
    open val bankCode: String,
    open val iban: String?,
    open val customerId: String,
    open val accountType: AccountType?,
    open val currency: String?, // TODO: may parse to a value object
    open val accountHolderName: String,
    open val productName: String?,
    open val accountLimit: String?,
    open val allowedJobNames: List<String>,
    @Transient // can be restored from bank.supportedJobs and this.allowedJobNames
    open var allowedJobs: List<JobParameters> = listOf()
) {

    internal constructor() : this("", null, Laenderkennzeichen.Germany, "", null, "", null, null, "", null, null, listOf()) // for object deserializers


    open val isAccountTypeSupportedByApplication: Boolean
        get() = FinTsClient.SupportedAccountTypes.contains(accountType)
                || allowedJobNames.contains(CustomerSegmentId.Balance.id)
                || allowedJobNames.contains(CustomerSegmentId.AccountTransactionsMt940.id)
                || allowedJobNames.contains(CustomerSegmentId.SecuritiesAccountBalance.id)



    /**
     * Count days for which transactions are stored on bank server (if available).
     */
    open var serverTransactionsRetentionDays: Int? = null


    @SerialName("supportedFeatures")
    protected open val _supportedFeatures = mutableSetOf<AccountFeature>()

    open val supportedFeatures: Collection<AccountFeature>
        get() = ArrayList(_supportedFeatures) // make a copy, don't pass original (and mutable) _supportedFeatures Set to outside


    open val supportsRetrievingBalance: Boolean
        get() = supportsFeature(AccountFeature.RetrieveBalance)

    open val supportsRetrievingAccountTransactions: Boolean
        get() = supportsFeature(AccountFeature.RetrieveAccountTransactions)

    open val supportsTransferringMoney: Boolean
        get() = supportsFeature(AccountFeature.TransferMoney)

    open val supportsRealTimeTransfer: Boolean
        get() = supportsFeature(AccountFeature.RealTimeTransfer)


    open fun supportsFeature(feature: AccountFeature): Boolean {
        return _supportedFeatures.contains(feature)
    }

    open fun setSupportsFeature(feature: AccountFeature, isSupported: Boolean) {
        if (isSupported) {
            _supportedFeatures.add(feature)
        }
        else {
            _supportedFeatures.remove(feature)
        }
    }


    override fun toString(): String {
        return "$productName $accountIdentifier $accountHolderName"
    }

}