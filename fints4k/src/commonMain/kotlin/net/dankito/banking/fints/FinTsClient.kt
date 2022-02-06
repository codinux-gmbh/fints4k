package net.dankito.banking.fints

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dankito.banking.fints.callback.FinTsClientCallback
import net.dankito.banking.fints.log.MessageLogCollector
import net.dankito.banking.fints.messages.MessageBuilder
import net.dankito.banking.fints.messages.MessageBuilderResult
import net.dankito.banking.fints.messages.datenelemente.implementierte.Dialogsprache
import net.dankito.banking.fints.messages.datenelemente.implementierte.KundensystemStatusWerte
import net.dankito.banking.fints.messages.datenelemente.implementierte.signatur.Sicherheitsfunktion
import net.dankito.banking.fints.messages.datenelemente.implementierte.signatur.VersionDesSicherheitsverfahrens
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.*
import net.dankito.banking.fints.messages.segmente.id.CustomerSegmentId
import net.dankito.banking.fints.messages.segmente.id.ISegmentId
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.InstituteSegmentId
import net.dankito.banking.fints.response.BankResponse
import net.dankito.banking.fints.response.ResponseParser
import net.dankito.banking.fints.response.client.*
import net.dankito.banking.fints.response.segments.*
import net.dankito.banking.fints.tan.FlickerCodeDecoder
import net.dankito.banking.fints.tan.TanImageDecoder
import net.dankito.banking.fints.transactions.IAccountTransactionsParser
import net.dankito.banking.fints.transactions.Mt940AccountTransactionsParser
import net.dankito.banking.fints.util.IBase64Service
import net.dankito.banking.fints.util.PureKotlinBase64Service
import net.dankito.utils.multiplatform.log.Logger
import net.dankito.utils.multiplatform.log.LoggerFactory
import net.dankito.banking.fints.webclient.IWebClient
import net.dankito.banking.fints.webclient.KtorWebClient
import net.dankito.banking.fints.webclient.WebClientResponse
import net.dankito.utils.multiplatform.Date
import net.dankito.utils.multiplatform.getInnerExceptionMessage


open class FinTsClient(
    protected val callback: FinTsClientCallback,
    protected val webClient: IWebClient = KtorWebClient(),
    protected val base64Service: IBase64Service = PureKotlinBase64Service(),
    protected val messageBuilder: MessageBuilder = MessageBuilder(),
    protected val responseParser: ResponseParser = ResponseParser(),
    protected val mt940Parser: IAccountTransactionsParser = Mt940AccountTransactionsParser(),
    protected val messageLogCollector: MessageLogCollector = MessageLogCollector(),
    protected val product: ProductData = ProductData("15E53C26816138699C7B6A3E8", "1.0.0") // TODO: get version dynamically
) {

    companion object {
        val SupportedAccountTypes = listOf(AccountType.Girokonto, AccountType.Festgeldkonto, AccountType.Kreditkartenkonto)

        const val OneDayMillis = 24 * 60 * 60 * 1000L
        const val NinetyDaysMillis = 90 * OneDayMillis


        private val log = LoggerFactory.getLogger(FinTsClient::class)
    }


    open val messageLogWithoutSensitiveData: List<MessageLogEntry>
            get() = messageLogCollector.messageLogWithoutSensitiveData

    protected open val messageLogAppender: IMessageLogAppender = object : IMessageLogAppender {

        override fun logError(message: String, e: Exception?, logger: Logger?, bank: BankData?) {
            messageLogCollector.logError(message, e, logger, bank)
        }

    }


    /**
     * Retrieves information about bank (e.g. supported HBCI versions, FinTS server address,
     * supported jobs, ...).
     *
     * On success [bank] parameter is updated afterwards.
     */
    open fun getAnonymousBankInfoAsync(bank: BankData, callback: (FinTsClientResponse) -> Unit) {

        GlobalScope.launch {
            getAnonymousBankInfo(bank, callback)
        }
    }

    /**
     * Retrieves information about bank (e.g. supported HBCI versions, FinTS server address,
     * supported jobs, ...).
     *
     * On success [bank] parameter is updated afterwards.
     */
    open fun getAnonymousBankInfo(bank: BankData, callback: (FinTsClientResponse) -> Unit) {
        getAnonymousBankInfoInternal(bank) { response ->
            callback(FinTsClientResponse(response))
        }
    }

    protected open fun getAnonymousBankInfoInternal(bank: BankData, callback: (BankResponse) -> Unit) {
        val dialogContext = DialogContext(bank, product)

        val message = messageBuilder.createAnonymousDialogInitMessage(dialogContext)

        getAndHandleResponseForMessage(message, dialogContext) { response ->
            if (response.successful) {
                updateBankData(bank, response)

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


    open fun getUsersTanMethods(bank: BankData, callback: (FinTsClientResponse) -> Unit) {
        getUsersTanMethodsInternal(bank, true) {
            callback(FinTsClientResponse(it))
        }
    }

    protected open fun getUsersTanMethodsInternal(bank: BankData, closeDialog: Boolean = false, callback: (BankResponse) -> Unit) {
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

            handleGetUsersTanMethodsResponse(response, dialogContext, callback)
        }
    }

    protected open fun handleGetUsersTanMethodsResponse(response: BankResponse, dialogContext: DialogContext, callback: (BankResponse) -> Unit) {
        val getUsersTanMethodsResponse = GetUserTanMethodsResponse(response)

        if (getUsersTanMethodsResponse.successful) { // TODO: really update data only on complete successfully response? as it may contain useful information anyway  // TODO: extract method for this code part
            updateBankData(dialogContext.bank, getUsersTanMethodsResponse)
            updateCustomerData(dialogContext.bank, getUsersTanMethodsResponse)
        }

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
        getAnonymousBankInfoInternal(bank) { anonymousBankInfoResponse ->
            if (anonymousBankInfoResponse.successful == false) {
                callback(anonymousBankInfoResponse)
            }
            else if (bank.tanMethodSupportedByBank.isEmpty()) { // should only be a theoretical error
                callback(BankResponse(true,
                    errorMessage = "Die TAN Verfahren der Bank konnten nicht ermittelt werden")) // TODO: translate
            }
            else {
                bank.tanMethodsAvailableForUser = bank.tanMethodSupportedByBank
                getUsersTanMethod(bank) { didSelectTanMethod ->
                    if (didSelectTanMethod) {
                        val dialogContext = DialogContext(bank, product)

                        initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks(dialogContext) { initDialogResponse ->
                            closeDialog(dialogContext)

                            callback(initDialogResponse)
                        }
                    }
                    else {
                        callback(BankResponse(false))
                    }
                }
            }
        }
    }


    protected open fun getAccounts(bank: BankData, callback: (BankResponse) -> Unit) {

        val dialogContext = DialogContext(bank, product, false)

        initDialogWithStrongCustomerAuthenticationAfterSuccessfulPreconditionChecks(dialogContext) { response ->
            closeDialog(dialogContext)

            if (response.successful) {
                updateBankData(bank, response)
                updateCustomerData(bank, response)
            }

            callback(response)
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
                updateBankData(bank, response)
                updateCustomerData(bank, response)

                closeDialog(dialogContext)
            }

            callback(FinTsClientResponse(response))
        }
    }


    open fun addAccountAsync(parameter: AddAccountParameter, callback: (AddAccountResponse) -> Unit) {
        val bank = parameter.bank

        /*      First dialog: Get user's basic data like BPD, customer system ID and her TAN methods     */

        getUsersTanMethodsInternal(bank) { newUserInfoResponse ->

            if (newUserInfoResponse.successful == false) { // bank parameter (FinTS server address, ...) already seem to be wrong
                callback(AddAccountResponse(newUserInfoResponse, bank))
                return@getUsersTanMethodsInternal
            }

            getUsersTanMethod(bank) { didSelectTanMethod ->

                if (didSelectTanMethod == false) {
                    callback(AddAccountResponse(BankResponse(false), bank))
                    return@getUsersTanMethod
                }

                /*      Second dialog: some banks require that in order to initialize a dialog with strong customer authorization TAN media is required       */

                if (isJobSupported(bank, CustomerSegmentId.TanMediaList)) {
                    getTanMediaList(bank, TanMedienArtVersion.Alle, TanMediumKlasse.AlleMedien) {
                        addAccountGetAccountsAndTransactions(parameter, bank, callback)
                    }
                } else {
                    addAccountGetAccountsAndTransactions(parameter, bank, callback)
                }
            }
        }
    }

    protected open fun addAccountGetAccountsAndTransactions(parameter: AddAccountParameter, bank: BankData,
                                                            callback: (AddAccountResponse) -> Unit) {

        /*      Third dialog: Now we can initialize our first dialog with strong customer authorization. Use it to get UPD and customer's accounts        */

        getAccounts(bank) { getAccountsResponse ->

            if (getAccountsResponse.successful == false) {
                callback(AddAccountResponse(getAccountsResponse, bank))
                return@getAccounts
            }

            /*      Fourth dialog (if requested): Try to retrieve account balances and transactions of last 90 days without TAN     */

            if (parameter.fetchBalanceAndTransactions) {
                addAccountGetAccountBalancesAndTransactions(bank, getAccountsResponse, callback)
            }
            else {
                addAccountDone(bank, getAccountsResponse, mapOf(), callback)
            }
        }
    }

    protected open fun addAccountGetAccountBalancesAndTransactions(bank: BankData, getAccountsResponse: BankResponse,
                                                                   callback: (AddAccountResponse) -> Unit) {

        val retrievedAccountData = bank.accounts.associateBy( { it }, { RetrievedAccountData.unsuccessful(it) } ).toMutableMap()

        val accountsSupportingRetrievingTransactions = bank.accounts.filter { it.supportsRetrievingBalance || it.supportsRetrievingAccountTransactions }
        val countAccountsSupportingRetrievingTransactions = accountsSupportingRetrievingTransactions.size
        var countRetrievedAccounts = 0

        if (countAccountsSupportingRetrievingTransactions == 0) {
            addAccountDone(bank, getAccountsResponse, retrievedAccountData, callback)
        }

        accountsSupportingRetrievingTransactions.forEach { account ->
            tryGetTransactionsOfLast90DaysWithoutTan(bank, account) { response ->
                retrievedAccountData.put(account, response.retrievedData.first())

                countRetrievedAccounts++
                if (countRetrievedAccounts == countAccountsSupportingRetrievingTransactions) {
                    addAccountDone(bank, getAccountsResponse, retrievedAccountData, callback)
                }
            }
        }
    }

    protected open fun addAccountDone(bank: BankData, getAccountsResponse: BankResponse,
                                      retrievedAccountData: Map<AccountData, RetrievedAccountData>,
                                      callback: (AddAccountResponse) -> Unit) {

        callback(AddAccountResponse(getAccountsResponse, bank, retrievedAccountData.values.toList()))
    }


    /**
     * Some banks support that according to PSD2 account transactions may be retrieved without
     * a TAN (= no strong customer authorization needed).
     *
     * Check if bank supports this.
     */
    open fun tryGetTransactionsOfLast90DaysWithoutTan(bank: BankData, account: AccountData, callback: (GetTransactionsResponse) -> Unit) {

        val ninetyDaysAgo = Date(Date.today.millisSinceEpoch - NinetyDaysMillis)

        getTransactionsAsync(GetTransactionsParameter(account, account.supportsRetrievingBalance, ninetyDaysAgo, abortIfTanIsRequired = true), bank) { response ->
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
                mayGetBalance(parameter, dialogContext) { balanceResponse ->
                    if (dialogContext.didBankCloseDialog) {
                        callback(GetTransactionsResponse(balanceResponse, RetrievedAccountData.unsuccessfulList(parameter.account)))
                    }
                    else {
                        getTransactionsAfterInitAndGetBalance(parameter, dialogContext, balanceResponse, callback)
                    }
                }
            }
        }
    }

    protected open fun getTransactionsAfterInitAndGetBalance(parameter: GetTransactionsParameter, dialogContext: DialogContext,
                                                             balanceResponse: BankResponse, callback: (GetTransactionsResponse) -> Unit) {
        var balance: Money? = balanceResponse.getFirstSegmentById<BalanceSegment>(InstituteSegmentId.Balance)?.let {
                Money(it.balance, it.currency)
            }
        val bookedTransactions = mutableSetOf<AccountTransaction>()
        val unbookedTransactions = mutableSetOf<Any>()

        val message = messageBuilder.createGetTransactionsMessage(parameter, dialogContext)

        var remainingMt940String = ""

        dialogContext.abortIfTanIsRequired = parameter.abortIfTanIsRequired

        dialogContext.chunkedResponseHandler = { response ->
            response.getFirstSegmentById<ReceivedAccountTransactions>(InstituteSegmentId.AccountTransactionsMt940)?.let { transactionsSegment ->
                val (chunkTransaction, remainder) = mt940Parser.parseTransactionsChunk(remainingMt940String + transactionsSegment.bookedTransactionsString, parameter.account)

                bookedTransactions.addAll(chunkTransaction)
                remainingMt940String = remainder

                parameter.retrievedChunkListener?.invoke(bookedTransactions)
            }

            response.getFirstSegmentById<ReceivedCreditCardTransactionsAndBalance>(InstituteSegmentId.CreditCardTransactions)?.let { transactionsSegment ->
                balance = Money(transactionsSegment.balance.amount, transactionsSegment.balance.currency ?: "EUR")
                bookedTransactions.addAll(transactionsSegment.transactions.map { AccountTransaction(parameter.account, it.amount, it.description, it.bookingDate, it.transactionDescriptionBase ?: "", null, null, "", it.valueDate) })
            }
        }

        getAndHandleResponseForMessage(message, dialogContext) { response ->
            closeDialog(dialogContext)

            val successful = response.successful && (parameter.alsoRetrieveBalance == false || balance != null)
            val fromDate = parameter.fromDate
                ?: parameter.account.countDaysForWhichTransactionsAreKept?.let { Date(Date.today.millisSinceEpoch - it * OneDayMillis) }
                ?: bookedTransactions.map { it.valueDate }.sortedBy { it.millisSinceEpoch }.firstOrNull()
            val retrievedData = RetrievedAccountData(parameter.account, successful, balance, bookedTransactions, unbookedTransactions, fromDate, parameter.toDate ?: Date.today)

            callback(GetTransactionsResponse(response, listOf(retrievedData),
                if (parameter.maxCountEntries != null) parameter.isSettingMaxCountEntriesAllowedByBank else null
            ))
        }
    }

    protected open fun mayGetBalance(parameter: GetTransactionsParameter, dialogContext: DialogContext, callback: (BankResponse) -> Unit) {
        if (parameter.alsoRetrieveBalance && parameter.account.supportsRetrievingBalance) {
            val message = messageBuilder.createGetBalanceMessage(parameter.account, dialogContext)

            getAndHandleResponseForMessage(message, dialogContext) { response ->
                callback(response)
            }
        }
        else {
            callback(BankResponse(false, errorMessage = "Either not requested to get balance or account does not support retrieving balance. " +
                    "Should retrieve balance = ${parameter.alsoRetrieveBalance}, account supports retrieving balance = ${parameter.account.supportsRetrievingBalance}."))
        }
    }


    open fun getTanMediaListAsync(bank: BankData, tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle,
                                  tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien,
                                  callback: (GetTanMediaListResponse) -> Unit) {

        GlobalScope.launch {
            getTanMediaList(bank, tanMediaKind, tanMediumClass, callback)
        }
    }

    open fun getTanMediaList(bank: BankData, tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle,
                             tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien, callback: (GetTanMediaListResponse) -> Unit) {

        sendMessageAndHandleResponse(bank, CustomerSegmentId.TanMediaList, false, { dialogContext ->
            messageBuilder.createGetTanMediaListMessage(dialogContext, tanMediaKind, tanMediumClass)
        }) { response ->
            handleGetTanMediaListResponse(response, bank, callback)
        }
    }

    private fun handleGetTanMediaListResponse(response: BankResponse, bank: BankData, callback: (GetTanMediaListResponse) -> Unit) {
        // TAN media list (= TAN generator list) is only returned for users with chipTAN TAN methods
        val tanMediaList = if (response.successful == false) null
        else response.getFirstSegmentById<TanMediaList>(InstituteSegmentId.TanMediaList)

        tanMediaList?.let {
            bank.tanMedia = it.tanMedia
        }

        callback(GetTanMediaListResponse(response, tanMediaList))
    }


    open fun changeTanMedium(newActiveTanMedium: TanGeneratorTanMedium, bank: BankData, callback: (FinTsClientResponse) -> Unit) {
        changeTanMediumInternal(newActiveTanMedium, bank) { response ->
            callback(FinTsClientResponse(response))
        }
    }

    protected open fun changeTanMediumInternal(newActiveTanMedium: TanGeneratorTanMedium, bank: BankData, callback: (BankResponse) -> Unit) {

        if (bank.changeTanMediumParameters?.enteringAtcAndTanRequired == true) {
            this.callback.enterTanGeneratorAtc(bank, newActiveTanMedium) { enteredAtc ->
                if (enteredAtc.hasAtcBeenEntered == false) {
                    val message = "Bank requires to enter ATC and TAN in order to change TAN medium." // TODO: translate
                    callback(BankResponse(false, errorMessage = message))
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
            messageBuilder.createBankTransferMessage(bankTransferData, account, dialogContext)
        }) { response ->
            callback(FinTsClientResponse(response))
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

        getAndHandleResponseForMessage(message, dialogContext) { response ->

            if (response.successful) {
                updateBankData(dialogContext.bank, response)
                updateCustomerData(dialogContext.bank, response)
            }

            callback(response)
        }
    }

    protected open fun initDialogMessageWithoutStrongCustomerAuthenticationAfterSuccessfulChecks(dialogContext: DialogContext, segmentIdForTwoStepTanProcess: CustomerSegmentId?,
                                                                                                 callback: (BankResponse) -> Unit) {

        val message = messageBuilder.createInitDialogMessageWithoutStrongCustomerAuthentication(dialogContext, segmentIdForTwoStepTanProcess)

        getAndHandleResponseForMessage(message, dialogContext) { response ->
            if (response.successful) {
                updateBankData(dialogContext.bank, response)
                updateCustomerData(dialogContext.bank, response)
            }

            callback(response)
        }
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
        if (bank.tanMethodSupportedByBank.isEmpty() || bank.supportedJobs.isEmpty()) {
            getUsersTanMethodsInternal(bank) { getBankInfoResponse ->
                if (getBankInfoResponse.successful == false || bank.tanMethodSupportedByBank.isEmpty()
                    || bank.supportedJobs.isEmpty()) {

                    callback(BankResponse(false, errorMessage =
                    "Could not retrieve basic bank data like supported tan methods or supported jobs")) // TODO: translate // TODO: add as messageToShowToUser
                }
                else {
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
                getUsersTanMethodsInternal(bank) {
                    if (bank.tanMethodsAvailableForUser.isEmpty()) { // could not retrieve supported tan methods for user
                        callback(BankResponse(false, noTanMethodSelected = true))
                    }
                    else {
                        getUsersTanMethod(bank) {
                            callback(BankResponse(bank.isTanMethodSelected, noTanMethodSelected = !!!bank.isTanMethodSelected))
                        }
                    }
                }
            }
            else {
                getUsersTanMethod(bank) {
                    callback(BankResponse(bank.isTanMethodSelected, noTanMethodSelected = !!!bank.isTanMethodSelected))
                }
            }
        }
        else {
            callback(BankResponse(bank.isTanMethodSelected, noTanMethodSelected = !!!bank.isTanMethodSelected))
        }
    }

    protected open fun getUsersTanMethod(bank: BankData, done: (Boolean) -> Unit) {
        if (bank.tanMethodsAvailableForUser.size == 1) { // user has only one TAN method -> set it and we're done
            bank.selectedTanMethod = bank.tanMethodsAvailableForUser.first()
            done(true)
        }
        else {
            // we know user's supported tan methods, now ask user which one to select
            callback.askUserForTanMethod(bank.tanMethodsAvailableForUser, selectSuggestedTanMethod(bank)) { selectedTanMethod ->
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

    protected open fun selectSuggestedTanMethod(bank: BankData): TanMethod? {
        return bank.tanMethodsAvailableForUser.firstOrNull { it.type != TanMethodType.ChipTanUsb && it.type != TanMethodType.SmsTan && it.type != TanMethodType.ChipTanManuell }
            ?: bank.tanMethodsAvailableForUser.firstOrNull { it.type != TanMethodType.ChipTanUsb && it.type != TanMethodType.SmsTan }
            ?: bank.tanMethodsAvailableForUser.firstOrNull { it.type != TanMethodType.ChipTanUsb }
            ?: bank.tanMethodsAvailableForUser.firstOrNull()
    }


    protected open fun getAndHandleResponseForMessage(message: MessageBuilderResult, dialogContext: DialogContext, callback: (BankResponse) -> Unit) {
        if (message.createdMessage == null) {
            callback(BankResponse(false, messageCreationError = message))
        }
        else {
            getAndHandleResponseForMessage(message.createdMessage, dialogContext) { response ->
                handleMayRequiresTan(response, dialogContext) { handledResponse ->
                    // if there's a Aufsetzpunkt (continuationId) set, then response is not complete yet, there's more information to fetch by sending this Aufsetzpunkt
                    handledResponse.aufsetzpunkt?.let { continuationId ->
                        if (handledResponse.followUpResponse == null) { // for re-sent messages followUpResponse is already set and dialog already closed -> would be overwritten with an error response that dialog is closed
                            if (message.isSendEnteredTanMessage() == false) { // for sending TAN no follow up message can be created -> filter out, otherwise chunkedResponseHandler would get called twice for same response
                                dialogContext.chunkedResponseHandler?.invoke(handledResponse)
                            }

                            getFollowUpMessageForContinuationId(handledResponse, continuationId, message, dialogContext) { followUpResponse ->
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

    protected open fun fireAndForgetMessage(message: MessageBuilderResult, dialogContext: DialogContext) {
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


    protected open fun getFollowUpMessageForContinuationId(response: BankResponse, continuationId: String, message: MessageBuilderResult,
                                                           dialogContext: DialogContext, callback: (BankResponse?) -> Unit) {

        messageBuilder.rebuildMessageWithContinuationId(message, continuationId, dialogContext)?.let { followUpMessage ->
            getAndHandleResponseForMessage(followUpMessage, dialogContext, callback)
        }
        ?: run { callback(null) }
    }


    protected open fun addMessageLog(message: String, type: MessageLogEntryType, dialogContext: DialogContext) {
        messageLogCollector.addMessageLog(message, type, dialogContext.bank)
    }

    protected open fun logError(message: String, dialogContext: DialogContext, e: Exception?) {
        messageLogAppender.logError(message, e, log, dialogContext.bank)
    }


    protected open fun handleMayRequiresTan(response: BankResponse, dialogContext: DialogContext, callback: (BankResponse) -> Unit) { // TODO: use response from DialogContext

        if (response.isStrongAuthenticationRequired) {
            if (dialogContext.abortIfTanIsRequired) {
                response.tanRequiredButWeWereToldToAbortIfSo = true

                callback(response)
                return
            }
            else if (response.tanResponse != null) {
                response.tanResponse?.let { tanResponse ->
                    handleEnteringTanRequired(tanResponse, response, dialogContext, callback)
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

    protected open fun handleEnteringTanRequired(tanResponse: TanResponse, response: BankResponse, dialogContext: DialogContext, callback: (BankResponse) -> Unit) {
        val bank = dialogContext.bank // TODO: copy required data to TanChallenge
        val tanChallenge = createTanChallenge(tanResponse, bank)

        this.callback.enterTan(bank, tanChallenge)  { enteredTanResult ->
            handleEnterTanResult(enteredTanResult, tanResponse, response, dialogContext, callback)
        }
    }

    protected open fun createTanChallenge(tanResponse: TanResponse, bank: BankData): TanChallenge {
        // TODO: is this true for all tan methods?
        val messageToShowToUser = tanResponse.challenge ?: ""
        val challenge = tanResponse.challengeHHD_UC ?: ""
        val tanMethod = bank.selectedTanMethod

        return when (tanMethod.type) {
            TanMethodType.ChipTanFlickercode ->
                FlickerCodeTanChallenge(FlickerCodeDecoder().decodeChallenge(challenge, tanMethod.hhdVersion ?: HHDVersion.HHD_1_4), // HHD 1.4 is currently the most used version
                    messageToShowToUser, challenge, tanMethod, tanResponse.tanMediaIdentifier)

            TanMethodType.ChipTanQrCode, TanMethodType.ChipTanPhotoTanMatrixCode,
            TanMethodType.QrCode, TanMethodType.photoTan ->
                ImageTanChallenge(TanImageDecoder().decodeChallenge(challenge), messageToShowToUser, challenge, tanMethod, tanResponse.tanMediaIdentifier)

            else -> TanChallenge(messageToShowToUser, challenge, tanMethod, tanResponse.tanMediaIdentifier)
        }
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


        changeTanMediumInternal(changeTanMediumTo, dialogContext.bank) { changeTanMediumResponse ->
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
            callback(BankResponse(false, errorMessage = errorMessage)) // should never come to this
        }
    }


    protected open fun updateBankData(bank: BankData, response: BankResponse) {
        response.getFirstSegmentById<BankParameters>(InstituteSegmentId.BankParameters)?.let { bankParameters ->
            bank.bpdVersion = bankParameters.bpdVersion
            bank.bankCode = bankParameters.bankCode
            bank.countryCode = bankParameters.bankCountryCode
            bank.countMaxJobsPerMessage = bankParameters.countMaxJobsPerMessage
            bank.supportedHbciVersions = bankParameters.supportedHbciVersions
            bank.supportedLanguages = bankParameters.supportedLanguages

//            bank.bic = bankParameters. // TODO: where's the BIC?
        }

        response.getFirstSegmentById<PinInfo>(InstituteSegmentId.PinInfo)?.let { pinInfo ->
            bank.pinInfo = pinInfo
        }

        response.getFirstSegmentById<TanInfo>(InstituteSegmentId.TanInfo)?.let { tanInfo ->
            bank.tanMethodSupportedByBank = mapToTanMethods(tanInfo)
        }

        response.getFirstSegmentById<CommunicationInfo>(InstituteSegmentId.CommunicationInfo)?.let { communicationInfo ->
            communicationInfo.parameters.firstOrNull { it.type == Kommunikationsdienst.Https }?.address?.let { address ->
                bank.finTs3ServerAddress = if (address.startsWith("https://", true)) address else "https://$address"
            }
        }

        response.getFirstSegmentById<SepaAccountInfo>(InstituteSegmentId.SepaAccountInfo)?.let { sepaAccountInfo ->
            sepaAccountInfo.account.bic?.let {
                bank.bic = it // TODO: really set BIC on bank then?
            }
        }

        response.getFirstSegmentById<SepaAccountInfo>(InstituteSegmentId.SepaAccountInfo)?.let { sepaAccountInfo ->
            sepaAccountInfo.account.bic?.let {
                bank.bic = it // TODO: really set BIC on bank then?
            }
        }

        response.getFirstSegmentById<ChangeTanMediaParameters>(InstituteSegmentId.ChangeTanMediaParameters)?.let { parameters ->
            bank.changeTanMediumParameters = parameters
        }

        if (response.supportedJobs.isNotEmpty()) {
            bank.supportedJobs = response.supportedJobs
        }
    }

    protected open fun updateCustomerData(bank: BankData, response: BankResponse) {
        response.getFirstSegmentById<BankParameters>(InstituteSegmentId.BankParameters)?.let { bankParameters ->
            // TODO: ask user if there is more than one supported language? But it seems that almost all banks only support German.
            if (bank.selectedLanguage == Dialogsprache.Default && bankParameters.supportedLanguages.isNotEmpty()) {
                bank.selectedLanguage = bankParameters.supportedLanguages.first()
            }
        }

        response.getFirstSegmentById<ReceivedSynchronization>(InstituteSegmentId.Synchronization)?.let { synchronization ->
            synchronization.customerSystemId?.let {
                bank.customerSystemId = it

                bank.customerSystemStatus = KundensystemStatusWerte.Benoetigt // TODO: didn't find out for sure yet, but i think i read somewhere, that this has to be set when customerSystemId is set
            }
        }

        response.getSegmentsById<AccountInfo>(InstituteSegmentId.AccountInfo).forEach { accountInfo ->
            var accountHolderName = accountInfo.accountHolderName1
            accountInfo.accountHolderName2?.let {
                accountHolderName += it // TODO: add a whitespace in between?
            }
            bank.customerName = accountHolderName

            findExistingAccount(bank, accountInfo)?.let { account ->
                // TODO: update AccountData. But can this ever happen that an account changes?
            }
            ?: run {
                val newAccount = AccountData(accountInfo.accountIdentifier, accountInfo.subAccountAttribute,
                    accountInfo.bankCountryCode, accountInfo.bankCode, accountInfo.iban, accountInfo.customerId,
                    mapAccountType(accountInfo), accountInfo.currency, accountHolderName, accountInfo.productName,
                    accountInfo.accountLimit, accountInfo.allowedJobNames)

                bank.supportedJobs.filterIsInstance<RetrieveAccountTransactionsParameters>().sortedByDescending { it.segmentVersion }.firstOrNull { newAccount.allowedJobNames.contains(it.jobName) }?.let { transactionsParameters ->
                    newAccount.countDaysForWhichTransactionsAreKept = transactionsParameters.countDaysForWhichTransactionsAreKept
                }

                bank.addAccount(newAccount)
            }

            // TODO: may also make use of other info
        }

        response.getFirstSegmentById<SepaAccountInfo>(InstituteSegmentId.SepaAccountInfo)?.let { sepaAccountInfo ->
            // TODO: make use of information
            sepaAccountInfo.account.iban?.let {

            }
        }

        response.getFirstSegmentById<UserParameters>(InstituteSegmentId.UserParameters)?.let { userParameters ->
            bank.updVersion = userParameters.updVersion

            if (bank.customerName.isEmpty()) {
                userParameters.username?.let {
                    bank.customerName = it
                }
            }

            // TODO: may also make use of other info
        }

        response.getFirstSegmentById<CommunicationInfo>(InstituteSegmentId.CommunicationInfo)?.let { communicationInfo ->
            if (bank.selectedLanguage != communicationInfo.defaultLanguage) {
                bank.selectedLanguage = communicationInfo.defaultLanguage
            }
        }

        val supportedJobs = response.supportedJobs
        if (supportedJobs.isNotEmpty()) { // if allowedJobsForBank is empty than bank didn't send any allowed job
            for (account in bank.accounts) {
                setAllowedJobsForAccount(bank, account, supportedJobs)
            }
        }
        else if (bank.supportedJobs.isNotEmpty()) {
            for (account in bank.accounts) {
                if (account.allowedJobs.isEmpty()) {
                    setAllowedJobsForAccount(bank, account, bank.supportedJobs)
                }
            }
        }

        if (response.supportedTanMethodsForUser.isNotEmpty()) {
            bank.tanMethodsAvailableForUser = response.supportedTanMethodsForUser.mapNotNull { findTanMethod(it, bank) }

            if (bank.tanMethodsAvailableForUser.firstOrNull { it.securityFunction == bank.selectedTanMethod.securityFunction } == null) { // supportedTanMethods don't contain selectedTanMethod anymore
                bank.resetSelectedTanMethod()
            }
        }
    }

    protected open fun findTanMethod(securityFunction: Sicherheitsfunktion, bank: BankData): TanMethod? {
        return bank.tanMethodSupportedByBank.firstOrNull { it.securityFunction == securityFunction }
    }

    protected open fun setAllowedJobsForAccount(bank: BankData, account: AccountData, supportedJobs: List<JobParameters>) {
        val allowedJobsForAccount = mutableListOf<JobParameters>()

        for (job in supportedJobs) {
            if (isJobSupported(account, job)) {
                allowedJobsForAccount.add(job)
            }
        }

        account.allowedJobs = allowedJobsForAccount

        account.setSupportsFeature(AccountFeature.RetrieveAccountTransactions, messageBuilder.supportsGetTransactions(account))
        account.setSupportsFeature(AccountFeature.RetrieveBalance, messageBuilder.supportsGetBalance(account))
        account.setSupportsFeature(AccountFeature.TransferMoney, messageBuilder.supportsBankTransfer(bank, account))
        account.setSupportsFeature(AccountFeature.RealTimeTransfer, messageBuilder.supportsSepaRealTimeTransfer(bank, account))
    }

    protected open fun mapToTanMethods(tanInfo: TanInfo): List<TanMethod> {
        return tanInfo.tanProcedureParameters.methodParameters.mapNotNull {
            mapToTanMethod(it)
        }
    }

    protected open fun mapToTanMethod(parameters: TanMethodParameters): TanMethod? {
        val methodName = parameters.methodName

        // we filter out iTAN and Einschritt-Verfahren as they are not permitted anymore according to PSD2
        if (methodName.toLowerCase() == "itan") {
            return null
        }

        return TanMethod(methodName, parameters.securityFunction,
            mapToTanMethodType(parameters) ?: TanMethodType.EnterTan, mapHhdVersion(parameters),
            parameters.maxTanInputLength, parameters.allowedTanFormat,
            parameters.nameOfTanMediumRequired == BezeichnungDesTanMediumsErforderlich.BezeichnungDesTanMediumsMussAngegebenWerden)
    }

    protected open fun mapToTanMethodType(parameters: TanMethodParameters): TanMethodType? {
        val name = parameters.methodName.toLowerCase()

        return when {
            // names are like 'chipTAN (comfort) manuell', 'Smart(-)TAN plus (manuell)' and
            // technical identification is 'HHD'. Exception:  there's one that states itself as 'chipTAN (Manuell)'
            // but its ZkaTanMethod is set to 'HHDOPT1' -> handle ChipTanManuell before ChipTanFlickercode
            parameters.zkaTanMethod == ZkaTanMethod.HHD || name.contains("manuell") ->
                TanMethodType.ChipTanManuell

            // names are like 'chipTAN optisch/comfort', 'SmartTAN (plus) optic/USB', 'chipTAN (Flicker)' and
            // technical identification is 'HHDOPT1'
            parameters.zkaTanMethod == ZkaTanMethod.HHDOPT1 ||
                    tanMethodNameContains(name, "optisch", "optic", "comfort", "flicker") ->
                TanMethodType.ChipTanFlickercode

            // 'Smart-TAN plus optisch / USB' seems to be a Flickertan method -> test for 'optisch' first
            name.contains("usb") -> TanMethodType.ChipTanUsb

            // QRTAN+ from 1822 direct has nothing to do with chipTAN QR.
            name.contains("qr") -> {
                if (tanMethodNameContains(name, "chipTAN", "Smart")) TanMethodType.ChipTanQrCode
                else TanMethodType.QrCode
            }

            // photoTAN from Commerzbank (comdirect), Deutsche Bank, norisbank has nothing to do with chipTAN photo
            name.contains("photo") -> {
                // e.g. 'Smart-TAN photo' / description 'Challenge'
                if (tanMethodNameContains(name, "chipTAN", "Smart")) TanMethodType.ChipTanPhotoTanMatrixCode
                // e.g. 'photoTAN-Verfahren', description 'Freigabe durch photoTAN'
                else TanMethodType.photoTan
            }

            tanMethodNameContains(name, "SMS", "mobile", "mTAN") -> TanMethodType.SmsTan

            // 'flateXSecure' identifies itself as 'PPTAN' instead of 'AppTAN'
            // 'activeTAN-Verfahren' can actually be used either with an app or a reader; it's like chipTAN QR but without a chip card
            tanMethodNameContains(name, "push", "app", "BestSign", "SecureGo", "TAN2go", "activeTAN", "easyTAN", "SecurePlus", "TAN+")
                    || technicalTanMethodIdentificationContains(parameters, "SECURESIGN", "PPTAN") ->
                TanMethodType.AppTan

            // we filter out iTAN and Einschritt-Verfahren as they are not permitted anymore according to PSD2
            else -> null
        }
    }

    protected open fun mapHhdVersion(parameters: TanMethodParameters): HHDVersion? {
        return when {
            technicalTanMethodIdentificationContains(parameters, "HHD1.4") -> HHDVersion.HHD_1_4
            technicalTanMethodIdentificationContains(parameters, "HHD1.3") -> HHDVersion.HHD_1_3
            parameters.versionZkaTanMethod?.contains("1.4") == true -> HHDVersion.HHD_1_4
            parameters.versionZkaTanMethod?.contains("1.3") == true -> HHDVersion.HHD_1_4
            else -> null
        }
    }

    protected open fun tanMethodNameContains(name: String, vararg namesToTest: String): Boolean {
        namesToTest.forEach { nameToTest ->
            if (name.contains(nameToTest.toLowerCase())) {
                return true
            }
        }

        return false
    }

    protected open fun technicalTanMethodIdentificationContains(parameters: TanMethodParameters, vararg valuesToTest: String): Boolean {
        valuesToTest.forEach { valueToTest ->
            if (parameters.technicalTanMethodIdentification.contains(valueToTest, true)) {
                return true
            }
        }

        return false
    }


    protected open fun isJobSupported(bank: BankData, segmentId: ISegmentId): Boolean {
        return bank.supportedJobs.map { it.jobName }.contains(segmentId.id)
    }

    protected open fun isJobSupported(account: AccountData, supportedJob: JobParameters): Boolean {
        for (allowedJobName in account.allowedJobNames) {
            if (allowedJobName == supportedJob.jobName) {
                return true
            }
        }

        return false
    }

    protected open fun findExistingAccount(bank: BankData, accountInfo: AccountInfo): AccountData? {
        bank.accounts.forEach { account ->
            if (account.accountIdentifier == accountInfo.accountIdentifier
                && account.productName == accountInfo.productName) {

                return account
            }
        }

        return null
    }

    protected open fun mapAccountType(accountInfo: AccountInfo): AccountType? {
        if (accountInfo.accountType == null || accountInfo.accountType == AccountType.Sonstige) {
            accountInfo.productName?.let { name ->
                // comdirect doesn't set account type field but names its bank accounts according to them like 'Girokonto', 'Tagesgeldkonto', ...
                return when {
                    name.contains("Girokonto", true) -> AccountType.Girokonto
                    name.contains("Festgeld", true) -> AccountType.Festgeldkonto
                    name.contains("Tagesgeld", true) -> AccountType.Sparkonto // learnt something new today:  according to Wikipedia some direct banks offer a modern version of saving accounts as 'Tagesgeldkonto'
                    name.contains("Kreditkarte", true) -> AccountType.Kreditkartenkonto
                    else -> accountInfo.accountType
                }
            }
        }

        return accountInfo.accountType
    }

}