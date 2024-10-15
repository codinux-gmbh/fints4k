package net.dankito.banking.client.model.response

import net.codinux.banking.fints.model.BankData
import net.codinux.banking.fints.model.MessageLogEntry


open class TransferMoneyResponse(
    error: ErrorCode?,
    errorMessage: String?,
    messageLog: List<MessageLogEntry>,
    finTsModel: BankData? = null,
    serializedFinTsModel: String? = null
) : FinTsClientResponse(error, errorMessage, messageLog, finTsModel, serializedFinTsModel)