package net.dankito.banking.fints.model

import net.dankito.utils.multiplatform.Date


open class RetrievedAccountData(
    open val account: AccountData,
    open val successfullyRetrievedData: Boolean,
    open val balance: Money?,
    open var bookedTransactions: Collection<AccountTransaction>,
    open var unbookedTransactions: Collection<Any>,
    open val retrievedTransactionsFrom: Date?,
    open val retrievedTransactionsTo: Date?,
    open val errorMessage: String? = null
) {

    companion object {

        fun balanceAndTransactionsNotRequestedByUser(account: AccountData): RetrievedAccountData {
            return RetrievedAccountData(account, true, null, listOf(), listOf(), null, null)
        }

        fun unsuccessful(account: AccountData): RetrievedAccountData {
            return RetrievedAccountData(account, false, null, listOf(), listOf(), null, null)
        }

        fun unsuccessfulList(account: AccountData): List<RetrievedAccountData> {
            return listOf(unsuccessful(account))
        }

    }


    override fun toString(): String {
        return "Was retrieving AccountData for ${account.accountIdentifier} successful? $successfullyRetrievedData. Balance = ${balance}, ${bookedTransactions.size} transactions"
    }

}