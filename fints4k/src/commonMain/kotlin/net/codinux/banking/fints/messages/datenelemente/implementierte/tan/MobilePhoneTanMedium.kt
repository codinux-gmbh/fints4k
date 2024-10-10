package net.codinux.banking.fints.messages.datenelemente.implementierte.tan

import kotlinx.serialization.Serializable
import net.dankito.banking.client.model.BankAccountIdentifier

@Serializable
open class MobilePhoneTanMedium(
    val concealedPhoneNumber: String?,
    val phoneNumber: String?,
    val smsDebitAccount: BankAccountIdentifier? = null
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MobilePhoneTanMedium) return false

        if (concealedPhoneNumber != other.concealedPhoneNumber) return false
        if (phoneNumber != other.phoneNumber) return false
        if (smsDebitAccount != other.smsDebitAccount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = concealedPhoneNumber.hashCode()
        result = 31 * result + phoneNumber.hashCode()
        result = 31 * result + smsDebitAccount.hashCode()
        return result
    }


    override fun toString(): String {
        return phoneNumber ?: concealedPhoneNumber ?: ""
    }

}