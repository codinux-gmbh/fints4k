import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import net.dankito.banking.fints.FinTsClientDeprecated
import net.dankito.banking.fints.callback.SimpleFinTsClientCallback
import net.dankito.banking.fints.model.AddAccountParameter
import net.dankito.banking.fints.model.RetrievedAccountData
import net.dankito.banking.fints.response.client.AddAccountResponse
import net.dankito.utils.multiplatform.extensions.*
import platform.posix.exit

fun main(args: Array<String>) {
  if (args.size < 4) {
    println("Bitte geben Sie Ihre Bankzugangsdaten ein in der Reihenfolge: <Bankleitzahl> <Login name> <Password> <FinTS Serveradresse der Bank>\r\n" +
      "Z. B.: ./fints4k.kexe 10050000 \"Mein Loginname\" GeheimesPasswort \"https://banking-be3.s-fints-pt-be.de/fints30\"")
    exit(0)
  }

  Application().retrieveAccountData(args[0], args[1], args[2], args[3])
}

class Application {

  fun retrieveAccountData(bankCode: String, customerId: String, pin: String, finTs3ServerAddress: String) {
    runBlocking {
      val client = FinTsClientDeprecated(SimpleFinTsClientCallback())

      val response = client.addAccountAsync(AddAccountParameter(bankCode, customerId, pin, finTs3ServerAddress))

      println("Retrieved response from ${response.bank.bankName} for ${response.bank.customerName}")

      displayRetrievedAccountData(response)
    }
  }

  private fun displayRetrievedAccountData(response: AddAccountResponse) {
    if (response.retrievedData.isEmpty()) {
      println()

      if (response.bank.accounts.isEmpty()) {
        println("No data retrieved")
      } else {
        println("No transactions retrieved for accounts:")
        response.bank.accounts.forEach { account -> println("- $account") }
      }
    }

    response.retrievedData.forEach { data ->
      println()
      println("${data.account}:")
      println()

      if (data.bookedTransactions.isEmpty()) {
        println("No transactions retrieved for this account")
      } else {
        displayTransactions(data)
      }
    }
  }

  private fun displayTransactions(data: RetrievedAccountData) {
    val countTransactionsDigits = data.bookedTransactions.size.numberOfDigits
    val largestAmountDigits = data.bookedTransactions.maxByOrNull { it.amount.displayString.length }?.amount?.displayString?.length ?: 0

    data.bookedTransactions.sortedByDescending { it.valueDate }.forEachIndexed { transactionIndex, transaction ->
      println("${(transactionIndex + 1).toStringWithMinDigits(countTransactionsDigits, " ")}. ${formatDate(transaction.valueDate)} " +
        "${transaction.amount.displayString.ensureMinStringLength(largestAmountDigits, " ")} ${transaction.otherPartyName ?: ""} - ${transaction.reference}")
    }
  }

  private fun formatDate(date: LocalDate): String {
    return date.dayOfMonth.toStringWithTwoDigits() + "." + date.monthNumber.toStringWithTwoDigits() + "." + date.year
  }

}