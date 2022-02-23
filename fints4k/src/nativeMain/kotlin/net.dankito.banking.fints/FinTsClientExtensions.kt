package net.dankito.banking.fints

import kotlinx.coroutines.runBlocking
import net.dankito.banking.client.model.parameter.GetAccountDataParameter
import net.dankito.banking.client.model.parameter.TransferMoneyParameter
import net.dankito.banking.client.model.response.GetAccountDataResponse
import net.dankito.banking.client.model.response.TransferMoneyResponse


fun FinTsClient.getAccountData(bankCode: String, loginName: String, password: String): GetAccountDataResponse {
  return runBlocking { getAccountDataAsync(bankCode, loginName, password) }
}

fun FinTsClient.getAccountData(param: GetAccountDataParameter): GetAccountDataResponse {
  return runBlocking { getAccountDataAsync(param) }
}


fun FinTsClient.transferMoney(param: TransferMoneyParameter): TransferMoneyResponse {
  return runBlocking { transferMoneyAsync(param) }
}