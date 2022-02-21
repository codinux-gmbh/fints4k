import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import net.dankito.banking.client.model.AccountTransaction
import net.dankito.banking.client.model.CustomerAccount
import net.dankito.banking.client.model.parameter.GetAccountDataParameter
import net.dankito.banking.fints.FinTsClient
import net.dankito.banking.fints.callback.SimpleFinTsClientCallback
import net.dankito.banking.fints.getAccountData
import net.dankito.banking.fints.model.TanChallenge
import net.dankito.utils.multiplatform.extensions.*
import platform.posix.exit

fun main(args: Array<String>) {
  if (args.size < 3) {
    println("Bitte geben Sie Ihre Bankzugangsdaten ein in der Reihenfolge: <Bankleitzahl> <Login name> <Password>\r\n" +
      "Z. B.: ./fints4k.kexe 10050000 \"Mein Loginname\" GeheimesPasswort")
    exit(0)
  }

  Application().retrieveAccountData(args[0], args[1], args[2])
}

class Application {

  fun retrieveAccountData(bankCode: String, loginName: String, password: String) {
    val client = FinTsClient(SimpleFinTsClientCallback { tanChallenge -> enterTan(tanChallenge) })

    val response = client.getAccountData(GetAccountDataParameter(bankCode, loginName, password))

    if (response.error != null) {
      println("An error occurred: ${response.error}${response.errorMessage?.let { " $it" }}")
    }

    response.customerAccount?.let { account ->
      println("Retrieved response from ${account.bankName} for ${account.customerName}")

      displayRetrievedAccountData(account)
    }
  }


  private fun enterTan(tanChallenge: TanChallenge) {
    println("A TAN is required:")
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
      println("${(transactionIndex + 1).toStringWithMinDigits(countTransactionsDigits, " ")}. ${formatDate(transaction.valueDate)} " +
        "${transaction.amount.displayString.ensureMinStringLength(largestAmountDigits, " ")} ${transaction.otherPartyName ?: ""} - ${transaction.reference}")
    }
  }

  private fun formatDate(date: LocalDate): String {
    return date.dayOfMonth.toStringWithTwoDigits() + "." + date.monthNumber.toStringWithTwoDigits() + "." + date.year
  }

}