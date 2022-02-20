package net.dankito.banking.client.model.parameter

import kotlinx.datetime.LocalDate
import net.dankito.banking.fints.model.BankData
import net.dankito.banking.fints.model.TanMethodType
import net.dankito.banking.client.model.BankAccountIdentifier


open class GetAccountDataParameter(
    bankCode: String,
    loginName: String,
    password: String,
    finTsServerAddress: String, // TODO: get rid of this
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
    preferredTanMedium: String? = null,
    abortIfTanIsRequired: Boolean = false,
    finTsModel: BankData? = null
) : FinTsClientParameter(bankCode, loginName, password, finTsServerAddress, preferredTanMethods, preferredTanMedium, abortIfTanIsRequired, finTsModel) {

    open val retrieveOnlyAccountInfo: Boolean
        get() = retrieveBalance == false && retrieveTransactions == RetrieveTransactions.No

}