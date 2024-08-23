package net.codinux.banking.fints.config

import net.codinux.banking.fints.FinTsJobExecutor
import net.codinux.banking.fints.RequestExecutor
import net.codinux.banking.fints.messages.MessageBuilder
import net.codinux.banking.fints.model.mapper.ModelMapper
import net.codinux.banking.fints.util.FinTsServerAddressFinder
import net.codinux.banking.fints.util.IBase64Service
import net.codinux.banking.fints.util.PureKotlinBase64Service
import net.codinux.banking.fints.util.TanMethodSelector
import net.codinux.banking.fints.webclient.IWebClient
import net.codinux.banking.fints.webclient.KtorWebClient

class FinTsClientConfiguration(
    var options: FinTsClientOptions = FinTsClientOptions(),
    messageBuilder: MessageBuilder = MessageBuilder(),
    webClient: IWebClient = KtorWebClient(),
    base64Service: IBase64Service = PureKotlinBase64Service(),
    requestExecutor: RequestExecutor = RequestExecutor(messageBuilder, webClient, base64Service),
    modelMapper: ModelMapper = ModelMapper(messageBuilder),
    tanMethodSelector: TanMethodSelector = TanMethodSelector(),
    var jobExecutor: FinTsJobExecutor = FinTsJobExecutor(requestExecutor, messageBuilder, modelMapper, tanMethodSelector),
    var finTsServerAddressFinder: FinTsServerAddressFinder = FinTsServerAddressFinder()
)