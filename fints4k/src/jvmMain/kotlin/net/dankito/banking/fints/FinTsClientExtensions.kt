package net.dankito.banking.fints

import kotlinx.coroutines.runBlocking
import net.dankito.banking.client.model.parameter.GetAccountDataParameter
import net.dankito.banking.client.model.response.GetAccountDataResponse


fun FinTsClient.getAccountData(bankCode: String, loginName: String, password: String): GetAccountDataResponse {
  return runBlocking { getAccountDataAsync(bankCode, loginName, password) }
}

fun FinTsClient.getAccountData(param: GetAccountDataParameter): GetAccountDataResponse {
  return runBlocking { getAccountDataAsync(param) }
}