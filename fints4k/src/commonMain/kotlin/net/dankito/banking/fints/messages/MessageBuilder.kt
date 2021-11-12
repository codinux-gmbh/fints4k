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

        return createUnsignedMessageBuilderResult(context, listOf(
            IdentifikationsSegment(generator.resetSegmentNumber(1), context),
            Verarbeitungsvorbereitung(generator.getNextSegmentNumber(), context)
        ))
    }

    open fun createAnonymousDialogEndMessage(context: JobContext): MessageBuilderResult {

        return createUnsignedMessageBuilderResult(context, listOf(
            Dialogende(generator.resetSegmentNumber(1), context.dialog)
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
            IdentifikationsSegment(generator.resetSegmentNumber(2), context),
            Verarbeitungsvorbereitung(generator.getNextSegmentNumber(), context)
        )

        if (segmentIdForTwoStepTanProcess != null) {
            segments.add(createTwoStepTanSegment(context, segmentIdForTwoStepTanProcess))
        }
        else if (context.bank.isTanMethodSelected) {
            segments.add(createTwoStepTanSegment(context, CustomerSegmentId.Identification))
        }

        if (context.bank.customerSystemId == KundensystemID.Anonymous) {
            segments.add(Synchronisierung(generator.getNextSegmentNumber(), Synchronisierungsmodus.NeueKundensystemIdZurueckmelden))
        }

        return createSignedMessageBuilderResult(context, segments)
    }

    open fun createSynchronizeCustomerSystemIdMessage(context: JobContext): MessageBuilderResult {

        return createSignedMessageBuilderResult(context, listOf(
            IdentifikationsSegment(generator.resetSegmentNumber(2), context),
            Verarbeitungsvorbereitung(generator.getNextSegmentNumber(), context),
            createTwoStepTanSegment(context, CustomerSegmentId.Identification),
            Synchronisierung(generator.getNextSegmentNumber(), Synchronisierungsmodus.NeueKundensystemIdZurueckmelden)
        ))
    }

    open fun createDialogEndMessage(context: JobContext): MessageBuilderResult {

        return createSignedMessageBuilderResult(context, listOf(
            Dialogende(generator.resetSegmentNumber(2), context.dialog)
        ))
    }


    open fun createGetTransactionsMessage(context: JobContext, parameter: GetTransactionsParameter): MessageBuilderResult {

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
                                                         parameter: GetTransactionsParameter): MessageBuilderResult {

        if (parameter.maxCountEntries != null) {
            parameter.isSettingMaxCountEntriesAllowedByBank = determineIsSettingMaxCountEntriesAllowed(context.bank, InstituteSegmentId.AccountTransactionsMt940Parameters, listOf(5, 6, 7))
        }

        val transactionsJob = if (result.isAllowed(7)) KontoumsaetzeZeitraumMt940Version7(generator.resetSegmentNumber(2), parameter, context.bank)
        else if (result.isAllowed(6)) KontoumsaetzeZeitraumMt940Version6(generator.resetSegmentNumber(2), parameter)
        else KontoumsaetzeZeitraumMt940Version5(generator.resetSegmentNumber(2), parameter)

        val segments = mutableListOf<Segment>(transactionsJob)

        addTanSegmentIfRequired(context, CustomerSegmentId.AccountTransactionsMt940, segments)

        return createSignedMessageBuilderResult(context, segments)
    }

    protected open fun determineIsSettingMaxCountEntriesAllowed(bank: BankData, segmentId: ISegmentId, supportedJobVersions: List<Int>): Boolean {
        return bank.supportedJobs.filterIsInstance<RetrieveAccountTransactionsParameters>()
            .filter { it.segmentId == segmentId.id && supportedJobVersions.contains(it.segmentVersion) }
            .firstOrNull { it.settingCountEntriesAllowed } != null
    }

    protected open fun createGetCreditCardTransactionsMessage(context: JobContext, result: MessageBuilderResult,
                                                              parameter: GetTransactionsParameter): MessageBuilderResult {

        val segments = mutableListOf<Segment>(KreditkartenUmsaetze(generator.resetSegmentNumber(2), parameter))

        addTanSegmentIfRequired(context, CustomerSegmentId.CreditCardTransactions, segments)

        return createSignedMessageBuilderResult(context, segments)
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
            val balanceJob = if (result.isAllowed(5)) SaldenabfrageVersion5(generator.resetSegmentNumber(2), account)
            else SaldenabfrageVersion7(generator.resetSegmentNumber(2), account, context.bank)

            val segments = mutableListOf<Segment>(balanceJob)

            addTanSegmentIfRequired(context, CustomerSegmentId.Balance, segments)

            return createSignedMessageBuilderResult(context, segments)
        }

        return result
    }

    open fun supportsGetBalance(account: AccountData): Boolean {
        return supportsGetBalanceMessage(account).isJobVersionSupported
    }

    protected open fun supportsGetBalanceMessage(account: AccountData): MessageBuilderResult {
        return getSupportedVersionsOfJobForAccount(CustomerSegmentId.Balance, account, listOf(5, 7))
    }


    open fun createGetTanMediaListMessage(context: JobContext,
                                          tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle,
                                          tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien): MessageBuilderResult {

        val result = getSupportedVersionsOfJobForBank(CustomerSegmentId.TanMediaList, context.bank, listOf(2, 3, 4, 5))

        if (result.isJobVersionSupported) {
            val segments = listOf(
                TanGeneratorListeAnzeigen(result.getHighestAllowedVersion!!,
                    generator.resetSegmentNumber(2), tanMediaKind, tanMediumClass)
            )

            return createSignedMessageBuilderResult(context, segments)
        }

        return result
    }

    // TODO: no HKTAN needed?
    open fun createChangeTanMediumMessage(context: JobContext, newActiveTanMedium: TanGeneratorTanMedium,
                                          tan: String? = null, atc: Int? = null): MessageBuilderResult {

        val result = getSupportedVersionsOfJobForBank(CustomerSegmentId.ChangeTanMedium, context.bank, listOf(1, 2, 3))

        if (result.isJobVersionSupported) {
            val segments = listOf(
                TanGeneratorTanMediumAnOderUmmelden(result.getHighestAllowedVersion!!, generator.resetSegmentNumber(2),
                    context.bank, newActiveTanMedium, tan, atc)
            )

            return createSignedMessageBuilderResult(context, segments)
        }

        return result
    }

    open fun createSendEnteredTanMessage(context: JobContext, enteredTan: String, tanResponse: TanResponse): MessageBuilderResult {

        val tanProcess = if (tanResponse.tanProcess == TanProcess.TanProcess1) TanProcess.TanProcess1 else TanProcess.TanProcess2

        val segments = listOf(
            ZweiSchrittTanEinreichung(generator.resetSegmentNumber(2), tanProcess, null,
                tanResponse.jobHashValue, tanResponse.jobReference, false, null, tanResponse.tanMediaIdentifier)
        )

        return createSignedMessageBuilderResult(context, createSignedMessage(context, enteredTan, segments), segments)
    }


    open fun createBankTransferMessage(context: JobContext, data: BankTransferData, account: AccountData): MessageBuilderResult {

        val segmentId = if (data.realTimeTransfer) CustomerSegmentId.SepaRealTimeTransfer else CustomerSegmentId.SepaBankTransfer

        val (result, urn) = supportsBankTransferAndSepaVersion(context.bank, account, segmentId)

        if (result.isJobVersionSupported && urn != null) {
            val segments = mutableListOf<Segment>(SepaBankTransferBase(segmentId, generator.resetSegmentNumber(2),
                urn, context.bank.customerName, account, context.bank.bic, data))

            addTanSegmentIfRequired(context, segmentId, segments)

            return createSignedMessageBuilderResult(context, segments)
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

        return createSignedMessageBuilderResult(context, message.messageBodySegments)
    }

    protected open fun createSignedMessageBuilderResult(context: JobContext, segments: List<Segment>): MessageBuilderResult {
        return createSignedMessageBuilderResult(context, createSignedMessage(context, segments), segments)
    }

    protected open fun createSignedMessageBuilderResult(context: JobContext, createdMessage: String, segments: List<Segment>): MessageBuilderResult {
        return createMessageBuilderResult(context, createdMessage, segments)
    }

    protected open fun createUnsignedMessageBuilderResult(context: JobContext, segments: List<Segment>): MessageBuilderResult {
        return createMessageBuilderResult(context, createMessage(context, segments), segments)
    }

    protected open fun createMessageBuilderResult(context: JobContext, createdMessage: String, segments: List<Segment>): MessageBuilderResult {
        val message = MessageBuilderResult(createdMessage, segments)

        val dialog = context.dialog

        dialog.previousMessageInDialog = dialog.currentMessage

        dialog.currentMessage = message

        return message
    }


    open fun createSignedMessage(context: JobContext, payloadSegments: List<Segment>): String {

        return createSignedMessage(context, null, payloadSegments)
    }

    open fun createSignedMessage(context: JobContext, tan: String? = null,
                                 payloadSegments: List<Segment>): String {

        val date = utils.formatDateTodayAsInt()
        val time = utils.formatTimeNowAsInt()

        val signedPayload = signPayload(2, context, date, time, tan, payloadSegments)

        val encryptedPayload = encryptPayload(context, date, time, signedPayload)

        return createMessage(context, encryptedPayload)
    }

    open fun createMessage(context: JobContext, payloadSegments: List<Segment>): String {

        val dialog = context.dialog
        dialog.increaseMessageNumber()

        val formattedPayload = formatPayload(payloadSegments)

        val ending = Nachrichtenabschluss(generator.getNextSegmentNumber(), dialog)
        val formattedEnding = ending.format()

        val messageSize = calculateMessageSize(formattedPayload, formattedEnding, dialog)

        val header = Nachrichtenkopf(ISegmentNumberGenerator.FirstSegmentNumber, messageSize, dialog)

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


    protected open fun signPayload(headerSegmentNumber: Int, context: JobContext, date: Int, time: Int,
                                   tan: String? = null, payloadSegments: List<Segment>): List<Segment> {

        val controlReference = createControlReference()

        val signatureHeader = PinTanSignaturkopf(
            headerSegmentNumber,
            context,
            controlReference,
            date,
            time
        )

        val signatureEnding = Signaturabschluss(
            generator.getNextSegmentNumber(),
            controlReference,
            context.bank.pin,
            tan
        )

        return listOf(signatureHeader, *payloadSegments.toTypedArray(), signatureEnding)
    }

    protected open fun createControlReference(): String {
        return Random(Date().millisSinceEpoch).nextInt().absoluteValue.toString()
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

    protected open fun addTanSegmentIfRequired(context: JobContext, segmentId: CustomerSegmentId, segments: MutableList<Segment>) {
        if (isTanRequiredForJob(context, segmentId)) {
            segments.add(createTwoStepTanSegment(context, segmentId))
        }
    }

    protected open fun createTwoStepTanSegment(context: JobContext, segmentId: CustomerSegmentId): ZweiSchrittTanEinreichung {
        return ZweiSchrittTanEinreichung(generator.getNextSegmentNumber(), TanProcess.TanProcess4, segmentId,
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