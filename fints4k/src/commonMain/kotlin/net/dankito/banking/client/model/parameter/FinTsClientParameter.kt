package net.dankito.banking.client.model.parameter

import net.codinux.banking.fints.model.BankData
import net.codinux.banking.fints.model.TanMethodType
import net.dankito.banking.client.model.CustomerCredentials


// TODO: Rename to BankingClientRequest(Base)?
open class FinTsClientParameter(
  bankCode: String,
  loginName: String,
  password: String,

  open val preferredTanMethods: List<TanMethodType>? = null,
  open val tanMethodsNotSupportedByApplication: List<TanMethodType>? = null,
  open val preferredTanMedium: String? = null, // the ID of the medium
  open val abortIfTanIsRequired: Boolean = false,
  open val finTsModel: BankData? = null
) : CustomerCredentials(bankCode, loginName, password)