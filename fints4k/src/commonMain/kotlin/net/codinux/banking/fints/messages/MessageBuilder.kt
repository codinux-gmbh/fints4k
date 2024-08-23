package net.codinux.banking.fints.messages

import net.codinux.banking.fints.extensions.randomWithSeed
import net.codinux.banking.fints.messages.datenelemente.implementierte.Aufsetzpunkt
import net.codinux.banking.fints.messages.datenelemente.implementierte.KundensystemID
import net.codinux.banking.fints.messages.datenelemente.implementierte.Synchronisierungsmodus
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanGeneratorTanMedium
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMedienArtVersion
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMediumKlasse
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanProcess
import net.codinux.banking.fints.messages.segmente.Segment
import net.codinux.banking.fints.messages.segmente.Synchronisierung
import net.codinux.banking.fints.messages.segmente.id.CustomerSegmentId
import net.codinux.banking.fints.messages.segmente.id.ISegmentId
import net.codinux.banking.fints.messages.segmente.implementierte.*
import net.codinux.banking.fints.messages.segmente.implementierte.sepa.SepaBankTransferBase
import net.codinux.banking.fints.messages.segmente.implementierte.tan.TanGeneratorListeAnzeigen
import net.codinux.banking.fints.messages.segmente.implementierte.tan.TanGeneratorTanMediumAnOderUmmelden
import net.codinux.banking.fints.messages.segmente.implementierte.umsaetze.*
import net.codinux.banking.fints.model.*
import net.codinux.banking.fints.response.InstituteSegmentId
import net.codinux.banking.fints.response.segments.*
import net.codinux.banking.fints.util.FinTsUtils
import kotlin.math.absoluteValue


/**
 * Takes the Segments of they payload, may signs and encrypts them, calculates message size,
 * adds the message header and ending, and formats the whole message to string.
 */
open class MessageBuilder(protected val utils: FinTsUtils = FinTsUtils()) {

    companion object {
        const val MessageHeaderMinLength = 28

        const val AddedSeparatorsLength = 3

        private const val MessageHeaderSegmentNumber = 1

        private const val UnsignedMessagePayloadSegmentNumberStart = MessageHeaderSegmentNumber + 1

        private const val SignatureHeaderSegmentNumber = MessageHeaderSegmentNumber + 1

        private const val SignedMessagePayloadFirstSegmentNumber = SignatureHeaderSegmentNumber + 1
    }


    /**
     * Um Kunden die Möglichkeit zu geben, sich anonym anzumelden, um sich bspw. über die
     * angebotenen Geschäftsvorfälle fremder Kreditinstitute (von denen sie keine BPD besitzen)
     * zu informieren bzw. nicht-signierungspflichtige Aufträge bei fremden Kreditinstituten
     * einreichen zu können, kann sich der Kunde anonym (als Gast) anmelden.
     *
     * Bei anonymen Dialogen werden Nachrichten weder signiert, noch können sie verschlüsselt und komprimiert werden.
     */
    /**
     * Auch im Rahmen einer Eröffnung eines anonymen Dialogs muss ein Kun-
    denprodukt, das die starke Kundenauthentifizierung unterstützt, in die Dia-
    loginitialisierungsnachricht ein Segment HKTAN ab Segmentversion #6 ein-
    stellen. Auf diese Weise ist eine Signalisierung der SCA-Fähigkeit möglich
    und dem Kundensystem können in der Antwort bei Bedarf geeignete BPD
    übermittelt werden, wenn das Kreditinstitut dies unterstützt.
     (PinTan S. 35)
     */
    open fun createAnonymousDialogInitMessage(context: JobContext): MessageBuilderResult {

        return createUnsignedMessageBuilderResult(context, MessageType.AnonymousDialogInit, listOf(
            IdentifikationsSegment(UnsignedMessagePayloadSegmentNumberStart, context),
            Verarbeitungsvorbereitung(UnsignedMessagePayloadSegmentNumberStart + 1, context)
        ))
    }

    open fun createAnonymousDialogEndMessage(context: JobContext): MessageBuilderResult {

        return createUnsignedMessageBuilderResult(context, MessageType.DialogEnd, listOf(
            Dialogende(UnsignedMessagePayloadSegmentNumberStart, context.dialog)
        ))
    }


    open fun createInitDialogMessage(context: JobContext): MessageBuilderResult {
        return createInitDialogMessage(context, null)
    }

    /**
     * Im Rahmen der PIN/TAN-Management-Geschäftsvorfälle (vgl. Kapitel C.3) ist in be-
    stimmten Situationen eine Einreichung ohne starke Kundenauthentifizierung erfor-
    derlich (Authentifizierungsklasse 4, vgl. Kapitel B.3). Daher wird in einem solchen
    Fall das Element Segmentkennung in HKTAN ab #6 mit der Segmentkennung des
    jeweiligen Geschäftsvorfalls belegt, der dann isoliert in diesem Dialog eingereicht
    wird.
    (PinTan S. 35)

     * Beim Erstzugang mit einem neuen TAN-Verfahren liegt einem Kundenprodukt
    ggf. noch keine TAN-Medien-Bezeichnung für dieses Verfahren vor. In diesem
    Fall muss der Geschäftsvorfall Anzeige der verfügbaren TAN-Medien
    (HKTAB) ohne starke Kundenauthentifizierung durchführbar sein. (..)

    In das DE Segmentkennung in HKTAN wird der Wert HKTAB eingestellt.
    Der vom Kundenprodukt hier als Füllwert gelieferte Inhalt des
    Elementes Bezeichnung des TAN-Mediums in HKTAN ist vom
    Kreditinstitut in dieser Situation zu ignorieren. (..)

    Anschließend hat das Kundensystem den Dialog durch Senden einer
    Dialogendenachricht (HKEND) zu beenden.

    Zweiter Dialog – Starke Kundenauthentifizierung
    o Nun wird unter Verwendung eines zugelassenen TAN-Verfahrens
    und TAN-Mediums ein zweiter Dialog zum Durchführen einer starken
    Kundenauthentifizierung eröffnet. Die SCA ist obligatorisch, da es
    sich um die erste Nutzung dieses TAN-Verfahrens inkl. des gewähl-
    ten TAN-Mediums handelt.
    o Im Rahmen dieses Dialoges können nach erfolgreicher Durchführung
    der starken Kundenauthentifizierung beliebige Geschäftsvorfälle
    durchgeführt werden.

    (PinTan S. 37/38)
     */
    open fun createInitDialogMessageWithoutStrongCustomerAuthentication(context: JobContext, segmentIdForTwoStepTanProcess: CustomerSegmentId?): MessageBuilderResult {
        return createInitDialogMessage(context, segmentIdForTwoStepTanProcess)
    }

    protected open fun createInitDialogMessage(context: JobContext, segmentIdForTwoStepTanProcess: CustomerSegmentId?): MessageBuilderResult {
        val segments = mutableListOf(
            IdentifikationsSegment(SignedMessagePayloadFirstSegmentNumber, context),
            Verarbeitungsvorbereitung(SignedMessagePayloadFirstSegmentNumber + 1, context)
        )

        if (segmentIdForTwoStepTanProcess != null) {
            segments.add(createTwoStepTanSegment(context, segmentIdForTwoStepTanProcess, SignedMessagePayloadFirstSegmentNumber + 2))
        } else if (context.bank.isTanMethodSelected) {
            segments.add(createTwoStepTanSegment(context, CustomerSegmentId.Identification, SignedMessagePayloadFirstSegmentNumber + 2))
        }

        if (context.bank.customerSystemId == KundensystemID.Anonymous) {
            segments.add(Synchronisierung(segments.size + 3, Synchronisierungsmodus.NeueKundensystemIdZurueckmelden))
        }

        return createSignedMessageBuilderResult(context, MessageType.DialogInit, segments)
    }

    open fun createSynchronizeCustomerSystemIdMessage(context: JobContext): MessageBuilderResult {
        return createSignedMessageBuilderResult(context, MessageType.SynchronizeCustomerSystemId, listOf(
            IdentifikationsSegment(SignedMessagePayloadFirstSegmentNumber, context),
            Verarbeitungsvorbereitung(SignedMessagePayloadFirstSegmentNumber + 1, context),
            createTwoStepTanSegment(context, CustomerSegmentId.Identification, SignedMessagePayloadFirstSegmentNumber + 2),
            Synchronisierung(SignedMessagePayloadFirstSegmentNumber + 3, Synchronisierungsmodus.NeueKundensystemIdZurueckmelden)
        ))
    }

    open fun createDialogEndMessage(context: JobContext): MessageBuilderResult {

        return createSignedMessageBuilderResult(context, MessageType.DialogEnd, listOf(
            Dialogende(SignedMessagePayloadFirstSegmentNumber, context.dialog)
        ))
    }


    open fun createGetTransactionsMessage(context: JobContext, parameter: GetAccountTransactionsParameter): MessageBuilderResult {

        val result = supportsGetTransactionsMt940(parameter.account)

        if (result.isJobVersionSupported) {
            return createGetTransactionsMessageMt940(context, result, parameter)
        }

        val creditCardResult = supportsGetCreditCardTransactions(parameter.account)

        if (creditCardResult.isJobVersionSupported) {
            return createGetCreditCardTransactionsMessage(context, result, parameter)
        }

        return result
    }

    protected open fun createGetTransactionsMessageMt940(context: JobContext, result: MessageBuilderResult,
                                                         parameter: GetAccountTransactionsParameter): MessageBuilderResult {

        if (parameter.maxCountEntries != null) {
            parameter.isSettingMaxCountEntriesAllowedByBank = determineIsSettingMaxCountEntriesAllowed(context.bank, InstituteSegmentId.AccountTransactionsMt940Parameters, listOf(5, 6, 7))
        }

        val segmentNumber = SignedMessagePayloadFirstSegmentNumber

        val transactionsJob = if (result.isAllowed(7)) KontoumsaetzeZeitraumMt940Version7(segmentNumber, parameter, context.bank)
        else if (result.isAllowed(6)) KontoumsaetzeZeitraumMt940Version6(segmentNumber, parameter)
        else KontoumsaetzeZeitraumMt940Version5(segmentNumber, parameter)

        val segments = mutableListOf<Segment>(transactionsJob)

        addTanSegmentIfRequired(context, CustomerSegmentId.AccountTransactionsMt940, segments, segmentNumber + 1)

        return createSignedMessageBuilderResult(context, MessageType.GetTransactions, segments)
    }

    protected open fun determineIsSettingMaxCountEntriesAllowed(bank: BankData, segmentId: ISegmentId, supportedJobVersions: List<Int>): Boolean {
        return bank.supportedJobs.filterIsInstance<RetrieveAccountTransactionsParameters>()
            .filter { it.segmentId == segmentId.id && supportedJobVersions.contains(it.segmentVersion) }
            .firstOrNull { it.settingCountEntriesAllowed } != null
    }

    protected open fun createGetCreditCardTransactionsMessage(context: JobContext, result: MessageBuilderResult,
                                                              parameter: GetAccountTransactionsParameter): MessageBuilderResult {

        val segments = mutableListOf<Segment>(KreditkartenUmsaetze(SignedMessagePayloadFirstSegmentNumber, parameter))

        addTanSegmentIfRequired(context, CustomerSegmentId.CreditCardTransactions, segments, SignedMessagePayloadFirstSegmentNumber + 1)

        return createSignedMessageBuilderResult(context, MessageType.GetCreditCardTransactions, segments)
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


    open fun createGetBalanceMessage(context: JobContext, account: AccountData): MessageBuilderResult {

        val result = supportsGetBalanceMessage(account)

        if (result.isJobVersionSupported) {
            val segmentNumber = SignedMessagePayloadFirstSegmentNumber

            val balanceJob = if (result.isAllowed(6)) SaldenabfrageVersion6(segmentNumber, account)
            else if (result.isAllowed(7)) SaldenabfrageVersion7(segmentNumber, account, context.bank)
            else if (result.isAllowed(8)) SaldenabfrageVersion8(segmentNumber, account, context.bank)
            else SaldenabfrageVersion5(segmentNumber, account)

            val segments = mutableListOf<Segment>(balanceJob)

            addTanSegmentIfRequired(context, CustomerSegmentId.Balance, segments, segmentNumber + 1)

            return createSignedMessageBuilderResult(context, MessageType.GetBalance, segments)
        }

        return result
    }

    open fun supportsGetBalance(account: AccountData): Boolean {
        return supportsGetBalanceMessage(account).isJobVersionSupported
    }

    protected open fun supportsGetBalanceMessage(account: AccountData): MessageBuilderResult {
        return getSupportedVersionsOfJobForAccount(CustomerSegmentId.Balance, account, listOf(5, 6, 7, 8))
    }


    open fun createGetTanMediaListMessage(context: JobContext,
                                          tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle,
                                          tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien): MessageBuilderResult {

        val result = getSupportedVersionsOfJobForBank(CustomerSegmentId.TanMediaList, context.bank, listOf(2, 3, 4, 5))

        if (result.isJobVersionSupported) {
            val segments = listOf(
                TanGeneratorListeAnzeigen(result.getHighestAllowedVersion!!, SignedMessagePayloadFirstSegmentNumber, tanMediaKind, tanMediumClass)
            )

            return createSignedMessageBuilderResult(context, MessageType.GetTanMedia, segments)
        }

        return result
    }

    // TODO: no HKTAN needed?
    open fun createChangeTanMediumMessage(context: JobContext, newActiveTanMedium: TanGeneratorTanMedium,
                                          tan: String? = null, atc: Int? = null): MessageBuilderResult {

        val result = getSupportedVersionsOfJobForBank(CustomerSegmentId.ChangeTanMedium, context.bank, listOf(1, 2, 3))

        if (result.isJobVersionSupported) {
            val segments = listOf(
                TanGeneratorTanMediumAnOderUmmelden(result.getHighestAllowedVersion!!, SignedMessagePayloadFirstSegmentNumber,
                    context.bank, newActiveTanMedium, tan, atc)
            )

            return createSignedMessageBuilderResult(context, MessageType.ChangeTanMedium, segments)
        }

        return result
    }

    open fun createSendEnteredTanMessage(context: JobContext, enteredTan: String, tanResponse: TanResponse): MessageBuilderResult {

        val tanProcess = if (tanResponse.tanProcess == TanProcess.TanProcess1) TanProcess.TanProcess1 else TanProcess.TanProcess2

        val segments = listOf(
            ZweiSchrittTanEinreichung(SignedMessagePayloadFirstSegmentNumber, tanProcess, null,
                tanResponse.jobHashValue, tanResponse.jobReference, false, null, tanResponse.tanMediaIdentifier)
        )

        return createSignedMessageBuilderResult(context, MessageType.Tan, createSignedMessage(context, enteredTan, segments), segments)
    }


    open fun createBankTransferMessage(context: JobContext, data: BankTransferData, account: AccountData): MessageBuilderResult {

        val segmentId = if (data.realTimeTransfer) CustomerSegmentId.SepaRealTimeTransfer else CustomerSegmentId.SepaBankTransfer

        val (result, urn) = supportsBankTransferAndSepaVersion(context.bank, account, segmentId)

        if (result.isJobVersionSupported && urn != null) {
            val segments = mutableListOf<Segment>(SepaBankTransferBase(segmentId, SignedMessagePayloadFirstSegmentNumber,
                urn, context.bank.customerName, account, context.bank.bic, data))

            addTanSegmentIfRequired(context, segmentId, segments, SignedMessagePayloadFirstSegmentNumber + 1)

            return createSignedMessageBuilderResult(context, MessageType.TransferMoney, segments)
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


    open fun rebuildMessageWithContinuationId(context: JobContext, message: MessageBuilderResult, continuationId: String): MessageBuilderResult? {

//        val copiedSegments = message.messageBodySegments.map {  }
        val aufsetzpunkte = message.messageBodySegments.flatMap { it.dataElementsAndGroups }.filterIsInstance<Aufsetzpunkt>()

        if (aufsetzpunkte.isEmpty()) {
//            return MessageBuilderResult(message.isJobAllowed, message.isJobVersionSupported, message.allowedVersions, message.supportedVersions, null)
            return null
        }

        aufsetzpunkte.forEach { it.resetContinuationId(continuationId) }

        return rebuildMessage(context, message)
    }

    open fun rebuildMessage(context: JobContext, message: MessageBuilderResult): MessageBuilderResult {

        return createSignedMessageBuilderResult(context, context.dialog.messageType, message.messageBodySegments)
    }

    protected open fun createSignedMessageBuilderResult(context: JobContext, type: MessageType, segments: List<Segment>): MessageBuilderResult {
        return createSignedMessageBuilderResult(context, type, createSignedMessage(context, segments), segments)
    }

    protected open fun createSignedMessageBuilderResult(context: JobContext, type: MessageType, createdMessage: String, segments: List<Segment>): MessageBuilderResult {
        return createMessageBuilderResult(context, type, createdMessage, segments)
    }

    protected open fun createUnsignedMessageBuilderResult(context: JobContext, type: MessageType, segments: List<Segment>): MessageBuilderResult {
        return createMessageBuilderResult(context, type, createMessage(context, segments), segments)
    }

    protected open fun createMessageBuilderResult(context: JobContext, type: MessageType, createdMessage: String, segments: List<Segment>): MessageBuilderResult {
        val message = MessageBuilderResult(createdMessage, segments)

        context.dialog.setNextMessage(type, message)

        return message
    }


    open fun createSignedMessage(context: JobContext, payloadSegments: List<Segment>): String {

        return createSignedMessage(context, null, payloadSegments)
    }

    open fun createSignedMessage(context: JobContext, tan: String? = null, payloadSegments: List<Segment>): String {

        val date = utils.formatDateTodayAsInt()
        val time = utils.formatTimeNowAsInt()

        val signedPayload = signPayload(context, date, time, tan, payloadSegments)

        val encryptedPayload = encryptPayload(context, date, time, signedPayload)

        return createMessage(context, encryptedPayload, payloadSegments.size)
    }

    open fun createMessage(context: JobContext, payloadSegments: List<Segment>, countWrappedSegments: Int = 0): String {

        val dialog = context.dialog
        dialog.increaseMessageNumber()

        val formattedPayload = formatPayload(payloadSegments)

        // if there are segments wrapped like in signed message body, we have to add these segments to segment count; +2 for Message Header and -Ending
        val ending = Nachrichtenabschluss(payloadSegments.size + countWrappedSegments + 2, dialog)
        val formattedEnding = ending.format()

        val messageSize = calculateMessageSize(formattedPayload, formattedEnding, dialog)

        val header = Nachrichtenkopf(MessageHeaderSegmentNumber, messageSize, dialog)

        return listOf(header.format(), formattedPayload, formattedEnding)
            .joinToString(Separators.SegmentSeparator, postfix = Separators.SegmentSeparator)
    }

    protected open fun calculateMessageSize(formattedPayload: String, formattedEnding: String, dialogContext: DialogContext): Int {
        // we don't know Header's length yet - but already have to know its length in order to calculate message length.
        // -> generate header with a known minimum header length added to message body length to calculate header length
        val minMessageSize = formattedPayload.length + MessageHeaderMinLength + formattedEnding.length + AddedSeparatorsLength
        val headerWithMinMessageSize = Nachrichtenkopf(MessageHeaderSegmentNumber, minMessageSize, dialogContext).format()

        return formattedPayload.length + headerWithMinMessageSize.length + formattedEnding.length + AddedSeparatorsLength
    }


    protected open fun signPayload(context: JobContext, date: Int, time: Int,
                                   tan: String? = null, payloadSegments: List<Segment>): List<Segment> {

        val controlReference = createControlReference()

        val signatureHeader = PinTanSignaturkopf(
            SignatureHeaderSegmentNumber, // is always 2
            context,
            controlReference,
            date,
            time
        )

        val signatureEnding = Signaturabschluss(
            payloadSegments.size + 3, // +3: Message Header (1), Signatur Header (2), Signature Ending
            controlReference,
            context.bank.pin,
            tan
        )

        return listOf(signatureHeader, *payloadSegments.toTypedArray(), signatureEnding)
    }

    protected open fun createControlReference(): String {
        return randomWithSeed().nextInt().absoluteValue.toString()
    }


    protected open fun encryptPayload(context: JobContext, date: Int, time: Int,
                                      payload: List<Segment>): List<Segment> {

        val encryptionHeader = PinTanVerschluesselungskopf(context, date, time)

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

    protected open fun addTanSegmentIfRequired(context: JobContext, segmentId: CustomerSegmentId, segments: MutableList<Segment>, segmentNumber: Int) {
        if (isTanRequiredForJob(context, segmentId)) {
            segments.add(createTwoStepTanSegment(context, segmentId, segmentNumber))
        }
    }

    protected open fun createTwoStepTanSegment(context: JobContext, segmentId: CustomerSegmentId, segmentNumber: Int): ZweiSchrittTanEinreichung {
        return ZweiSchrittTanEinreichung(segmentNumber, TanProcess.TanProcess4, segmentId,
            tanMediaIdentifier = getTanMediaIdentifierIfRequired(context))
    }

    protected open fun getTanMediaIdentifierIfRequired(context: JobContext): String? {
        val bank = context.bank

        if (bank.isTanMethodSelected && bank.selectedTanMethod.nameOfTanMediumRequired) {
            return bank.selectedTanMedium?.mediumName
        }

        return null
    }

    protected open fun isTanRequiredForJob(context: JobContext, segmentId: CustomerSegmentId): Boolean {
        return context.bank.pinInfo?.jobTanConfiguration?.first { it.segmentId == segmentId.id }?.tanRequired
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