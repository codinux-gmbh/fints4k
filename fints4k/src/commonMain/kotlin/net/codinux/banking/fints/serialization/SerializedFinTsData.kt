package net.codinux.banking.fints.serialization

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.codinux.banking.fints.messages.datenelemente.implementierte.*
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMedium
import net.codinux.banking.fints.model.AccountData
import net.codinux.banking.fints.model.PinInfo
import net.codinux.banking.fints.model.TanMethod
import net.codinux.banking.fints.serialization.jobparameter.DetailedSerializableJobParameters
import net.codinux.banking.fints.serialization.jobparameter.SerializableJobParameters

@OptIn(ExperimentalSerializationApi::class)
@Serializable
class SerializedFinTsData(
    val bankCode: String,
    val customerId: String,
    val pin: String,
    val finTs3ServerAddress: String,
    val bic: String,

    val bankName: String,
    val countryCode: Int,
    val bpdVersion: Int,

    val userId: String,
    val customerName: String,
    val updVersion: Int,

    val tanMethodsSupportedByBank: List<TanMethod>,
    val identifierOfTanMethodsAvailableForUser: List<String> = listOf(),
    val selectedTanMethodIdentifier: String,
    val tanMedia: List<TanMedium> = listOf(),
    val selectedTanMediumIdentifier: String? = null,

    val supportedLanguages: List<Dialogsprache> = listOf(),
    val selectedLanguage: Dialogsprache = Dialogsprache.Default,
    val customerSystemId: String = KundensystemID.Anonymous,
    val customerSystemStatus: KundensystemStatusWerte = KundensystemStatus.SynchronizingCustomerSystemId,

    val countMaxJobsPerMessage: Int = 0,

    val supportedHbciVersions: List<HbciVersion> = listOf(),
    val supportedJobs: List<SerializableJobParameters> = listOf(),
    val supportedDetailedJobs: List<DetailedSerializableJobParameters> = listOf(),
    val jobsRequiringTan: Set<String> = emptySet(),

    val pinInfo: PinInfo? = null,

    val accounts: List<AccountData>
) {

    @EncodeDefault
    private val modelVersion: String = "0.6.0"

}