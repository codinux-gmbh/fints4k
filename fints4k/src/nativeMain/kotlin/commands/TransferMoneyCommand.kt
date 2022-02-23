package commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import net.dankito.banking.client.model.parameter.TransferMoneyParameter
import net.dankito.banking.fints.model.AccountData
import net.dankito.banking.fints.model.Amount
import net.dankito.banking.fints.model.Currency
import net.dankito.banking.fints.model.Money


class TransferMoneyCommand : CliktCommand("Transfers money from your account to a recipient", name = "transfer", printHelpOnEmptyArgs = true) {

  val recipientName by argument("Recipient name", "Der Name des Empfängers")
  val recipientAccountIdentifier by argument("Recipient account identifier", "In den meisten Fällen die IBAN des Empfängers")
  val amount by argument("Amount", "The amount to transfer to recipient")

  val reference by argument("Reference", "Verwendungszweck (optional). Max. 160 Zeichen, keine Sonderzeichen wie Umlaute etc.")
  val recipientBankIdentifier by option("-b", "--bic", help = "Recipient's Bank Identifier, in most cases the BIC. Muss nur für Auslands-Überweisungen angegeben werden. Für deutsche Banken kann die BIC aus der IBAN abgeleitet werden.")


  val config by requireObject<Map<String, Any>>()


  override fun run() {
    val commonConfig = config[ConfigNames.CommonConfig] as CommonConfig
    val (app, bankCode, loginName, password, preferredTanMethods, abortIfRequiresTan) = commonConfig



    app.transferMoney(TransferMoneyParameter(bankCode, loginName, password, null, recipientName, recipientAccountIdentifier, recipientBankIdentifier,
      Money(Amount(amount), Currency.DefaultCurrencyCode), reference, false, preferredTanMethods, abortIfTanIsRequired = abortIfRequiresTan,
      selectAccountToUseForTransfer = { accounts ->
      selectAccountToUseForTransfer(accounts)
    }))
  }

  private fun selectAccountToUseForTransfer(accounts: List<AccountData>): AccountData? {
    println("There are multiple accounts that support money transfer. Which one would you like to use?")
    println()

    accounts.forEachIndexed { index, account ->
      println("[${index + 1}] $account")
    }

    println()
    print("Enter the index of the account: ")
    readLine()?.toIntOrNull()?.let { selectedIndex ->
      if (selectedIndex > 0 && (selectedIndex - 1) < accounts.size) {
        return accounts[selectedIndex - 1]
      }
    }

    println("Not a valid index entered. Valid indices are 1 - ${accounts.size}") // TODO: print this + "or enter an empty string to cancel" and loop for valid input?

    return null
  }

}