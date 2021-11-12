package net.dankito.banking.fints.response.client

import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.BankResponse


open class AddAccountResponse(
    context: JobContext,
    response: BankResponse,
    open val bank: BankData,
    retrievedData: List<RetrievedAccountData> = listOf()
) : GetTransactionsResponse(context, response, retrievedData) {

    override val successful: Boolean
        get() = super.successful && bank.accounts.isNotEmpty()

}