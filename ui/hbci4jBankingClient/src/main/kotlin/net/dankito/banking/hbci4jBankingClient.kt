package net.dankito.banking

import net.dankito.banking.model.AccountCredentials
import net.dankito.banking.model.ConnectResult
import net.dankito.banking.ui.BankingClientCallback
import net.dankito.banking.ui.IBankingClient
import net.dankito.banking.ui.model.*
import net.dankito.banking.ui.model.mapper.IModelCreator
import net.dankito.banking.ui.model.parameters.GetTransactionsParameter
import net.dankito.banking.ui.model.parameters.TransferMoneyData
import net.dankito.banking.ui.model.responses.AddAccountResponse
import net.dankito.banking.ui.model.responses.BankingClientResponse
import net.dankito.banking.ui.model.responses.GetTransactionsResponse
import net.dankito.banking.util.AccountTransactionMapper
import net.dankito.banking.util.hbci4jModelMapper
import net.dankito.banking.util.*
import net.dankito.utils.ThreadPool
import net.dankito.utils.multiplatform.*
import net.dankito.utils.multiplatform.Date
import org.kapott.hbci.GV.HBCIJob
import org.kapott.hbci.GV_Result.GVRKUms
import org.kapott.hbci.GV_Result.GVRSaldoReq
import org.kapott.hbci.manager.HBCIHandler
import org.kapott.hbci.manager.HBCIUtils
import org.kapott.hbci.manager.HBCIVersion
import org.kapott.hbci.passport.AbstractHBCIPassport
import org.kapott.hbci.passport.HBCIPassport
import org.kapott.hbci.status.HBCIExecStatus
import org.kapott.hbci.structures.Value
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*


open class hbci4jBankingClient(
    protected val bank: TypedBankData,
    modelCreator: IModelCreator,
    protected val dataFolder: File,
    protected val asyncRunner: IAsyncRunner = ThreadPoolAsyncRunner(ThreadPool()),
    protected val callback: BankingClientCallback
) : IBankingClient {

    companion object {
        // the date format is hard coded in HBCIUtils.string2DateISO()
        val HbciLibDateFormat = SimpleDateFormat("yyyy-MM-dd")

        const val NinetyDaysInMilliseconds = 90 * 24 * 60 * 60 * 1000L

        private val log = LoggerFactory.getLogger(hbci4jBankingClient::class.java)
    }


    protected val credentials = AccountCredentials(bank)


    protected val mapper = hbci4jModelMapper(modelCreator)

    protected val accountTransactionMapper = AccountTransactionMapper(modelCreator)


    override val messageLogWithoutSensitiveData: List<MessageLogEntry> = listOf() // TODO: implement


    override fun addAccountAsync(callback: (AddAccountResponse) -> Unit) {
        asyncRunner.runAsync {
            callback(addAccount())
        }
    }

    open fun addAccount(): AddAccountResponse {
        val connection = connect()
        closeConnection(connection)

        if(connection.successful) {
            connection.passport?.let { passport ->
                val accounts = passport.accounts
                if (accounts == null || accounts.size == 0) {
                    log.error("Keine Konten ermittelbar")
                    return AddAccountResponse(bank, "Keine Konten ermittelbar") // TODO: translate
                }

                this.bank.accounts = mapper.mapAccounts(bank, accounts, passport)

                return tryToRetrieveAccountTransactionsForAddedAccounts(bank)
            }
        }

        return AddAccountResponse(bank, connection.error?.getInnerExceptionMessage() ?: "Could not connect")
    }

    protected open fun tryToRetrieveAccountTransactionsForAddedAccounts(bank: TypedBankData): AddAccountResponse {
        var userCancelledAction = false

        val retrievedData = bank.accounts.map { account ->
            if (account.supportsRetrievingAccountTransactions) {
                val response = getTransactionsOfLast90Days(account)

                if (response.userCancelledAction) {
                    userCancelledAction = true
                }

                response.retrievedData.first()
            }
            else {
                RetrievedAccountData.unsuccessful(account)
            }
        }

        return AddAccountResponse(bank, retrievedData, null, false, userCancelledAction)
    }


    /**
     * According to PSD2 for the account transactions of the last 90 days the two-factor authorization does not have to
     * be applied. It depends on the bank if they request a second factor or not.
     *
     * So we simply try to retrieve at account transactions of the last 90 days and see if a second factor is required
     * or not.
     */
    open fun getTransactionsOfLast90DaysAsync(account: TypedBankAccount, callback: (GetTransactionsResponse) -> Unit) {
        asyncRunner.runAsync {
            callback(getTransactionsOfLast90Days(account))
        }
    }

    /**
     * According to PSD2 for the account transactions of the last 90 days the two-factor authorization does not have to
     * be applied. It depends on the bank if they request a second factor or not.
     *
     * So we simply try to retrieve at account transactions of the last 90 days and see if a second factor is required
     * or not.
     */
    open fun getTransactionsOfLast90Days(account: TypedBankAccount): GetTransactionsResponse {
        val ninetyDaysAgo = Date(Date.today.time - NinetyDaysInMilliseconds)

        return getTransactions(GetTransactionsParameter(account, account.supportsRetrievingBalance, ninetyDaysAgo)) // TODO: implement abortIfTanIsRequired
    }

    override fun getTransactionsAsync(parameter: GetTransactionsParameter, callback: (GetTransactionsResponse) -> Unit) {
        asyncRunner.runAsync {
            callback(getTransactions(parameter))
        }
    }

    protected open fun getTransactions(parameter: GetTransactionsParameter): GetTransactionsResponse {
        val connection = connect()
        val account = parameter.account

        connection.handle?.let { handle ->
            try {
                val (nullableBalanceJob, accountTransactionsJob, status) = executeJobsForGetAccountTransactions(handle, parameter)

                // Pruefen, ob die Kommunikation mit der Bank grundsaetzlich geklappt hat
                if (!status.isOK) {
                    log.error("Could not connect to bank ${credentials.bankCode} $status: ${status.errorString}")
                    return GetTransactionsResponse(account,"Could not connect to bank ${credentials.bankCode}: $status")
                }

                // Auswertung des Saldo-Abrufs.
                var balance = BigDecimal.Zero
                if (parameter.alsoRetrieveBalance && nullableBalanceJob != null) {
                    val balanceResult = nullableBalanceJob.jobResult as GVRSaldoReq
                    if(balanceResult.isOK == false) {
                        log.error("Could not fetch balance of bank account $account: $balanceResult", balanceResult.getJobStatus().exceptions)
                        return GetTransactionsResponse(account,"Could not fetch balance of bank account $account: $balanceResult")
                    }

                    balance = balanceResult.entries[0].ready.value.bigDecimalValue.toBigDecimal()
                }


                // Das Ergebnis des Jobs koennen wir auf "GVRKUms" casten. Jobs des Typs "KUmsAll"
                // liefern immer diesen Typ.
                val result = accountTransactionsJob.jobResult as GVRKUms

                // Pruefen, ob der Abruf der Umsaetze geklappt hat
                if (result.isOK == false) {
                    log.error("Could not get fetch account transactions of bank account $account: $result", result.getJobStatus().exceptions)
                    return GetTransactionsResponse(account,"Could not fetch account transactions of bank account $account: $result")
                }

                return GetTransactionsResponse(RetrievedAccountData(account, true, balance.toBigDecimal(),
                    accountTransactionMapper.mapTransactions(account, result), listOf(), parameter.fromDate, parameter.toDate))
            }
            catch(e: Exception) {
                log.error("Could not get account transactions for bank ${credentials.bankCode}", e)
                return GetTransactionsResponse(account, e.getInnerExceptionMessage())
            }
            finally {
                closeConnection(connection)
            }
        }

        closeConnection(connection)

        return GetTransactionsResponse(account, connection.error?.getInnerExceptionMessage() ?: "Could not connect")
    }

    protected open fun executeJobsForGetAccountTransactions(handle: HBCIHandler, parameter: GetTransactionsParameter): Triple<HBCIJob?, HBCIJob, HBCIExecStatus> {
        val konto = mapper.mapToKonto(parameter.account)

        // 1. Auftrag fuer das Abrufen des Saldos erzeugen
        var balanceJob: HBCIJob? = null
        if (parameter.alsoRetrieveBalance) {
            val createdBalanceJob = handle.newJob("SaldoReq")
            createdBalanceJob.setParam("my", konto) // festlegen, welches Konto abgefragt werden soll.
            createdBalanceJob.addToQueue() // Zur Liste der auszufuehrenden Auftraege hinzufuegen

            balanceJob = createdBalanceJob
        }
        // 2. Auftrag fuer das Abrufen der Umsaetze erzeugen
        val accountTransactionsJob = handle.newJob("KUmsAll")
        accountTransactionsJob.setParam("my", konto) // festlegen, welches Konto abgefragt werden soll.
        // evtl. Datum setzen, ab welchem die Ausz??ge geholt werden sollen
        parameter.fromDate?.let {
            accountTransactionsJob.setParam("startdate", HbciLibDateFormat.format(it))
        }
        accountTransactionsJob.addToQueue() // Zur Liste der auszufuehrenden Auftraege hinzufuegen

        // Hier koennen jetzt noch weitere Auftraege fuer diesen Bankzugang hinzugefuegt
        // werden. Z.Bsp. Ueberweisungen.

        // Alle Auftraege aus der Liste ausfuehren.
        val status = handle.execute()

        return Triple(balanceJob, accountTransactionsJob, status)
    }


    override fun transferMoneyAsync(data: TransferMoneyData, callback: (BankingClientResponse) -> Unit) {
        asyncRunner.runAsync {
            callback(transferMoney(data))
        }
    }

    open fun transferMoney(data: TransferMoneyData): BankingClientResponse {
        val connection = connect()

        connection.handle?.let { handle ->
            try {
                createTransferCashJob(handle, data)

                val status = handle.execute()

                return BankingClientResponse(status.isOK, status.toString())
            } catch(e: Exception) {
                log.error("Could not transfer cash for account ${data.account}" , e)
                return BankingClientResponse(false, e.getInnerExceptionMessage())
            }
            finally {
                closeConnection(connection)
            }
        }

        return BankingClientResponse(false, connection.error?.getInnerExceptionMessage() ?: "Could not connect")
    }

    protected open fun createTransferCashJob(handle: HBCIHandler, data: TransferMoneyData) {
        // TODO: implement real-time transfer
        val transferCashJob = handle.newJob("UebSEPA")

        val source = mapper.mapToKonto(data.account)
        val destination = mapper.mapToKonto(data)
        val amount = Value(data.amount, "EUR")

        transferCashJob.setParam("src", source)
        transferCashJob.setParam("dst", destination)
        transferCashJob.setParam("btg", amount)
        transferCashJob.setParam("usage", data.reference)

        transferCashJob.addToQueue()
    }


    override fun dataChanged(bank: TypedBankData) {
        if (bank.bankCode != credentials.bankCode || bank.userName != credentials.customerId || bank.password != credentials.password) {
            getPassportFile(credentials).delete()
        }

        credentials.bankCode = bank.bankCode
        credentials.customerId = bank.userName
        credentials.password = bank.password
    }

    override fun deletedBank(bank: TypedBankData, wasLastAccountWithThisCredentials: Boolean) {
        getPassportFile(credentials).delete()
    }


    protected open fun connect(): ConnectResult {
        return connect(credentials, HBCIVersion.HBCI_300)
    }

    protected open fun connect(credentials: AccountCredentials, version: HBCIVersion): ConnectResult {
        // HBCI4Java initialisieren
        // In "props" koennen optional Kernel-Parameter abgelegt werden, die in der Klasse
        // org.kapott.hbci.manager.HBCIUtils (oben im Javadoc) beschrieben sind.
        val props = Properties()
        HBCIUtils.init(props, HbciCallback(credentials, bank, mapper, callback))

        // In der Passport-Datei speichert HBCI4Java die Daten des Bankzugangs (Bankparameterdaten, Benutzer-Parameter, etc.).
        // Die Datei kann problemlos geloescht werden. Sie wird beim naechsten mal automatisch neu erzeugt,
        // wenn der Parameter "client.passport.PinTan.init" den Wert "1" hat (siehe unten).
        // Wir speichern die Datei der Einfachheit halber im aktuellen Verzeichnis.
        val passportFile = getPassportFile(credentials)

        // Wir setzen die Kernel-Parameter zur Laufzeit. Wir koennten sie alternativ
        // auch oben in "props" setzen.
        HBCIUtils.setParam("client.passport.default", "PinTan") // Legt als Verfahren PIN/TAN fest.
        HBCIUtils.setParam("client.passport.PinTan.filename", passportFile.absolutePath)
        HBCIUtils.setParam("client.passport.PinTan.init", "1")

        var handle: HBCIHandler? = null
        var passport: HBCIPassport? = null

        try {
            // Erzeugen des Passport-Objektes.
            passport = AbstractHBCIPassport.getInstance()

            // Konfigurieren des Passport-Objektes.
            // Das kann alternativ auch alles ueber den Callback unten geschehen

            // Das Land.
            passport.country = "DE"

            // Server-Adresse angeben. Koennen wir entweder manuell eintragen oder direkt von HBCI4Java ermitteln lassen
            val info = HBCIUtils.getBankInfo(credentials.bankCode)
            passport.host = info.pinTanAddress

            // TCP-Port des Servers. Bei PIN/TAN immer 443, da das ja ueber HTTPS laeuft.
            passport.port = 443

            // Art der Nachrichten-Codierung. Bei Chipkarte/Schluesseldatei wird
            // "None" verwendet. Bei PIN/TAN kommt "Base64" zum Einsatz.
            passport.filterType = "Base64"

            // Verbindung zum Server aufbauen
            handle = HBCIHandler(version.getId(), passport)


        }
        catch(e: Exception) {
            log.error("Could not connect to bank ${credentials.bankCode}", e)
            closeConnection(handle, passport)

            return ConnectResult(false, e)
        }

        return ConnectResult(true, null, handle, passport)
    }

    protected open fun getPassportFile(credentials: AccountCredentials): File {
        val hbciClientFolder = File(dataFolder, "hbci4j-client")
        hbciClientFolder.mkdirs()

        return File(hbciClientFolder, "passport_${credentials.bankCode}_${credentials.customerId}.dat")
    }

    protected open fun closeConnection(connection: ConnectResult) {
        closeConnection(connection.handle, connection.passport)
    }

    protected open fun closeConnection(handle: HBCIHandler?, passport: HBCIPassport?) {
        // Sicherstellen, dass sowohl Passport als auch Handle nach Beendigung geschlossen werden.
        try {
            handle?.close()

            passport?.close()

            HBCIUtils.doneThread() // i hate static variables, here's one of the reasons why: Old callbacks and therefore credentials get stored in static variables and therefor always the first entered credentials have been used
        } catch(e: Exception) { log.error("Could not close connection", e) }
    }

}