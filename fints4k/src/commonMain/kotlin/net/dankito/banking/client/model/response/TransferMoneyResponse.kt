package net.dankito.banking.client.model.response

import net.dankito.banking.fints.model.BankData
import net.dankito.banking.fints.model.MessageLogEntry


open class TransferMoneyResponse(
  error: ErrorCode?,
  errorMessage: String?,
  messageLogWithoutSensitiveData: List<MessageLogEntry>,
  finTsModel: BankData? = null
) : FinTsClientResponse(error, errorMessage, messageLogWithoutSensitiveData, finTsModel)