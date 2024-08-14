package net.dankito.banking.client.model.response

import net.dankito.banking.fints.model.BankData
import net.dankito.banking.fints.model.MessageLogEntry


// TODO: rename to BankingClientResponse?
open class FinTsClientResponse(
  open val error: ErrorCode?,
  open val errorMessage: String?,
  open val messageLogWithoutSensitiveData: List<MessageLogEntry>,
  open val finTsModel: BankData? = null
) {

  internal constructor() : this(null, null, listOf()) // for object deserializers


  open val successful: Boolean
    get() = error == null

  open val errorCodeAndMessage: String
    get() = "$error${errorMessage?.let { " $it" }}"

}