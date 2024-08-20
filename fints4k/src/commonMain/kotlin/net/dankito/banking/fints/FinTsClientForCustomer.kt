package net.dankito.banking.fints

import net.dankito.banking.fints.callback.FinTsClientCallback
import net.dankito.banking.fints.config.FinTsClientConfiguration
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.client.AddAccountResponse
import net.dankito.banking.fints.response.client.FinTsClientResponse
import net.dankito.banking.fints.response.client.GetAccountTransactionsResponse


open class FinTsClientForCustomer(
    val bank: BankData,
    config: FinTsClientConfiguration,
    callback: FinTsClientCallback
) {

    constructor(bank: BankData, callback: FinTsClientCallback) : this(bank, FinTsClientConfiguration(), callback)


    protected val client = FinTsClientDeprecated(config, callback)


    open val messageLogWithoutSensitiveData: List<MessageLogEntry> = mutableListOf()

    open fun setCallback(callback: FinTsClientCallback) {
        client.callback = callback
    }


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