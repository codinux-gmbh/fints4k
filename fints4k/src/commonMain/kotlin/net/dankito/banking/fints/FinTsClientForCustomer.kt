package net.dankito.banking.fints

import net.dankito.banking.fints.callback.FinTsClientCallback
import net.dankito.banking.fints.messages.MessageBuilder
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.model.mapper.ModelMapper
import net.dankito.banking.fints.response.client.AddAccountResponse
import net.dankito.banking.fints.response.client.FinTsClientResponse
import net.dankito.banking.fints.response.client.GetTransactionsResponse
import net.dankito.banking.fints.transactions.IAccountTransactionsParser
import net.dankito.banking.fints.transactions.Mt940AccountTransactionsParser
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


    protected val client = FinTsClient(callback, FinTsJobExecutor(requestExecutor, messageBuilder, modelMapper, tanMethodSelector), product)


    open val messageLogWithoutSensitiveData: List<MessageLogEntry> = mutableListOf()

    open fun setCallback(callback: FinTsClientCallback) {
        client.callback = callback
    }


    open fun addAccountAsync(callback: (AddAccountResponse) -> Unit) {
        addAccountAsync(bank.toAddAccountParameter(), callback)
    }

    open fun addAccountAsync(parameter: AddAccountParameter, callback: (AddAccountResponse) -> Unit) {
        client.addAccountAsync(parameter, callback)
    }


    open fun getTransactionsAsync(parameter: GetTransactionsParameter, callback: (GetTransactionsResponse) -> Unit) {
        client.getTransactionsAsync(parameter, bank, callback)
    }


    open fun doBankTransferAsync(bankTransferData: BankTransferData, account: AccountData, callback: (FinTsClientResponse) -> Unit) {
        client.doBankTransferAsync(bankTransferData, bank, account, callback)
    }

}