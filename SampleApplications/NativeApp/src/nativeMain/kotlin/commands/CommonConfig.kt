package commands

import NativeApp
import net.codinux.banking.fints.model.TanMethodType


data class CommonConfig(
  val app: NativeApp,

  val bankCode: String,
  val loginName: String,
  val password: String,

  val preferredTanMethods: List<TanMethodType>?,
  val abortIfRequiresTan: Boolean
)