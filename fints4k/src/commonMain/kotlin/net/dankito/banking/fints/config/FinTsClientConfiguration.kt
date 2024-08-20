package net.dankito.banking.fints.config

import net.dankito.banking.fints.FinTsJobExecutor
import net.dankito.banking.fints.RequestExecutor
import net.dankito.banking.fints.log.MessageLogCollector
import net.dankito.banking.fints.messages.MessageBuilder
import net.dankito.banking.fints.model.mapper.ModelMapper
import net.dankito.banking.fints.util.FinTsServerAddressFinder
import net.dankito.banking.fints.util.IBase64Service
import net.dankito.banking.fints.util.PureKotlinBase64Service
import net.dankito.banking.fints.util.TanMethodSelector
import net.dankito.banking.fints.webclient.IWebClient
import net.dankito.banking.fints.webclient.KtorWebClient

class FinTsClientConfiguration(
    var options: FinTsClientOptions = FinTsClientOptions(),
    messageBuilder: MessageBuilder = MessageBuilder(),
    webClient: IWebClient = KtorWebClient(),
    base64Service: IBase64Service = PureKotlinBase64Service(),
    requestExecutor: RequestExecutor = RequestExecutor(messageBuilder, webClient, base64Service),
    modelMapper: ModelMapper = ModelMapper(messageBuilder),
    tanMethodSelector: TanMethodSelector = TanMethodSelector(),
    var jobExecutor: FinTsJobExecutor = FinTsJobExecutor(requestExecutor, messageBuilder, modelMapper, tanMethodSelector),
    var finTsServerAddressFinder: FinTsServerAddressFinder = FinTsServerAddressFinder(),
    var messageLogCollector: MessageLogCollector = MessageLogCollector()
)