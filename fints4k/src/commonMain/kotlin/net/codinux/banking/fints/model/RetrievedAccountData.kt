package net.codinux.banking.fints.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import net.codinux.banking.fints.extensions.nowAtEuropeBerlin


open class RetrievedAccountData(
    open val account: AccountData,
    open val successfullyRetrievedData: Boolean,
    open val balance: Money?,
    open var bookedTransactions: Collection<AccountTransaction>,
    open var unbookedTransactions: Collection<Any>,
    open val retrievalTime: LocalDateTime,
    open val retrievedTransactionsFrom: LocalDate?,
    open val retrievedTransactionsTo: LocalDate?,
    open val errorMessage: String? = null
) {

    companion object {

        fun unsuccessful(account: AccountData): RetrievedAccountData {
            return RetrievedAccountData(account, false, null, listOf(), listOf(), LocalDateTime.nowAtEuropeBerlin(), null, null)
        }

    }


    override fun toString(): String {
        return "Was retrieving AccountData for ${account.accountIdentifier} successful? $successfullyRetrievedData. Balance = ${balance}, ${bookedTransactions.size} transactions"
    }

}