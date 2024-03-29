package net.dankito.banking.fints

import net.dankito.banking.fints.callback.FinTsClientCallback
import net.dankito.banking.fints.messages.MessageBuilder
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.model.mapper.ModelMapper
import net.dankito.banking.fints.response.client.AddAccountResponse
import net.dankito.banking.fints.response.client.FinTsClientResponse
import net.dankito.banking.fints.response.client.GetAccountTransactionsResponse
import net.dankito.banking.fints.util.IBase64Service
import net.dankito.banking.fints.util.PureKotlinBase64Service
import net.dankito.banking.fints.util.TanMethodSelector
import net.dankito.banking.fints.webclient.IWebClient
import net.dankito.banking.fints.webclient.KtorWebClient


open class FinTsClientForCustomer(
    val bank: BankData,
    callback: FinTsClientCallback,
    requestExecutor: RequestExecutor = RequestExecutor(),
    messageBuilder: MessageBuilder = MessageBuilder(),
    modelMapper: ModelMapper = ModelMapper(messageBuilder),
    protected open val tanMethodSelector: TanMethodSelector = TanMethodSelector(),
    product: ProductData = ProductData("15E53C26816138699C7B6A3E8", "1.0.0") // TODO: get version dynamically)
) {

    constructor(bank: BankData, callback: FinTsClientCallback, webClient: IWebClient = KtorWebClient(), base64Service: IBase64Service = PureKotlinBase64Service(),
                product: ProductData = ProductData("15E53C26816138699C7B6A3E8", "1.0.0"))  // TODO: get version dynamically)
            : this(bank, callback, RequestExecutor(MessageBuilder(), webClient, base64Service), product = product)


    protected val client = FinTsClientDeprecated(callback, FinTsJobExecutor(requestExecutor, messageBuilder, modelMapper, tanMethodSelector), product)


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