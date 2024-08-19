package commands

import NativeApp
import net.dankito.banking.fints.model.TanMethodType


data class CommonConfig(
  val app: NativeApp,

  val bankCode: String,
  val loginName: String,
  val password: String,

  val preferredTanMethods: List<TanMethodType>?,
  val abortIfRequiresTan: Boolean
)