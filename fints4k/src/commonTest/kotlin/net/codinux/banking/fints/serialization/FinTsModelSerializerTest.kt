package net.codinux.banking.fints.serialization

import net.codinux.banking.fints.messages.MessageBuilder
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Laenderkennzeichen
import net.codinux.banking.fints.messages.datenelemente.implementierte.Dialogsprache
import net.codinux.banking.fints.messages.datenelemente.implementierte.HbciVersion
import net.codinux.banking.fints.messages.datenelemente.implementierte.KundensystemStatusWerte
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMediumStatus
import net.codinux.banking.fints.model.AccountData
import net.codinux.banking.fints.model.AccountFeature
import net.codinux.banking.fints.model.BankData
import net.codinux.banking.fints.model.mapper.ModelMapper
import net.codinux.banking.fints.response.InstituteSegmentId
import net.codinux.banking.fints.response.ResponseParser
import net.codinux.banking.fints.response.segments.AccountType
import net.codinux.banking.fints.response.segments.TanInfo
import net.codinux.banking.fints.response.segments.TanMediaList
import net.codinux.banking.fints.test.assertContains
import net.codinux.banking.fints.test.assertSize
import net.codinux.banking.fints.test.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FinTsModelSerializerTest {

    companion object {
        private val bankCode = "10010010"
        private val bic = "ABCDDEBBXXX"
        private val bankName = "Abzockbank"
        private val serverAddress = "https://abzockbank.de/fints"
        private val bpd = 17

        private val customerId = "SuperUser"
        private val password = "Liebe"
        private val customerName = "Monika Superfrau"
        private val upd = 27

    }


    private val underTest = FinTsModelSerializer()


    @Test
    fun serializeToJson() {
        val bank = createTestData()

        val result = underTest.serializeToJson(bank, true)

        assertEquals(serializedFinTsData, result)
    }

    @Test
    fun deserializeFromJson() {
        val result = underTest.deserializeFromJson(serializedFinTsData)

        assertNotNull(result)

        assertSize(8, result.tanMethodsSupportedByBank)
        assertSize(4, result.tanMethodsAvailableForUser)
        assertContains(result.tanMethodsSupportedByBank, result.tanMethodsAvailableForUser) // check that it contains exactly the same object instances
        assertNotNull(result.selectedTanMethod)
        assertContains(result.tanMethodsSupportedByBank, result.selectedTanMethod) // check that it contains exactly the same object instance

        assertSize(3, result.tanMedia)
        assertNotNull(result.selectedTanMedium)
        assertContains(result.tanMedia, result.selectedTanMedium) // check that it contains exactly the same object instance

        assertSize(14, result.supportedJobs)
        assertSize(33, result.jobsRequiringTan)

        result.accounts.forEach { account ->
            assertTrue(account.allowedJobs.isNotEmpty())
            assertContains(result.supportedJobs, account.allowedJobs) // check that it contains exactly the same object instances
        }

        assertEquals(serializedFinTsData, underTest.serializeToJson(result, true))
    }


    private fun createTestData(): BankData {
        val parser = ResponseParser()
        val mapper = ModelMapper(MessageBuilder())

        val bankResponse = parser.parse("""
            HIRMS:5:2:4+3050::BPD nicht mehr aktuell, aktuelle Version enthalten.+3920::Zugelassene Zwei-Schritt-Verfahren für den Benutzer.:910:911:912:913+0020::Der Auftrag wurde ausgeführt.'
            HISALS:145:5:4+1+1'
            HISALS:12:8:4+1+1+0+J'
            HIKAZS:123:5:4+1+1+360:J:N'
            HICCSS:96:1:4+1+1+0'
            HIIPZS:22:1:4+1+1+0+;:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.001.03:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.001.09'
            DIKKUS:67:2:4+1+1+0+90:N:J'
            HITABS:153:4:4+1+1+0'
            HITAUS:154:1:4+1+1+0+N:N:J'
            HITANS:169:6:4+1+1+1+J:N:0:910:2:HHD1.3.0:::chipTAN manuell:6:1:TAN-Nummer:3:J:2:N:0:0:N:N:00:0:N:1:911:2:HHD1.3.2OPT:HHDOPT1:1.3.2:chipTAN optisch:6:1:TAN-Nummer:3:J:2:N:0:0:N:N:00:0:N:1:912:2:HHD1.3.2USB:HHDUSB1:1.3.2:chipTAN-USB:6:1:TAN-Nummer:3:J:2:N:0:0:N:N:00:0:N:1:913:2:Q1S:Secoder_UC:1.2.0:chipTAN-QR:6:1:TAN-Nummer:3:J:2:N:0:0:N:N:00:0:N:1:920:2:smsTAN:::smsTAN:6:1:TAN-Nummer:3:J:2:N:0:0:N:N:00:2:N:5:921:2:pushTAN:::pushTAN:6:1:TAN-Nummer:3:J:2:N:0:0:N:N:00:2:N:2:900:2:iTAN:::iTAN:6:1:TAN-Nummer:3:J:2:N:0:0:N:N:00:0:N:0'
            HITANS:170:7:4+1+1+1+N:N:0:922:2:pushTAN-dec:Decoupled::pushTAN 2.0:::Aufforderung:2048:J:2:N:0:0:N:N:00:2:N:2:180:1:1:J:J:923:2:pushTAN-cas:Decoupled::pushTAN 2.0:::Aufforderung:2048:J:2:N:0:0:N:N:00:2:N:5:180:1:1:J:J'
            HITAB:5:4:3+1+G:2:1234567890::::::::::SparkassenCard (Debitkarte)::::::::+G:1:1234567891::::::::::SparkassenCard (Debitkarte)::::::::'
            HITAB:5:4:3+0+A:1:::::::::::Alle Geräte::::::::'
            HIPINS:78:1:3+1+1+0+5:20:6:VR-NetKey oder Alias::HKTAN:N:HKKAZ:J:HKSAL:N:HKEKA:N:HKPAE:J:HKPSP:N:HKQTG:N:HKCSA:J:HKCSB:N:HKCSL:J:HKCCS:J:HKSPA:N:HKDSE:J:HKBSE:J:HKBME:J:HKCDL:J:HKPPD:J:HKCDN:J:HKDSB:N:HKCUB:N:HKDSW:J:HKAUB:J:HKBBS:N:HKDMB:N:HKDBS:N:HKBMB:N:HKECA:N:HKCMB:N:HKCME:J:HKCML:J:HKWDU:N:HKWPD:N:HKDME:J:HKCCM:J:HKCDB:N:HKCDE:J:HKCSE:J:HKCUM:J:HKKAU:N:HKKIF:N:HKBAZ:N:HKZDF:J:HKCAZ:J:HKDDB:N:HKDDE:J:HKDDL:J:HKDDN:J:HKKAA:N:HKPOF:N:HKIPS:N:HKIPZ:J:HKBML:J:HKBSA:J:HKBSL:J:HKDML:J:HKDSA:J:HKDSL:J:HKZDA:J:HKZDL:N:GKVPU:N:GKVPD:N'
            HISPAS:42:1:4+20+1+0+N:N:N:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.003.03:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.008.003.02:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.001.03:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.008.001.02'
            HISPAS:43:2:4+20+1+0+N:N:N:N:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.003.03:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.008.003.02:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.001.03:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.008.001.02'
            HISPAS:44:3:4+20+1+0+N:N:N:N:0:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.003.03:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.008.003.02:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.001.03:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.008.001.02'
        """.trimIndent().replace("\n", "").replace("\r", ""))

        val tanMethods = bankResponse.getSegmentsById<TanInfo>(InstituteSegmentId.TanInfo).flatMap { mapper.mapToTanMethods(it) }
        val tanMedia = bankResponse.getSegmentsById<TanMediaList>(InstituteSegmentId.TanMediaList).flatMap { it.tanMedia }

        return BankData(
            bankCode, customerId, password, serverAddress, bic, bankName, Laenderkennzeichen.Germany, bpd,
            customerId, customerName, upd,

            tanMethods, tanMethods.filter { it.securityFunction.code.startsWith("91") }, tanMethods.first { it.securityFunction.code == "912" },
            tanMedia, tanMedia.first { it.status == TanMediumStatus.Aktiv },

            listOf(Dialogsprache.German, Dialogsprache.English), Dialogsprache.German, "47", KundensystemStatusWerte.Benoetigt,
            1, listOf(HbciVersion.FinTs_3_0_0)
        ).apply {
            this.addAccount(createCheckingAccount(this, listOf("HKSAL", "HKKAZ", "HKCCS", "HKIPZ")))
            this.addAccount(createCreditCardAccount(this, listOf("DKKKU")))

            mapper.updateBankData(this, bankResponse)

            this.jobsRequiringTan = this.jobsRequiringTan.sorted().toSet() // sort them for comparability in tests
        }
    }

    private fun createCheckingAccount(bank: BankData, allowedJobNames: List<String>) =
        createAccount(bank, AccountType.Girokonto, "12345678", "Kontokorrent", allowedJobNames, AccountFeature.entries)

    private fun createCreditCardAccount(bank: BankData, allowedJobNames: List<String>) =
        createAccount(bank, AccountType.Kreditkartenkonto, "4321876521096543", "Visa-Card", allowedJobNames, listOf(AccountFeature.RetrieveAccountTransactions))

    private fun createAccount(bank: BankData, type: AccountType, accountIdentifier: String, productName: String? = null, allowedJobNames: List<String>, features: Collection<AccountFeature> = emptyList(), subAccountAttribute: String? = null) = AccountData(
        accountIdentifier, subAccountAttribute, Laenderkennzeichen.Germany, bankCode, "DE11$bankCode$accountIdentifier", customerId, type, "EUR", customerName, productName, "T:1000,:EUR", allowedJobNames, bank.supportedJobs.filter { allowedJobNames.contains(it.jobName) }
    ).apply {
        this.serverTransactionsRetentionDays = 270

        features.forEach { feature ->
            this.setSupportsFeature(feature, true)
        }
    }


    private val serializedFinTsData = """
        {
            "bankCode": "10010010",
            "customerId": "SuperUser",
            "pin": "Liebe",
            "finTs3ServerAddress": "https://abzockbank.de/fints",
            "bic": "ABCDDEBBXXX",
            "bankName": "Abzockbank",
            "countryCode": 280,
            "bpdVersion": 17,
            "userId": "SuperUser",
            "customerName": "Monika Superfrau",
            "updVersion": 27,
            "tanMethodsSupportedByBank": [
                {
                    "displayName": "chipTAN manuell",
                    "securityFunction": "PIN_TAN_910",
                    "type": "ChipTanManuell",
                    "hhdVersion": "HHD_1_3",
                    "maxTanInputLength": 6,
                    "allowedTanFormat": "Numeric"
                },
                {
                    "displayName": "chipTAN optisch",
                    "securityFunction": "PIN_TAN_911",
                    "type": "ChipTanFlickercode",
                    "hhdVersion": "HHD_1_3",
                    "maxTanInputLength": 6,
                    "allowedTanFormat": "Numeric"
                },
                {
                    "displayName": "chipTAN-USB",
                    "securityFunction": "PIN_TAN_912",
                    "type": "ChipTanUsb",
                    "hhdVersion": "HHD_1_3",
                    "maxTanInputLength": 6,
                    "allowedTanFormat": "Numeric"
                },
                {
                    "displayName": "chipTAN-QR",
                    "securityFunction": "PIN_TAN_913",
                    "type": "ChipTanQrCode",
                    "maxTanInputLength": 6,
                    "allowedTanFormat": "Numeric"
                },
                {
                    "displayName": "smsTAN",
                    "securityFunction": "PIN_TAN_920",
                    "type": "SmsTan",
                    "maxTanInputLength": 6,
                    "allowedTanFormat": "Numeric",
                    "nameOfTanMediumRequired": true
                },
                {
                    "displayName": "pushTAN",
                    "securityFunction": "PIN_TAN_921",
                    "type": "AppTan",
                    "maxTanInputLength": 6,
                    "allowedTanFormat": "Numeric",
                    "nameOfTanMediumRequired": true
                },
                {
                    "displayName": "pushTAN 2.0",
                    "securityFunction": "PIN_TAN_922",
                    "type": "DecoupledTan",
                    "nameOfTanMediumRequired": true,
                    "hktanVersion": 7,
                    "decoupledParameters": {
                        "manualConfirmationAllowed": true,
                        "periodicStateRequestsAllowed": true,
                        "maxNumberOfStateRequests": 180,
                        "initialDelayInSecondsForStateRequest": 1,
                        "delayInSecondsForNextStateRequest": 1
                    }
                },
                {
                    "displayName": "pushTAN 2.0",
                    "securityFunction": "PIN_TAN_923",
                    "type": "DecoupledTan",
                    "nameOfTanMediumRequired": true,
                    "hktanVersion": 7,
                    "decoupledParameters": {
                        "manualConfirmationAllowed": true,
                        "periodicStateRequestsAllowed": true,
                        "maxNumberOfStateRequests": 180,
                        "initialDelayInSecondsForStateRequest": 1,
                        "delayInSecondsForNextStateRequest": 1
                    }
                }
            ],
            "identifierOfTanMethodsAvailableForUser": [
                "910",
                "911",
                "912",
                "913"
            ],
            "selectedTanMethodIdentifier": "912",
            "tanMedia": [
                {
                    "mediumClass": "TanGenerator",
                    "status": "Verfuegbar",
                    "mediumName": "SparkassenCard (Debitkarte)",
                    "tanGenerator": {
                        "cardNumber": "1234567890",
                        "cardSequenceNumber": null,
                        "cardType": null,
                        "validFrom": null,
                        "validTo": null
                    }
                },
                {
                    "mediumClass": "TanGenerator",
                    "status": "Aktiv",
                    "mediumName": "SparkassenCard (Debitkarte)",
                    "tanGenerator": {
                        "cardNumber": "1234567891",
                        "cardSequenceNumber": null,
                        "cardType": null,
                        "validFrom": null,
                        "validTo": null
                    }
                },
                {
                    "mediumClass": "AlleMedien",
                    "status": "Aktiv",
                    "mediumName": "Alle Geräte"
                }
            ],
            "selectedTanMediumIdentifier": "TanGenerator SparkassenCard (Debitkarte) Aktiv 1234567891 null",
            "supportedLanguages": [
                "German",
                "English"
            ],
            "selectedLanguage": "German",
            "customerSystemId": "47",
            "customerSystemStatus": "Benoetigt",
            "countMaxJobsPerMessage": 1,
            "supportedHbciVersions": [
                "FinTs_3_0_0"
            ],
            "supportedJobs": [
                {
                    "jobName": "HKSAL",
                    "maxCountJobs": 1,
                    "minimumCountSignatures": 1,
                    "securityClass": null,
                    "segmentId": "HISALS",
                    "segmentNumber": 145,
                    "segmentVersion": 5,
                    "segmentString": "HISALS:145:5:4+1+1"
                },
                {
                    "jobName": "HKSAL",
                    "maxCountJobs": 1,
                    "minimumCountSignatures": 1,
                    "securityClass": 0,
                    "segmentId": "HISALS",
                    "segmentNumber": 12,
                    "segmentVersion": 8,
                    "segmentString": "HISALS:12:8:4+1+1+0+J"
                },
                {
                    "jobName": "HKCCS",
                    "maxCountJobs": 1,
                    "minimumCountSignatures": 1,
                    "securityClass": 0,
                    "segmentId": "HICCSS",
                    "segmentNumber": 96,
                    "segmentVersion": 1,
                    "segmentString": "HICCSS:96:1:4+1+1+0"
                },
                {
                    "jobName": "HKIPZ",
                    "maxCountJobs": 1,
                    "minimumCountSignatures": 1,
                    "securityClass": 0,
                    "segmentId": "HIIPZS",
                    "segmentNumber": 22,
                    "segmentVersion": 1,
                    "segmentString": "HIIPZS:22:1:4+1+1+0+;:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.001.03:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.001.09"
                },
                {
                    "jobName": "HKTAB",
                    "maxCountJobs": 1,
                    "minimumCountSignatures": 1,
                    "securityClass": 0,
                    "segmentId": "HITABS",
                    "segmentNumber": 153,
                    "segmentVersion": 4,
                    "segmentString": "HITABS:153:4:4+1+1+0"
                },
                {
                    "jobName": "HKTAN",
                    "maxCountJobs": 1,
                    "minimumCountSignatures": 1,
                    "securityClass": 1,
                    "segmentId": "HITANS",
                    "segmentNumber": 169,
                    "segmentVersion": 6,
                    "segmentString": "HITANS:169:6:4+1+1+1+J:N:0:910:2:HHD1.3.0:::chipTAN manuell:6:1:TAN-Nummer:3:J:2:N:0:0:N:N:00:0:N:1:911:2:HHD1.3.2OPT:HHDOPT1:1.3.2:chipTAN optisch:6:1:TAN-Nummer:3:J:2:N:0:0:N:N:00:0:N:1:912:2:HHD1.3.2USB:HHDUSB1:1.3.2:chipTAN-USB:6:1:TAN-Nummer:3:J:2:N:0:0:N:N:00:0:N:1:913:2:Q1S:Secoder_UC:1.2.0:chipTAN-QR:6:1:TAN-Nummer:3:J:2:N:0:0:N:N:00:0:N:1:920:2:smsTAN:::smsTAN:6:1:TAN-Nummer:3:J:2:N:0:0:N:N:00:2:N:5:921:2:pushTAN:::pushTAN:6:1:TAN-Nummer:3:J:2:N:0:0:N:N:00:2:N:2:900:2:iTAN:::iTAN:6:1:TAN-Nummer:3:J:2:N:0:0:N:N:00:0:N:0"
                },
                {
                    "jobName": "HKTAN",
                    "maxCountJobs": 1,
                    "minimumCountSignatures": 1,
                    "securityClass": 1,
                    "segmentId": "HITANS",
                    "segmentNumber": 170,
                    "segmentVersion": 7,
                    "segmentString": "HITANS:170:7:4+1+1+1+N:N:0:922:2:pushTAN-dec:Decoupled::pushTAN 2.0:::Aufforderung:2048:J:2:N:0:0:N:N:00:2:N:2:180:1:1:J:J:923:2:pushTAN-cas:Decoupled::pushTAN 2.0:::Aufforderung:2048:J:2:N:0:0:N:N:00:2:N:5:180:1:1:J:J"
                },
                {
                    "jobName": "HKPIN",
                    "maxCountJobs": 1,
                    "minimumCountSignatures": 1,
                    "securityClass": 0,
                    "segmentId": "HIPINS",
                    "segmentNumber": 78,
                    "segmentVersion": 1,
                    "segmentString": "HIPINS:78:1:3+1+1+0+5:20:6:VR-NetKey oder Alias::HKTAN:N:HKKAZ:J:HKSAL:N:HKEKA:N:HKPAE:J:HKPSP:N:HKQTG:N:HKCSA:J:HKCSB:N:HKCSL:J:HKCCS:J:HKSPA:N:HKDSE:J:HKBSE:J:HKBME:J:HKCDL:J:HKPPD:J:HKCDN:J:HKDSB:N:HKCUB:N:HKDSW:J:HKAUB:J:HKBBS:N:HKDMB:N:HKDBS:N:HKBMB:N:HKECA:N:HKCMB:N:HKCME:J:HKCML:J:HKWDU:N:HKWPD:N:HKDME:J:HKCCM:J:HKCDB:N:HKCDE:J:HKCSE:J:HKCUM:J:HKKAU:N:HKKIF:N:HKBAZ:N:HKZDF:J:HKCAZ:J:HKDDB:N:HKDDE:J:HKDDL:J:HKDDN:J:HKKAA:N:HKPOF:N:HKIPS:N:HKIPZ:J:HKBML:J:HKBSA:J:HKBSL:J:HKDML:J:HKDSA:J:HKDSL:J:HKZDA:J:HKZDL:N:GKVPU:N:GKVPD:N"
                }
            ],
            "supportedDetailedJobs": [
                {
                    "type": "RetrieveAccountTransactionsParameters",
                    "jobParameters": {
                        "jobName": "HKKAZ",
                        "maxCountJobs": 1,
                        "minimumCountSignatures": 1,
                        "securityClass": null,
                        "segmentId": "HIKAZS",
                        "segmentNumber": 123,
                        "segmentVersion": 5,
                        "segmentString": "HIKAZS:123:5:4+1+1+360:J:N"
                    },
                    "serverTransactionsRetentionDays": 360,
                    "settingCountEntriesAllowed": true,
                    "settingAllAccountAllowed": false
                },
                {
                    "type": "RetrieveAccountTransactionsParameters",
                    "jobParameters": {
                        "jobName": "DKKKU",
                        "maxCountJobs": 1,
                        "minimumCountSignatures": 1,
                        "securityClass": 0,
                        "segmentId": "DIKKUS",
                        "segmentNumber": 67,
                        "segmentVersion": 2,
                        "segmentString": "DIKKUS:67:2:4+1+1+0+90:N:J"
                    },
                    "serverTransactionsRetentionDays": 90,
                    "settingCountEntriesAllowed": false,
                    "settingAllAccountAllowed": true
                },
                {
                    "type": "ChangeTanMediaParameters",
                    "jobParameters": {
                        "jobName": "HKTAU",
                        "maxCountJobs": 1,
                        "minimumCountSignatures": 1,
                        "securityClass": 0,
                        "segmentId": "HITAUS",
                        "segmentNumber": 154,
                        "segmentVersion": 1,
                        "segmentString": "HITAUS:154:1:4+1+1+0+N:N:J"
                    },
                    "enteringTanListNumberRequired": false,
                    "enteringCardSequenceNumberRequired": false,
                    "enteringAtcAndTanRequired": true,
                    "enteringCardTypeAllowed": false,
                    "accountInfoRequired": false,
                    "allowedCardTypes": [
                    ]
                },
                {
                    "type": "SepaAccountInfoParameters",
                    "jobParameters": {
                        "jobName": "HKSPA",
                        "maxCountJobs": 20,
                        "minimumCountSignatures": 1,
                        "securityClass": 0,
                        "segmentId": "HISPAS",
                        "segmentNumber": 42,
                        "segmentVersion": 1,
                        "segmentString": "HISPAS:42:1:4+20+1+0+N:N:N:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.003.03:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.008.003.02:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.001.03:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.008.001.02"
                    },
                    "retrieveSingleAccountAllowed": false,
                    "nationalAccountRelationshipAllowed": false,
                    "structuredReferenceAllowed": false,
                    "settingMaxAllowedEntriesAllowed": false,
                    "countReservedReferenceLength": 0,
                    "supportedSepaFormats": [
                        "urn:iso:std:iso:20022:tech:xsd:pain.001.003.03",
                        "urn:iso:std:iso:20022:tech:xsd:pain.008.003.02",
                        "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03",
                        "urn:iso:std:iso:20022:tech:xsd:pain.008.001.02"
                    ]
                },
                {
                    "type": "SepaAccountInfoParameters",
                    "jobParameters": {
                        "jobName": "HKSPA",
                        "maxCountJobs": 20,
                        "minimumCountSignatures": 1,
                        "securityClass": 0,
                        "segmentId": "HISPAS",
                        "segmentNumber": 43,
                        "segmentVersion": 2,
                        "segmentString": "HISPAS:43:2:4+20+1+0+N:N:N:N:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.003.03:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.008.003.02:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.001.03:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.008.001.02"
                    },
                    "retrieveSingleAccountAllowed": false,
                    "nationalAccountRelationshipAllowed": false,
                    "structuredReferenceAllowed": false,
                    "settingMaxAllowedEntriesAllowed": false,
                    "countReservedReferenceLength": 0,
                    "supportedSepaFormats": [
                        "urn:iso:std:iso:20022:tech:xsd:pain.001.003.03",
                        "urn:iso:std:iso:20022:tech:xsd:pain.008.003.02",
                        "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03",
                        "urn:iso:std:iso:20022:tech:xsd:pain.008.001.02"
                    ]
                },
                {
                    "type": "SepaAccountInfoParameters",
                    "jobParameters": {
                        "jobName": "HKSPA",
                        "maxCountJobs": 20,
                        "minimumCountSignatures": 1,
                        "securityClass": 0,
                        "segmentId": "HISPAS",
                        "segmentNumber": 44,
                        "segmentVersion": 3,
                        "segmentString": "HISPAS:44:3:4+20+1+0+N:N:N:N:0:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.003.03:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.008.003.02:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.001.001.03:urn?:iso?:std?:iso?:20022?:tech?:xsd?:pain.008.001.02"
                    },
                    "retrieveSingleAccountAllowed": false,
                    "nationalAccountRelationshipAllowed": false,
                    "structuredReferenceAllowed": false,
                    "settingMaxAllowedEntriesAllowed": false,
                    "countReservedReferenceLength": 0,
                    "supportedSepaFormats": [
                        "urn:iso:std:iso:20022:tech:xsd:pain.001.003.03",
                        "urn:iso:std:iso:20022:tech:xsd:pain.008.003.02",
                        "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03",
                        "urn:iso:std:iso:20022:tech:xsd:pain.008.001.02"
                    ]
                }
            ],
            "jobsRequiringTan": [
                "HKAUB",
                "HKBME",
                "HKBML",
                "HKBSA",
                "HKBSE",
                "HKBSL",
                "HKCAZ",
                "HKCCM",
                "HKCCS",
                "HKCDE",
                "HKCDL",
                "HKCDN",
                "HKCME",
                "HKCML",
                "HKCSA",
                "HKCSE",
                "HKCSL",
                "HKCUM",
                "HKDDE",
                "HKDDL",
                "HKDDN",
                "HKDME",
                "HKDML",
                "HKDSA",
                "HKDSE",
                "HKDSL",
                "HKDSW",
                "HKIPZ",
                "HKKAZ",
                "HKPAE",
                "HKPPD",
                "HKZDA",
                "HKZDF"
            ],
            "pinInfo": {
                "minPinLength": 5,
                "maxPinLength": 20,
                "minTanLength": 6,
                "userIdHint": "VR-NetKey oder Alias",
                "customerIdHint": null
            },
            "accounts": [
                {
                    "accountIdentifier": "12345678",
                    "subAccountAttribute": null,
                    "bankCountryCode": 280,
                    "bankCode": "10010010",
                    "iban": "DE111001001012345678",
                    "customerId": "SuperUser",
                    "accountType": "Girokonto",
                    "currency": "EUR",
                    "accountHolderName": "Monika Superfrau",
                    "productName": "Kontokorrent",
                    "accountLimit": "T:1000,:EUR",
                    "allowedJobNames": [
                        "HKSAL",
                        "HKKAZ",
                        "HKCCS",
                        "HKIPZ"
                    ],
                    "serverTransactionsRetentionDays": 270,
                    "supportedFeatures": [
                        "RetrieveBalance",
                        "RetrieveAccountTransactions",
                        "TransferMoney",
                        "RealTimeTransfer"
                    ]
                },
                {
                    "accountIdentifier": "4321876521096543",
                    "subAccountAttribute": null,
                    "bankCountryCode": 280,
                    "bankCode": "10010010",
                    "iban": "DE11100100104321876521096543",
                    "customerId": "SuperUser",
                    "accountType": "Kreditkartenkonto",
                    "currency": "EUR",
                    "accountHolderName": "Monika Superfrau",
                    "productName": "Visa-Card",
                    "accountLimit": "T:1000,:EUR",
                    "allowedJobNames": [
                        "DKKKU"
                    ],
                    "serverTransactionsRetentionDays": 270,
                    "supportedFeatures": [
                        "RetrieveAccountTransactions"
                    ]
                }
            ],
            "modelVersion": "0.6.0"
        }
    """.trimIndent()
}