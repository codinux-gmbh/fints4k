package net.codinux.banking.fints.model

import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Laenderkennzeichen
import net.codinux.banking.fints.messages.datenelemente.implementierte.*
import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.Sicherheitsfunktion
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMedium
import net.codinux.banking.fints.messages.segmente.id.ISegmentId
import net.codinux.banking.fints.response.segments.ChangeTanMediaParameters
import net.codinux.banking.fints.response.segments.JobParameters


open class BankData(
    open var bankCode: String,
    open var customerId: String,
    open var pin: String,
    open var finTs3ServerAddress: String,
    open var bic: String,

    open var bankName: String = "",
    open var countryCode: Int = Laenderkennzeichen.Germany, // TODO: currently we only support German banks. But change this if ever other countries get supported
    open var bpdVersion: Int = BPDVersion.VersionNotReceivedYet,

    open var userId: String = customerId,
    open var customerName: String = "",
    open var updVersion: Int = UPDVersion.VersionNotReceivedYet,

    open var tanMethodsSupportedByBank: List<TanMethod> = listOf(),
    open var tanMethodsAvailableForUser: List<TanMethod> = listOf(),
    open var selectedTanMethod: TanMethod = TanMethodNotSelected,
    open var tanMedia: List<TanMedium> = listOf(),
    open var selectedTanMedium: TanMedium? = null,

    open var supportedLanguages: List<Dialogsprache> = listOf(),
    open var selectedLanguage: Dialogsprache = Dialogsprache.Default,
    open var customerSystemId: String = KundensystemID.Anonymous,
    open var customerSystemStatus: KundensystemStatusWerte = KundensystemStatus.SynchronizingCustomerSystemId,

    /**
     * Maximale Anzahl an Geschäftsvorfallsarten, die pro Nachricht zulässig ist.
     * Der Wert ‚0’ gibt an, dass keine Restriktionen bzgl. der Anzahl an Geschäftsvorfallsarten bestehen.
     */
    open var countMaxJobsPerMessage: Int = 0,

    open var supportedHbciVersions: List<HbciVersion> = listOf(),
    open var supportedJobs: List<JobParameters> = listOf(),
    open var jobsRequiringTan: Set<String> = emptySet()
) {

    companion object {
        val SecurityFunctionNotSelected = Sicherheitsfunktion.Einschritt_Verfahren

        val TanMethodNotSelected = TanMethod("NOT_SELECTED", SecurityFunctionNotSelected, TanMethodType.EnterTan)

        // TODO: is the BIC really needed at anonymous dialog init?
        fun anonymous(bankCode: String, finTs3ServerAddress: String, bic: String): BankData {
            return BankData(bankCode, KundenID.Anonymous, "", finTs3ServerAddress, bic, customerSystemStatus = KundensystemStatusWerte.NichtBenoetigt)
        }
    }


    internal constructor() : this("", "", "", "", "") // for object deserializers


    open var pinInfo: PinInfo? = null

    open val changeTanMediumParameters: ChangeTanMediaParameters?
        get() = supportedJobs.filterIsInstance<ChangeTanMediaParameters>().firstOrNull()


    protected open val _accounts = mutableListOf<AccountData>()

    open val accounts: List<AccountData>
        get() = ArrayList(_accounts)


    open val isTanMethodSelected: Boolean
        get() = selectedTanMethod != TanMethodNotSelected


    open fun addAccount(account: AccountData) {
        _accounts.add(account)
    }

    open fun removeAccount(account: AccountData) {
        _accounts.remove(account)
    }


    open fun doesJobRequireTan(segmentId: ISegmentId): Boolean = doesJobRequireTan(segmentId.id)

    open fun doesJobRequireTan(segmentId: String): Boolean =
        jobsRequiringTan.contains(segmentId)

    /**
     * Some banks use a special bank code for online banking that doesn't match bank's bank code, e. g. Hypo Vereinsbank
     */
    open val bankCodeForOnlineBanking: String
        get() {
            // for UniCredit / HypVereinsbank for online banking '70020270' has to be used as bank code
            if (bankName.contains("unicredit", true) || bic.startsWith("HYVEDE")) {
                return "70020270"
            }

            return bankCode
        }


    open fun resetBpdVersion() {
        bpdVersion = BPDVersion.VersionNotReceivedYet
    }

    open fun resetUpdVersion() {
        updVersion = UPDVersion.VersionNotReceivedYet
    }

    open fun resetSelectedTanMethod() {
        selectedTanMethod = TanMethodNotSelected
    }


    override fun toString(): String {
        return "$bankCode $customerId"
    }

}