package net.dankito.banking.fints.rest.model.dto.response


open class GetAccountsTransactionsResponseDto(
    open val transactionsPerAccount: List<RestResponse<GetAccountTransactionsResponseDto>>
)