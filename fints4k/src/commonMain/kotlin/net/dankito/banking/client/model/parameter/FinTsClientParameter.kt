package net.dankito.banking.client.model.parameter

import net.dankito.banking.fints.model.BankData
import net.dankito.banking.fints.model.TanMethodType
import net.dankito.banking.client.model.CustomerCredentials


// TODO: Rename to BankingClientRequest(Base)?
open class FinTsClientParameter(
  bankCode: String,
  loginName: String,
  password: String,
  finTsServerAddress: String, // TODO: get rid of this

  open val preferredTanMethods: List<TanMethodType>? = null,
  open val preferredTanMedium: String? = null, // the ID of the medium
  open val abortIfTanIsRequired: Boolean = false,
  open val finTsModel: BankData? = null
) : CustomerCredentials(bankCode, loginName, password, finTsServerAddress)