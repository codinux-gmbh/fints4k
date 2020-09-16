package net.dankito.banking.ui.model

import net.dankito.utils.multiplatform.BigDecimal
import net.dankito.utils.multiplatform.Date


typealias TypedBankAccount = IBankAccount<IAccountTransaction>


interface IBankAccount<TTransaction: IAccountTransaction> : OrderedDisplayable {
    val customer: ICustomer<*, *>
    var identifier: String
    var accountHolderName: String
    var iban: String?
    var subAccountNumber: String?
    var customerId: String
    var balance: BigDecimal
    var currency: String
    var type: BankAccountType
    var productName: String?
    var accountLimit: String?
    var lastRetrievedTransactionsTimestamp: Date?
    var supportsRetrievingAccountTransactions: Boolean
    var supportsRetrievingBalance: Boolean
    var supportsTransferringMoney: Boolean
    var supportsInstantPaymentMoneyTransfer: Boolean
    var bookedTransactions: List<TTransaction>
    var unbookedTransactions: List<Any>
    var technicalId: String
    var haveAllTransactionsBeenFetched: Boolean
    var userSetDisplayName: String?


    override val displayName: String
        get() {
            return userSetDisplayName ?: productName ?: subAccountNumber ?: identifier
        }


    fun addBookedTransactions(retrievedBookedTransactions: List<TTransaction>) {
        val uniqueTransactions = this.bookedTransactions.toMutableSet()

        uniqueTransactions.addAll(retrievedBookedTransactions)

        this.bookedTransactions = uniqueTransactions.toList()
    }

    fun addUnbookedTransactions(retrievedUnbookedTransactions: List<Any>) {
        val uniqueUnbookedTransactions = this.unbookedTransactions.toMutableSet()

        uniqueUnbookedTransactions.addAll(retrievedUnbookedTransactions)

        this.unbookedTransactions = uniqueUnbookedTransactions.toList()
    }


    val stringRepresentation: String
        get() = "$accountHolderName ($identifier)"
    
}