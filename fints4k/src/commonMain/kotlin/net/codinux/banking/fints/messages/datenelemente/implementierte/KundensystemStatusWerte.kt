package net.codinux.banking.fints.messages.datenelemente.implementierte


enum class KundensystemStatusWerte(override val code: String) : ICodeEnum {

    NichtBenoetigt("0"),

    Benoetigt("1")

}