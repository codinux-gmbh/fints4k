package net.dankito.banking.client.model.parameter

import kotlinx.datetime.LocalDate
import net.codinux.banking.fints.model.BankData
import net.codinux.banking.fints.model.TanMethodType
import net.dankito.banking.client.model.BankAccountIdentifier


open class GetAccountDataParameter(
    bankCode: String,
    loginName: String,
    password: String,
    /**
     * Optionally specify for which bank account to retrieve the account data.
     * If not set the data for all bank accounts of this account will be retrieved.
     */
    open val accounts: List<BankAccountIdentifier>? = null,
    open val retrieveBalance: Boolean = true,
    open val retrieveTransactions: RetrieveTransactions = RetrieveTransactions.OfLast90Days,
    open val retrieveTransactionsFrom: LocalDate? = null,
    open val retrieveTransactionsTo: LocalDate? = null,

    preferredTanMethods: List<TanMethodType>? = null,
    tanMethodsNotSupportedByApplication: List<TanMethodType>? = null,
    preferredTanMedium: String? = null,
    abortIfTanIsRequired: Boolean = false,
    finTsModel: BankData? = null,
    serializedFinTsModel: String? = null,
    open val defaultBankValues: BankData? = null
) : FinTsClientParameter(bankCode, loginName, password, preferredTanMethods, tanMethodsNotSupportedByApplication, preferredTanMedium, abortIfTanIsRequired, finTsModel, serializedFinTsModel) {

    open val retrieveOnlyAccountInfo: Boolean
        get() = retrieveBalance == false && retrieveTransactions == RetrieveTransactions.No

}