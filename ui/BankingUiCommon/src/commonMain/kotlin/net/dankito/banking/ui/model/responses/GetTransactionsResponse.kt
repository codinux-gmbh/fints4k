package net.dankito.banking.ui.model.responses

import net.dankito.utils.multiplatform.BigDecimal
import net.dankito.banking.ui.model.IAccountTransaction
import net.dankito.banking.ui.model.TypedBankAccount


open class GetTransactionsResponse(
    val bankAccount: TypedBankAccount,
    isSuccessful: Boolean,
    errorToShowToUser: String?,
    val bookedTransactions: List<IAccountTransaction> = listOf(),
    val unbookedTransactions: List<Any> = listOf(),
    val balance: BigDecimal? = null,
    userCancelledAction: Boolean = false,
    val tanRequiredButWeWereToldToAbortIfSo: Boolean = false
)
    : BankingClientResponse(isSuccessful, errorToShowToUser, userCancelledAction)
