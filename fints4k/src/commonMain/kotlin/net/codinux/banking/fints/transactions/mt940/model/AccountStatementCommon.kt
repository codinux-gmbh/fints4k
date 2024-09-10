package net.codinux.banking.fints.transactions.mt940.model

open class AccountStatementCommon(

    /**
     * Referenznummer, die vom Sender als eindeutige Kennung für die Nachricht vergeben wurde
     * (z.B. als Referenz auf stornierte Nachrichten).
     *
     * Die Referenz darf nicht mit "/" starten oder enden; darf nicht "//" enthalten
     *
     * Max length = 16
     */
    val orderReferenceNumber: String,

    /**
     * Bezugsreferenz oder „NONREF“.
     *
     * Die Referenz darf nicht mit "/" starten oder enden; darf nicht "//" enthalten
     *
     * Max length = 16
     */
    val referenceNumber: String?,

    /**
     * xxxxxxxxxxx/Konto-Nr. oder yyyyyyyy/Konto-Nr.
     * wobei xxxxxxxxxxx = S.W.I.F.T.-Code
     * yyyyyyyy = Bankleitzahl
     * Konto-Nr. = max. 23 Stellen (ggf. mit Währung)
     *
     * Zukünftig kann hier auch die IBAN angegeben werden.
     *
     * Max length = 35
     */
    val bankCodeBicOrIban: String,

    val accountIdentifier: String?,

    /**
     * Falls eine Auszugsnummer nicht unterstützt wird, ist „0“ einzustellen.
     *
     * Max length = 5
     */
    val statementNumber: Int,

    /**
     * „/“ kommt nach statementNumber falls Blattnummer belegt.
     *
     * beginnend mit „1“
     *
     * Max length = 5
     */
    val sheetNumber: Int?,

    val transactions: List<Transaction>,

) {

    // for object deserializers
    private constructor() : this("", "", "", null, 0, null, listOf())


    val isStatementNumberSupported: Boolean
        get() = statementNumber != 0


    override fun toString(): String {
        return "$bankCodeBicOrIban, ${transactions.size} transactions"
    }

}