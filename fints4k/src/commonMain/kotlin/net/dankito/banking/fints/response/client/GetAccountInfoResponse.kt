package net.dankito.banking.fints.response.client

import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.BankResponse


open class GetAccountInfoResponse(
    context: JobContext,
    getAccountsResponse: BankResponse,
) : FinTsClientResponse(context, getAccountsResponse) {

    open val bank: BankData = context.bank

    override val successful: Boolean
        get() = super.successful && bank.accounts.isNotEmpty()

}