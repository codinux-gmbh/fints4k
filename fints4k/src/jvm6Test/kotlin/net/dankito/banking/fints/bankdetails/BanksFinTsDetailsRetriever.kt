package net.dankito.banking.fints.bankdetails

import kotlinx.coroutines.runBlocking
import net.dankito.banking.fints.FinTsClient
import net.dankito.banking.bankfinder.InMemoryBankFinder
import net.dankito.banking.fints.callback.NoOpFinTsClientCallback
import net.dankito.banking.fints.messages.MessageBuilder
import net.dankito.banking.fints.messages.MessageBuilderResult
import net.dankito.banking.fints.messages.Separators
import net.dankito.banking.fints.messages.datenelemente.implementierte.Dialogsprache
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.AuftraggeberkontoErforderlich
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.BezeichnungDesTanMediumsErforderlich
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.SmsAbbuchungskontoErforderlich
import net.dankito.banking.fints.model.*
import net.dankito.banking.bankfinder.BankInfo
import net.dankito.banking.fints.response.BankResponse
import net.dankito.banking.fints.response.segments.SepaAccountInfoParameters
import net.dankito.banking.fints.response.segments.TanInfo
import net.dankito.banking.fints.response.segments.TanMethodParameters
import net.dankito.banking.fints.util.*
import net.dankito.banking.fints.webclient.KtorWebClient
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.junit.Ignore
import kotlin.test.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


@Ignore // not a real test, run manually to retrieve FinTS information from all banks
class BanksFinTsDetailsRetriever {

    companion object {
        private val OutputFolderDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")

        private val log = LoggerFactory.getLogger(BanksFinTsDetailsRetriever::class.java)
    }


    private val bankFinder = InMemoryBankFinder()

    private val product: ProductData = ProductData("15E53C26816138699C7B6A3E8", "1.0.0") // TODO: get version dynamically

    private val messageBuilder = MessageBuilder()

    private val finTsClient = object : FinTsClient(NoOpFinTsClientCallback(), KtorWebClient(), PureKotlinBase64Service()) {

        fun getAndHandleResponseForMessagePublic(message: MessageBuilderResult, dialogContext: DialogContext, callback: (BankResponse) -> Unit) {
            getAndHandleResponseForMessage(message, dialogContext, callback)
        }

        fun updateBankDataPublic(bank: BankData, response: BankResponse) {
            super.updateBankData(bank, response)
        }

        fun mapToTanMethodTypePublic(parameters: TanMethodParameters): TanMethodType? {
            return super.mapToTanMethodType(parameters)
        }
    }


    private val requestNotSuccessful = mutableListOf<BankInfo>()

    private val tanMethodParameter = mutableMapOf<String, MutableSet<TanMethodParameters>>()
    private val tanMethodTypes = mutableMapOf<TanMethodType?, MutableSet<TanMethodParameters>>()

    private val tanMethodParameterTechnicalIdentification = mutableSetOf<String>()
    private val tanMethodParameterVersionDkTanMethod = mutableSetOf<String?>()

    private val requiresSmsAbbuchungskonto = mutableListOf<BankInfo>()
    private val requiresAuftraggeberkonto = mutableListOf<BankInfo>()
    private val requiresChallengeClass = mutableListOf<BankInfo>()
    private val signatureStructured = mutableListOf<BankInfo>()
    private val requiresNameOfTanMedia = mutableListOf<BankInfo>()
    private val requiresHhdUcResponse = mutableListOf<BankInfo>()

    private val doesNotSupportHKTAN6 = mutableListOf<BankInfo>()
    private val doesNotSupportHKSAL5or7 = mutableListOf<BankInfo>()
    private val doesNotSupportHKKAZ5to7 = mutableListOf<BankInfo>()
    private val doesNotSupportHKCCS1 = mutableListOf<BankInfo>()

    private val supportsHhd13 = mutableListOf<BankInfo>()
    private val supportsHhd14 = mutableListOf<BankInfo>()

    private val doesNotSupportPain_001_001_or_003_03 = mutableListOf<BankInfo>()


    @Test
    fun retrieveAllBanksFinTsDetails() {

        val allBanks = bankFinder.getBankList()
        val banksSupportingFinTs = allBanks.filter { it.supportsFinTs3_0 }

        val outputFolder = File("bankData", OutputFolderDateFormat.format(Date()))
        val responsesFolder = File(outputFolder, "responses")
        responsesFolder.mkdirs()

        val csvFile = FileWriter(File(outputFolder, "bank_details.csv"))
        val csvPrinter = CSVPrinter(csvFile, CSVFormat.DEFAULT.withHeader(
            "BLZ", "Name", "Ort", "BPD", "Tanverfahren", "Technische Tanverfahrennamen", "HHD 1.3?", "HHD 1.4?",
            "HKTAN 6?", "HKTAN", "HKSAL 5?", "HKSAL", "HKKAZ 5-7?", "HKKAZ", "HKCAZ", "HKCCS 1?", "HKCCS",
            "pain.001.001.03?", "SEPA Formate", "Sprachen", "Untersstützte Geschäftsvorfälle"
        ))


        val uniqueBanks = banksSupportingFinTs.associateBy { "${it.bankCode}_${it.name}" }
        var bankIndex = 0

        uniqueBanks.forEach { bankName, bankInfo ->
            log.info("[${++bankIndex}] Getting details for $bankName ...")

            getAndSaveBankDetails(bankInfo, responsesFolder, csvPrinter)
        }

        printStatistics()

        csvPrinter.close()
        csvFile.close()
    }


    private fun getAnonymousBankInfo(bank: BankData): BankResponse {
        val dialogContext = DialogContext(bank, product)
        val requestBody = messageBuilder.createAnonymousDialogInitMessage(dialogContext)

        val anonymousBankInfoResponse = AtomicReference<BankResponse>()
        val countDownLatch = CountDownLatch(1)

        finTsClient.getAndHandleResponseForMessagePublic(requestBody, dialogContext) {
            anonymousBankInfoResponse.set(it)
            countDownLatch.countDown()
        }

        countDownLatch.await(30, TimeUnit.SECONDS)

        finTsClient.updateBankDataPublic(bank, anonymousBankInfoResponse.get())

        return anonymousBankInfoResponse.get()
    }

    private fun getAndSaveBankDetails(bankInfo: BankInfo, responsesFolder: File, csvPrinter: CSVPrinter) = runBlocking {
        val bank = BankData.anonymous(bankInfo.bankCode, bankInfo.pinTanAddress ?: "", bankInfo.bic)
        bank.bankName = bankInfo.name

        val anonymousBankInfoResponse = getAnonymousBankInfo(bank)

        File(responsesFolder, "${bankInfo.bankCode}_${bankInfo.name.replace('/', '-')}").writeText(
            anonymousBankInfoResponse.receivedSegments.joinToString(System.lineSeparator()) { it.segmentString + Separators.SegmentSeparator })

        if (anonymousBankInfoResponse.successful == false) {
            requestNotSuccessful.add(bankInfo)
            log.warn("Did not receive response from bank $bankInfo: ${anonymousBankInfoResponse.receivedSegments.joinToString(System.lineSeparator()) { it.segmentString + Separators.SegmentSeparator }}")

            return@runBlocking
        }


        val supportsHKTAN6 = supportsJobInVersion(bank, "HKTAN", 6)
        val supportsHKSAL5or7 = supportsJobInVersion(bank, "HKSAL", listOf(5, 7))
        val supportsHKKAZ5to7 = supportsJobInVersion(bank, "HKKAZ", listOf(5, 6, 7))
        val supportsHKCCS1 = supportsJobInVersion(bank, "HKCCS", 1)

        val tanInfo = anonymousBankInfoResponse.receivedSegments.filterIsInstance(TanInfo::class.java)
        val tanMethodParameters = tanInfo.flatMap { it.tanProcedureParameters.methodParameters }
        val supportedTanMethods = tanMethodParameters.map { it.technicalTanMethodIdentification }
        val hhd13Supported = supportedTanMethods.firstOrNull { it.startsWith("hhd1.3", true) } != null
        val hhd14Supported = supportedTanMethods.firstOrNull { it.startsWith("hhd1.4", true) } != null

        val supportedHKTANVersions = tanInfo.map { it.segmentVersion }
        val supportedHKSALVersions = getSupportedVersions(bank, "HKSAL")
        val supportedHKKAZVersions = getSupportedVersions(bank, "HKKAZ")
        val supportedHKCAZVersions = getSupportedVersions(bank, "HKCAZ")
        val supportedHKCCSVersions = getSupportedVersions(bank, "HKCCS")

        val sepaAccountInfoParameters = anonymousBankInfoResponse.receivedSegments.filterIsInstance<SepaAccountInfoParameters>()
        val supportedSepaFormats = sepaAccountInfoParameters.flatMap { it.supportedSepaFormats }.map { it.substring(it.indexOf(":xsd:") + ":xsd:".length) }
        val supportsPain_001_001_or_003_03 = supportedSepaFormats.firstOrNull { it.contains("pain.001.001.03") or it.contains("pain.001.003.03") } != null

        csvPrinter.printRecord(bankInfo.bankCode, bankInfo.name, bankInfo.city,
            bank.bpdVersion,
            bank.tanMethodSupportedByBank.joinToString(", ") { it.securityFunction.code + ": " + it.displayName + " (" + it.type + ")" },
            supportedTanMethods.joinToString(", "),
            hhd13Supported,
            hhd14Supported,
            supportsHKTAN6,
            supportedHKTANVersions.joinToString(", "),
            supportsHKSAL5or7,
            supportedHKSALVersions.joinToString(", "),
            supportsHKKAZ5to7,
            supportedHKKAZVersions.joinToString(", "),
            supportedHKCAZVersions.joinToString(", "),
            supportsHKCCS1,
            supportedHKCCSVersions.joinToString(", "),
            supportsPain_001_001_or_003_03,
            supportedSepaFormats.joinToString(", "),
            bank.supportedLanguages.filter { it != Dialogsprache.German }.joinToString(", ") { it.name },
            bank.supportedJobs.joinToString(", ") { it.jobName + " " + it.segmentVersion }
        )

        tanMethodParameters.forEach { methodParameter ->
            if (tanMethodParameter.containsKey(methodParameter.methodName) == false) {
                tanMethodParameter.put(methodParameter.methodName, mutableSetOf(methodParameter))
            }
            else {
                tanMethodParameter[methodParameter.methodName]?.add(methodParameter)
            }

            val tanMethodType = finTsClient.mapToTanMethodTypePublic(methodParameter)
            if (tanMethodTypes.containsKey(tanMethodType) == false) {
                tanMethodTypes.put(tanMethodType, mutableSetOf(methodParameter))
            }
            else {
                tanMethodTypes[tanMethodType]?.add(methodParameter)
            }

            tanMethodParameterTechnicalIdentification.add(methodParameter.technicalTanMethodIdentification)
            tanMethodParameterVersionDkTanMethod.add(methodParameter.versionDkTanMethod)

            if (methodParameter.smsDebitAccountRequired == SmsAbbuchungskontoErforderlich.SmsAbbuchungskontoMussAngegebenWerden) {
                requiresSmsAbbuchungskonto.add(bankInfo)
            }
            if (methodParameter.initiatorAccountRequired == AuftraggeberkontoErforderlich.AuftraggeberkontoMussAngegebenWerdenWennImGeschaeftsvorfallEnthalten) {
                requiresAuftraggeberkonto.add(bankInfo)
            }
            if (methodParameter.challengeClassRequired) {
                requiresChallengeClass.add(bankInfo)
            }
            if (methodParameter.signatureStructured) {
                signatureStructured.add(bankInfo)
            }
            if (methodParameter.nameOfTanMediumRequired == BezeichnungDesTanMediumsErforderlich.BezeichnungDesTanMediumsMussAngegebenWerden) {
                requiresNameOfTanMedia.add(bankInfo)
            }
            if (methodParameter.hhdUcResponseRequired) {
                requiresHhdUcResponse.add(bankInfo)
            }
        }

        if (supportsHKTAN6 == false) {
            doesNotSupportHKTAN6.add(bankInfo)
        }
        if (supportsHKSAL5or7 == false) {
            doesNotSupportHKSAL5or7.add(bankInfo)
        }
        if (supportsHKKAZ5to7 == false) {
            doesNotSupportHKKAZ5to7.add(bankInfo)
        }
        if (supportsHKCCS1 == false) {
            doesNotSupportHKCCS1.add(bankInfo)
        }

        if (hhd13Supported) {
            supportsHhd13.add(bankInfo)
        }
        if (hhd14Supported) {
            supportsHhd14.add(bankInfo)
        }

        if (supportsPain_001_001_or_003_03 == false) {
            doesNotSupportPain_001_001_or_003_03.add(bankInfo)
        }
    }

    private fun getSupportedVersions(bank: BankData, jobName: String): List<Int> {
        return bank.supportedJobs.filter { it.jobName == jobName }.map { it.segmentVersion }
    }

    private fun supportsJobInVersion(bank: BankData, jobName: String, version: Int): Boolean {
        return supportsJobInVersion(bank, jobName, listOf(version))
    }

    private fun supportsJobInVersion(bank: BankData, jobName: String, versions: List<Int>): Boolean {
        return bank.supportedJobs.firstOrNull { it.jobName == jobName && versions.contains(it.segmentVersion) } != null
    }


    private fun printStatistics() {
        log.info("Did not receive response from Banks ${printBanks(requestNotSuccessful)}")

        log.info("Mapped tanMethodTypes: ${tanMethodTypes.map { System.lineSeparator() + it.key + ": " + it.value.map { it.methodName + " " + it.dkTanMethod + " " + it.technicalTanMethodIdentification + " (" + it.descriptionToShowToUser + ")" }.toSet().joinToString(", ") }}\n\n")
        log.info("TanMethodParameters:${tanMethodParameter.map { System.lineSeparator() + it.key + ": " + it.value.map { it.securityFunction.code + " " + it.dkTanMethod + " " + it.technicalTanMethodIdentification + " (" + it.descriptionToShowToUser + ")" }.toSet().joinToString(", ") } }\n\n")

        log.info("TanMethodParameters TechnicalIdentification:${tanMethodParameterTechnicalIdentification.joinToString(", ") } \n\n")
        log.info("TanMethodParameters VersionDkTanMethod:${tanMethodParameterVersionDkTanMethod.joinToString(", ") } \n\n")

        log.info("Requires SmsAbbuchungskonto ${printBanks(requiresSmsAbbuchungskonto)}") // no (only 2)
        log.info("Requires Auftraggeberkonto ${printBanks(requiresAuftraggeberkonto)}") // yes, a lot of (12631)
        log.info("Requires ChallengeClass ${printBanks(requiresChallengeClass)}") // no
        log.info("Has structured signature ${printBanks(signatureStructured)}") // yes, a lot of (12651)
        log.info("Requires NameOfTanMedia ${printBanks(requiresNameOfTanMedia)}") // yes, a lot of (912)
        log.info("Requires HhdUcResponse ${printBanks(requiresHhdUcResponse)}") // no (only 2)

        log.info("Banks supporting HHD 1.3 (${supportsHhd13.size}):${printBanks(supportsHhd13)}")
        log.info("Banks supporting HHD 1.4 (${supportsHhd14.size}):${printBanks(supportsHhd14)}")

        log.info("Banks not supporting HKTAN 6 ${printBanks(doesNotSupportHKTAN6)}")
        log.info("Banks not supporting HKSAL 5 or 7 ${printBanks(doesNotSupportHKSAL5or7)}")
        log.info("Banks not supporting HKKAZ 5-7 ${printBanks(doesNotSupportHKKAZ5to7)}")
        log.info("Banks not supporting HKCCS 1 ${printBanks(doesNotSupportHKCCS1)}")

        log.info("Banks not supporting pain.001.001.03 or pain.001.003.03 ${printBanks(doesNotSupportPain_001_001_or_003_03)}")
    }

    private fun printBanks(banks: List<BankInfo>): String {
        return "(${banks.size}):${ banks.joinToString { System.lineSeparator() + it } }\n\n\n"
    }

}