package net.codinux.banking.fints.model

import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMedium
import net.codinux.banking.fints.response.BankResponse
import net.codinux.banking.fints.response.client.FinTsClientResponse


open class EnterTanResult(
    val enteredTan: String?,
    val userApprovedDecoupledTan: Boolean? = null,
    val responseAfterApprovingDecoupledTan: BankResponse? = null,
    val changeTanMethodTo: TanMethod? = null,
    val changeTanMediumTo: TanMedium? = null,
    val changeTanMediumResultCallback: ((FinTsClientResponse) -> Unit)? = null
) {

    override fun toString(): String {
        if (userApprovedDecoupledTan == true) {
            return "User approved Decoupled TAN"
        }

        if (changeTanMethodTo != null) {
            return "User asks to change TAN method to $changeTanMethodTo"
        }

        if (changeTanMediumTo != null) {
            return "User asks to change TAN medium to $changeTanMediumTo"
        }

        return "enteredTan = $enteredTan"
    }

}