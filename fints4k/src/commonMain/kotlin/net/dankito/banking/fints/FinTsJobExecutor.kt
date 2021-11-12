package net.dankito.banking.fints

import net.dankito.banking.fints.callback.FinTsClientCallback
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
import net.dankito.banking.fints.response.client.FinTsClientResponse
import net.dankito.banking.fints.response.client.GetTanMediaListResponse
import net.dankito.banking.fints.response.client.GetTransactionsResponse
import net.dankito.banking.fints.response.client.GetUserTanMethodsResponse
import net.dankito.banking.fints.response.segments.*
import net.dankito.banking.fints.tan.FlickerCodeDecoder
import net.dankito.banking.fints.tan.TanImageDecoder
import net.dankito.banking.fints.transactions.IAccountTransactionsParser
import net.dankito.banking.fints.transactions.Mt940AccountTransactionsParser
import net.dankito.banking.fints.util.TanMethodSelector
import net.dankito.utils.multiplatform.log.LoggerFactory
import net.dankito.utils.multiplatform.Date
import net.dankito.utils.multiplatform.ObjectReference


/**
 * Low level class that executes concrete business transactions (= FinTS Geschäftsvorfälle).
 *
 * In almost all cases you want to use [FinTsClient] which wraps these business transactions to a higher level API.
 */
open class FinTsJobExecutor(
    open var callback: FinTsClientCallback,
    protected open val requestExecutor: RequestExecutor = RequestExecutor(),
    protected open val messageBuilder: MessageBuilder = MessageBuilder(),
    protected open val mt940Parser: IAccountTransactionsParser = Mt940AccountTransactionsParser(),
    protected open val modelMapper: ModelMapper = ModelMapper(messageBuilder),
    protected open val tanMethodSelector: TanMethodSelector = TanMethodSelector(),
    protected open val product: ProductData = ProductData("15E53C26816138699C7B6A3E8", "1.0.0") // TODO: get version dynamically
) {

    companion object {
        private val log = LoggerFactory.getLogger(FinTsJobExecutor::class)
    }


    open val messageLogWithoutSensitiveData: List<MessageLogEntry>
        get() = requestExecutor.messageLogWithoutSensitiveData


    init {
        mt940Parser.logAppender = requestExecutor.messageLogAppender // TODO: find a better solution to append messages to MessageLog
    }


    open fun getAnonymousBankInfo(bank: BankData, callback: (BankResponse) -> Unit) {
        val dialogContext = DialogContext(bank, product)

        val message = messageBuilder.createAnonymousDialogInitMessage(dialogContext)

        getAndHandleResponseForMessage(message, dialogContext) { response ->
            if (response.successful) {
                closeAnonymousDialog(dialogContext, response)
            }

            callback(response)
        }
    }

    protected open fun closeAnonymousDialog(dialogContext: DialogContext, response: BankResponse) {

        // bank already closed dialog -> there's no need to send dialog end message
        if (dialogContext.closeDialog == false || dialogContext.didBankCloseDialog) {
            return
        }

        val dialogEndRequestBody = messageBuilder.createAnonymousDialogEndMessage(dialogContext)

        fireAndForgetMessage(dialogEndRequestBody, dialogContext)
    }


    /**
     * Retrieves basic data like user's TAN methods, ... in an not authenticated Init dialog.
     *
     * This is the first step to do when adding a new account as for almost all other jobs user's selected TAN method has to be specified.
     *
     * Be aware this method resets BPD, UPD and selected TAN method!
     */
    open fun retrieveBasicDataLikeUsersTanMethods(bank: BankData, preferredTanMethods: List<TanMethodType>? = null, preferredTanMedium: String? = null,
                                                  closeDialog: Boolean = false, callback: (BankResponse) -> Unit) {
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
        val dialogContext = DialogContext(bank, product, closeDialog, versionOfSecurityMethod = VersionDesSicherheitsverfahrens.Version_1)

        val message = messageBuilder.createInitDialogMessage(dialogContext)

        getAndHandleResponseForMessage(message, dialogContext) { response ->
            closeDialog(dialogContext)

            handleGetUsersTanMethodsResponse(response, dialogContext) { getTanMethodsResponse ->
                if (bank.tanMethodsAvailableForUser.isEmpty()) { // could not retrieve supported tan methods for user
                    callback(getTanMethodsResponse)
                } else {
                    getUsersTanMethod(bank, preferredTanMethods) {
                        if (bank.isTanMethodSelected == false) {
                            callback(getTanMethodsResponse)
                        } else if (bank.tanMedia.isEmpty() && isJobSupported(bank, CustomerSegmentId.TanMediaList)) { // tan media not retrieved yet
                            getTanMediaList(bank, TanMedienArtVersion.Alle, TanMediumKlasse.AlleMedien, preferredTanMedium) {
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

    protected open fun handleGetUsersTanMethodsResponse(response: BankResponse, dialogContext: DialogContext, callback: (BankResponse) -> Unit) {
        val getUsersTanMethodsResponse = GetUserTanMethodsResponse(response)

        // even though it is required by specification some banks don't support retrieving user's TAN method by setting TAN method to '999'
        if (bankDoesNotSupportRetrievingUsersTanMethods(getUsersTanMethodsResponse)) {
            getBankDataForNewUserViaAnonymousDialog(dialogContext.bank, callback) // TODO: should not be necessary anymore
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
    protected open fun getBankDataForNewUserViaAnonymousDialog(bank: BankData, callback: (BankResponse) -> Unit) {
        getAnonymousBankInfo(bank) { anonymousBankInfoResponse ->
            if (anonymousBankInfoResponse.successful == false) {
                callback(anonymousBankInfoResponse)
            } else if (bank.tanMethodsSupportedByBank.isEmpty()) { // should only be a theoretical error
                callback(BankResponse(true, internalError = "Die TAN Verfahren der Bank konnten nicht ermittelt werden")) // TODO: translate
            } else {
                bank.tanMethodsAvailableForUser = bank.tanMethodsSupportedByBank

                getUsersTanMethod(bank) { didSelectTanMethod ->
                    if (didSelectTanMethod) {
                        val dialogContext = DialogContext(bank, product)

                        initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks(dialogContext) { initDialogResponse ->
                            closeDialog(dialogContext)

                            callback(initDialogResponse)
                        }
                    } else {
                        callback(createNoTanMethodSelectedResponse(bank))
                    }
                }
            }
        }
    }


    open fun getAccounts(bank: BankData, callback: (BankResponse) -> Unit) {

        val dialogContext = DialogContext(bank, product, false)

        initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks(dialogContext) { response ->
            closeDialog(dialogContext)

            callback(response)
        }
    }


    open fun getTransactionsAsync(parameter: GetTransactionsParameter, bank: BankData, callback: (GetTransactionsResponse) -> Unit) {

        val dialogContext = DialogContext(bank, product)

        initDialogWithStrongCustomerAuthentication(dialogContext) { initDialogResponse ->

            if (initDialogResponse.successful == false) {
                callback(GetTransactionsResponse(initDialogResponse, RetrievedAccountData.unsuccessfulList(parameter.account)))
            }
            else {
                // we now retrieved the fresh account information from FinTS server, use that one
                 parameter.account = getUpdatedAccount(bank, parameter.account)

                mayGetBalance(parameter, dialogContext) { balanceResponse ->
                    if (dialogContext.didBankCloseDialog) {
                        callback(GetTransactionsResponse(balanceResponse ?: initDialogResponse, RetrievedAccountData.unsuccessfulList(parameter.account)))
                    }
                    else {
                        getTransactionsAfterInitAndGetBalance(parameter, dialogContext, balanceResponse, callback)
                    }
                }
            }
        }
    }

    private fun getUpdatedAccount(bank: BankData, account: AccountData): AccountData {
        return bank.accounts.firstOrNull { it.accountIdentifier == account.accountIdentifier } ?: account
    }

    protected open fun getTransactionsAfterInitAndGetBalance(parameter: GetTransactionsParameter, dialogContext: DialogContext,
                                                             balanceResponse: BankResponse?, callback: (GetTransactionsResponse) -> Unit) {
        var balance: Money? = balanceResponse?.getFirstSegmentById<BalanceSegment>(InstituteSegmentId.Balance)?.let {
            Money(it.balance, it.currency)
        }
        val bookedTransactions = mutableSetOf<AccountTransaction>()
        val unbookedTransactions = mutableSetOf<Any>()

        val message = messageBuilder.createGetTransactionsMessage(parameter, dialogContext)

        var remainingMt940String = ""

        dialogContext.abortIfTanIsRequired = parameter.abortIfTanIsRequired

        dialogContext.chunkedResponseHandler = { response ->
            response.getFirstSegmentById<ReceivedAccountTransactions>(InstituteSegmentId.AccountTransactionsMt940)?.let { transactionsSegment ->
                val (chunkTransaction, remainder) = mt940Parser.parseTransactionsChunk(remainingMt940String + transactionsSegment.bookedTransactionsString,
                    dialogContext.bank, parameter.account)

                bookedTransactions.addAll(chunkTransaction)
                remainingMt940String = remainder

                parameter.retrievedChunkListener?.invoke(bookedTransactions)
            }

            response.getFirstSegmentById<ReceivedCreditCardTransactionsAndBalance>(InstituteSegmentId.CreditCardTransactions)?.let { creditCardTransactionsSegment ->
                balance = Money(creditCardTransactionsSegment.balance.amount, creditCardTransactionsSegment.balance.currency ?: "EUR")
                bookedTransactions.addAll(creditCardTransactionsSegment.transactions.map { AccountTransaction(parameter.account, it.amount, it.description, it.bookingDate, it.transactionDescriptionBase ?: "", null, null, "", it.valueDate) })
            }
        }

        getAndHandleResponseForMessage(message, dialogContext) { response ->
            closeDialog(dialogContext)

            val successful = response.tanRequiredButWeWereToldToAbortIfSo
                    || (response.successful && (parameter.alsoRetrieveBalance == false || balance != null))
            val fromDate = parameter.fromDate
                ?: parameter.account.countDaysForWhichTransactionsAreKept?.let { Date.today.addDays(it * -1) }
                ?: bookedTransactions.map { it.valueDate }.sortedBy { it.millisSinceEpoch }.firstOrNull()
            val retrievedData = RetrievedAccountData(parameter.account, successful, balance, bookedTransactions, unbookedTransactions, fromDate, parameter.toDate ?: Date.today, response.internalError)

            callback(
                GetTransactionsResponse(response, listOf(retrievedData),
                if (parameter.maxCountEntries != null) parameter.isSettingMaxCountEntriesAllowedByBank else null
            )
            )
        }
    }

    protected open fun mayGetBalance(parameter: GetTransactionsParameter, dialogContext: DialogContext, callback: (BankResponse?) -> Unit) {
        if (parameter.alsoRetrieveBalance && parameter.account.supportsRetrievingBalance) {
            val message = messageBuilder.createGetBalanceMessage(parameter.account, dialogContext)

            getAndHandleResponseForMessage(message, dialogContext) { response ->
                callback(response)
            }
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
    protected open fun synchronizeCustomerSystemId(bank: BankData, callback: (FinTsClientResponse) -> Unit) {

        val dialogContext = DialogContext(bank, product)
        val message = messageBuilder.createSynchronizeCustomerSystemIdMessage(dialogContext)

        getAndHandleResponseForMessage(message, dialogContext) { response ->
            if (response.successful) {
                closeDialog(dialogContext)
            }

            callback(FinTsClientResponse(response))
        }
    }


    open fun getTanMediaList(bank: BankData, tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle, tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien,
                             callback: (GetTanMediaListResponse) -> Unit) {
        getTanMediaList(bank, tanMediaKind, tanMediumClass, null, callback)
    }

    protected open fun getTanMediaList(bank: BankData, tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle, tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien,
                             preferredTanMedium: String? = null, callback: (GetTanMediaListResponse) -> Unit) {

        sendMessageAndHandleResponse(bank, CustomerSegmentId.TanMediaList, false, { dialogContext ->
            messageBuilder.createGetTanMediaListMessage(dialogContext, tanMediaKind, tanMediumClass)
        }) { response ->
            handleGetTanMediaListResponse(response, bank, preferredTanMedium, callback)
        }
    }

    protected open fun handleGetTanMediaListResponse(response: BankResponse, bank: BankData, preferredTanMedium: String? = null, callback: (GetTanMediaListResponse) -> Unit) {
        // TAN media list (= TAN generator list) is only returned for users with chipTAN TAN methods
        val tanMediaList = if (response.successful == false) null
        else response.getFirstSegmentById<TanMediaList>(InstituteSegmentId.TanMediaList)

        tanMediaList?.let {
            bank.tanMedia = it.tanMedia

            bank.selectedTanMedium = preferredTanMedium?.let { bank.tanMedia.firstOrNull { it.mediumName == preferredTanMedium } }
                ?: bank.selectedTanMedium?.let { selected -> bank.tanMedia.firstOrNull { it.mediumName == selected.mediumName } } // try to find selectedTanMedium in new TanMedia instances
                ?: bank.tanMedia.firstOrNull { it.mediumName != null }
        }

        callback(GetTanMediaListResponse(response, tanMediaList))
    }


    open fun changeTanMedium(newActiveTanMedium: TanGeneratorTanMedium, bank: BankData, callback: (BankResponse) -> Unit) {

        if (bank.changeTanMediumParameters?.enteringAtcAndTanRequired == true) {
            this.callback.enterTanGeneratorAtc(bank, newActiveTanMedium) { enteredAtc ->
                if (enteredAtc.hasAtcBeenEntered == false) {
                    val message = "Bank requires to enter ATC and TAN in order to change TAN medium." // TODO: translate
                    callback(BankResponse(false, internalError = message))
                }
                else {
                    sendChangeTanMediumMessage(bank, newActiveTanMedium, enteredAtc, callback)
                }
            }
        }
        else {
            sendChangeTanMediumMessage(bank, newActiveTanMedium, null, callback)
        }
    }

    protected open fun sendChangeTanMediumMessage(bank: BankData, newActiveTanMedium: TanGeneratorTanMedium, enteredAtc: EnterTanGeneratorAtcResult?,
                                                  callback: (BankResponse) -> Unit) {

        sendMessageAndHandleResponse(bank, null, true, { dialogContext ->
            messageBuilder.createChangeTanMediumMessage(newActiveTanMedium, dialogContext, enteredAtc?.tan, enteredAtc?.atc)
        }) { response ->
            callback(response)
        }
    }


    open fun doBankTransferAsync(bankTransferData: BankTransferData, bank: BankData, account: AccountData, callback: (FinTsClientResponse) -> Unit) {

        sendMessageAndHandleResponse(bank, null, true, { dialogContext ->
            val updatedAccount = getUpdatedAccount(bank, account)
            messageBuilder.createBankTransferMessage(bankTransferData, updatedAccount, dialogContext)
        }) { response ->
            callback(FinTsClientResponse(response))
        }
    }


    protected open fun getAndHandleResponseForMessage(message: MessageBuilderResult, dialogContext: DialogContext, callback: (BankResponse) -> Unit) {
        requestExecutor.getAndHandleResponseForMessage(message, dialogContext,
            { tanResponse, bankResponse, tanRequiredCallback ->
                // if we receive a message that tells us a TAN is required below callback doesn't get called for that message -> update data here
                // for Hypovereinsbank it's absolutely necessary to update bank data (more specific: PinInfo / HIPINS) after first strong authentication dialog init response
                // as HIPINS differ in anonymous and in authenticated dialog. Anonymous dialog tells us for HKSAL and HKKAZ no TAN is needed
                updateBankAndCustomerDataIfResponseSuccessful(dialogContext, bankResponse)
                handleEnteringTanRequired(tanResponse, bankResponse, dialogContext, tanRequiredCallback)
            }) { response ->
            // TODO: really update data only on complete successfully response? as it may contain useful information anyway  // TODO: extract method for this code part
            updateBankAndCustomerDataIfResponseSuccessful(dialogContext, response)

            callback(response)
        }
    }

    protected open fun fireAndForgetMessage(message: MessageBuilderResult, dialogContext: DialogContext) {
        requestExecutor.fireAndForgetMessage(message, dialogContext)
    }


    protected open fun handleEnteringTanRequired(tanResponse: TanResponse, response: BankResponse, dialogContext: DialogContext, callback: (BankResponse) -> Unit) {
        val bank = dialogContext.bank // TODO: copy required data to TanChallenge
        val tanChallenge = createTanChallenge(tanResponse, bank)

        val userDidCancelEnteringTan = ObjectReference(false)

        this.callback.enterTan(bank, tanChallenge)  { enteredTanResult ->
            userDidCancelEnteringTan.value = true

            handleEnterTanResult(enteredTanResult, tanResponse, response, dialogContext, callback)
        }

        mayRetrieveAutomaticallyIfUserEnteredDecoupledTan(tanChallenge, tanResponse, userDidCancelEnteringTan, dialogContext)
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

    protected open fun mayRetrieveAutomaticallyIfUserEnteredDecoupledTan(tanChallenge: TanChallenge, tanResponse: TanResponse,
                                                                         userDidCancelEnteringTan: ObjectReference<Boolean>, dialogContext: DialogContext
    ) {
        dialogContext.bank.selectedTanMethod.decoupledParameters?.let { decoupledTanMethodParameters ->
            if (tanResponse.tanProcess == TanProcess.AppTan && decoupledTanMethodParameters.periodicStateRequestsAllowed) {
                automaticallyRetrieveIfUserEnteredDecoupledTan(tanChallenge, userDidCancelEnteringTan, dialogContext)
            }
        }
    }

    protected open fun automaticallyRetrieveIfUserEnteredDecoupledTan(tanChallenge: TanChallenge, userDidCancelEnteringTan: ObjectReference<Boolean>, dialogContext: DialogContext) {
        log.info("automaticallyRetrieveIfUserEnteredDecoupledTan() called for $tanChallenge")
    }

    protected open fun handleEnterTanResult(enteredTanResult: EnterTanResult, tanResponse: TanResponse, response: BankResponse,
                                            dialogContext: DialogContext, callback: (BankResponse) -> Unit) {

        if (enteredTanResult.changeTanMethodTo != null) {
            handleUserAsksToChangeTanMethodAndResendLastMessage(enteredTanResult.changeTanMethodTo, dialogContext, callback)
        }
        else if (enteredTanResult.changeTanMediumTo is TanGeneratorTanMedium) {
            handleUserAsksToChangeTanMediumAndResendLastMessage(enteredTanResult.changeTanMediumTo, dialogContext,
                enteredTanResult.changeTanMediumResultCallback, callback)
        }
        else if (enteredTanResult.enteredTan == null) {
            // i tried to send a HKTAN with cancelJob = true but then i saw there are no tan methods that support cancellation (at least not at my bank)
            // but it's not required anyway, tan times out after some time. Simply don't respond anything and close dialog
            response.tanRequiredButUserDidNotEnterOne = true

            callback(response)
        }
        else {
            sendTanToBank(enteredTanResult.enteredTan, tanResponse, dialogContext, callback)
        }
    }

    protected open fun sendTanToBank(enteredTan: String, tanResponse: TanResponse, dialogContext: DialogContext, callback: (BankResponse) -> Unit) {

        val message = messageBuilder.createSendEnteredTanMessage(enteredTan, tanResponse, dialogContext)

        getAndHandleResponseForMessage(message, dialogContext, callback)
    }

    protected open fun handleUserAsksToChangeTanMethodAndResendLastMessage(changeTanMethodTo: TanMethod, dialogContext: DialogContext, callback: (BankResponse) -> Unit) {

        dialogContext.bank.selectedTanMethod = changeTanMethodTo


        val lastCreatedMessage = dialogContext.currentMessage

        lastCreatedMessage?.let { closeDialog(dialogContext) }

        resendMessageInNewDialog(lastCreatedMessage, dialogContext, callback)
    }

    protected open fun handleUserAsksToChangeTanMediumAndResendLastMessage(changeTanMediumTo: TanGeneratorTanMedium,
                                                                           dialogContext: DialogContext,
                                                                           changeTanMediumResultCallback: ((FinTsClientResponse) -> Unit)?,
                                                                           callback: (BankResponse) -> Unit) {

        val lastCreatedMessage = dialogContext.currentMessage

        lastCreatedMessage?.let { closeDialog(dialogContext) }


        changeTanMedium(changeTanMediumTo, dialogContext.bank) { changeTanMediumResponse ->
            changeTanMediumResultCallback?.invoke(FinTsClientResponse(changeTanMediumResponse))

            if (changeTanMediumResponse.successful == false || lastCreatedMessage == null) {
                callback(changeTanMediumResponse)
            }
            else {
                resendMessageInNewDialog(lastCreatedMessage, dialogContext, callback)
            }
        }
    }


    protected open fun resendMessageInNewDialog(lastCreatedMessage: MessageBuilderResult?, previousDialogContext: DialogContext, callback: (BankResponse) -> Unit) {

        if (lastCreatedMessage != null) { // do not use previousDialogContext.currentMessage as this may is previous dialog's dialog close message
            val newDialogContext = DialogContext(previousDialogContext.bank, previousDialogContext.product, chunkedResponseHandler = previousDialogContext.chunkedResponseHandler)

            initDialogWithStrongCustomerAuthentication(newDialogContext) { initDialogResponse ->
                if (initDialogResponse.successful == false) {
                    callback(initDialogResponse)
                }
                else {
                    val newMessage = messageBuilder.rebuildMessage(lastCreatedMessage, newDialogContext)

                    getAndHandleResponseForMessage(newMessage, newDialogContext) { response ->
                        closeDialog(newDialogContext)

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


    protected open fun sendMessageAndHandleResponse(bank: BankData, segmentForNonStrongCustomerAuthenticationTwoStepTanProcess: CustomerSegmentId? = null,
                                                    closeDialog: Boolean = true, createMessage: (DialogContext) -> MessageBuilderResult, callback: (BankResponse) -> Unit) {

        val dialogContext = DialogContext(bank, product, closeDialog)

        if (segmentForNonStrongCustomerAuthenticationTwoStepTanProcess == null) {
            initDialogWithStrongCustomerAuthentication(dialogContext) { initDialogResponse ->
                sendMessageAndHandleResponseAfterDialogInitialization(dialogContext, initDialogResponse, createMessage, callback)
            }
        }
        else {
            initDialogMessageWithoutStrongCustomerAuthenticationAfterSuccessfulChecks(dialogContext, segmentForNonStrongCustomerAuthenticationTwoStepTanProcess) { initDialogResponse ->
                sendMessageAndHandleResponseAfterDialogInitialization(dialogContext, initDialogResponse, createMessage, callback)
            }
        }
    }

    private fun sendMessageAndHandleResponseAfterDialogInitialization(dialogContext: DialogContext, initDialogResponse: BankResponse, createMessage: (DialogContext) -> MessageBuilderResult, callback: (BankResponse) -> Unit) {

        if (initDialogResponse.successful == false) {
            callback(initDialogResponse)
        }
        else {
            val message = createMessage(dialogContext)

            getAndHandleResponseForMessage(message, dialogContext) { response ->
                closeDialog(dialogContext)

                callback(response)
            }
        }
    }

    protected open fun initDialogWithStrongCustomerAuthentication(dialogContext: DialogContext, callback: (BankResponse) -> Unit) {

        // we first need to retrieve supported tan methods and jobs before we can do anything
        ensureBasicBankDataRetrieved(dialogContext.bank) { retrieveBasicBankDataResponse ->
            if (retrieveBasicBankDataResponse.successful == false) {
                callback(retrieveBasicBankDataResponse)
            }
            else {
                // as in the next step we have to supply user's tan method, ensure user selected his or her
                ensureTanMethodIsSelected(dialogContext.bank) { tanMethodSelectedResponse ->
                    if (tanMethodSelectedResponse.successful == false) {
                        callback(tanMethodSelectedResponse)
                    }
                    else {
                        initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks(dialogContext, callback)
                    }
                }
            }
        }
    }

    protected open fun initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks(dialogContext: DialogContext, callback: (BankResponse) -> Unit) {

        val message = messageBuilder.createInitDialogMessage(dialogContext)

        getAndHandleResponseForMessage(message, dialogContext, callback)
    }

    protected open fun initDialogMessageWithoutStrongCustomerAuthenticationAfterSuccessfulChecks(dialogContext: DialogContext, segmentIdForTwoStepTanProcess: CustomerSegmentId?,
                                                                                                 callback: (BankResponse) -> Unit) {

        val message = messageBuilder.createInitDialogMessageWithoutStrongCustomerAuthentication(dialogContext, segmentIdForTwoStepTanProcess)

        getAndHandleResponseForMessage(message, dialogContext, callback)
    }

    protected open fun closeDialog(dialogContext: DialogContext) {

        // bank already closed dialog -> there's no need to send dialog end message
        if (dialogContext.closeDialog == false || dialogContext.didBankCloseDialog) {
            return
        }

        val dialogEndRequestBody = messageBuilder.createDialogEndMessage(dialogContext)

        fireAndForgetMessage(dialogEndRequestBody, dialogContext)
    }


    protected open fun ensureBasicBankDataRetrieved(bank: BankData, callback: (BankResponse) -> Unit) {
        if (bank.tanMethodsSupportedByBank.isEmpty() || bank.supportedJobs.isEmpty()) {
            retrieveBasicDataLikeUsersTanMethods(bank) { getBankInfoResponse ->
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

    protected open fun ensureTanMethodIsSelected(bank: BankData, callback: (BankResponse) -> Unit) {
        if (bank.isTanMethodSelected == false) {
            if (bank.tanMethodsAvailableForUser.isEmpty()) {
                retrieveBasicDataLikeUsersTanMethods(bank) { retrieveBasicDataResponse ->
                    callback(retrieveBasicDataResponse)
                }
            }
            else {
                getUsersTanMethod(bank) {
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

    open fun getUsersTanMethod(bank: BankData, preferredTanMethods: List<TanMethodType>? = null, done: (Boolean) -> Unit) {
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
            callback.askUserForTanMethod(bank.tanMethodsAvailableForUser, suggestedTanMethod) { selectedTanMethod ->
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

    protected open fun updateBankAndCustomerDataIfResponseSuccessful(dialogContext: DialogContext, response: BankResponse) {
        if (response.successful) {
            updateBankAndCustomerData(dialogContext.bank, response)
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