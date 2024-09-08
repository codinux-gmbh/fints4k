package net.codinux.banking.fints.model.mapper

import net.codinux.banking.fints.messages.MessageBuilder
import net.codinux.banking.fints.messages.datenelemente.implementierte.Dialogsprache
import net.codinux.banking.fints.messages.datenelemente.implementierte.KundensystemStatusWerte
import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.Sicherheitsfunktion
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.BezeichnungDesTanMediumsErforderlich
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.DkTanMethod
import net.codinux.banking.fints.messages.segmente.id.ISegmentId
import net.codinux.banking.fints.model.*
import net.codinux.banking.fints.response.BankResponse
import net.codinux.banking.fints.response.InstituteSegmentId
import net.codinux.banking.fints.response.segments.*


open class ModelMapper(
    protected open val messageBuilder: MessageBuilder // TODO: may extract class that contains common methods of ModelMapper and MessageBuilder
) {


    open fun updateBankData(bank: BankData, response: BankResponse) {
        response.getFirstSegmentById<BankParameters>(InstituteSegmentId.BankParameters)?.let { bankParameters ->
            bank.bpdVersion = bankParameters.bpdVersion
            bank.bankCode = bankParameters.bankCode
            bank.countryCode = bankParameters.bankCountryCode
            bank.countMaxJobsPerMessage = bankParameters.countMaxJobsPerMessage
            bank.supportedHbciVersions = bankParameters.supportedHbciVersions
            bank.supportedLanguages = bankParameters.supportedLanguages

            if (bank.bankName.isBlank()) {
                // just as fallback as this value contains a lot of confusing and irrelevant names (like DB24 for Deutsche Bank and Rechenzentrum Bayer. Gen. for Bavarian Raiffeisen banks)
                bank.bankName = bankParameters.bankName
            }
        }

        response.getFirstSegmentById<PinInfo>(InstituteSegmentId.PinInfo)?.let { pinInfo ->
            bank.pinInfo = pinInfo
        }

        val tanInfos = response.getSegmentsById<TanInfo>(InstituteSegmentId.TanInfo)
        if (tanInfos.isNotEmpty()) {
            bank.tanMethodsSupportedByBank = tanInfos.flatMap { tanInfo -> mapToTanMethods(tanInfo) }
        }

        response.getFirstSegmentById<CommunicationInfo>(InstituteSegmentId.CommunicationInfo)?.let { communicationInfo ->
            communicationInfo.parameters.firstOrNull { it.type == Kommunikationsdienst.Https }?.address?.let { address ->
                bank.finTs3ServerAddress = if (address.startsWith("https://", true)) address else "https://$address"
            }
        }

        response.getFirstSegmentById<SepaAccountInfo>(InstituteSegmentId.SepaAccountInfo)?.let { sepaAccountInfo ->
            sepaAccountInfo.account.bic?.let {
                bank.bic = it
            }
        }

        response.getFirstSegmentById<ChangeTanMediaParameters>(InstituteSegmentId.ChangeTanMediaParameters)?.let { parameters ->
            bank.changeTanMediumParameters = parameters
        }

        if (response.supportedJobs.isNotEmpty()) {
            bank.supportedJobs = response.supportedJobs
        }
    }

    open fun updateCustomerData(bank: BankData, response: BankResponse) {
        response.getFirstSegmentById<BankParameters>(InstituteSegmentId.BankParameters)?.let { bankParameters ->
            // TODO: ask user if there is more than one supported language? But it seems that almost all banks only support German.
            if (bank.selectedLanguage == Dialogsprache.Default && bankParameters.supportedLanguages.isNotEmpty()) {
                bank.selectedLanguage = bankParameters.supportedLanguages.first()
            }
        }

        response.getFirstSegmentById<CommunicationInfo>(InstituteSegmentId.CommunicationInfo)?.let { communicationInfo ->
            // actually it's the default language, not the selected one, but we don't offer a way to select language
            // and expect in first message, where language is set to 0 = signal that we don't know bank server's supported
            // languages, we simply use bank's default language, which is returned in BPD (and is always except one bank German anyway)
            bank.selectedLanguage = communicationInfo.defaultLanguage
        }

        response.getFirstSegmentById<ReceivedSynchronization>(InstituteSegmentId.Synchronization)?.let { synchronization ->
            synchronization.customerSystemId?.let {
                bank.customerSystemId = it
                // now that we have a Kundensystem-ID, Kundensystem-Status has to be set to 1 = Benoetigt
                bank.customerSystemStatus = KundensystemStatusWerte.Benoetigt
            }
        }

        response.getSegmentsById<AccountInfo>(InstituteSegmentId.AccountInfo).forEach { accountInfo ->
            val accountHolderName = if (accountInfo.accountHolderName2.isNullOrBlank()) accountInfo.accountHolderName1.trim() // Baader Bank adds a lot of white spaces at end
            else accountInfo.accountHolderName1.trim() + " " + accountInfo.accountHolderName2.trim()

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
                        newAccount.serverTransactionsRetentionDays = transactionsParameters.serverTransactionsRetentionDays
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
        return bank.tanMethodsSupportedByBank.firstOrNull { it.securityFunction == securityFunction }
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
            mapToTanMethod(it, tanInfo.segmentVersion)
        }
    }

    protected open fun mapToTanMethod(parameters: TanMethodParameters, hktanVersion: Int): TanMethod? {
        val methodName = parameters.methodName

        // we filter out iTAN and Einschritt-Verfahren as they are not permitted anymore according to PSD2
        if (methodName.lowercase() == "itan") {
            return null
        }

        return TanMethod(methodName, parameters.securityFunction,
            mapToTanMethodType(parameters) ?: TanMethodType.EnterTan, mapHhdVersion(parameters),
            parameters.maxTanInputLength, parameters.allowedTanFormat,
            parameters.nameOfTanMediumRequired == BezeichnungDesTanMediumsErforderlich.BezeichnungDesTanMediumsMussAngegebenWerden,
            hktanVersion, mapDecoupledTanMethodParameters(parameters))
    }

    protected open fun mapToTanMethodType(parameters: TanMethodParameters): TanMethodType? {
        val name = parameters.methodName.lowercase()

        return when {
            // names are like 'chipTAN (comfort) manuell', 'Smart(-)TAN plus (manuell)' and
            // technical identification is 'HHD'. Exception:  there's one that states itself as 'chipTAN (Manuell)'
            // but its DkTanMethod is set to 'HHDOPT1' -> handle ChipTanManuell before ChipTanFlickercode
            parameters.dkTanMethod == DkTanMethod.HHD || name.contains("manuell") ->
                TanMethodType.ChipTanManuell

            // names are like 'chipTAN optisch/comfort', 'SmartTAN (plus) optic/USB', 'chipTAN (Flicker)' and
            // technical identification is 'HHDOPT1'
            parameters.dkTanMethod == DkTanMethod.HHDOPT1 ||
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

            parameters.dkTanMethod == DkTanMethod.Decoupled -> TanMethodType.DecoupledTan

            parameters.dkTanMethod == DkTanMethod.DecoupledPush -> TanMethodType.DecoupledPushTan

            // 'flateXSecure' identifies itself as 'PPTAN' instead of 'AppTAN'
            // 'activeTAN-Verfahren' can actually be used either with an app or a reader; it's like chipTAN QR but without a chip card
            parameters.dkTanMethod == DkTanMethod.App
                    || tanMethodNameContains(name, "push", "app", "BestSign", "SecureGo", "TAN2go", "activeTAN", "easyTAN", "SecurePlus", "TAN+")
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
            parameters.versionDkTanMethod?.contains("1.4") == true -> HHDVersion.HHD_1_4
            parameters.versionDkTanMethod?.contains("1.3") == true -> HHDVersion.HHD_1_4
            else -> null
        }
    }

    protected open fun tanMethodNameContains(name: String, vararg namesToTest: String): Boolean {
        namesToTest.forEach { nameToTest ->
            if (name.contains(nameToTest.lowercase())) {
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

    protected open fun mapDecoupledTanMethodParameters(parameters: TanMethodParameters): DecoupledTanMethodParameters? {
        parameters.manualConfirmationAllowedForDecoupled?.let { manualConfirmationAllowed ->
            return DecoupledTanMethodParameters(
                manualConfirmationAllowed,
                parameters.periodicDecoupledStateRequestsAllowed ?: false, // this and the following values are all set when manualConfirmationAllowedForDecoupled is set
                parameters.maxNumberOfStateRequestsForDecoupled ?: 0,
                parameters.initialDelayInSecondsForDecoupledStateRequest ?: Int.MAX_VALUE,
                parameters.delayInSecondsForNextDecoupledStateRequests ?: Int.MAX_VALUE
            )
        }

        return null
    }


    open fun isJobSupported(bank: BankData, segmentId: ISegmentId): Boolean {
        return bank.supportedJobs.map { it.jobName }.contains(segmentId.id)
    }

    open fun isJobSupported(account: AccountData, supportedJob: JobParameters): Boolean {
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

    fun mapToActionRequiringTan(type: JobContextType): ActionRequiringTan = when(type) {
        JobContextType.AnonymousBankInfo -> ActionRequiringTan.GetAnonymousBankInfo
        JobContextType.GetTanMedia -> ActionRequiringTan.GetTanMedia
        JobContextType.ChangeTanMedium -> ActionRequiringTan.ChangeTanMedium
        JobContextType.GetAccountInfo -> ActionRequiringTan.GetAccountInfo
        // TODO: may split actions and create two JobContexts, one for GetAccountInfo and one for GetTransactions
        JobContextType.AddAccount -> ActionRequiringTan.GetTransactions
        JobContextType.GetTransactions -> ActionRequiringTan.GetTransactions
        JobContextType.TransferMoney -> ActionRequiringTan.TransferMoney
    }

}