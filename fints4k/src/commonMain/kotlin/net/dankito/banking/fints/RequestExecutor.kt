package net.dankito.banking.fints

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
import net.dankito.utils.multiplatform.getAllExceptionMessagesJoined
import net.dankito.utils.multiplatform.log.LoggerFactory


open class RequestExecutor(
    protected open val messageBuilder: MessageBuilder = MessageBuilder(),
    protected open val webClient: IWebClient = KtorWebClient(),
    protected open val base64Service: IBase64Service = PureKotlinBase64Service()
) {

    companion object {
        private val log = LoggerFactory.getLogger(FinTsJobExecutor::class)
    }


    open fun getAndHandleResponseForMessage(message: MessageBuilderResult, context: JobContext,
                                            tanRequiredCallback: (TanResponse, BankResponse, callback: (BankResponse) -> Unit) -> Unit, callback: (BankResponse) -> Unit) {
        if (message.createdMessage == null) {
            log.error("Could not create FinTS message to be sent to bank. isJobAllowed ${message.isJobAllowed}, isJobVersionSupported = ${message.isJobVersionSupported}," +
              "allowedVersions = ${message.allowedVersions}, supportedVersions = ${message.supportedVersions}.")
            callback(BankResponse(false, messageThatCouldNotBeCreated = message, internalError = "Could not create FinTS message to be sent to bank")) // TODO: translate
        }
        else {
            getAndHandleResponseForMessage(context, message.createdMessage) { response ->
                handleMayRequiresTan(context, response, tanRequiredCallback) { handledResponse ->
                    // if there's a Aufsetzpunkt (continuationId) set, then response is not complete yet, there's more information to fetch by sending this Aufsetzpunkt
                    handledResponse.aufsetzpunkt?.let { continuationId ->
                        if (handledResponse.followUpResponse == null) { // for re-sent messages followUpResponse is already set and dialog already closed -> would be overwritten with an error response that dialog is closed
                            if (message.isSendEnteredTanMessage() == false) { // for sending TAN no follow up message can be created -> filter out, otherwise chunkedResponseHandler would get called twice for same response
                                context.dialog.chunkedResponseHandler?.invoke(handledResponse)
                            }

                            getFollowUpMessageForContinuationId(context, handledResponse, continuationId, message, tanRequiredCallback) { followUpResponse ->
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
                            context.dialog.chunkedResponseHandler?.invoke(handledResponse)
                        }

                        callback(handledResponse)
                    }
                }
            }
        }
    }

    protected open fun getAndHandleResponseForMessage(context: JobContext, requestBody: String, callback: (BankResponse) -> Unit) {
        addMessageLog(context, MessageLogEntryType.Sent, requestBody)

        getResponseForMessage(requestBody, context.bank.finTs3ServerAddress) { webResponse ->
            val response = handleResponse(context, webResponse)

            val dialog = context.dialog
            dialog.response = response

            response.messageHeader?.let { header -> dialog.dialogId = header.dialogId }
            dialog.didBankCloseDialog = response.didBankCloseDialog

            callback(response)
        }
    }

    protected open fun getResponseForMessage(requestBody: String, finTs3ServerAddress: String, callback: (WebClientResponse) -> Unit) {
        val encodedRequestBody = base64Service.encode(requestBody)

        webClient.post(finTs3ServerAddress, encodedRequestBody, "application/octet-stream", IWebClient.DefaultUserAgent, callback)
    }

    open fun fireAndForgetMessage(context: JobContext, message: MessageBuilderResult) {
        message.createdMessage?.let { requestBody ->
            addMessageLog(context, MessageLogEntryType.Sent, requestBody)

            getResponseForMessage(requestBody, context.bank.finTs3ServerAddress) { }

            // if really needed add received response to message log here
        }
    }

    protected open fun handleResponse(context: JobContext, webResponse: WebClientResponse): BankResponse {
        val responseBody = webResponse.body

        if (webResponse.successful && responseBody != null) {

            try {
                val decodedResponse = decodeBase64Response(responseBody)

                addMessageLog(context, MessageLogEntryType.Received, decodedResponse)

                return context.responseParser.parse(decodedResponse)
            } catch (e: Exception) {
                logError(context, "Could not decode responseBody:\r\n'$responseBody'", e)

                return BankResponse(false, internalError = e.getAllExceptionMessagesJoined())
            }
        }
        else {
            val bank = context.bank
            logError(context, "Request to $bank (${bank.finTs3ServerAddress}) failed", webResponse.error)
        }

        return BankResponse(false, internalError = webResponse.error?.getAllExceptionMessagesJoined())
    }

    protected open fun decodeBase64Response(responseBody: String): String {
        return base64Service.decode(responseBody.replace("\r", "").replace("\n", ""))
    }


    protected open fun getFollowUpMessageForContinuationId(context: JobContext, response: BankResponse, continuationId: String, message: MessageBuilderResult,
                                                           tanRequiredCallback: (TanResponse, BankResponse, callback: (BankResponse) -> Unit) -> Unit,
                                                           callback: (BankResponse?) -> Unit) {

        messageBuilder.rebuildMessageWithContinuationId(context, message, continuationId)?.let { followUpMessage ->
            getAndHandleResponseForMessage(followUpMessage, context, tanRequiredCallback, callback)
        }
        ?: run { callback(null) }
    }


    protected open fun handleMayRequiresTan(context: JobContext, response: BankResponse,
                                            tanRequiredCallback: (TanResponse, BankResponse, callback: (BankResponse) -> Unit) -> Unit,
                                            callback: (BankResponse) -> Unit) { // TODO: use response from DialogContext

        if (response.isStrongAuthenticationRequired) {
            if (context.dialog.abortIfTanIsRequired) {
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


    protected open fun addMessageLog(context: JobContext, type: MessageLogEntryType, message: String) {
        context.addMessageLog(type, message)
    }

    protected open fun logError(context: JobContext, message: String, e: Exception?) {
        context.logError(RequestExecutor::class, message, e)
    }

}