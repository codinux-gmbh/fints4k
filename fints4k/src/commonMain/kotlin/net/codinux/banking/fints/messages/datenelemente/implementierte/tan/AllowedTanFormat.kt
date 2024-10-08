package net.codinux.banking.fints.messages.datenelemente.implementierte.tan

import net.codinux.banking.fints.messages.datenelemente.implementierte.ICodeEnum


/**
 * Angabe des erwarteten Formates der TAN im konkreten Zwei-Schritt-Verfahren.
 */
enum class AllowedTanFormat(override val code: String) : ICodeEnum {

    Numeric("1"),

    Alphanumeric("2")

}