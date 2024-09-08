package net.codinux.banking.fints.transactions.mt940.model

open class RemittanceInformationField(
    val unparsedReference: String,
    /**
     * AT 02 Name des Überweisenden
     * AT 03 Name des Zahlungsempfängers (bei mehr als 54 Zeichen wird der Name gekürzt)
     */
    val otherPartyName: String?,
    /**
     * BLZ Überweisender / Zahlungsempfänger
     * Bei SEPA-Zahlungen BIC des Überweisenden / Zahlungsempfängers.
     */
    val otherPartyBankId: String?,
    /**
     * AT 01 IBAN des Überweisenden (Zahlungseingang Überweisung)
     * AT 04 IBAN des Zahlungsempfängers (Eingang Lastschrift)
     */
    val otherPartyAccountId: String?,
    /**
     * Buchungstext, z. B. DAUERAUFTRAG, BARGELDAUSZAHLUNG, ONLINE-UEBERWEISUNG, FOLGELASTSCHRIFT, ...
     */
    val postingText: String?,
    /**
     * Primanoten-Nr.
     */
    val journalNumber: String?,
    /**
     * Bei R-Transaktionen siehe Tabelle der
     * SEPA-Rückgabecodes, bei SEPALastschriften siehe optionale Belegung
     * bei GVC 104 und GVC 105 (GVC = Geschäftsvorfallcode)
     */
    val textKeyAddition: String?
) {

    /**
     * (DDAT10; CT-AT41 - Angabe verpflichtend)
     * (NOTPROVIDED wird nicht eingestellt.
     * Im Falle von Schecks wird hinter EREF+ die Konstante „SCHECK-NR. “, gefolgt von der Schecknummer angegeben (erst
     * nach Migration Scheckvordruck auf ISO 20022; November 2016, entspricht dem Inhalt der EndToEndId des
     * entsprechenden Scheckumsatzes).
     */
    var endToEndReference: String? = null

    var customerReference: String? = null

    /**
     * (DD-AT01 - Angabe verpflichtend)
     */
    var mandateReference: String? = null

    /**
     * (DD-AT02 - Angabe verpflichtend bei SEPALastschriften, nicht jedoch bei SEPARücklastschriften)
     */
    var creditorIdentifier: String? = null

    /**
     * (CT-AT10- Angabe verpflichtend,)
     * Entweder CRED oder DEBT
     */
    var originatorsIdentificationCode: String? = null

    /**
     * Summe aus Auslagenersatz und Bearbeitungsprovision bei einer nationalen Rücklastschrift
     * sowie optionalem Zinsausgleich.
     */
    var compensationAmount: String? = null

    /**
     * Betrag der ursprünglichen Lastschrift
     */
    var originalAmount: String? = null

    /**
     * (DD-AT22; CT-AT05 -Angabe verpflichtend, nicht jedoch bei RTransaktionen52)
     */
    var sepaReference: String? = null

    /**
     * Abweichender Überweisender (CT-AT08) / Abweichender Zahlungsempfänger (DD-AT38)
     * (optional)53
     */
    var deviantOriginator: String? = null

    /**
     * Abweichender Zahlungsempfänger (CT-AT28) /
     * Abweichender Zahlungspflichtiger ((DDAT15)
     * (optional)53
     */
    var deviantRecipient: String? = null

    var referenceWithNoSpecialType: String? = null


    override fun toString(): String {
        return "$otherPartyName $unparsedReference"
    }

}