package net.dankito.banking.fints.rest.service.model

import net.dankito.banking.fints.response.client.GetTransactionsResponse
import net.dankito.banking.fints.rest.model.ResponseHolder


class GetAccountsTransactionsResponse(
    val transactionsPerAccount: List<ResponseHolder<GetTransactionsResponse>>
)