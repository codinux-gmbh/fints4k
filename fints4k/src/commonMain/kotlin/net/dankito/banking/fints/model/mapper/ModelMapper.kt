package net.dankito.banking.fints.model.mapper

import net.dankito.banking.fints.messages.MessageBuilder
import net.dankito.banking.fints.messages.datenelemente.implementierte.Dialogsprache
import net.dankito.banking.fints.messages.datenelemente.implementierte.KundensystemStatusWerte
import net.dankito.banking.fints.messages.datenelemente.implementierte.signatur.Sicherheitsfunktion
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.BezeichnungDesTanMediumsErforderlich
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.DkTanMethod
import net.dankito.banking.fints.messages.segmente.id.ISegmentId
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.BankResponse
import net.dankito.banking.fints.response.InstituteSegmentId
import net.dankito.banking.fints.response.segments.*


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

    open fun updateCustomerData(bank: BankData, response: BankResponse) {
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
            parameters.nameOfTanMediumRequired == BezeichnungDesTanMediumsErforderlich.BezeichnungDesTanMediumsMussAngegebenWerden,
            mapDecoupledTanMethodParameters(parameters))
    }

    protected open fun mapToTanMethodType(parameters: TanMethodParameters): TanMethodType? {
        val name = parameters.methodName.toLowerCase()

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

    protected open fun mapDecoupledTanMethodParameters(parameters: TanMethodParameters): DecoupledTanMethodParameters? {
        parameters.manualConfirmationAllowedForDecoupled?.let { manualConfirmationAllowed ->
            return DecoupledTanMethodParameters(
                manualConfirmationAllowed,
                parameters.periodicStateRequestsAllowedForDecoupled ?: false, // this and the following values are all set when manualConfirmationAllowedForDecoupled is set
                parameters.maxNumberOfStateRequestsForDecoupled ?: 0,
                parameters.initialDelayInSecondsForStateRequestsForDecoupled ?: Int.MAX_VALUE,
                parameters.delayInSecondsForNextStateRequestsForDecoupled ?: Int.MAX_VALUE
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

}