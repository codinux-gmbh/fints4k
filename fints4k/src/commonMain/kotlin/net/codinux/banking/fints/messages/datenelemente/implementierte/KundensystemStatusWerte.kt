package net.codinux.banking.fints.messages.datenelemente.implementierte

/**
 * Information darüber, ob die Kundensystem-ID erforderlich ist.
 */
enum class KundensystemStatusWerte(override val code: String) : ICodeEnum {

    /**
     * Kundensystem-ID wird nicht benötigt (HBCI DDV-Verfahren und
     * chipkartenbasierte Verfahren ab Sicherheitsprofil-Version 3)
     * und PinTan bis über HKSYN eine Kundensystem-ID vom Bankserver abgerufen wurde.
     */
    NichtBenoetigt("0"),

    /**
     * Kundensystem-ID wird benötigt (sonstige HBCI RAH- /
     * RDH- und PIN/TAN-Verfahren)
     */
    Benoetigt("1")

}