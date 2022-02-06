package net.dankito.banking.fints.response.client

import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.BankResponse


open class AddAccountResponse(
    context: JobContext,
    getAccountsResponse: BankResponse,
    open val retrievedTransactionsResponses: List<GetAccountTransactionsResponse> = listOf()
) : FinTsClientResponse(context, getAccountsResponse) {

    open val bank: BankData = context.bank

    override val successful: Boolean
        get() = super.successful && bank.accounts.isNotEmpty()
                && bank.accounts.size == retrievedTransactionsResponses.size
                && retrievedTransactionsResponses.none { it.noTanMethodSelected }
                && retrievedTransactionsResponses.none { it.isPinLocked }
                && retrievedTransactionsResponses.none { it.wrongCredentialsEntered }
                && retrievedTransactionsResponses.none { it.internalError != null }
                && retrievedTransactionsResponses.none { it.errorMessagesFromBank.isNotEmpty() }

    override val internalError: String?
        get() = super.internalError
            ?: retrievedTransactionsResponses.mapNotNull { it.internalError }.joinToString("\r\n")
                .ifBlank { null } // if mapNotNull { it.internalError } results in an empty list, then joinToString() results in an empty string -> return null then

    override val errorMessagesFromBank: List<String>
        get() {
            val allMessages = super.errorMessagesFromBank.toMutableList()
            allMessages.addAll(retrievedTransactionsResponses.flatMap { it.errorMessagesFromBank })
            return allMessages
        }

    open val retrievedData: List<RetrievedAccountData>
        get() = retrievedTransactionsResponses.mapNotNull { it.retrievedData }

}