package util

import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.VfsOpenMode
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.writeString
import kotlinx.coroutines.runBlocking
import net.dankito.banking.client.model.AccountTransaction
import net.dankito.banking.client.model.BankAccount
import net.dankito.banking.client.model.CustomerAccount


/**
 * A very basic implementation of a CSV writer. Do not use in production
 */
open class CsvWriter {

  companion object {
    const val NewLine = "\r\n"
  }


  open fun writeToFile(outputFile: VfsFile, valueSeparator: String, customer: CustomerAccount) {
    runBlocking {
      val stream = outputFile.open(VfsOpenMode.CREATE_OR_TRUNCATE)

      // print header
      stream.writeString(listOf("Bank", "Account", "Date", "Amount", "Currency", "Booking text", "Reference", "Other party name", "Other party bank id", "Other party account id").joinToString(valueSeparator))
      stream.writeString(NewLine)

      customer.accounts.forEach { writeToFile(stream, valueSeparator, customer, it) }

      stream.close()
    }
  }

  protected open suspend fun writeToFile(stream: AsyncStream, valueSeparator: String, customer: CustomerAccount, account: BankAccount) {
    account.bookedTransactions.forEach { writeToFile(stream, valueSeparator, customer, account, it) }
  }

  protected open suspend fun writeToFile(stream: AsyncStream, valueSeparator: String, customer: CustomerAccount, account: BankAccount, transaction: AccountTransaction) {
    val amount = if (valueSeparator == ";") transaction.amount.amount.string.replace('.', ',') else transaction.amount.amount.string.replace(',', '.')

    stream.writeString(listOf(customer.bankName, account.identifier, transaction.valueDate, amount, transaction.amount.currency, ensureNotNull(transaction.postingText), wrap(transaction.reference ?: ""),
      ensureNotNull(transaction.otherPartyName), ensureNotNull(transaction.otherPartyBankId), ensureNotNull(transaction.otherPartyAccountId)).joinToString(valueSeparator))

    stream.writeString(NewLine)
  }

  /**
   * Wraps values that potentially contain the value separator
   */
  protected open fun wrap(value: String): String {
    return "\"$value\""
  }

  /**
   * Ensures that 'null' doesn't get written to output
   */
  protected open fun ensureNotNull(value: Any?): Any {
    return value ?: ""
  }

}