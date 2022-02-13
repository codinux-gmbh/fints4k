package net.dankito.banking.fints

import kotlinx.datetime.LocalDate
import net.dankito.banking.fints.messages.MessageBuilder
import net.dankito.banking.fints.messages.MessageBuilderResult
import net.dankito.banking.fints.messages.datenelemente.implementierte.signatur.VersionDesSicherheitsverfahrens
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.*
import net.dankito.banking.fints.messages.segmente.id.CustomerSegmentId
import net.dankito.banking.fints.messages.segmente.id.ISegmentId
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.model.mapper.ModelMapper
import net.dankito.banking.fints.response.BankResponse
import net.dankito.banking.fints.response.InstituteSegmentId
import net.dankito.banking.fints.response.client.*
import net.dankito.banking.fints.response.segments.*
import net.dankito.banking.fints.tan.FlickerCodeDecoder
import net.dankito.banking.fints.tan.TanImageDecoder
import net.dankito.banking.fints.util.TanMethodSelector
import net.dankito.utils.multiplatform.log.LoggerFactory
import net.dankito.utils.multiplatform.ObjectReference
import net.dankito.utils.multiplatform.extensions.millisSinceEpochAtEuropeBerlin
import net.dankito.utils.multiplatform.extensions.minusDays
import net.dankito.utils.multiplatform.extensions.todayAtEuropeBerlin
import net.dankito.utils.multiplatform.extensions.todayAtSystemDefaultTimeZone


/**
 * Low level class that executes concrete business transactions (= FinTS Geschäftsvorfälle).
 *
 * In almost all cases you want to use [FinTsClientDeprecated] which wraps these business transactions to a higher level API.
 */
open class FinTsJobExecutor(
    protected open val requestExecutor: RequestExecutor = RequestExecutor(),
    protected open val messageBuilder: MessageBuilder = MessageBuilder(),
    protected open val modelMapper: ModelMapper = ModelMapper(messageBuilder),
    protected open val tanMethodSelector: TanMethodSelector = TanMethodSelector()
) {

    companion object {
        private val log = LoggerFactory.getLogger(FinTsJobExecutor::class)
    }


    open fun getAnonymousBankInfo(context: JobContext, callback: (BankResponse) -> Unit) {
        context.startNewDialog()

        val message = messageBuilder.createAnonymousDialogInitMessage(context)

        getAndHandleResponseForMessage(context, message) { response ->
            if (response.successful) {
                closeAnonymousDialog(context, response)
            }

            callback(response)
        }
    }

    protected open fun closeAnonymousDialog(context: JobContext, response: BankResponse) {

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
    open fun retrieveBasicDataLikeUsersTanMethods(context: JobContext, preferredTanMethods: List<TanMethodType>? = null, preferredTanMedium: String? = null,
                                                  closeDialog: Boolean = false, callback: (BankResponse) -> Unit) {
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
        context.startNewDialog(closeDialog, versionOfSecurityProcedure = VersionDesSicherheitsverfahrens.Version_1)

        val message = messageBuilder.createInitDialogMessage(context)

        getAndHandleResponseForMessage(context, message) { response ->
            closeDialog(context)

            handleGetUsersTanMethodsResponse(context, response) { getTanMethodsResponse ->
                if (bank.tanMethodsAvailableForUser.isEmpty()) { // could not retrieve supported tan methods for user
                    callback(getTanMethodsResponse)
                } else {
                    getUsersTanMethod(context, preferredTanMethods) {
                        if (bank.isTanMethodSelected == false) {
                            callback(getTanMethodsResponse)
                        } else if (bank.tanMedia.isEmpty() && isJobSupported(bank, CustomerSegmentId.TanMediaList)) { // tan media not retrieved yet
                            getTanMediaList(context, TanMedienArtVersion.Alle, TanMediumKlasse.AlleMedien, preferredTanMedium) {
                                callback(getTanMethodsResponse) // TODO: judge if bank requires selecting TAN media and if though evaluate getTanMediaListResponse
                            }
                        } else {
                            callback(getTanMethodsResponse)
                        }
                    }
                }
            }
        }
    }

    protected open fun handleGetUsersTanMethodsResponse(context: JobContext, response: BankResponse, callback: (BankResponse) -> Unit) {
        val getUsersTanMethodsResponse = GetUserTanMethodsResponse(response)

        // even though it is required by specification some banks don't support retrieving user's TAN method by setting TAN method to '999'
        if (bankDoesNotSupportRetrievingUsersTanMethods(getUsersTanMethodsResponse)) {
            getBankDataForNewUserViaAnonymousDialog(context, callback) // TODO: should not be necessary anymore
        }
        else {
            callback(getUsersTanMethodsResponse)
        }
    }

    protected open fun bankDoesNotSupportRetrievingUsersTanMethods(response: BankResponse): Boolean {
        return response.successful == false &&
                response.segmentFeedbacks.flatMap { it.feedbacks }.firstOrNull { it.responseCode == 9200 &&
                        it.message == "Gewähltes Zwei-Schritt-Verfahren nicht unterstützt." } != null
    }

    // TODO: this is only a quick fix. Find a better and general solution
    protected open fun getBankDataForNewUserViaAnonymousDialog(context: JobContext, callback: (BankResponse) -> Unit) {
        getAnonymousBankInfo(context) { anonymousBankInfoResponse ->
            val bank = context.bank

            if (anonymousBankInfoResponse.successful == false) {
                callback(anonymousBankInfoResponse)
            } else if (bank.tanMethodsSupportedByBank.isEmpty()) { // should only be a theoretical error
                callback(BankResponse(true, internalError = "Die TAN Verfahren der Bank konnten nicht ermittelt werden")) // TODO: translate
            } else {
                bank.tanMethodsAvailableForUser = bank.tanMethodsSupportedByBank

                getUsersTanMethod(context) { didSelectTanMethod ->
                    if (didSelectTanMethod) {
                        initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks(context) { initDialogResponse ->
                            closeDialog(context)

                            callback(initDialogResponse)
                        }
                    } else {
                        callback(createNoTanMethodSelectedResponse(bank))
                    }
                }
            }
        }
    }


    open fun getAccounts(context: JobContext, callback: (BankResponse) -> Unit) {
        initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks(context) { response ->
            closeDialog(context)

            callback(response)
        }
    }


    open fun getTransactionsAsync(context: JobContext, parameter: GetAccountTransactionsParameter, callback: (GetAccountTransactionsResponse) -> Unit) {

        val dialogContext = context.startNewDialog()

        initDialogWithStrongCustomerAuthentication(context) { initDialogResponse ->

            if (initDialogResponse.successful == false) {
                callback(GetAccountTransactionsResponse(context, initDialogResponse, RetrievedAccountData.unsuccessful(parameter.account)))
            }
            else {
                // we now retrieved the fresh account information from FinTS server, use that one
                 parameter.account = getUpdatedAccount(context, parameter.account)

                mayGetBalance(context, parameter) { balanceResponse ->
                    if (dialogContext.didBankCloseDialog) {
                        callback(GetAccountTransactionsResponse(context, balanceResponse ?: initDialogResponse, RetrievedAccountData.unsuccessful(parameter.account)))
                    }
                    else {
                        getTransactionsAfterInitAndGetBalance(context, parameter, balanceResponse, callback)
                    }
                }
            }
        }
    }

    private fun getUpdatedAccount(context: JobContext, account: AccountData): AccountData {
        return context.bank.accounts.firstOrNull { it.accountIdentifier == account.accountIdentifier } ?: account
    }

    protected open fun getTransactionsAfterInitAndGetBalance(context: JobContext, parameter: GetAccountTransactionsParameter,
                                                             balanceResponse: BankResponse?, callback: (GetAccountTransactionsResponse) -> Unit) {
        var balance: Money? = balanceResponse?.getFirstSegmentById<BalanceSegment>(InstituteSegmentId.Balance)?.let {
            Money(it.balance, it.currency)
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
                remainingMt940String = remainder

                parameter.retrievedChunkListener?.invoke(bookedTransactions)
            }

            response.getFirstSegmentById<ReceivedCreditCardTransactionsAndBalance>(InstituteSegmentId.CreditCardTransactions)?.let { creditCardTransactionsSegment ->
                balance = Money(creditCardTransactionsSegment.balance.amount, creditCardTransactionsSegment.balance.currency ?: "EUR")
                bookedTransactions.addAll(creditCardTransactionsSegment.transactions.map { AccountTransaction(parameter.account, it.amount, it.description, it.bookingDate, it.transactionDescriptionBase ?: "", null, null, "", it.valueDate) })
            }
        }

        getAndHandleResponseForMessage(context, message) { response ->
            closeDialog(context)

            val successful = response.tanRequiredButWeWereToldToAbortIfSo
                    || (response.successful && (parameter.alsoRetrieveBalance == false || balance != null))
            val fromDate = parameter.fromDate
                ?: parameter.account.countDaysForWhichTransactionsAreKept?.let { LocalDate.todayAtSystemDefaultTimeZone().minusDays(it) }
                ?: bookedTransactions.minByOrNull { it.valueDate.millisSinceEpochAtEuropeBerlin }?.valueDate
            val retrievedData = RetrievedAccountData(parameter.account, successful, balance, bookedTransactions, unbookedTransactions, fromDate, parameter.toDate ?: LocalDate.todayAtEuropeBerlin(), response.internalError)

            callback(GetAccountTransactionsResponse(context, response, retrievedData,
                if (parameter.maxCountEntries != null) parameter.isSettingMaxCountEntriesAllowedByBank else null))
        }
    }

    protected open fun mayGetBalance(context: JobContext, parameter: GetAccountTransactionsParameter, callback: (BankResponse?) -> Unit) {
        if (parameter.alsoRetrieveBalance && parameter.account.supportsRetrievingBalance) {
            val message = messageBuilder.createGetBalanceMessage(context, parameter.account)

            getAndHandleResponseForMessage(context, message, callback)
        }
        else {
            callback(null)
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
    protected open fun synchronizeCustomerSystemId(context: JobContext, callback: (FinTsClientResponse) -> Unit) {

        context.startNewDialog()

        val message = messageBuilder.createSynchronizeCustomerSystemIdMessage(context)

        getAndHandleResponseForMessage(context, message) { response ->
            if (response.successful) {
                closeDialog(context)
            }

            callback(FinTsClientResponse(context, response))
        }
    }


    open fun getTanMediaList(context: JobContext, tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle, tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien,
                             callback: (GetTanMediaListResponse) -> Unit) {
        getTanMediaList(context, tanMediaKind, tanMediumClass, null, callback)
    }

    protected open fun getTanMediaList(context: JobContext, tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle, tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien,
                             preferredTanMedium: String? = null, callback: (GetTanMediaListResponse) -> Unit) {

        sendMessageAndHandleResponse(context, CustomerSegmentId.TanMediaList, false, {
            messageBuilder.createGetTanMediaListMessage(context, tanMediaKind, tanMediumClass)
        }) { response ->
            handleGetTanMediaListResponse(context, response, preferredTanMedium, callback)
        }
    }

    protected open fun handleGetTanMediaListResponse(context: JobContext, response: BankResponse, preferredTanMedium: String? = null, callback: (GetTanMediaListResponse) -> Unit) {
        val bank = context.bank

        // TAN media list (= TAN generator list) is only returned for users with chipTAN TAN methods
        val tanMediaList = if (response.successful == false) null
        else response.getFirstSegmentById<TanMediaList>(InstituteSegmentId.TanMediaList)

        tanMediaList?.let {
            bank.tanMedia = it.tanMedia

            bank.selectedTanMedium = preferredTanMedium?.let { bank.tanMedia.firstOrNull { it.mediumName == preferredTanMedium } }
                ?: bank.selectedTanMedium?.let { selected -> bank.tanMedia.firstOrNull { it.mediumName == selected.mediumName } } // try to find selectedTanMedium in new TanMedia instances
                ?: bank.tanMedia.firstOrNull { it.mediumName != null }
        }

        callback(GetTanMediaListResponse(context, response, tanMediaList))
    }


    open fun changeTanMedium(context: JobContext, newActiveTanMedium: TanGeneratorTanMedium, callback: (BankResponse) -> Unit) {
        val bank = context.bank

        if (bank.changeTanMediumParameters?.enteringAtcAndTanRequired == true) {
            context.callback.enterTanGeneratorAtc(bank, newActiveTanMedium) { enteredAtc ->
                if (enteredAtc.hasAtcBeenEntered == false) {
                    val message = "Bank requires to enter ATC and TAN in order to change TAN medium." // TODO: translate
                    callback(BankResponse(false, internalError = message))
                }
                else {
                    sendChangeTanMediumMessage(context, newActiveTanMedium, enteredAtc, callback)
                }
            }
        }
        else {
            sendChangeTanMediumMessage(context, newActiveTanMedium, null, callback)
        }
    }

    protected open fun sendChangeTanMediumMessage(context: JobContext, newActiveTanMedium: TanGeneratorTanMedium, enteredAtc: EnterTanGeneratorAtcResult?,
                                                  callback: (BankResponse) -> Unit) {

        sendMessageAndHandleResponse(context, null, true, {
            messageBuilder.createChangeTanMediumMessage(context, newActiveTanMedium, enteredAtc?.tan, enteredAtc?.atc)
        }, callback)
    }


    open fun doBankTransferAsync(context: JobContext, bankTransferData: BankTransferData, callback: (FinTsClientResponse) -> Unit) {

        sendMessageAndHandleResponse(context, null, true, {
            val updatedAccount = getUpdatedAccount(context, context.account!!)
            messageBuilder.createBankTransferMessage(context, bankTransferData, updatedAccount)
        }) { response ->
            callback(FinTsClientResponse(context, response))
        }
    }


    protected open fun getAndHandleResponseForMessage(context: JobContext, message: MessageBuilderResult, callback: (BankResponse) -> Unit) {
        requestExecutor.getAndHandleResponseForMessage(message, context,
            { tanResponse, bankResponse, tanRequiredCallback ->
                // if we receive a message that tells us a TAN is required below callback doesn't get called for that message -> update data here
                // for Hypovereinsbank it's absolutely necessary to update bank data (more specific: PinInfo / HIPINS) after first strong authentication dialog init response
                // as HIPINS differ in anonymous and in authenticated dialog. Anonymous dialog tells us for HKSAL and HKKAZ no TAN is needed
                updateBankAndCustomerDataIfResponseSuccessful(context, bankResponse)
                handleEnteringTanRequired(context, tanResponse, bankResponse, tanRequiredCallback)
            }) { response ->
            // TODO: really update data only on complete successfully response? as it may contain useful information anyway  // TODO: extract method for this code part
            updateBankAndCustomerDataIfResponseSuccessful(context, response)

            callback(response)
        }
    }

    protected open fun fireAndForgetMessage(context: JobContext, message: MessageBuilderResult) {
        requestExecutor.fireAndForgetMessage(context, message)
    }


    protected open fun handleEnteringTanRequired(context: JobContext, tanResponse: TanResponse, response: BankResponse, callback: (BankResponse) -> Unit) {
        val bank = context.bank // TODO: copy required data to TanChallenge
        val tanChallenge = createTanChallenge(tanResponse, bank)

        val userDidCancelEnteringTan = ObjectReference(false)

        context.callback.enterTan(bank, tanChallenge)  { enteredTanResult ->
            userDidCancelEnteringTan.value = true

            handleEnterTanResult(context, enteredTanResult, tanResponse, response, callback)
        }

        mayRetrieveAutomaticallyIfUserEnteredDecoupledTan(context, tanChallenge, tanResponse, userDidCancelEnteringTan)
    }

    protected open fun createTanChallenge(tanResponse: TanResponse, bank: BankData): TanChallenge {
        // TODO: is this true for all tan methods?
        val messageToShowToUser = tanResponse.challenge ?: ""
        val challenge = tanResponse.challengeHHD_UC ?: ""
        val tanMethod = bank.selectedTanMethod

        return when (tanMethod.type) {
            TanMethodType.ChipTanFlickercode ->
                FlickerCodeTanChallenge(
                    FlickerCodeDecoder().decodeChallenge(challenge, tanMethod.hhdVersion ?: HHDVersion.HHD_1_4), // HHD 1.4 is currently the most used version
                    messageToShowToUser, challenge, tanMethod, tanResponse.tanMediaIdentifier)

            TanMethodType.ChipTanQrCode, TanMethodType.ChipTanPhotoTanMatrixCode,
            TanMethodType.QrCode, TanMethodType.photoTan ->
                ImageTanChallenge(TanImageDecoder().decodeChallenge(challenge), messageToShowToUser, challenge, tanMethod, tanResponse.tanMediaIdentifier)

            else -> TanChallenge(messageToShowToUser, challenge, tanMethod, tanResponse.tanMediaIdentifier)
        }
    }

    protected open fun mayRetrieveAutomaticallyIfUserEnteredDecoupledTan(context: JobContext, tanChallenge: TanChallenge, tanResponse: TanResponse,
                                                                         userDidCancelEnteringTan: ObjectReference<Boolean>
    ) {
        context.bank.selectedTanMethod.decoupledParameters?.let { decoupledTanMethodParameters ->
            if (tanResponse.tanProcess == TanProcess.AppTan && decoupledTanMethodParameters.periodicStateRequestsAllowed) {
                automaticallyRetrieveIfUserEnteredDecoupledTan(context, tanChallenge, userDidCancelEnteringTan)
            }
        }
    }

    protected open fun automaticallyRetrieveIfUserEnteredDecoupledTan(context: JobContext, tanChallenge: TanChallenge, userDidCancelEnteringTan: ObjectReference<Boolean>) {
        log.info("automaticallyRetrieveIfUserEnteredDecoupledTan() called for $tanChallenge")
    }

    protected open fun handleEnterTanResult(context: JobContext, enteredTanResult: EnterTanResult, tanResponse: TanResponse,
                                            response: BankResponse, callback: (BankResponse) -> Unit) {

        if (enteredTanResult.changeTanMethodTo != null) {
            handleUserAsksToChangeTanMethodAndResendLastMessage(context, enteredTanResult.changeTanMethodTo, callback)
        }
        else if (enteredTanResult.changeTanMediumTo is TanGeneratorTanMedium) {
            handleUserAsksToChangeTanMediumAndResendLastMessage(context, enteredTanResult.changeTanMediumTo,
                enteredTanResult.changeTanMediumResultCallback, callback)
        }
        else if (enteredTanResult.enteredTan == null) {
            // i tried to send a HKTAN with cancelJob = true but then i saw there are no tan methods that support cancellation (at least not at my bank)
            // but it's not required anyway, tan times out after some time. Simply don't respond anything and close dialog
            response.tanRequiredButUserDidNotEnterOne = true

            callback(response)
        }
        else {
            sendTanToBank(context, enteredTanResult.enteredTan, tanResponse, callback)
        }
    }

    protected open fun sendTanToBank(context: JobContext, enteredTan: String, tanResponse: TanResponse, callback: (BankResponse) -> Unit) {

        val message = messageBuilder.createSendEnteredTanMessage(context, enteredTan, tanResponse)

        getAndHandleResponseForMessage(context, message, callback)
    }

    protected open fun handleUserAsksToChangeTanMethodAndResendLastMessage(context: JobContext, changeTanMethodTo: TanMethod, callback: (BankResponse) -> Unit) {

        context.bank.selectedTanMethod = changeTanMethodTo


        val lastCreatedMessage = context.dialog.currentMessage

        lastCreatedMessage?.let { closeDialog(context) }

        resendMessageInNewDialog(context, lastCreatedMessage, callback)
    }

    protected open fun handleUserAsksToChangeTanMediumAndResendLastMessage(context: JobContext, changeTanMediumTo: TanGeneratorTanMedium,
                                                                           changeTanMediumResultCallback: ((FinTsClientResponse) -> Unit)?,
                                                                           callback: (BankResponse) -> Unit) {

        val lastCreatedMessage = context.dialog.currentMessage

        lastCreatedMessage?.let { closeDialog(context) }


        changeTanMedium(context, changeTanMediumTo) { changeTanMediumResponse ->
            changeTanMediumResultCallback?.invoke(FinTsClientResponse(context, changeTanMediumResponse))

            if (changeTanMediumResponse.successful == false || lastCreatedMessage == null) {
                callback(changeTanMediumResponse)
            }
            else {
                resendMessageInNewDialog(context, lastCreatedMessage, callback)
            }
        }
    }


    protected open fun resendMessageInNewDialog(context: JobContext, lastCreatedMessage: MessageBuilderResult?, callback: (BankResponse) -> Unit) {

        if (lastCreatedMessage != null) { // do not use previousDialogContext.currentMessage as this may is previous dialog's dialog close message
            context.startNewDialog(chunkedResponseHandler = context.dialog.chunkedResponseHandler)

            initDialogWithStrongCustomerAuthentication(context) { initDialogResponse ->
                if (initDialogResponse.successful == false) {
                    callback(initDialogResponse)
                }
                else {
                    val newMessage = messageBuilder.rebuildMessage(context, lastCreatedMessage)

                    getAndHandleResponseForMessage(context, newMessage) { response ->
                        closeDialog(context)

                        callback(response)
                    }
                }
            }
        }
        else {
            val errorMessage = "There's no last action (like retrieve account transactions, transfer money, ...) to re-send with new TAN method. Probably an internal programming error." // TODO: translate
            callback(BankResponse(false, internalError = errorMessage)) // should never come to this
        }
    }


    protected open fun sendMessageAndHandleResponse(context: JobContext, segmentForNonStrongCustomerAuthenticationTwoStepTanProcess: CustomerSegmentId? = null,
                                                    closeDialog: Boolean = true, createMessage: () -> MessageBuilderResult, callback: (BankResponse) -> Unit) {

        context.startNewDialog(closeDialog)

        if (segmentForNonStrongCustomerAuthenticationTwoStepTanProcess == null) {
            initDialogWithStrongCustomerAuthentication(context) { initDialogResponse ->
                sendMessageAndHandleResponseAfterDialogInitialization(context, initDialogResponse, createMessage, callback)
            }
        }
        else {
            initDialogMessageWithoutStrongCustomerAuthenticationAfterSuccessfulChecks(context, segmentForNonStrongCustomerAuthenticationTwoStepTanProcess) { initDialogResponse ->
                sendMessageAndHandleResponseAfterDialogInitialization(context, initDialogResponse, createMessage, callback)
            }
        }
    }

    private fun sendMessageAndHandleResponseAfterDialogInitialization(context: JobContext, initDialogResponse: BankResponse,
                                                                      createMessage: () -> MessageBuilderResult, callback: (BankResponse) -> Unit) {

        if (initDialogResponse.successful == false) {
            callback(initDialogResponse)
        }
        else {
            val message = createMessage()

            getAndHandleResponseForMessage(context, message) { response ->
                closeDialog(context)

                callback(response)
            }
        }
    }

    protected open fun initDialogWithStrongCustomerAuthentication(context: JobContext, callback: (BankResponse) -> Unit) {

        // we first need to retrieve supported tan methods and jobs before we can do anything
        ensureBasicBankDataRetrieved(context) { retrieveBasicBankDataResponse ->
            if (retrieveBasicBankDataResponse.successful == false) {
                callback(retrieveBasicBankDataResponse)
            }
            else {
                // as in the next step we have to supply user's tan method, ensure user selected his or her
                ensureTanMethodIsSelected(context) { tanMethodSelectedResponse ->
                    if (tanMethodSelectedResponse.successful == false) {
                        callback(tanMethodSelectedResponse)
                    }
                    else {
                        initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks(context, callback)
                    }
                }
            }
        }
    }

    protected open fun initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks(context: JobContext, callback: (BankResponse) -> Unit) {

        context.startNewDialog(false) // don't know if it's ok for all invocations of this method to set closeDialog to false (was actually only set in getAccounts())

        val message = messageBuilder.createInitDialogMessage(context)

        getAndHandleResponseForMessage(context, message, callback)
    }

    protected open fun initDialogMessageWithoutStrongCustomerAuthenticationAfterSuccessfulChecks(context: JobContext, segmentIdForTwoStepTanProcess: CustomerSegmentId?,
                                                                                                 callback: (BankResponse) -> Unit) {

        val message = messageBuilder.createInitDialogMessageWithoutStrongCustomerAuthentication(context, segmentIdForTwoStepTanProcess)

        getAndHandleResponseForMessage(context, message, callback)
    }

    protected open fun closeDialog(context: JobContext) {

        // bank already closed dialog -> there's no need to send dialog end message
        if (shouldNotCloseDialog(context)) {
            return
        }

        val dialogEndRequestBody = messageBuilder.createDialogEndMessage(context)

        fireAndForgetMessage(context, dialogEndRequestBody)
    }

    private fun shouldNotCloseDialog(context: JobContext): Boolean {
        return context.dialog.closeDialog == false || context.dialog.didBankCloseDialog
    }


    protected open fun ensureBasicBankDataRetrieved(context: JobContext, callback: (BankResponse) -> Unit) {
        val bank = context.bank

        if (bank.tanMethodsSupportedByBank.isEmpty() || bank.supportedJobs.isEmpty()) {
            retrieveBasicDataLikeUsersTanMethods(context) { getBankInfoResponse ->
                if (getBankInfoResponse.successful == false) {
                    callback(getBankInfoResponse)
                } else if (bank.tanMethodsSupportedByBank.isEmpty() || bank.supportedJobs.isEmpty()) {
                    callback(BankResponse(false, internalError =
                    "Could not retrieve basic bank data like supported tan methods or supported jobs")) // TODO: translate // TODO: add as messageToShowToUser
                } else {
                    callback(BankResponse(true))
                }
            }
        }
        else {
            callback(BankResponse(true))
        }
    }

    protected open fun ensureTanMethodIsSelected(context: JobContext, callback: (BankResponse) -> Unit) {
        val bank = context.bank

        if (bank.isTanMethodSelected == false) {
            if (bank.tanMethodsAvailableForUser.isEmpty()) {
                retrieveBasicDataLikeUsersTanMethods(context) { retrieveBasicDataResponse ->
                    callback(retrieveBasicDataResponse)
                }
            }
            else {
                getUsersTanMethod(context) {
                    callback(createNoTanMethodSelectedResponse(bank))
                }
            }
        }
        else {
            callback(createNoTanMethodSelectedResponse(bank))
        }
    }

    private fun createNoTanMethodSelectedResponse(bank: BankData): BankResponse {
        val noTanMethodSelected = !!!bank.isTanMethodSelected
        val errorMessage = if (noTanMethodSelected) "User did not select a TAN method" else null // TODO: translate

        return BankResponse(true, noTanMethodSelected = noTanMethodSelected, internalError = errorMessage)
    }

    open fun getUsersTanMethod(context: JobContext, preferredTanMethods: List<TanMethodType>? = null, done: (Boolean) -> Unit) {
        val bank = context.bank

        if (bank.tanMethodsAvailableForUser.size == 1) { // user has only one TAN method -> set it and we're done
            bank.selectedTanMethod = bank.tanMethodsAvailableForUser.first()
            done(true)
        }
        else {
            tanMethodSelector.findPreferredTanMethod(bank.tanMethodsAvailableForUser, preferredTanMethods)?.let {
                bank.selectedTanMethod = it
                done(true)
                return
            }

            // we know user's supported tan methods, now ask user which one to select
            val suggestedTanMethod = tanMethodSelector.getSuggestedTanMethod(bank.tanMethodsAvailableForUser)
            context.callback.askUserForTanMethod(bank.tanMethodsAvailableForUser, suggestedTanMethod) { selectedTanMethod ->
                if (selectedTanMethod != null) {
                    bank.selectedTanMethod = selectedTanMethod
                    done(true)
                }
                else {
                    done(false)
                }
            }
        }
    }


    protected open fun updateBankData(bank: BankData, response: BankResponse) {
        modelMapper.updateBankData(bank, response)
    }

    protected open fun updateBankAndCustomerDataIfResponseSuccessful(context: JobContext, response: BankResponse) {
        if (response.successful) {
            updateBankAndCustomerData(context.bank, response)
        }
    }

    protected open fun updateBankAndCustomerData(bank: BankData, response: BankResponse) {
        updateBankData(bank, response)

        modelMapper.updateCustomerData(bank, response)
    }


    open fun isJobSupported(bank: BankData, segmentId: ISegmentId): Boolean {
        return modelMapper.isJobSupported(bank, segmentId)
    }

}