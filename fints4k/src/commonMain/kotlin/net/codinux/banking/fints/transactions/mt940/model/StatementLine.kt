package net.codinux.banking.fints.transactions.mt940.model

import kotlinx.datetime.LocalDate
import net.codinux.banking.fints.model.Amount


open class StatementLine(

    /**
     * Soll/Haben-Kennung
     *
     * “C” = Credit (Habensaldo)
     * ”D” = Debit (Sollsaldo)
     * „RC“ = Storno Haben
     * „RD“ = Storno Soll
     *
     * Max length = 2
     */
    val isCredit: Boolean,

    val isReversal: Boolean,

    /**
     * Valuta (JJMMTT)
     *
     * Length = 6
     */
    val valueDate: LocalDate,

    /**
     * MMTT
     *
     * Length = 4
     */
    val bookingDate: LocalDate?,

    /**
     * dritte Stelle der Währungsbezeichnung, falls sie zur Unterscheidung notwendig ist
     *
     * Length = 1
     */
    val currencyType: String?,

    /**
     * in Kontowährung
     *
     * Max length = 15
     */
    val amount: Amount,

    /**
     * Codes see p. 177 bottom - 179
     *
     * After constant „N“
     *
     * Length = 3
     */
    val postingKey: String,

    /**
     * Kundenreferenz.
     * Bei Nichtbelegung wird „NONREF“ eingestellt, zum Beispiel bei Schecknummer
     * Wenn „KREF+“ eingestellt ist, dann erfolgt die Angabe der Referenznummer in Tag :86: .
     */
    val customerReference: String?,

    /**
     * Bankreferenz
     */
    val bankReference: String?,

    /**
     * Währungsart und Umsatzbetrag in Ursprungswährung (original currency
     * amount) in folgendem
     * Format:
     * /OCMT/3a..15d/
     * sowie Währungsart und
     * Gebührenbetrag
     * (charges) in folgendem
     * Format:
     * /CHGS/3a..15d/
     * 3a = 3-stelliger
     * Währungscode gemäß
     * ISO 4217
     * ..15d = Betrag mit Komma
     * als Dezimalzeichen (gemäß SWIFT-Konvention).
     * Im Falle von SEPALastschriftrückgaben ist
     * das Feld /OCMT/ mit dem
     * Originalbetrag und das
     * Feld /CHGS/ mit der
     * Summe aus Entgelten
     * sowie Zinsausgleich zu
     * belegen.
     */
    val furtherInformationOriginalAmountAndCharges: String? = null

) {

    override fun toString(): String {
        return "$valueDate ${if (isCredit) "+" else "-"}$amount"
    }

}