package net.codinux.banking.fints

import net.codinux.banking.fints.callback.FinTsClientCallback
import net.codinux.banking.fints.config.FinTsClientConfiguration
import net.codinux.banking.fints.model.*
import net.codinux.banking.fints.response.client.AddAccountResponse
import net.codinux.banking.fints.response.client.FinTsClientResponse
import net.codinux.banking.fints.response.client.GetAccountTransactionsResponse


open class FinTsClientForCustomer(
    val bank: BankData,
    config: FinTsClientConfiguration,
    callback: FinTsClientCallback
) {

    constructor(bank: BankData, callback: FinTsClientCallback) : this(bank, FinTsClientConfiguration(), callback)


    protected val client = FinTsClientDeprecated(config, callback)


    open val messageLog: List<MessageLogEntry> = mutableListOf()


    open suspend fun addAccountAsync(): AddAccountResponse {
        return addAccountAsync(bank.toAddAccountParameter())
    }

    open suspend fun addAccountAsync(parameter: AddAccountParameter): AddAccountResponse {
        return client.addAccountAsync(parameter)
    }


    open suspend fun getAccountTransactionsAsync(parameter: GetAccountTransactionsParameter): GetAccountTransactionsResponse {
        return client.getAccountTransactionsAsync(parameter)
    }


    open suspend fun doBankTransferAsync(bankTransferData: BankTransferData, account: AccountData): FinTsClientResponse {
        return client.doBankTransferAsync(bankTransferData, bank, account)
    }

}