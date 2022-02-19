package net.dankito.banking.fints.model

import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanMedium
import net.dankito.banking.fints.response.client.FinTsClientResponse


open class EnterTanResult(
    val enteredTan: String?,
    val changeTanMethodTo: TanMethod? = null,
    val changeTanMediumTo: TanMedium? = null,
    val changeTanMediumResultCallback: ((FinTsClientResponse) -> Unit)? = null
) {

    override fun toString(): String {
        if (changeTanMethodTo != null) {
            return "User asks to change TAN method to $changeTanMethodTo"
        }

        if (changeTanMediumTo != null) {
            return "User asks to change TAN medium to $changeTanMediumTo"
        }

        return "enteredTan = $enteredTan"
    }

}