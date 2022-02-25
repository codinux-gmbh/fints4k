package net.dankito.banking.fints

import kotlinx.coroutines.runBlocking
import net.dankito.banking.client.model.parameter.GetAccountDataParameter
import net.dankito.banking.client.model.parameter.TransferMoneyParameter
import net.dankito.banking.client.model.response.GetAccountDataResponse
import net.dankito.banking.client.model.response.TransferMoneyResponse
import net.dankito.banking.fints.model.Money


fun FinTsClient.getAccountData(bankCode: String, loginName: String, password: String): GetAccountDataResponse {
  return runBlocking { getAccountDataAsync(bankCode, loginName, password) }
}

fun FinTsClient.getAccountData(param: GetAccountDataParameter): GetAccountDataResponse {
  return runBlocking { getAccountDataAsync(param) }
}


fun FinTsClient.transferMoney(bankCode: String, loginName: String, password: String, recipientName: String, recipientAccountIdentifier: String,
                                    amount: Money, reference: String? = null): TransferMoneyResponse {
  return runBlocking { transferMoneyAsync(bankCode, loginName, password, recipientName, recipientAccountIdentifier, amount, reference) }
}

fun FinTsClient.transferMoney(param: TransferMoneyParameter): TransferMoneyResponse {
  return runBlocking { transferMoneyAsync(param) }
}