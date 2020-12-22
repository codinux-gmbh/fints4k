package net.dankito.banking.fints

import net.dankito.banking.fints.log.IMessageLogAppender
import net.dankito.banking.fints.log.MessageLogCollector
import net.dankito.banking.fints.messages.MessageBuilder
import net.dankito.banking.fints.messages.MessageBuilderResult
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.BankResponse
import net.dankito.banking.fints.response.ResponseParser
import net.dankito.banking.fints.response.segments.TanResponse
import net.dankito.banking.fints.util.IBase64Service
import net.dankito.banking.fints.util.PureKotlinBase64Service
import net.dankito.banking.fints.webclient.IWebClient
import net.dankito.banking.fints.webclient.KtorWebClient
import net.dankito.banking.fints.webclient.WebClientResponse
import net.dankito.utils.multiplatform.getInnerExceptionMessage
import net.dankito.utils.multiplatform.log.Logger
import net.dankito.utils.multiplatform.log.LoggerFactory


open class RequestExecutor(
    protected open val messageBuilder: MessageBuilder = MessageBuilder(),
    protected open val webClient: IWebClient = KtorWebClient(),
    protected open val base64Service: IBase64Service = PureKotlinBase64Service(),
    protected open val responseParser: ResponseParser = ResponseParser(),
    protected open val messageLogCollector: MessageLogCollector = MessageLogCollector()
) {

    companion object {
        private val log = LoggerFactory.getLogger(FinTsJobExecutor::class)
    }


    open val messageLogWithoutSensitiveData: List<MessageLogEntry>
        get() = messageLogCollector.messageLogWithoutSensitiveData

    internal open val messageLogAppender: IMessageLogAppender = object : IMessageLogAppender {

        override fun logError(message: String, e: Exception?, logger: Logger?, bank: BankData?) {
            messageLogCollector.logError(message, e, logger, bank)
        }

    }


    open fun getAndHandleResponseForMessage(message: MessageBuilderResult, dialogContext: DialogContext,
                                            tanRequiredCallback: (TanResponse, BankResponse, callback: (BankResponse) -> Unit) -> Unit, callback: (BankResponse) -> Unit) {
        if (message.createdMessage == null) {
            callback(BankResponse(false, messageCreationError = message))
        }
        else {
            getAndHandleResponseForMessage(message.createdMessage, dialogContext) { response ->
                handleMayRequiresTan(response, dialogContext, tanRequiredCallback) { handledResponse ->
                    // if there's a Aufsetzpunkt (continuationId) set, then response is not complete yet, there's more information to fetch by sending this Aufsetzpunkt
                    handledResponse.aufsetzpunkt?.let { continuationId ->
                        if (handledResponse.followUpResponse == null) { // for re-sent messages followUpResponse is already set and dialog already closed -> would be overwritten with an error response that dialog is closed
                            if (message.isSendEnteredTanMessage() == false) { // for sending TAN no follow up message can be created -> filter out, otherwise chunkedResponseHandler would get called twice for same response
                                dialogContext.chunkedResponseHandler?.invoke(handledResponse)
                            }

                            getFollowUpMessageForContinuationId(handledResponse, continuationId, message, dialogContext, tanRequiredCallback) { followUpResponse ->
                                handledResponse.followUpResponse = followUpResponse
                                handledResponse.hasFollowUpMessageButCouldNotReceiveIt = handledResponse.followUpResponse == null

                                callback(handledResponse)
                            }
                        }
                        else {
                            callback(handledResponse)
                        }
                    }
                    ?: run {
                        // e.g. response = enter TAN response, but handledResponse is then response after entering TAN, e.g. account transactions
                        // -> chunkedResponseHandler would get called for same handledResponse multiple times
                        if (response == handledResponse) {
                            dialogContext.chunkedResponseHandler?.invoke(handledResponse)
                        }

                        callback(handledResponse)
                    }
                }
            }
        }
    }

    protected open fun getAndHandleResponseForMessage(requestBody: String, dialogContext: DialogContext, callback: (BankResponse) -> Unit) {
        addMessageLog(requestBody, MessageLogEntryType.Sent, dialogContext)

        getResponseForMessage(requestBody, dialogContext.bank.finTs3ServerAddress) { webResponse ->
            val response = handleResponse(webResponse, dialogContext)

            dialogContext.response = response

            response.messageHeader?.let { header -> dialogContext.dialogId = header.dialogId }
            dialogContext.didBankCloseDialog = response.didBankCloseDialog

            callback(response)
        }
    }

    protected open fun getResponseForMessage(requestBody: String, finTs3ServerAddress: String, callback: (WebClientResponse) -> Unit) {
        val encodedRequestBody = base64Service.encode(requestBody)

        webClient.post(finTs3ServerAddress, encodedRequestBody, "application/octet-stream", IWebClient.DefaultUserAgent, callback)
    }

    open fun fireAndForgetMessage(message: MessageBuilderResult, dialogContext: DialogContext) {
        message.createdMessage?.let { requestBody ->
            addMessageLog(requestBody, MessageLogEntryType.Sent, dialogContext)

            getResponseForMessage(requestBody, dialogContext.bank.finTs3ServerAddress) { }

            // if really needed add received response to message log here
        }
    }

    protected open fun handleResponse(webResponse: WebClientResponse, dialogContext: DialogContext): BankResponse {
        val responseBody = webResponse.body

        if (webResponse.successful && responseBody != null) {

            try {
                val decodedResponse = decodeBase64Response(responseBody)

                addMessageLog(decodedResponse, MessageLogEntryType.Received, dialogContext)

                return responseParser.parse(decodedResponse)
            } catch (e: Exception) {
                logError("Could not decode responseBody:\r\n'$responseBody'", dialogContext, e)

                return BankResponse(false, errorMessage = e.getInnerExceptionMessage())
            }
        }
        else {
            val bank = dialogContext.bank
            logError("Request to $bank (${bank.finTs3ServerAddress}) failed", dialogContext, webResponse.error)
        }

        return BankResponse(false, errorMessage = webResponse.error?.getInnerExceptionMessage())
    }

    protected open fun decodeBase64Response(responseBody: String): String {
        return base64Service.decode(responseBody.replace("\r", "").replace("\n", ""))
    }


    protected open fun getFollowUpMessageForContinuationId(response: BankResponse, continuationId: String, message: MessageBuilderResult, dialogContext: DialogContext,
                                                           tanRequiredCallback: (TanResponse, BankResponse, callback: (BankResponse) -> Unit) -> Unit,
                                                           callback: (BankResponse?) -> Unit) {

        messageBuilder.rebuildMessageWithContinuationId(message, continuationId, dialogContext)?.let { followUpMessage ->
            getAndHandleResponseForMessage(followUpMessage, dialogContext, tanRequiredCallback, callback)
        }
        ?: run { callback(null) }
    }


    protected open fun handleMayRequiresTan(response: BankResponse, dialogContext: DialogContext,
                                            tanRequiredCallback: (TanResponse, BankResponse, callback: (BankResponse) -> Unit) -> Unit,
                                            callback: (BankResponse) -> Unit) { // TODO: use response from DialogContext

        if (response.isStrongAuthenticationRequired) {
            if (dialogContext.abortIfTanIsRequired) {
                response.tanRequiredButWeWereToldToAbortIfSo = true

                callback(response)
                return
            }
            else if (response.tanResponse != null) {
                response.tanResponse?.let { tanResponse ->
                    tanRequiredCallback(tanResponse, response, callback)
                }

                return
            }
        }

        // TODO: check if response contains '3931 TAN-Generator gesperrt, Synchronisierung erforderlich' or
        //  '3933 TAN-Generator gesperrt, Synchronisierung erforderlich Kartennummer ##########' message,
        //  call callback.enterAtc() and implement and call HKTSY job  (p. 77)

        // TODO: also check '9931 Sperrung des Kontos nach %1 Fehlversuchen' -> if %1 == 3 synchronize TAN generator
        //  as it's quite unrealistic that user entered TAN wrong three times, in most cases TAN generator is not synchronized

        callback(response)
    }


    protected open fun addMessageLog(message: String, type: MessageLogEntryType, dialogContext: DialogContext) {
        messageLogCollector.addMessageLog(message, type, dialogContext.bank)
    }

    protected open fun logError(message: String, dialogContext: DialogContext, e: Exception?) {
        messageLogAppender.logError(message, e, log, dialogContext.bank)
    }

}