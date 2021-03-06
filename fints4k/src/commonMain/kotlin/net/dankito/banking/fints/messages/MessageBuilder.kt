package net.dankito.banking.fints.messages

import net.dankito.banking.fints.messages.datenelemente.implementierte.Aufsetzpunkt
import net.dankito.banking.fints.messages.datenelemente.implementierte.KundensystemID
import net.dankito.banking.fints.messages.datenelemente.implementierte.Synchronisierungsmodus
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanGeneratorTanMedium
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMedienArtVersion
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMediumKlasse
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanProcess
import net.dankito.banking.fints.messages.segmente.ISegmentNumberGenerator
import net.dankito.banking.fints.messages.segmente.Segment
import net.dankito.banking.fints.messages.segmente.SegmentNumberGenerator
import net.dankito.banking.fints.messages.segmente.Synchronisierung
import net.dankito.banking.fints.messages.segmente.id.CustomerSegmentId
import net.dankito.banking.fints.messages.segmente.id.ISegmentId
import net.dankito.banking.fints.messages.segmente.implementierte.*
import net.dankito.banking.fints.messages.segmente.implementierte.sepa.SepaBankTransferBase
import net.dankito.banking.fints.messages.segmente.implementierte.tan.TanGeneratorListeAnzeigen
import net.dankito.banking.fints.messages.segmente.implementierte.tan.TanGeneratorTanMediumAnOderUmmelden
import net.dankito.banking.fints.messages.segmente.implementierte.umsaetze.*
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.InstituteSegmentId
import net.dankito.banking.fints.response.segments.*
import net.dankito.banking.fints.util.FinTsUtils
import net.dankito.utils.multiplatform.Date
import kotlin.math.absoluteValue
import kotlin.random.Random


/**
 * Takes the Segments of they payload, may signs and encrypts them, calculates message size,
 * adds the message header and ending, and formats the whole message to string.
 */
open class MessageBuilder(protected val generator: ISegmentNumberGenerator = SegmentNumberGenerator(),
                          protected val utils: FinTsUtils = FinTsUtils()) {

    companion object {
        const val MessageHeaderMinLength = 28

        const val AddedSeparatorsLength = 3
    }


    /**
     * Um Kunden die M??glichkeit zu geben, sich anonym anzumelden, um sich bspw. ??ber die
     * angebotenen Gesch??ftsvorf??lle fremder Kreditinstitute (von denen sie keine BPD besitzen)
     * zu informieren bzw. nicht-signierungspflichtige Auftr??ge bei fremden Kreditinstituten
     * einreichen zu k??nnen, kann sich der Kunde anonym (als Gast) anmelden.
     *
     * Bei anonymen Dialogen werden Nachrichten weder signiert, noch k??nnen sie verschl??sselt und komprimiert werden.
     */
    /**
     * Auch im Rahmen einer Er??ffnung eines anonymen Dialogs muss ein Kun-
    denprodukt, das die starke Kundenauthentifizierung unterst??tzt, in die Dia-
    loginitialisierungsnachricht ein Segment HKTAN ab Segmentversion #6 ein-
    stellen. Auf diese Weise ist eine Signalisierung der SCA-F??higkeit m??glich
    und dem Kundensystem k??nnen in der Antwort bei Bedarf geeignete BPD
    ??bermittelt werden, wenn das Kreditinstitut dies unterst??tzt.
     (PinTan S. 35)
     */
    open fun createAnonymousDialogInitMessage(dialogContext: DialogContext): MessageBuilderResult {

        return createUnsignedMessageBuilderResult(dialogContext, listOf(
            IdentifikationsSegment(generator.resetSegmentNumber(1), dialogContext),
            Verarbeitungsvorbereitung(generator.getNextSegmentNumber(), dialogContext)
        ))
    }

    open fun createAnonymousDialogEndMessage(dialogContext: DialogContext): MessageBuilderResult {

        return createUnsignedMessageBuilderResult(dialogContext, listOf(
            Dialogende(generator.resetSegmentNumber(1), dialogContext)
        ))
    }


    open fun createInitDialogMessage(dialogContext: DialogContext): MessageBuilderResult {
        return createInitDialogMessage(dialogContext, null)
    }

    /**
     * Im Rahmen der PIN/TAN-Management-Gesch??ftsvorf??lle (vgl. Kapitel C.3) ist in be-
    stimmten Situationen eine Einreichung ohne starke Kundenauthentifizierung erfor-
    derlich (Authentifizierungsklasse 4, vgl. Kapitel B.3). Daher wird in einem solchen
    Fall das Element Segmentkennung in HKTAN ab #6 mit der Segmentkennung des
    jeweiligen Gesch??ftsvorfalls belegt, der dann isoliert in diesem Dialog eingereicht
    wird.
    (PinTan S. 35)

     * Beim Erstzugang mit einem neuen TAN-Verfahren liegt einem Kundenprodukt
    ggf. noch keine TAN-Medien-Bezeichnung f??r dieses Verfahren vor. In diesem
    Fall muss der Gesch??ftsvorfall Anzeige der verf??gbaren TAN-Medien
    (HKTAB) ohne starke Kundenauthentifizierung durchf??hrbar sein. (..)

    In das DE Segmentkennung in HKTAN wird der Wert HKTAB eingestellt.
    Der vom Kundenprodukt hier als F??llwert gelieferte Inhalt des
    Elementes Bezeichnung des TAN-Mediums in HKTAN ist vom
    Kreditinstitut in dieser Situation zu ignorieren. (..)

    Anschlie??end hat das Kundensystem den Dialog durch Senden einer
    Dialogendenachricht (HKEND) zu beenden.

    Zweiter Dialog ??? Starke Kundenauthentifizierung
    o Nun wird unter Verwendung eines zugelassenen TAN-Verfahrens
    und TAN-Mediums ein zweiter Dialog zum Durchf??hren einer starken
    Kundenauthentifizierung er??ffnet. Die SCA ist obligatorisch, da es
    sich um die erste Nutzung dieses TAN-Verfahrens inkl. des gew??hl-
    ten TAN-Mediums handelt.
    o Im Rahmen dieses Dialoges k??nnen nach erfolgreicher Durchf??hrung
    der starken Kundenauthentifizierung beliebige Gesch??ftsvorf??lle
    durchgef??hrt werden.

    (PinTan S. 37/38)
     */
    open fun createInitDialogMessageWithoutStrongCustomerAuthentication(dialogContext: DialogContext, segmentIdForTwoStepTanProcess: CustomerSegmentId?): MessageBuilderResult {
        return createInitDialogMessage(dialogContext, segmentIdForTwoStepTanProcess)
    }

    protected open fun createInitDialogMessage(dialogContext: DialogContext, segmentIdForTwoStepTanProcess: CustomerSegmentId?): MessageBuilderResult {

        val segments = mutableListOf(
            IdentifikationsSegment(generator.resetSegmentNumber(2), dialogContext),
            Verarbeitungsvorbereitung(generator.getNextSegmentNumber(), dialogContext)
        )

        if (segmentIdForTwoStepTanProcess != null) {
            segments.add(createTwoStepTanSegment(segmentIdForTwoStepTanProcess, dialogContext))
        }
        else if (dialogContext.bank.isTanMethodSelected) {
            segments.add(createTwoStepTanSegment(CustomerSegmentId.Identification, dialogContext))
        }

        if (dialogContext.bank.customerSystemId == KundensystemID.Anonymous) {
            segments.add(Synchronisierung(generator.getNextSegmentNumber(), Synchronisierungsmodus.NeueKundensystemIdZurueckmelden))
        }

        return createSignedMessageBuilderResult(dialogContext, segments)
    }

    open fun createSynchronizeCustomerSystemIdMessage(dialogContext: DialogContext): MessageBuilderResult {

        return createSignedMessageBuilderResult(dialogContext, listOf(
            IdentifikationsSegment(generator.resetSegmentNumber(2), dialogContext),
            Verarbeitungsvorbereitung(generator.getNextSegmentNumber(), dialogContext),
            createTwoStepTanSegment(CustomerSegmentId.Identification, dialogContext),
            Synchronisierung(generator.getNextSegmentNumber(), Synchronisierungsmodus.NeueKundensystemIdZurueckmelden)
        ))
    }

    open fun createDialogEndMessage(dialogContext: DialogContext): MessageBuilderResult {

        return createSignedMessageBuilderResult(dialogContext, listOf(
            Dialogende(generator.resetSegmentNumber(2), dialogContext)
        ))
    }


    open fun createGetTransactionsMessage(parameter: GetTransactionsParameter, dialogContext: DialogContext): MessageBuilderResult {

        val result = supportsGetTransactionsMt940(parameter.account)

        if (result.isJobVersionSupported) {
            return createGetTransactionsMessageMt940(result, parameter, dialogContext)
        }

        val creditCardResult = supportsGetCreditCardTransactions(parameter.account)

        if (creditCardResult.isJobVersionSupported) {
            return createGetCreditCardTransactionsMessage(result, parameter, dialogContext)
        }

        return result
    }

    protected open fun createGetTransactionsMessageMt940(result: MessageBuilderResult, parameter: GetTransactionsParameter,
                                                  dialogContext: DialogContext): MessageBuilderResult {

        if (parameter.maxCountEntries != null) {
            parameter.isSettingMaxCountEntriesAllowedByBank = determineIsSettingMaxCountEntriesAllowed(dialogContext.bank, InstituteSegmentId.AccountTransactionsMt940Parameters, listOf(5, 6, 7))
        }

        val transactionsJob = if (result.isAllowed(7)) KontoumsaetzeZeitraumMt940Version7(generator.resetSegmentNumber(2), parameter, dialogContext.bank)
        else if (result.isAllowed(6)) KontoumsaetzeZeitraumMt940Version6(generator.resetSegmentNumber(2), parameter)
        else KontoumsaetzeZeitraumMt940Version5(generator.resetSegmentNumber(2), parameter)

        val segments = mutableListOf<Segment>(transactionsJob)

        addTanSegmentIfRequired(CustomerSegmentId.AccountTransactionsMt940, dialogContext, segments)

        return createSignedMessageBuilderResult(dialogContext, segments)
    }

    protected open fun determineIsSettingMaxCountEntriesAllowed(bank: BankData, segmentId: ISegmentId, supportedJobVersions: List<Int>): Boolean {
        return bank.supportedJobs.filterIsInstance<RetrieveAccountTransactionsParameters>()
            .filter { it.segmentId == segmentId.id && supportedJobVersions.contains(it.segmentVersion) }
            .firstOrNull { it.settingCountEntriesAllowed } != null
    }

    protected open fun createGetCreditCardTransactionsMessage(result: MessageBuilderResult, parameter: GetTransactionsParameter,
                                                         dialogContext: DialogContext): MessageBuilderResult {

        val segments = mutableListOf<Segment>(KreditkartenUmsaetze(generator.resetSegmentNumber(2), parameter))

        addTanSegmentIfRequired(CustomerSegmentId.CreditCardTransactions, dialogContext, segments)

        return createSignedMessageBuilderResult(dialogContext, segments)
    }

    open fun supportsGetTransactions(account: AccountData): Boolean {
        return supportsGetTransactionsMt940(account).isJobVersionSupported
                || supportsGetCreditCardTransactions(account).isJobVersionSupported
    }

    protected open fun supportsGetTransactionsMt940(account: AccountData): MessageBuilderResult {
        return getSupportedVersionsOfJobForAccount(CustomerSegmentId.AccountTransactionsMt940, account, listOf(5, 6, 7))
    }

    protected open fun supportsGetCreditCardTransactions(account: AccountData): MessageBuilderResult {
        return getSupportedVersionsOfJobForAccount(CustomerSegmentId.CreditCardTransactions, account, listOf(2))
    }


    open fun createGetBalanceMessage(account: AccountData, dialogContext: DialogContext): MessageBuilderResult {

        val result = supportsGetBalanceMessage(account)

        if (result.isJobVersionSupported) {
            val balanceJob = if (result.isAllowed(5)) SaldenabfrageVersion5(generator.resetSegmentNumber(2), account)
            else SaldenabfrageVersion7(generator.resetSegmentNumber(2), account, dialogContext.bank)

            val segments = mutableListOf<Segment>(balanceJob)

            addTanSegmentIfRequired(CustomerSegmentId.Balance, dialogContext, segments)

            return createSignedMessageBuilderResult(dialogContext, segments)
        }

        return result
    }

    open fun supportsGetBalance(account: AccountData): Boolean {
        return supportsGetBalanceMessage(account).isJobVersionSupported
    }

    protected open fun supportsGetBalanceMessage(account: AccountData): MessageBuilderResult {
        return getSupportedVersionsOfJobForAccount(CustomerSegmentId.Balance, account, listOf(5, 7))
    }


    open fun createGetTanMediaListMessage(dialogContext: DialogContext,
                                          tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle,
                                          tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien): MessageBuilderResult {

        val result = getSupportedVersionsOfJobForBank(CustomerSegmentId.TanMediaList, dialogContext.bank, listOf(2, 3, 4, 5))

        if (result.isJobVersionSupported) {
            val segments = listOf(
                TanGeneratorListeAnzeigen(result.getHighestAllowedVersion!!,
                    generator.resetSegmentNumber(2), tanMediaKind, tanMediumClass)
            )

            return createSignedMessageBuilderResult(dialogContext, segments)
        }

        return result
    }

    // TODO: no HKTAN needed?
    open fun createChangeTanMediumMessage(newActiveTanMedium: TanGeneratorTanMedium, dialogContext: DialogContext,
                                          tan: String? = null, atc: Int? = null): MessageBuilderResult {

        val result = getSupportedVersionsOfJobForBank(CustomerSegmentId.ChangeTanMedium, dialogContext.bank, listOf(1, 2, 3))

        if (result.isJobVersionSupported) {
            val segments = listOf(
                TanGeneratorTanMediumAnOderUmmelden(result.getHighestAllowedVersion!!, generator.resetSegmentNumber(2),
                    dialogContext.bank, newActiveTanMedium, tan, atc)
            )

            return createSignedMessageBuilderResult(dialogContext, segments)
        }

        return result
    }

    open fun createSendEnteredTanMessage(enteredTan: String, tanResponse: TanResponse, dialogContext: DialogContext): MessageBuilderResult {

        val tanProcess = if (tanResponse.tanProcess == TanProcess.TanProcess1) TanProcess.TanProcess1 else TanProcess.TanProcess2

        val segments = listOf(
            ZweiSchrittTanEinreichung(generator.resetSegmentNumber(2), tanProcess, null,
                tanResponse.jobHashValue, tanResponse.jobReference, false, null, tanResponse.tanMediaIdentifier)
        )

        return createSignedMessageBuilderResult(createSignedMessage(dialogContext, enteredTan, segments), dialogContext, segments)
    }


    open fun createBankTransferMessage(data: BankTransferData, account: AccountData, dialogContext: DialogContext): MessageBuilderResult {

        val segmentId = if (data.realTimeTransfer) CustomerSegmentId.SepaRealTimeTransfer else CustomerSegmentId.SepaBankTransfer

        val (result, urn) = supportsBankTransferAndSepaVersion(dialogContext.bank, account, segmentId)

        if (result.isJobVersionSupported && urn != null) {
            val segments = mutableListOf<Segment>(SepaBankTransferBase(segmentId, generator.resetSegmentNumber(2),
                urn, dialogContext.bank.customerName, account, dialogContext.bank.bic, data))

            addTanSegmentIfRequired(segmentId, dialogContext, segments)

            return createSignedMessageBuilderResult(dialogContext, segments)
        }

        return result
    }

    open fun supportsBankTransfer(bank: BankData, account: AccountData): Boolean {
        return supportsBankTransferAndSepaVersion(bank, account, CustomerSegmentId.SepaBankTransfer).first.isJobVersionSupported
    }

    open fun supportsSepaRealTimeTransfer(bank: BankData, account: AccountData): Boolean {
        return supportsBankTransferAndSepaVersion(bank, account, CustomerSegmentId.SepaRealTimeTransfer).first.isJobVersionSupported
    }

    protected open fun supportsBankTransferAndSepaVersion(bank: BankData, account: AccountData, segmentId: CustomerSegmentId): Pair<MessageBuilderResult, String?> {
        val result = getSupportedVersionsOfJobForAccount(segmentId, account, listOf(1))

        if (result.isJobVersionSupported) {

            getSepaUrnFor(CustomerSegmentId.SepaAccountInfoParameters, bank, "pain.001.001.03")?.let { urn ->
                return Pair(result, urn)
            }

            getSepaUrnFor(CustomerSegmentId.SepaAccountInfoParameters, bank, "pain.001.003.03")?.let { urn ->
                return Pair(result, urn)
            }

            return Pair(MessageBuilderResult(true, false, result.allowedVersions, result.supportedVersions, null), null) // TODO: how to tell that we don't support required SEPA pain version?
        }

        return Pair(result, null)
    }


    open fun rebuildMessageWithContinuationId(message: MessageBuilderResult, continuationId: String, dialogContext: DialogContext): MessageBuilderResult? {

//        val copiedSegments = message.messageBodySegments.map {  }
        val aufsetzpunkte = message.messageBodySegments.flatMap { it.dataElementsAndGroups }.filterIsInstance<Aufsetzpunkt>()

        if (aufsetzpunkte.isEmpty()) {
//            return MessageBuilderResult(message.isJobAllowed, message.isJobVersionSupported, message.allowedVersions, message.supportedVersions, null)
            return null
        }

        aufsetzpunkte.forEach { it.resetContinuationId(continuationId) }

        return rebuildMessage(message, dialogContext)
    }

    open fun rebuildMessage(message: MessageBuilderResult, dialogContext: DialogContext): MessageBuilderResult {

        return createSignedMessageBuilderResult(dialogContext, message.messageBodySegments)
    }

    protected open fun createSignedMessageBuilderResult(dialogContext: DialogContext, segments: List<Segment>): MessageBuilderResult {
        return createSignedMessageBuilderResult(createSignedMessage(dialogContext, segments), dialogContext, segments)
    }

    protected open fun createSignedMessageBuilderResult(createdMessage: String, dialogContext: DialogContext, segments: List<Segment>): MessageBuilderResult {
        val message = MessageBuilderResult(createdMessage, segments)

        dialogContext.previousMessageInDialog = dialogContext.currentMessage

        dialogContext.currentMessage = message

        return message
    }

    protected open fun createUnsignedMessageBuilderResult(dialogContext: DialogContext, segments: List<Segment>): MessageBuilderResult {
        val message = MessageBuilderResult(createMessage(dialogContext, segments), segments)

        dialogContext.previousMessageInDialog = dialogContext.currentMessage

        dialogContext.currentMessage = message

        return message
    }


    open fun createSignedMessage(dialogContext: DialogContext, payloadSegments: List<Segment>): String {

        return createSignedMessage(dialogContext, null, payloadSegments)
    }

    open fun createSignedMessage(dialogContext: DialogContext, tan: String? = null,
                                 payloadSegments: List<Segment>): String {

        val date = utils.formatDateTodayAsInt()
        val time = utils.formatTimeNowAsInt()

        val signedPayload = signPayload(2, dialogContext, date, time, tan, payloadSegments)

        val encryptedPayload = encryptPayload(dialogContext, date, time, signedPayload)

        return createMessage(dialogContext, encryptedPayload)
    }

    open fun createMessage(dialogContext: DialogContext, payloadSegments: List<Segment>): String {

        dialogContext.increaseMessageNumber()

        val formattedPayload = formatPayload(payloadSegments)

        val ending = Nachrichtenabschluss(generator.getNextSegmentNumber(), dialogContext)
        val formattedEnding = ending.format()

        val messageSize = calculateMessageSize(formattedPayload, formattedEnding, dialogContext)

        val header = Nachrichtenkopf(ISegmentNumberGenerator.FirstSegmentNumber, messageSize, dialogContext)

        return listOf(header.format(), formattedPayload, formattedEnding)
            .joinToString(Separators.SegmentSeparator, postfix = Separators.SegmentSeparator)
    }

    protected open fun calculateMessageSize(formattedPayload: String, formattedEnding: String, dialogContext: DialogContext): Int {
        // we don't know Header's length yet - but already have to know its length in order to calculate message length.
        // -> generate header with a known minimum header length added to message body length to calculate header length
        val minMessageSize = formattedPayload.length + MessageHeaderMinLength + formattedEnding.length + AddedSeparatorsLength
        val headerWithMinMessageSize = Nachrichtenkopf(ISegmentNumberGenerator.FirstSegmentNumber, minMessageSize, dialogContext).format()

        return formattedPayload.length + headerWithMinMessageSize.length + formattedEnding.length + AddedSeparatorsLength
    }


    protected open fun signPayload(headerSegmentNumber: Int, dialogContext: DialogContext, date: Int, time: Int,
                                   tan: String? = null, payloadSegments: List<Segment>): List<Segment> {

        val controlReference = createControlReference()

        val signatureHeader = PinTanSignaturkopf(
            headerSegmentNumber,
            dialogContext,
            controlReference,
            date,
            time
        )

        val signatureEnding = Signaturabschluss(
            generator.getNextSegmentNumber(),
            controlReference,
            dialogContext.bank.pin,
            tan
        )

        return listOf(signatureHeader, *payloadSegments.toTypedArray(), signatureEnding)
    }

    protected open fun createControlReference(): String {
        return Random(Date().millisSinceEpoch).nextInt().absoluteValue.toString()
    }


    protected open fun encryptPayload(dialogContext: DialogContext, date: Int, time: Int,
                               payload: List<Segment>): List<Segment> {

        val encryptionHeader = PinTanVerschluesselungskopf(dialogContext, date, time)

        val encryptedData = VerschluesselteDaten(formatPayload(payload) + Separators.SegmentSeparator)

        return listOf(encryptionHeader, encryptedData)
    }


    protected open fun formatPayload(payload: List<Segment>): String {
        return payload.joinToString(Separators.SegmentSeparator) { it.format() }
    }


    protected open fun getSupportedVersionsOfJobForBank(segmentId: CustomerSegmentId, bank: BankData,
                                                 supportedVersions: List<Int>): MessageBuilderResult {

        return getSupportedVersionsOfJob(segmentId, bank.supportedJobs, supportedVersions)
    }

    protected open fun getSupportedVersionsOfJobForAccount(segmentId: CustomerSegmentId, account: AccountData,
                                                           supportedVersions: List<Int>): MessageBuilderResult {

        return getSupportedVersionsOfJob(segmentId, account.allowedJobs, supportedVersions)
    }

    protected open fun getSupportedVersionsOfJob(segmentId: CustomerSegmentId, allSupportedJobs: List<JobParameters>,
                                                 supportedVersionsOfThisJob: List<Int>): MessageBuilderResult {

        val supportedJobsForThisSegment = allSupportedJobs.filter { it.jobName == segmentId.id }

        return getSupportedVersionsOfJob(supportedVersionsOfThisJob, supportedJobsForThisSegment)
    }

    protected open fun getSupportedVersionsOfJob(supportedVersions: List<Int>, allowedJobs: List<JobParameters>): MessageBuilderResult {
        if (allowedJobs.isNotEmpty()) {
            val allowedVersions = allowedJobs
                .map { it.segmentVersion }
                .sortedDescending()

            return MessageBuilderResult(
                allowedVersions.isNotEmpty(), allowedVersions.containsAny(supportedVersions),
                allowedVersions, supportedVersions, null
            )
        }

        return MessageBuilderResult(false)
    }

    protected open fun addTanSegmentIfRequired(segmentId: CustomerSegmentId, dialogContext: DialogContext, segments: MutableList<Segment>) {
        if (isTanRequiredForJob(segmentId, dialogContext)) {
            segments.add(createTwoStepTanSegment(segmentId, dialogContext))
        }
    }

    protected open fun createTwoStepTanSegment(segmentId: CustomerSegmentId, dialogContext: DialogContext): ZweiSchrittTanEinreichung {
        return ZweiSchrittTanEinreichung(generator.getNextSegmentNumber(), TanProcess.TanProcess4, segmentId, tanMediaIdentifier = getTanMediaIdentifierIfRequired(dialogContext))
    }

    protected open fun getTanMediaIdentifierIfRequired(dialogContext: DialogContext): String? {
        val bank = dialogContext.bank

        if (bank.isTanMethodSelected && bank.selectedTanMethod.nameOfTanMediumRequired) {
            return bank.tanMedia.firstOrNull { it.mediumName != null }?.mediumName
        }

        return null
    }

    protected open fun isTanRequiredForJob(segmentId: CustomerSegmentId, dialogContext: DialogContext): Boolean {
        return dialogContext.bank.pinInfo?.jobTanConfiguration?.first { it.segmentId == segmentId.id }?.tanRequired
            ?: false // TODO: actually in this case it's not allowed to execute job via PIN/TAN at all
    }

    protected open fun getSepaUrnFor(segmentId: CustomerSegmentId, bank: BankData, sepaDataFormat: String): String? {

        return bank.supportedJobs
            .filterIsInstance<SepaAccountInfoParameters>()
            .sortedByDescending { it.segmentVersion }
            .flatMap { it.supportedSepaFormats }
            .firstOrNull { it.contains(sepaDataFormat) }
    }


    // TODO: move to a library
    fun <T> Collection<T>.containsAny(otherCollection: Collection<T>): Boolean {
        for (otherItem in otherCollection) {
            if (this.contains(otherItem)) {
                return true
            }
        }

        return false
    }

}