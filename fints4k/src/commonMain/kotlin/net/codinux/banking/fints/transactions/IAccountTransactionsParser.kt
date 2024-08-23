package net.codinux.banking.fints.transactions

import net.codinux.banking.fints.log.IMessageLogAppender
import net.codinux.banking.fints.model.AccountData
import net.codinux.banking.fints.model.AccountTransaction
import net.codinux.banking.fints.model.BankData


interface IAccountTransactionsParser {

    var logAppender: IMessageLogAppender?


    fun parseTransactions(transactionsString: String, bank: BankData, account: AccountData): List<AccountTransaction>

    fun parseTransactionsChunk(transactionsChunk: String, bank: BankData, account: AccountData): Pair<List<AccountTransaction>, String>

}