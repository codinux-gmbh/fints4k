package commands

import NativeApp
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import net.dankito.banking.client.model.parameter.GetAccountDataParameter
import net.dankito.banking.client.model.parameter.RetrieveTransactions
import net.dankito.banking.fints.model.TanMethodType
import net.dankito.utils.multiplatform.extensions.todayAtEuropeBerlin


class fints4kCommandLineInterface : CliktCommand(name = "fints", printHelpOnEmptyArgs = true, invokeWithoutSubcommand = true) {
  init {
    versionOption("1.0.0 Alpha-10", names = setOf("-v", "--version"))
  }

  val bankCode by argument("Bankleitzahl", "Die Bankleitzahl deiner Bank")
  val loginName by argument("Loginname", "Dein Onlinebanking Loginname / Anmeldename")
  val password by argument("Passwort", "Dein Onlinebanking Passwort")
//  val password by option("-p", "--password", help = "Dein Onlinebanking Passwort").prompt("Passwort", hideInput = true)


  val retrieveBalance by option("-b", "--balance", help = "Den Kontostand abrufen. Defaults to true").flag(default = true)
  val retrieveTransactions by option("-t", "--transactions", help = "Die Kontoumsätze abrufen. Default: OfLast90Days. As most banks don't afford a TAN to get transactions of last 90 days")
    .enum<RetrieveTransactions>().default(RetrieveTransactions.OfLast90Days)

  val retrieveTransactionsFrom by option("--from", help = "The day as ISO date from which transactions should be retrieved like 2022-02-22. If set 'retrieveTransactions' gets set to '${RetrieveTransactions.AccordingToRetrieveFromAndTo}'")
    .validate { it.isNullOrBlank() || LocalDate.parse(it) != null }
  val retrieveTransactionsTo by option("--to", help = "The day as ISO date up to which account transactions are to be received like 2022-02-22. If set 'retrieveTransactions' gets set to '${RetrieveTransactions.AccordingToRetrieveFromAndTo}'")
    .validate { it.isNullOrBlank() || LocalDate.parse(it) != null }
  val retrieveTransactionsForLastNDays by option("-l", "--last-n-days", help = "Retrieve transactions for last n days. If set 'retrieveTransactions' gets set to '${RetrieveTransactions.AccordingToRetrieveFromAndTo}' and 'retrieveTransactionsFrom' will be ignored.").int()


  val preferredTanMethods by option("-m", "--tan-method", help = "Your preferred TAN methods to use if action affords a TAN. Can be repeated like '-m AppTan -m SmsTan'").enum<TanMethodType>().multiple()

  val abortIfRequiresTan by option("-a", "--abort-if-requires-tan", help = "If actions should be aborted if it affords a TAN. Defaults to false").flag(default = false)


  val config by findOrSetObject { mutableMapOf<String, Any>() }


  override fun run() {
    config[ConfigNames.CommonConfig] = CommonConfig(NativeApp(), bankCode, loginName, password, preferredTanMethods, abortIfRequiresTan)

    if (currentContext.invokedSubcommand == null) { // if no subcommand is selected execute default command = getAccountData()
      getAccountData()
    }
  }

  private fun getAccountData() {
    val commonConfig = config[ConfigNames.CommonConfig] as CommonConfig
    val (app, bankCode, loginName, password, preferredTanMethods, abortIfRequiresTan) = commonConfig


    val retrieveTransactionsFromDate = when { // retrieveTransactionsForLastNDays takes precedence over retrieveTransactionsFrom
      retrieveTransactionsForLastNDays != null -> LocalDate.todayAtEuropeBerlin().minus(retrieveTransactionsForLastNDays!!, DateTimeUnit.DAY)
      retrieveTransactionsFrom.isNullOrBlank() -> null
      else -> LocalDate.parse(retrieveTransactionsFrom!!) // retrieveTransactionsFrom must then be != null
    }

    val retrieveTransactionsToDate = if (retrieveTransactionsTo.isNullOrBlank()) null else LocalDate.parse(retrieveTransactionsTo!!)
    val effectiveRetrieveTransactions = if (retrieveTransactionsFromDate != null || retrieveTransactionsToDate != null) RetrieveTransactions.AccordingToRetrieveFromAndTo
    else retrieveTransactions


    app.getAccountData(GetAccountDataParameter(bankCode, loginName, password, null, retrieveBalance, effectiveRetrieveTransactions,
      retrieveTransactionsFromDate, retrieveTransactionsToDate, preferredTanMethods, abortIfTanIsRequired = abortIfRequiresTan))
  }

}