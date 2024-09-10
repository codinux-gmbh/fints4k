package net.codinux.banking.fints.model

import kotlin.jvm.JvmOverloads


open class AddAccountParameter @JvmOverloads constructor(
    open val bank: BankData,
    open val fetchBalanceAndTransactions: Boolean = true,
    open val preferredTanMethods: List<TanMethodType>? = null,
    open val tanMethodsNotSupportedByApplication: List<TanMethodType>? = null,
    open val preferredTanMedium: String? = null
) {

    constructor(bankCode: String, customerId: String, pin: String, finTs3ServerAddress: String)
            : this(bankCode, customerId, pin, finTs3ServerAddress, "")

    constructor(bankCode: String, customerId: String, pin: String, finTs3ServerAddress: String, bic: String = "", fetchBalanceAndTransactions: Boolean = true)
            : this(BankData(bankCode, customerId, pin, finTs3ServerAddress, bic), fetchBalanceAndTransactions)

}