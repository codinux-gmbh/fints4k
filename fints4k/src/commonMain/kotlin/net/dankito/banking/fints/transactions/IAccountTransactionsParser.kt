package net.dankito.banking.fints.transactions

import net.dankito.banking.fints.log.IMessageLogAppender
import net.dankito.banking.fints.model.AccountData
import net.dankito.banking.fints.model.AccountTransaction
import net.dankito.banking.fints.model.BankData


interface IAccountTransactionsParser {

    var logAppender: IMessageLogAppender?


    fun parseTransactions(transactionsString: String, bank: BankData, account: AccountData): List<AccountTransaction>

    fun parseTransactionsChunk(transactionsChunk: String, bank: BankData, account: AccountData): Pair<List<AccountTransaction>, String>

}