package net.dankito.banking.persistence

import net.dankito.utils.multiplatform.File
import net.dankito.banking.LuceneConfig
import net.dankito.banking.LuceneConfig.Companion.AmountFieldName
import net.dankito.banking.LuceneConfig.Companion.BalanceFieldName
import net.dankito.banking.LuceneConfig.Companion.BankAccountIdFieldName
import net.dankito.banking.LuceneConfig.Companion.BookingDateFieldName
import net.dankito.banking.LuceneConfig.Companion.DateSortFieldName
import net.dankito.banking.LuceneConfig.Companion.BookingTextFieldName
import net.dankito.banking.LuceneConfig.Companion.CurrencyFieldName
import net.dankito.banking.LuceneConfig.Companion.IdFieldName
import net.dankito.banking.LuceneConfig.Companion.OtherPartyAccountIdFieldName
import net.dankito.banking.LuceneConfig.Companion.OtherPartyBankCodeFieldName
import net.dankito.banking.LuceneConfig.Companion.OtherPartyNameFieldName
import net.dankito.banking.LuceneConfig.Companion.UsageFieldName
import net.dankito.banking.ui.model.Customer
import net.dankito.banking.ui.model.AccountTransaction
import net.dankito.banking.ui.model.BankAccount
import net.dankito.banking.util.ISerializer
import net.dankito.banking.util.JacksonJsonSerializer
import net.dankito.utils.lucene.index.DocumentsWriter
import net.dankito.utils.lucene.index.FieldBuilder
import org.apache.lucene.index.IndexableField
import org.slf4j.LoggerFactory


open class LuceneBankingPersistence(
    protected val indexFolder: File,
    databaseFolder: File,
    serializer: ISerializer = JacksonJsonSerializer()
) : BankingPersistenceJson(File(databaseFolder, "accounts.json"), serializer), IBankingPersistence {

    companion object {

        // i really hate this solution, but could find no other way to avoid app crashes when
        // Android app gets restored as previous IndexWriter is not not destroyed yet and holds
        // write lock and a new IndexWriter instance in DocumentsWriter gets instantiated
        protected var documentsWriter: DocumentsWriter? = null


        private val log = LoggerFactory.getLogger(LuceneBankingPersistence::class.java)

    }


    protected val fields = FieldBuilder()


    override fun saveOrUpdateAccountTransactions(bankAccount: BankAccount, transactions: List<AccountTransaction>) {
        val writer = getWriter()

        transactions.forEach { transaction ->
            writer.updateDocumentForNonNullFields(
                IdFieldName, transaction.id,
                *createFieldsForAccountTransaction(bankAccount, transaction).toTypedArray()
            )
        }

        writer.flushChangesToDisk()
    }

    protected open fun createFieldsForAccountTransaction(bankAccount: BankAccount, transaction: AccountTransaction): List<IndexableField?> {
        return listOf(
            fields.keywordField(BankAccountIdFieldName, bankAccount.id),
            fields.nullableFullTextSearchField(OtherPartyNameFieldName, transaction.otherPartyName, true),
            fields.fullTextSearchField(UsageFieldName, transaction.usage, true),
            fields.nullableFullTextSearchField(BookingTextFieldName, transaction.bookingText, true),

            fields.nullableStoredField(OtherPartyBankCodeFieldName, transaction.otherPartyBankCode),
            fields.nullableStoredField(OtherPartyAccountIdFieldName, transaction.otherPartyAccountId),
            fields.storedField(BookingDateFieldName, transaction.bookingDate),
            fields.storedField(AmountFieldName, transaction.amount),
            fields.storedField(CurrencyFieldName, transaction.currency),
            fields.nullableStoredField(BalanceFieldName, transaction.closingBalance), // TODO: remove

            fields.sortField(DateSortFieldName, transaction.valueDate)
        )
    }


    override fun deleteAccount(customer: Customer, allCustomers: List<Customer>) {
        try {
            deleteAccountTransactions(customer.accounts)
        } catch (e: Exception) {
            log.error("Could not delete account transactions of account $customer", e)
        }

        super.deleteAccount(customer, allCustomers)
    }

    protected open fun deleteAccountTransactions(bankAccounts: List<BankAccount>) {
        val writer = getWriter()

        val bankAccountIds = bankAccounts.map { it.id }
        writer.deleteDocumentsAndFlushChangesToDisk(BankAccountIdFieldName, *bankAccountIds.toTypedArray())
    }


    @Synchronized
    protected open fun getWriter(): DocumentsWriter {
        documentsWriter?.let { return it }

        val writer = DocumentsWriter(LuceneConfig.getAccountTransactionsIndexFolder(indexFolder))

        documentsWriter = writer

        return writer
    }

}