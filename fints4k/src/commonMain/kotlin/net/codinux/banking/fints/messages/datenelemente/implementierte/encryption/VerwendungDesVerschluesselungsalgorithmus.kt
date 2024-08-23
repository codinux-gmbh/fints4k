package net.codinux.banking.fints.messages.datenelemente.implementierte.encryption

import net.codinux.banking.fints.messages.datenelemente.implementierte.ICodeEnum


enum class VerwendungDesVerschluesselungsalgorithmus(override val code: String) : ICodeEnum {

    OwnerSymmetric("2")

}