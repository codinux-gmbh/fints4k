package net.codinux.banking.fints.response.client

import net.codinux.banking.fints.model.*
import net.codinux.banking.fints.response.BankResponse


open class AddAccountResponse(
    context: JobContext,
    getAccountsResponse: BankResponse,
    open val retrievedTransactionsResponses: List<GetAccountTransactionsResponse> = listOf()
) : GetAccountInfoResponse(context, getAccountsResponse) {

    override val successful: Boolean
        get() = super.successful
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

    override val messageLog: List<MessageLogEntry>
        get() = buildList {
            addAll(super.messageLog)
            retrievedTransactionsResponses.forEach {
                addAll(it.messageLog)
            }
        }

}