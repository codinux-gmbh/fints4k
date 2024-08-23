package net.codinux.banking.fints.model

import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.VersionDesSicherheitsverfahrens


open class MessageBaseData(
    open val bank: BankData,
    open val product: ProductData
) {

    open var versionOfSecurityProcedure: VersionDesSicherheitsverfahrens = VersionDesSicherheitsverfahrens.PinTanDefaultVersion  // for PinTan almost always the case except for getting a user's TAN methods
        protected set

}