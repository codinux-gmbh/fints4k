package net.codinux.banking.fints.messages.datenelemente.implementierte.tan

import net.codinux.banking.fints.messages.datenelemente.implementierte.ICodeEnum


/**
 * Abhängig vom Kreditinstitut und der Anzahl unterstützter TAN-Medien ist die Angabe der Bezeichnung des TAN-
 * Mediums erforderlich, damit der Kunde dem Institut mitteilen kann, welches der TAN-Medien er verwenden möchte.
 */
enum class BezeichnungDesTanMediumsErforderlich(override val code: String) : ICodeEnum {

    BezeichnungDesTanMediumsDarfNichtAngegebenWerden("0"),

    BezeichnungDesTanMediumsKannAngegebenWerden("1"),

    BezeichnungDesTanMediumsMussAngegebenWerden("2")

}