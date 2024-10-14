package net.codinux.banking.fints.serialization

import net.codinux.banking.fints.model.BankData
import net.codinux.banking.fints.response.segments.ChangeTanMediaParameters
import net.codinux.banking.fints.response.segments.JobParameters
import net.codinux.banking.fints.response.segments.RetrieveAccountTransactionsParameters
import net.codinux.banking.fints.response.segments.SepaAccountInfoParameters
import net.codinux.banking.fints.serialization.jobparameter.*
import net.codinux.log.logger

class SerializedFinTsDataMapper {

    private val log by logger()


    fun map(bank: BankData) = SerializedFinTsData(
        bank.bankCode,
        bank.customerId,
        bank.pin,
        bank.finTs3ServerAddress,
        bank.bic,

        bank.bankName,
        bank.countryCode,
        bank.bpdVersion,

        bank.userId,
        bank.customerName,
        bank.updVersion,

        bank.tanMethodsSupportedByBank,
        bank.tanMethodsAvailableForUser.map { it.securityFunction.code },
        bank.selectedTanMethod.securityFunction.code,
        bank.tanMedia,
        bank.selectedTanMedium?.identifier,

        bank.supportedLanguages,
        bank.selectedLanguage,
        bank.customerSystemId,
        bank.customerSystemStatus,

        bank.countMaxJobsPerMessage,

        bank.supportedHbciVersions,
        bank.supportedJobs.filterNot { isDetailedJobParameters(it) }.map { mapJobParameters(it) },
        bank.supportedJobs.filter { isDetailedJobParameters(it) }.mapNotNull { mapDetailedJobParameters(it) },
        bank.jobsRequiringTan,

        bank.pinInfo,

        bank.accounts
    )

    private fun isDetailedJobParameters(parameters: JobParameters): Boolean =
        parameters is RetrieveAccountTransactionsParameters
                || parameters is SepaAccountInfoParameters
                || parameters is ChangeTanMediaParameters

    private fun mapJobParameters(parameters: JobParameters) = SerializableJobParameters(
        parameters.jobName,
        parameters.maxCountJobs,
        parameters.minimumCountSignatures,
        parameters.securityClass,

        parameters.segmentId,
        parameters.segmentNumber,
        parameters.segmentVersion,

        parameters.segmentString
    )

    private fun mapDetailedJobParameters(parameters: JobParameters): DetailedSerializableJobParameters? = when (parameters) {
        is RetrieveAccountTransactionsParameters -> SerializableRetrieveAccountTransactionsParameters(mapJobParameters(parameters), parameters.serverTransactionsRetentionDays, parameters.settingCountEntriesAllowed, parameters.settingAllAccountAllowed, parameters.supportedCamtDataFormats)
        is SepaAccountInfoParameters -> SerializableSepaAccountInfoParameters(mapJobParameters(parameters), parameters.retrieveSingleAccountAllowed, parameters.nationalAccountRelationshipAllowed, parameters.structuredReferenceAllowed, parameters.settingMaxAllowedEntriesAllowed, parameters.countReservedReferenceLength, parameters.supportedSepaFormats)
        is ChangeTanMediaParameters -> SerializableChangeTanMediaParameters(mapJobParameters(parameters), parameters.enteringTanListNumberRequired, parameters.enteringCardSequenceNumberRequired, parameters.enteringAtcAndTanRequired, parameters.enteringCardTypeAllowed, parameters.accountInfoRequired, parameters.allowedCardTypes)
        else -> {
            log.warn { "${parameters::class} is said to be a DetailedJobParameters class, but found no mapping code for it" }
            null
        }
    }


    fun map(bank: SerializedFinTsData) = BankData(
        bank.bankCode,
        bank.customerId,
        bank.pin,
        bank.finTs3ServerAddress,
        bank.bic,

        bank.bankName,
        bank.countryCode,
        bank.bpdVersion,

        bank.userId,
        bank.customerName,
        bank.updVersion,

        bank.tanMethodsSupportedByBank,
        bank.tanMethodsSupportedByBank.filter { it.securityFunction.code in bank.identifierOfTanMethodsAvailableForUser },
        bank.tanMethodsSupportedByBank.first { it.securityFunction.code == bank.selectedTanMethodIdentifier },
        bank.tanMedia,
        bank.selectedTanMediumIdentifier?.let { id -> bank.tanMedia.firstOrNull { it.identifier == id } },

        bank.supportedLanguages,
        bank.selectedLanguage,
        bank.customerSystemId,
        bank.customerSystemStatus,

        bank.countMaxJobsPerMessage,

        bank.supportedHbciVersions,
        bank.supportedJobs.map { mapJobParameters(it) } + bank.supportedDetailedJobs.map { mapDetailedJobParameters(it) },
        bank.jobsRequiringTan
    ).apply {
        pinInfo = bank.pinInfo

        bank.accounts.forEach { account ->
            account.allowedJobs = this.supportedJobs.filter { it.jobName in account.allowedJobNames }
            this.addAccount(account)
        }
    }

    private fun mapJobParameters(parameters: SerializableJobParameters) = JobParameters(
        parameters.jobName,
        parameters.maxCountJobs,
        parameters.minimumCountSignatures,
        parameters.securityClass,

        parameters.segmentString
    )

    private fun mapDetailedJobParameters(parameters: DetailedSerializableJobParameters): JobParameters = when (parameters) {
        is SerializableRetrieveAccountTransactionsParameters -> RetrieveAccountTransactionsParameters(mapJobParameters(parameters.jobParameters), parameters.serverTransactionsRetentionDays, parameters.settingCountEntriesAllowed, parameters.settingAllAccountAllowed, parameters.supportedCamtDataFormats)
        is SerializableSepaAccountInfoParameters -> SepaAccountInfoParameters(mapJobParameters(parameters.jobParameters), parameters.retrieveSingleAccountAllowed, parameters.nationalAccountRelationshipAllowed, parameters.structuredReferenceAllowed, parameters.settingMaxAllowedEntriesAllowed, parameters.countReservedReferenceLength, parameters.supportedSepaFormats)
        is SerializableChangeTanMediaParameters -> ChangeTanMediaParameters(mapJobParameters(parameters.jobParameters), parameters.enteringTanListNumberRequired, parameters.enteringCardSequenceNumberRequired, parameters.enteringAtcAndTanRequired, parameters.enteringCardTypeAllowed, parameters.accountInfoRequired, parameters.allowedCardTypes)
    }

}