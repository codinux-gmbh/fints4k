package net.dankito.banking.client.model

import kotlinx.serialization.Serializable
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMedium
import net.dankito.banking.fints.model.TanMethod

//import net.dankito.banking.client.model.tan.TanMedium
//import net.dankito.banking.client.model.tan.TanMethod


@Serializable
open class CustomerAccount(
    override var bankCode: String,
    override var loginName: String,
    override var password: String,
    open var finTsServerAddress: String,
    open var bankName: String,
    open var bic: String,

    open var customerName: String = "",
    open var userId: String = loginName,

    open var accounts: List<BankAccount> = listOf(),

    // TODO: use that ones from .tan sub package
    open var tanMethods: List<TanMethod> = listOf(),
    open var selectedTanMethod: TanMethod? = null,
    open var tanMedia: List<TanMedium> = listOf(),
    open var selectedTanMedium: TanMedium? = null,
) : CustomerCredentials(bankCode, loginName, password) {


    override fun toString(): String {
        return "$bankName $loginName"
    }

}