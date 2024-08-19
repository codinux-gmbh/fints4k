import com.soywiz.korio.file.PathInfo
import com.soywiz.korio.file.isAbsolute
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.file.std.rootLocalVfs
import com.soywiz.korio.file.std.userHomeVfs
import com.soywiz.korio.lang.substr
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.dankito.banking.client.model.AccountTransaction
import net.dankito.banking.client.model.CustomerAccount
import net.dankito.banking.client.model.parameter.GetAccountDataParameter
import net.dankito.banking.client.model.parameter.RetrieveTransactions
import net.dankito.banking.client.model.parameter.TransferMoneyParameter
import net.dankito.banking.fints.FinTsClient
import net.dankito.banking.fints.callback.SimpleFinTsClientCallback
import net.dankito.banking.fints.getAccountData
import net.dankito.banking.fints.model.TanChallenge
import net.dankito.banking.fints.transferMoney
import net.dankito.utils.multiplatform.extensions.*
import util.CsvWriter
import util.OutputFormat


class NativeApp {

  private val client = FinTsClient(SimpleFinTsClientCallback { tanChallenge -> enterTan(tanChallenge) })


  fun getAccountData(bankCode: String, loginName: String, password: String) {
    getAccountData(GetAccountDataParameter(bankCode, loginName, password))
  }

  fun getAccountData(param: GetAccountDataParameter, outputFilePath: String? = null, outputFormat: OutputFormat = OutputFormat.Json) {
    displayTypeOfDataWeAreGoingToRetrieve(param)

    val response = client.getAccountData(param)

    if (response.error != null) {
      println("An error occurred: ${response.errorCodeAndMessage}")
    }

    response.customerAccount?.let { account ->
      if (outputFilePath != null) {
        writeResponseToFile(outputFilePath, outputFormat, account)
      } else {
        println("Retrieved response from ${account.bankName} for ${account.customerName}")

      displayRetrievedAccountData(account)
      }
    }
  }

  private fun displayTypeOfDataWeAreGoingToRetrieve(param: GetAccountDataParameter) {
    if (param.retrieveTransactions != RetrieveTransactions.No) {
      val from = when {
        param.retrieveTransactions == RetrieveTransactions.OfLast90Days -> "of last 90 days"
        param.retrieveTransactionsFrom != null -> "from ${param.retrieveTransactionsFrom}"
        else -> "since the beginning of time"
      }
      val to = when {
        param.retrieveTransactions == RetrieveTransactions.OfLast90Days -> ""
        param.retrieveTransactionsTo != null -> "to ${param.retrieveTransactionsTo}"
        else -> "till today"
      }

      println("Getting ${if (param.retrieveBalance) "balance and" else ""} account transactions $from $to")
    } else {
      println("Retrieving account info ${if (param.retrieveBalance) "and balance" else ""} for ${param.bankCode} ...")
    }
  }


  fun transferMoney(param: TransferMoneyParameter) {
    val response = client.transferMoney(param)

    if (response.error != null) {
      println("Could not transfer ${param.amount} to ${param.recipientName}: ${response.errorCodeAndMessage}")
    } else {
      println("Successfully transferred ${param.amount} to ${param.recipientName}")
    }
  }


  private fun enterTan(tanChallenge: TanChallenge) {
    println("A TAN is required for ${tanChallenge.forAction}:")
    println(tanChallenge.messageToShowToUser)
    println()

    print("TAN: ")
    val enteredTan = readLine()

    if (enteredTan.isNullOrBlank()) {
      tanChallenge.userDidNotEnterTan()
    } else {
      tanChallenge.userEnteredTan(enteredTan)
    }
  }


  private fun displayRetrievedAccountData(customer: CustomerAccount) {
    if (customer.accounts.isEmpty()) {
      println()
      println("No account data retrieved")
    } else if (customer.accounts.flatMap { it.bookedTransactions }.isEmpty()) {
      println()
      println("No transactions retrieved for accounts:")
      customer.accounts.forEach { println("- $it") }
    }

    customer.accounts.forEach { account ->
      println()
      println("${account}:")
      println("${account.balance}")
      println()

      if (account.bookedTransactions.isEmpty()) {
        println("No transactions retrieved for this account")
      } else {
        displayTransactions(account.bookedTransactions)
      }
    }
  }

  private fun displayTransactions(bookedTransactions: List<AccountTransaction>) {
    val countTransactionsDigits = bookedTransactions.size.numberOfDigits
    val largestAmountDigits = bookedTransactions.maxByOrNull { it.amount.displayString.length }?.amount?.displayString?.length ?: 0

    bookedTransactions.sortedByDescending { it.valueDate }.forEachIndexed { transactionIndex, transaction ->
      println("${(transactionIndex + 1).toStringWithMinDigits(countTransactionsDigits, ' ')}. ${formatDate(transaction.valueDate)} " +
        "${transaction.amount.displayString.padStart(largestAmountDigits, ' ')} ${transaction.otherPartyName ?: ""} - ${transaction.reference}")
    }
  }

  private fun formatDate(date: LocalDate): String {
    return date.dayOfMonth.toStringWithMinDigits(2) + "." + date.monthNumber.toStringWithMinDigits(2) + "." + date.year
  }


  private fun writeResponseToFile(outputFilePath: String, outputFormat: OutputFormat, customer: CustomerAccount) {
    try {
      val outputFileInfo = PathInfo(outputFilePath)
      val outputFile = if (outputFileInfo.isAbsolute()) rootLocalVfs.get(outputFilePath)
                      else if (outputFilePath.startsWith("~/")) userHomeVfs.get(outputFilePath.substr(2))
                      else localCurrentDirVfs.get(outputFilePath)
      println("Writing file to ${outputFile.absolutePath}")

      if (outputFormat == OutputFormat.Json) {
        val json = Json.encodeToString(customer)

        runBlocking { outputFile.writeString(json) }
      } else {
        CsvWriter().writeToFile(outputFile, if (outputFormat == OutputFormat.SemicolonSeparated) ";" else ",", customer)
      }
    } catch (e: Exception) {
      println("Could not write file to $outputFilePath: $e")
    }
  }


  val Int.numberOfDigits: Int
    get() {
      var number = this
      var count = 0

      while (number != 0) {
        number /= 10
        ++count
      }

      return count
    }

}