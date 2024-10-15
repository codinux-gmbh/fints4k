package net.codinux.banking.fints

import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import net.codinux.banking.fints.extensions.*
import net.codinux.log.logger
import net.codinux.banking.fints.messages.MessageBuilder
import net.codinux.banking.fints.messages.MessageBuilderResult
import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.VersionDesSicherheitsverfahrens
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.*
import net.codinux.banking.fints.messages.segmente.id.CustomerSegmentId
import net.codinux.banking.fints.messages.segmente.id.ISegmentId
import net.codinux.banking.fints.model.*
import net.codinux.banking.fints.model.mapper.ModelMapper
import net.codinux.banking.fints.response.BankResponse
import net.codinux.banking.fints.response.InstituteSegmentId
import net.codinux.banking.fints.response.client.*
import net.codinux.banking.fints.response.segments.*
import net.codinux.banking.fints.tan.FlickerCodeDecoder
import net.codinux.banking.fints.tan.TanImageDecoder
import net.codinux.banking.fints.util.TanMethodSelector
import net.codinux.log.Log
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds


/**
 * Low level class that executes concrete business transactions (= FinTS Geschäftsvorfälle).
 *
 * In almost all cases you want to use [FinTsClient] which wraps these business transactions to a higher level API.
 */
open class FinTsJobExecutor(
    protected open val requestExecutor: RequestExecutor = RequestExecutor(),
    protected open val messageBuilder: MessageBuilder = MessageBuilder(),
    protected open val modelMapper: ModelMapper = ModelMapper(messageBuilder),
    protected open val tanMethodSelector: TanMethodSelector = TanMethodSelector()
) {

    private val log by logger()


    open suspend fun getAnonymousBankInfo(context: JobContext): BankResponse {
        context.startNewDialog()

        val message = messageBuilder.createAnonymousDialogInitMessage(context)

        val response = getAndHandleResponseForMessage(context, message)

        if (response.successful) {
            closeAnonymousDialog(context, response)
        }

        return response
    }

    protected open suspend fun closeAnonymousDialog(context: JobContext, response: BankResponse) {

        // bank already closed dialog -> there's no need to send dialog end message
        if (shouldNotCloseDialog(context)) {
            return
        }

        val dialogEndRequestBody = messageBuilder.createAnonymousDialogEndMessage(context)

        fireAndForgetMessage(context, dialogEndRequestBody)
    }


    /**
     * Retrieves basic data like user's TAN methods, ... in an not authenticated Init dialog.
     *
     * This is the first step to do when adding a new account as for almost all other jobs user's selected TAN method has to be specified.
     *
     * Be aware this method resets BPD, UPD and selected TAN method!
     */
    open suspend fun retrieveBasicDataLikeUsersTanMethods(context: JobContext): BankResponse {
        val bank = context.bank

        // just to ensure settings are in its initial state and that bank sends us bank parameter (BPD),
        // user parameter (UPD) and allowed tan methods for user (therefore the resetSelectedTanMethod())
        bank.resetBpdVersion()
        bank.resetUpdVersion()
        /**
         * Sind dem Kundenprodukt die konkreten, für den Benutzer zugelassenen Sicherheitsverfahren nicht bekannt, so können
         * diese über eine Dialoginitialisierung mit Sicherheitsfunktion=999 angefordert werden. Die konkreten Verfahren
         * werden dann über den Rückmeldungscode=3920 zurückgemeldet. Im Rahmen dieses Prozesses darf keine UPD
         * zurückgeliefert werden und die Durchführung anderer Geschäftsvorfälle ist in einem solchen Dialog nicht erlaubt.
         */
        bank.resetSelectedTanMethod()

        // this is the only case where Einschritt-TAN-Verfahren is accepted: to get user's TAN methods
        context.startNewDialog(versionOfSecurityProcedure = VersionDesSicherheitsverfahrens.Version_1)

        val message = messageBuilder.createInitDialogMessage(context)

        val response =  getAndHandleResponseForMessage(context, message)

        closeDialog(context)

        val getTanMethodsResponse = handleGetUsersTanMethodsResponse(context, response)

        if (bank.tanMethodsAvailableForUser.isEmpty()) { // could not retrieve supported tan methods for user
            return getTanMethodsResponse
        } else {
            getUsersTanMethod(context)

            if (bank.isTanMethodSelected && bank.tanMedia.isEmpty() && bank.tanMethodsAvailableForUser.any { it.nameOfTanMediumRequired } && isJobSupported(bank, CustomerSegmentId.TanMediaList)) { // tan media not retrieved yet
                getTanMediaList(context, TanMedienArtVersion.Alle, TanMediumKlasse.AlleMedien, context.preferredTanMedium)

                return getTanMethodsResponse // TODO: judge if bank requires selecting TAN media and if though evaluate getTanMediaListResponse
            } else {
                return getTanMethodsResponse
            }
        }
    }

    protected open suspend fun handleGetUsersTanMethodsResponse(context: JobContext, response: BankResponse): BankResponse {
        val getUsersTanMethodsResponse = GetUserTanMethodsResponse(response)

        // even though it is required by specification some banks don't support retrieving user's TAN method by setting TAN method to '999'
        if (bankDoesNotSupportRetrievingUsersTanMethods(getUsersTanMethodsResponse)) {
            return getBankDataForNewUserViaAnonymousDialog(context) // TODO: should not be necessary anymore
        }
        else {
            return getUsersTanMethodsResponse
        }
    }

    protected open fun bankDoesNotSupportRetrievingUsersTanMethods(response: BankResponse): Boolean {
        return response.successful == false &&
                response.segmentFeedbacks.flatMap { it.feedbacks }.firstOrNull { it.responseCode == 9200 &&
                        it.message == "Gewähltes Zwei-Schritt-Verfahren nicht unterstützt." } != null
    }

    // TODO: this is only a quick fix. Find a better and general solution
    protected open suspend fun getBankDataForNewUserViaAnonymousDialog(context: JobContext): BankResponse {
        val anonymousBankInfoResponse = getAnonymousBankInfo(context)

        val bank = context.bank

        if (anonymousBankInfoResponse.successful == false) {
            return anonymousBankInfoResponse
        } else if (bank.tanMethodsSupportedByBank.isEmpty()) { // should only be a theoretical error
            return BankResponse(true, internalError = "Die TAN Verfahren der Bank konnten nicht ermittelt werden") // TODO: translate
        } else {
            bank.tanMethodsAvailableForUser = bank.tanMethodsSupportedByBank
                .filterNot { context.tanMethodsNotSupportedByApplication.contains(it.type) }

            val didSelectTanMethod = getUsersTanMethod(context)

            if (didSelectTanMethod) {
                val initDialogResponse =  initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks(context)

                closeDialog(context)

                return initDialogResponse
            } else {
                return createNoTanMethodSelectedResponse(bank)
            }
        }
    }


    open suspend fun getAccounts(context: JobContext): BankResponse {
        val response = initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks(context)

        closeDialog(context)

        return response
    }


    open suspend fun getTransactionsAsync(context: JobContext, parameter: GetAccountTransactionsParameter): GetAccountTransactionsResponse {

        val dialogContext = context.startNewDialog() // TODO: initDialogWithStrongCustomerAuthentication() also starts a new dialog in initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks()

        val initDialogResponse =  initDialogWithStrongCustomerAuthentication(context)

        if (initDialogResponse.successful == false) {
            return GetAccountTransactionsResponse(context, initDialogResponse, RetrievedAccountData.unsuccessful(parameter.account))
        }
        else {
            // we now retrieved the fresh account information from FinTS server, use that one
            parameter.account = getUpdatedAccount(context, parameter.account)

            val balanceResponse =  mayGetBalance(context, parameter)
            if (dialogContext.didBankCloseDialog) {
                return GetAccountTransactionsResponse(context, balanceResponse ?: initDialogResponse, RetrievedAccountData.unsuccessful(parameter.account))
            } else {
                return getTransactionsAfterInitAndGetBalance(context, parameter, balanceResponse)
            }
        }
    }

    protected open fun getUpdatedAccount(context: JobContext, account: AccountData): AccountData {
        return context.bank.accounts.firstOrNull { it.accountIdentifier == account.accountIdentifier } ?: account
    }

    protected open suspend fun getTransactionsAfterInitAndGetBalance(context: JobContext, parameter: GetAccountTransactionsParameter,
                                                             balanceResponse: BankResponse?): GetAccountTransactionsResponse {
        var balance: Money? = balanceResponse?.getFirstSegmentById<BalanceSegment>(InstituteSegmentId.Balance)?.let {
            Money(it.balance, it.currency)
        }

        // TODO: for larger portfolios there can be a Aufsetzpunkt, but for balances we currently do not support sending multiple messages
        val statementOfHoldings = balanceResponse?.getFirstSegmentById<SecuritiesAccountBalanceSegment>(InstituteSegmentId.SecuritiesAccountBalance)?.let {
            val statementOfHoldings = it.statementOfHoldings
            val statementOfHolding = statementOfHoldings.firstOrNull { it.totalBalance != null }
            if (statementOfHolding != null) {
                balance = Money(statementOfHolding.totalBalance!!, statementOfHolding.currency ?: Currency.DefaultCurrencyCode)
            }
            statementOfHoldings
        } ?: emptyList()

        if (parameter.account.supportsRetrievingAccountTransactions == false) {
            if (balanceResponse == null) {
                return GetAccountTransactionsResponse(context, BankResponse(false, "Balance could not be retrieved"), RetrievedAccountData.unsuccessful(parameter.account))
            } else {
                val successful = balance != null || balanceResponse.tanRequiredButWeWereToldToAbortIfSo
                val retrievedData = RetrievedAccountData(parameter.account, successful, balance, emptyList(), emptyList(), statementOfHoldings, Instant.nowExt(), null, null, balanceResponse?.internalError)

                return GetAccountTransactionsResponse(context, balanceResponse, retrievedData)
            }
        }


        val bookedTransactions = mutableSetOf<AccountTransaction>()
        val unbookedTransactions = mutableSetOf<Any>()

        val message = messageBuilder.createGetTransactionsMessage(context, parameter)

        var remainingMt940String = ""

        context.dialog.abortIfTanIsRequired = parameter.abortIfTanIsRequired

        context.dialog.chunkedResponseHandler = { response ->
            response.getFirstSegmentById<ReceivedAccountTransactions>(InstituteSegmentId.AccountTransactionsMt940)?.let { transactionsSegment ->
                val (chunkTransaction, remainder) = context.mt940Parser.parseTransactionsChunk(remainingMt940String + transactionsSegment.bookedTransactionsString,
                    context.bank, parameter.account)

                bookedTransactions.addAll(chunkTransaction)
                remainingMt940String = remainder ?: ""

                parameter.retrievedChunkListener?.invoke(bookedTransactions)
            }

            response.getFirstSegmentById<ReceivedCreditCardTransactionsAndBalance>(InstituteSegmentId.CreditCardTransactions)?.let { creditCardTransactionsSegment ->
                balance = Money(creditCardTransactionsSegment.balance.amount, creditCardTransactionsSegment.balance.currency ?: "EUR")
                bookedTransactions.addAll(creditCardTransactionsSegment.transactions.map { AccountTransaction(parameter.account, it.amount, it.description, it.bookingDate, it.valueDate, it.transactionDescriptionBase ?: "", null, null) })
            }
        }

        val startTime = Instant.nowExt()

        val response = getAndHandleResponseForMessage(context, message)

        closeDialog(context)

        val successful = response.tanRequiredButWeWereToldToAbortIfSo
            || (response.successful && (parameter.alsoRetrieveBalance == false || balance != null))
            || (parameter.account.supportsRetrievingAccountTransactions == false && balance != null)
        val fromDate = parameter.fromDate
            ?: parameter.account.serverTransactionsRetentionDays?.let { LocalDate.todayAtSystemDefaultTimeZone().minusDays(it) }
            ?: bookedTransactions.minByOrNull { it.valueDate }?.valueDate
        val retrievedData = RetrievedAccountData(parameter.account, successful, balance, bookedTransactions, unbookedTransactions, statementOfHoldings, startTime, fromDate, parameter.toDate ?: LocalDate.todayAtEuropeBerlin(), response.internalError)

        return GetAccountTransactionsResponse(context, response, retrievedData,
            if (parameter.maxCountEntries != null) parameter.isSettingMaxCountEntriesAllowedByBank else null)
    }

    protected open suspend fun mayGetBalance(context: JobContext, parameter: GetAccountTransactionsParameter): BankResponse? {
        if (parameter.alsoRetrieveBalance && parameter.account.supportsRetrievingBalance) {
            val message = messageBuilder.createGetBalanceMessage(context, parameter.account)

            return getAndHandleResponseForMessage(context, message)
        } else {
            return null
        }
    }


    /**
     * According to specification synchronizing customer system id is required:
     * "Die Kundensystem-ID ist beim HBCI RAH- / RDH- sowie dem PIN/TAN-Verfahren erforderlich."
     *
     * But as tests show this can be omitted.
     *
     * But when you do it, this has to be done in an extra dialog as dialog has to be initialized
     * with retrieved customer system id.
     *
     * If you change customer system id during a dialog your messages get rejected by bank institute.
     */
    protected open suspend fun synchronizeCustomerSystemId(context: JobContext): FinTsClientResponse {

        context.startNewDialog()

        val message = messageBuilder.createSynchronizeCustomerSystemIdMessage(context)

        val response = getAndHandleResponseForMessage(context, message)

        if (response.successful) {
            closeDialog(context)
        }

        return FinTsClientResponse(context, response)
    }


    open suspend fun getTanMediaList(context: JobContext, tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle, tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien): GetTanMediaListResponse {
        return getTanMediaList(context, tanMediaKind, tanMediumClass, null)
    }

    protected open suspend fun getTanMediaList(context: JobContext, tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle, tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien,
                             preferredTanMedium: String? = null): GetTanMediaListResponse {

        val response = sendMessageInNewDialogAndHandleResponse(context, CustomerSegmentId.TanMediaList, false) {
            messageBuilder.createGetTanMediaListMessage(context, tanMediaKind, tanMediumClass)
        }

        return handleGetTanMediaListResponse(context, response, preferredTanMedium)
    }

    protected open fun handleGetTanMediaListResponse(context: JobContext, response: BankResponse, preferredTanMedium: String? = null): GetTanMediaListResponse {
        val bank = context.bank

        // TAN media list (= TAN generator list) is only returned for users with chipTAN TAN methods
        val tanMediaList = if (response.successful == false) null
        else response.getFirstSegmentById<TanMediaList>(InstituteSegmentId.TanMediaList)

        tanMediaList?.let {
            bank.tanMedia = it.tanMedia

            bank.selectedTanMedium = preferredTanMedium?.let { bank.tanMedia.firstOrNull { it.mediumName == preferredTanMedium } }
                ?: bank.selectedTanMedium?.let { selected -> bank.tanMedia.firstOrNull { it.mediumName == selected.mediumName } } // try to find selectedTanMedium in new TanMedia instances
                ?: bank.tanMedia.firstOrNull { it.status == TanMediumStatus.Aktiv && it.mediumName != null }
                ?: bank.tanMedia.firstOrNull { it.mediumName != null }
        }

        return GetTanMediaListResponse(context, response, tanMediaList)
    }


    open suspend fun changeTanMedium(context: JobContext, newActiveTanMedium: TanMedium): BankResponse {
        val bank = context.bank

        if (bank.changeTanMediumParameters?.enteringAtcAndTanRequired == true) {
            val enteredAtc = context.callback.enterTanGeneratorAtc(bank, newActiveTanMedium)
            if (enteredAtc.hasAtcBeenEntered == false) {
                val message = "Bank requires to enter ATC and TAN in order to change TAN medium." // TODO: translate
                return BankResponse(false, internalError = message)
            } else {
                return sendChangeTanMediumMessage(context, newActiveTanMedium, enteredAtc)
            }
        }
        else {
            return sendChangeTanMediumMessage(context, newActiveTanMedium, null)
        }
    }

    protected open suspend fun sendChangeTanMediumMessage(context: JobContext, newActiveTanMedium: TanMedium, enteredAtc: EnterTanGeneratorAtcResult?): BankResponse {

        return sendMessageInNewDialogAndHandleResponse(context, null, true) {
            messageBuilder.createChangeTanMediumMessage(context, newActiveTanMedium, enteredAtc?.tan, enteredAtc?.atc)
        }
    }


    open suspend fun transferMoneyAsync(context: JobContext, bankTransferData: BankTransferData): FinTsClientResponse {

        val response = sendMessageInNewDialogAndHandleResponse(context, null, true) {
            val updatedAccount = getUpdatedAccount(context, context.account!!)
            messageBuilder.createBankTransferMessage(context, bankTransferData, updatedAccount)
        }

        return FinTsClientResponse(context, response)
    }


    protected open suspend fun getAndHandleResponseForMessage(context: JobContext, message: MessageBuilderResult): BankResponse {
        val response = requestExecutor.getAndHandleResponseForMessage(message, context, { tanResponse, bankResponse ->
                // if we receive a message that tells us a TAN is required below callback doesn't get called for that message -> update data here
                // for Hypovereinsbank it's absolutely necessary to update bank data (more specific: PinInfo / HIPINS) after first strong authentication dialog init response
                // as HIPINS differ in anonymous and in authenticated dialog. Anonymous dialog tells us for HKSAL and HKKAZ no TAN is needed
                updateBankAndCustomerDataIfResponseSuccessful(context, bankResponse)

                handleEnteringTanRequired(context, tanResponse, bankResponse)
            })

        // TODO: really update data only on complete successfully response? as it may contain useful information anyway  // TODO: extract method for this code part
        updateBankAndCustomerDataIfResponseSuccessful(context, response)

        return response
    }

    protected open suspend fun fireAndForgetMessage(context: JobContext, message: MessageBuilderResult) {
        requestExecutor.fireAndForgetMessage(context, message)
    }


    protected open suspend fun handleEnteringTanRequired(context: JobContext, tanResponse: TanResponse, response: BankResponse): BankResponse {
        // on all platforms run on Dispatchers.Main, but on iOS skip this (or wrap in withContext(Dispatchers.IO) )
//        val enteredTanResult = GlobalScope.async {
        val tanChallenge = createTanChallenge(tanResponse, modelMapper.mapToActionRequiringTan(context.type), context.bank, context.account)

        context.callback.enterTan(tanChallenge)

        mayRetrieveAutomaticallyIfUserEnteredDecoupledTan(context, tanChallenge, tanResponse)

        var invocationCount = 0 // TODO: remove again

        while (tanChallenge.isEnteringTanDone == false) {
            delay(500)

            if (++invocationCount % 10 == 0) {
                Log.info { "Waiting for TAN input invocation count: $invocationCount" }
            }

            val now = Instant.nowExt()
            if ((tanChallenge.tanExpirationTime != null && now > tanChallenge.tanExpirationTime) ||
                // most TANs a valid 5 - 15 minutes. So terminate wait process after that time
                (tanChallenge.tanExpirationTime == null && now > tanChallenge.challengeCreationTimestamp.plusMinutes(15))) {
                if (tanChallenge.isEnteringTanDone == false) {
Log.info { "Terminating waiting for TAN input" } // TODO: remove again

                    tanChallenge.tanExpired()
                }

                break
            }
        }

        val enteredTanResult = tanChallenge.enterTanResult!!

        return handleEnterTanResult(context, enteredTanResult, tanResponse, response)
    }

    protected open fun createTanChallenge(tanResponse: TanResponse, forAction: ActionRequiringTan, bank: BankData, account: AccountData? = null): TanChallenge {
        // TODO: is this true for all tan methods?
        val messageToShowToUser = tanResponse.challenge ?: ""
        val challenge = tanResponse.challengeHHD_UC ?: ""
        val tanMethod = bank.selectedTanMethod

        return when (tanMethod.type) {
            TanMethodType.ChipTanFlickercode ->
                FlickerCodeTanChallenge(
                    FlickerCodeDecoder().decodeChallenge(challenge, tanMethod.hhdVersion ?: getFallbackHhdVersion(challenge)),
                    forAction, messageToShowToUser, challenge, tanMethod, tanResponse.tanMediaIdentifier, bank, account, tanResponse.tanExpirationTime)

            TanMethodType.ChipTanQrCode, TanMethodType.ChipTanPhotoTanMatrixCode,
            TanMethodType.QrCode, TanMethodType.photoTan ->
                ImageTanChallenge(TanImageDecoder().decodeChallenge(challenge), forAction, messageToShowToUser, challenge, tanMethod, tanResponse.tanMediaIdentifier, bank, account, tanResponse.tanExpirationTime)

            else -> TanChallenge(forAction, messageToShowToUser, challenge, tanMethod, tanResponse.tanMediaIdentifier, bank, account, tanResponse.tanExpirationTime)
        }
    }

    protected open fun getFallbackHhdVersion(challenge: String): HHDVersion {
        if (challenge.length <= 35) { // is this true in all circumstances?
            return HHDVersion.HHD_1_3
        }

        return HHDVersion.HHD_1_4 // HHD 1.4 is currently the most used version
    }

    protected open suspend fun mayRetrieveAutomaticallyIfUserEnteredDecoupledTan(context: JobContext, tanChallenge: TanChallenge, tanResponse: TanResponse) {
        context.bank.selectedTanMethod.decoupledParameters?.let { decoupledTanMethodParameters ->
            if (decoupledTanMethodParameters.periodicStateRequestsAllowed) {
                val responseAfterApprovingDecoupledTan =
                    automaticallyRetrieveIfUserEnteredDecoupledTan(context, tanChallenge, tanResponse, decoupledTanMethodParameters)

                if (responseAfterApprovingDecoupledTan != null) {
                    tanChallenge.userApprovedDecoupledTan(responseAfterApprovingDecoupledTan)
                } else {
                    tanChallenge.userDidNotEnterTan()
                }
            }
        }
    }

    protected open suspend fun automaticallyRetrieveIfUserEnteredDecoupledTan(context: JobContext, tanChallenge: TanChallenge, tanResponse: TanResponse, parameters: DecoupledTanMethodParameters): BankResponse? {
        log.info { "automaticallyRetrieveIfUserEnteredDecoupledTan() called for $tanChallenge" }

        delay(max(5, parameters.initialDelayInSecondsForStateRequest).seconds)

        var iteration = 0
        val minWaitTime = when {
            parameters.maxNumberOfStateRequests <= 10 -> 30
            parameters.maxNumberOfStateRequests <= 24 -> 10
            else -> 3
        }
        val delayForNextStateRequest = max(minWaitTime, parameters.delayInSecondsForNextStateRequest).seconds

        while (iteration < parameters.maxNumberOfStateRequests) {
            try {
                val message = messageBuilder.createDecoupledTanStatusMessage(context, tanResponse)

                val response = getAndHandleResponseForMessage(context, message)

                val tanFeedbacks = response.segmentFeedbacks.filter { it.referenceSegmentNumber == MessageBuilder.SignedMessagePayloadFirstSegmentNumber }
                if (tanFeedbacks.isNotEmpty()) {
                    // new feedback code for Decoupled TAN: 0900 Sicherheitsfreigabe gültig
                    // Sparkasse responds for pushTan with: HIRMS:4:2:3+0020::Der Auftrag wurde ausgeführt.+0020::Die gebuchten Umsätze wurden übermittelt.'
                    val isTanApproved = tanFeedbacks.any { it.feedbacks.any { it.responseCode == 900 || it.responseCode == 20 } }
                    if (isTanApproved) {
                        return response
                    }
                }

                iteration++
                // sometimes delayInSecondsForNextStateRequests is only 1 or 2 seconds, that's too fast i think
                delay(delayForNextStateRequest)
            } catch (e: Throwable) {
                log.error(e) { "Could not check status of Decoupled TAN" }

                return null
            }
        }

        tanChallenge.tanExpired()

        return null
    }

    protected open suspend fun handleEnterTanResult(context: JobContext, enteredTanResult: EnterTanResult, tanResponse: TanResponse,
                                            response: BankResponse): BankResponse {

        if (enteredTanResult.changeTanMethodTo != null) {
            return handleUserAsksToChangeTanMethodAndResendLastMessage(context, enteredTanResult.changeTanMethodTo)
        } else if (enteredTanResult.changeTanMediumTo != null) {
            return handleUserAsksToChangeTanMediumAndResendLastMessage(context, enteredTanResult.changeTanMediumTo,
                enteredTanResult.changeTanMediumResultCallback)
        } else if (enteredTanResult.userApprovedDecoupledTan == true && enteredTanResult.responseAfterApprovingDecoupledTan != null) {
            return enteredTanResult.responseAfterApprovingDecoupledTan
        } else if (enteredTanResult.enteredTan == null) {
            // i tried to send a HKTAN with cancelJob = true but then i saw there are no tan methods that support cancellation (at least not at my bank)
            // but it's not required anyway, tan times out after some time. Simply don't respond anything and close dialog
            response.tanRequiredButUserDidNotEnterOne = true

            return response
        } else {
            return sendTanToBank(context, enteredTanResult.enteredTan, tanResponse)
        }
    }

    protected open suspend fun sendTanToBank(context: JobContext, enteredTan: String, tanResponse: TanResponse): BankResponse {

        val message = messageBuilder.createSendEnteredTanMessage(context, enteredTan, tanResponse)

        return getAndHandleResponseForMessage(context, message)
    }

    protected open suspend fun handleUserAsksToChangeTanMethodAndResendLastMessage(context: JobContext, changeTanMethodTo: TanMethod): BankResponse {

        context.bank.selectedTanMethod = changeTanMethodTo


        val lastCreatedMessage = context.dialog.currentMessage

        lastCreatedMessage?.let { closeDialog(context) }

        return resendMessageInNewDialog(context, lastCreatedMessage)
    }

    protected open suspend fun handleUserAsksToChangeTanMediumAndResendLastMessage(context: JobContext, changeTanMediumTo: TanMedium,
                                                                           changeTanMediumResultCallback: ((FinTsClientResponse) -> Unit)?): BankResponse {

        val lastCreatedMessage = context.dialog.currentMessage

        lastCreatedMessage?.let { closeDialog(context) }


        val changeTanMediumResponse =  changeTanMedium(context, changeTanMediumTo)

        changeTanMediumResultCallback?.invoke(FinTsClientResponse(context, changeTanMediumResponse))

        if (changeTanMediumResponse.successful == false || lastCreatedMessage == null) {
            return changeTanMediumResponse
        }
        else {
            return resendMessageInNewDialog(context, lastCreatedMessage)
        }
    }


    protected open suspend fun resendMessageInNewDialog(context: JobContext, lastCreatedMessage: MessageBuilderResult?): BankResponse {

        if (lastCreatedMessage != null) { // do not use previousDialogContext.currentMessage as this may is previous dialog's dialog close message
            context.startNewDialog(chunkedResponseHandler = context.dialog.chunkedResponseHandler)

            val initDialogResponse = initDialogWithStrongCustomerAuthentication(context)

            // if lastCreatedMessage was a dialog init message, there's no need to send this message again, we just initialized a new dialog in initDialogWithStrongCustomerAuthentication()
            if (initDialogResponse.successful == false || lastCreatedMessage.isDialogInitMessage()) {
                return initDialogResponse
            } else {
                val newMessage = messageBuilder.rebuildMessage(context, lastCreatedMessage)

                val response =  getAndHandleResponseForMessage(context, newMessage)

                closeDialog(context)

                return response
            }
        }
        else {
            val errorMessage = "There's no last action (like retrieve account transactions, transfer money, ...) to re-send with new TAN method. Probably an internal programming error." // TODO: translate
            return BankResponse(false, internalError = errorMessage) // should never come to this
        }
    }


    protected open suspend fun sendMessageInNewDialogAndHandleResponse(context: JobContext, segmentForNonStrongCustomerAuthenticationTwoStepTanProcess: CustomerSegmentId? = null,
                                                               closeDialog: Boolean = true, createMessage: () -> MessageBuilderResult): BankResponse {

        context.startNewDialog(closeDialog)

        val initDialogResponse = if (segmentForNonStrongCustomerAuthenticationTwoStepTanProcess == null) {
            initDialogWithStrongCustomerAuthentication(context)
        } else {
            initDialogMessageWithoutStrongCustomerAuthenticationAfterSuccessfulChecks(context, segmentForNonStrongCustomerAuthenticationTwoStepTanProcess)
        }

        return sendMessageAndHandleResponseAfterDialogInitialization(context, initDialogResponse, createMessage)
    }

    protected open suspend fun sendMessageAndHandleResponseAfterDialogInitialization(context: JobContext, initDialogResponse: BankResponse,
                                                                      createMessage: () -> MessageBuilderResult): BankResponse {

        if (initDialogResponse.successful == false) {
            return initDialogResponse
        }
        else {
            val message = createMessage()

            val response = getAndHandleResponseForMessage(context, message)

            closeDialog(context)

            return response
        }
    }

    protected open suspend fun initDialogWithStrongCustomerAuthentication(context: JobContext): BankResponse {

        // we first need to retrieve supported tan methods and jobs before we can do anything
        val retrieveBasicBankDataResponse = ensureBasicBankDataRetrieved(context)

        if (retrieveBasicBankDataResponse.successful == false) {
            return retrieveBasicBankDataResponse
        }
        else {
            // as in the next step we have to supply user's tan method, ensure user selected his or her
            val tanMethodSelectedResponse = ensureTanMethodIsSelected(context)

            if (tanMethodSelectedResponse.successful == false) {
                return tanMethodSelectedResponse
            }
            else {
                return initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks(context)
            }
        }
    }

    protected open suspend fun initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks(context: JobContext): BankResponse {

        context.startNewDialog() // don't know if it's ok for all invocations of this method to set closeDialog to false (was actually only set in getAccounts())

        val message = messageBuilder.createInitDialogMessage(context)

        return getAndHandleResponseForMessage(context, message)
    }

    protected open suspend fun initDialogMessageWithoutStrongCustomerAuthenticationAfterSuccessfulChecks(context: JobContext, segmentIdForTwoStepTanProcess: CustomerSegmentId?): BankResponse {

        val message = messageBuilder.createInitDialogMessageWithoutStrongCustomerAuthentication(context, segmentIdForTwoStepTanProcess)

        return getAndHandleResponseForMessage(context, message)
    }

    protected open suspend fun closeDialog(context: JobContext) {

        // bank already closed dialog -> there's no need to send dialog end message
        if (shouldNotCloseDialog(context)) {
            return
        }

        val dialogEndRequestBody = messageBuilder.createDialogEndMessage(context)

        fireAndForgetMessage(context, dialogEndRequestBody)
    }

    protected open fun shouldNotCloseDialog(context: JobContext): Boolean {
        return context.dialog.closeDialog == false || context.dialog.didBankCloseDialog
    }


    protected open suspend fun ensureBasicBankDataRetrieved(context: JobContext): BankResponse {
        val bank = context.bank

        if (bank.tanMethodsSupportedByBank.isEmpty() || bank.supportedJobs.isEmpty()) {
            val getBankInfoResponse = retrieveBasicDataLikeUsersTanMethods(context)

            if (getBankInfoResponse.successful == false) {
                return getBankInfoResponse
            } else if (bank.tanMethodsSupportedByBank.isEmpty() || bank.supportedJobs.isEmpty()) {
                return BankResponse(false, internalError =
                "Could not retrieve basic bank data like supported tan methods or supported jobs") // TODO: translate // TODO: add as messageToShowToUser
            } else {
                return BankResponse(true)
            }
        }
        else {
            return BankResponse(true)
        }
    }

    protected open suspend fun ensureTanMethodIsSelected(context: JobContext): BankResponse {
        val bank = context.bank

        if (bank.isTanMethodSelected == false) {
            if (bank.tanMethodsAvailableForUser.isEmpty()) {
                return retrieveBasicDataLikeUsersTanMethods(context)
            }
            else {
                getUsersTanMethod(context)

                return createNoTanMethodSelectedResponse(bank)
            }
        }
        else {
            return createNoTanMethodSelectedResponse(bank)
        }
    }

    protected open fun createNoTanMethodSelectedResponse(bank: BankData): BankResponse {
        val noTanMethodSelected = !!!bank.isTanMethodSelected
        val errorMessage = if (noTanMethodSelected) "User did not select a TAN method" else null // TODO: translate

        return BankResponse(true, noTanMethodSelected = noTanMethodSelected, internalError = errorMessage)
    }

    open suspend fun getUsersTanMethod(context: JobContext): Boolean {
        val bank = context.bank

        if (bank.tanMethodsAvailableForUser.size == 1) { // user has only one TAN method -> set it and we're done
            bank.selectedTanMethod = bank.tanMethodsAvailableForUser.first()
            return true
        }
        else {
            tanMethodSelector.findPreferredTanMethod(bank.tanMethodsAvailableForUser, context.preferredTanMethods, context.tanMethodsNotSupportedByApplication)?.let {
                bank.selectedTanMethod = it
                return true
            }

            // we know user's supported tan methods, now ask user which one to select
            val suggestedTanMethod = tanMethodSelector.getSuggestedTanMethod(bank.tanMethodsAvailableForUser, context.tanMethodsNotSupportedByApplication)

            val selectedTanMethod = context.callback.askUserForTanMethod(bank.tanMethodsAvailableForUser, suggestedTanMethod)

            if (selectedTanMethod != null) {
                bank.selectedTanMethod = selectedTanMethod
                return true
            }
            else {
                return false
            }
        }
    }


    protected open fun updateBankData(bank: BankData, response: BankResponse) {
        modelMapper.updateBankData(bank, response)
    }

    protected open fun updateBankAndCustomerDataIfResponseSuccessful(context: JobContext, response: BankResponse) {
        if (response.successful) {
            updateBankAndCustomerData(context.bank, response, context)
        }
    }

    protected open fun updateBankAndCustomerData(bank: BankData, response: BankResponse, context: JobContext) {
        updateBankData(bank, response)

        modelMapper.updateCustomerData(bank, response, context)
    }


    open fun isJobSupported(bank: BankData, segmentId: ISegmentId): Boolean {
        return modelMapper.isJobSupported(bank, segmentId)
    }

}