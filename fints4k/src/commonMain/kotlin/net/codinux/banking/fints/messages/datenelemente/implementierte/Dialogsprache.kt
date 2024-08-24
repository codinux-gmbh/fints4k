package net.codinux.banking.fints.messages.datenelemente.implementierte


enum class Dialogsprache(override val code: String) : ICodeEnum {

    /**
     * Der Kunde darf lediglich ein Sprachkennzeichen einstellen, das im Rahmen
     * der BPD vom Kreditinstitut an das Kundensystem übermittelt wurde.
     * Wenn noch keine BPD vorliegen, sollte das Kundenprodukt mit Hilfe eines
     * anonymen Dialogs die aktuelle BPD des Instituts ermitteln und die Standardsprache des Instituts einstellen, die in den Bankparameterdaten mitgeteilt
     * wird. Falls die BPD nicht abgerufen werden kann, ist der Wert „0“ einzustellen. Das Kreditinstitut antwortet in diesem Fall in seiner Standardsprache.
     */
    Default("0"),

    German("1"),

    English("2"),

    French("3")

}