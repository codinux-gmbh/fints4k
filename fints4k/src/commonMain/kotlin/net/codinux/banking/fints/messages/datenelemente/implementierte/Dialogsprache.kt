package net.codinux.banking.fints.messages.datenelemente.implementierte


enum class Dialogsprache(override val code: String) : ICodeEnum {

    Default("0"),

    German("1"),

    English("2"),

    French("3")

}