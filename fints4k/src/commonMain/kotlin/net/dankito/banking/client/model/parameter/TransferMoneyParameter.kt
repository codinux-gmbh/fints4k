package net.dankito.banking.client.model.parameter

import net.dankito.banking.client.model.BankAccountIdentifier
import net.codinux.banking.fints.model.AccountData
import net.codinux.banking.fints.model.BankData
import net.codinux.banking.fints.model.Money
import net.codinux.banking.fints.model.TanMethodType


open class TransferMoneyParameter(
  bankCode: String,
  loginName: String,
  password: String,
  /**
   * The account from which the money should be withdrawn.
   * If not specified fints4k retrieves all bank accounts and checks if there is exactly one that supports money transfer.
   * If no or more than one bank account supports money transfer, the error codes NoAccountSupportsMoneyTransfer or MoreThanOneAccountSupportsMoneyTransfer are returned.
   */
  open val remittanceAccount: BankAccountIdentifier? = null,

  open val recipientName: String,
  /**
   * The identifier of recipient's account. In most cases the IBAN.
   */
  open val recipientAccountIdentifier: String,
  /**
   * The identifier of recipient's bank. In most cases the BIC.
   * Can be omitted for German banks as the BIC can be derived from IBAN.
   */
  open val recipientBankIdentifier: String? = null,

  open val amount: Money,
  open val reference: String? = null,
  open val instantPayment: Boolean = false,

  preferredTanMethods: List<TanMethodType>? = null,
  tanMethodsNotSupportedByApplication: List<TanMethodType>? = null,
  preferredTanMedium: String? = null,
  abortIfTanIsRequired: Boolean = false,
  finTsModel: BankData? = null,

  open val selectAccountToUseForTransfer: ((List<AccountData>) -> AccountData?)? = null // TODO: use BankAccount instead of AccountData

) : FinTsClientParameter(bankCode, loginName, password, preferredTanMethods, tanMethodsNotSupportedByApplication, preferredTanMedium, abortIfTanIsRequired, finTsModel)