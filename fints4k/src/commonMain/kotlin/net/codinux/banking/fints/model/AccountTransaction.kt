package net.codinux.banking.fints.model

import kotlinx.datetime.LocalDate
import net.codinux.banking.fints.extensions.UnixEpochStart


open class AccountTransaction(
    val account: AccountData,
    val amount: Money,
    val reference: String?, // that was also new to me that reference may is null

    val bookingDate: LocalDate,
    val valueDate: LocalDate,

    /**
     * Name des Überweisenden oder Zahlungsempfängers
     */
    val otherPartyName: String?,
    /**
     * BIC des Überweisenden / Zahlungsempfängers
     */
    val otherPartyBankId: String?,
    /**
     * IBAN des Überweisenden oder Zahlungsempfängers
     */
    val otherPartyAccountId: String?,

    /**
     * Buchungstext, z. B. DAUERAUFTRAG, BARGELDAUSZAHLUNG, ONLINE-UEBERWEISUNG, FOLGELASTSCHRIFT, ...
     */
    val postingText: String?,

    val openingBalance: Money?,
    val closingBalance: Money?,

    /**
     * Auszugsnummer
     */
    val statementNumber: Int,
    /**
     * Blattnummer
     */
    val sheetNumber: Int?,

    /**
     * Kundenreferenz.
     */
    val customerReference: String?,
    /**
     * Bankreferenz
     */
    val bankReference: String?,
    /**
     * Währungsart und Umsatzbetrag in Ursprungswährung
     */
    val furtherInformation: String?,


    /*      Remittance information      */

    val endToEndReference: String?,
    val mandateReference: String?,
    val creditorIdentifier: String?,
    val originatorsIdentificationCode: String?,
    /**
     * Summe aus Auslagenersatz und Bearbeitungsprovision bei einer nationalen Rücklastschrift
     * sowie optionalem Zinsausgleich.
     */
    val compensationAmount: String?,
    /**
     * Betrag der ursprünglichen Lastschrift
     */
    val originalAmount: String?,
    /**
     * Abweichender Überweisender oder Zahlungsempfänger
     */
    val deviantOriginator: String?,
    /**
     * Abweichender Zahlungsempfänger oder Zahlungspflichtiger
     */
    val deviantRecipient: String?,
    val referenceWithNoSpecialType: String?,

    /**
     * Primanoten-Nr.
     */
    val journalNumber: String?,
    /**
     * Bei R-Transaktionen siehe Tabelle der
     * SEPA-Rückgabecodes, bei SEPALastschriften siehe optionale Belegung
     * bei GVC 104 und GVC 105 (GVC = Geschäftsvorfallcode)
     */
    val textKeyAddition: String?,

    /**
     * Referenznummer, die vom Sender als eindeutige Kennung für die Nachricht vergeben wurde
     * (z.B. als Referenz auf stornierte Nachrichten).
     */
    val orderReferenceNumber: String?,
    /**
     * Bezugsreferenz
     */
    val referenceNumber: String?,

    /**
     * Storno, ob die Buchung storniert wurde(?).
     * Aus:
     * „RC“ = Storno Haben
     * „RD“ = Storno Soll
     */
    val isReversal: Boolean
) {

    // for object deserializers
    internal constructor() : this(AccountData(), Money(Amount.Zero, ""), "", UnixEpochStart, UnixEpochStart, null, null, null, null)

    constructor(account: AccountData, amount: Money, unparsedReference: String, bookingDate: LocalDate, valueDate: LocalDate, otherPartyName: String?, otherPartyBankId: String?, otherPartyAccountId: String?, postingText: String? = null)
        : this(account, amount, unparsedReference, bookingDate, valueDate, otherPartyName, otherPartyBankId, otherPartyAccountId, postingText,
        null, null, 0, null,
        null, null, null, null, null, null, null, null, null, null, null,
        "", null, null, "", null, false)


    open val showOtherPartyName: Boolean
        get() = otherPartyName.isNullOrBlank() == false /* && type != "ENTGELTABSCHLUSS" && type != "AUSZAHLUNG" */ // TODO


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccountTransaction) return false

        if (account != other.account) return false
        if (amount != other.amount) return false
        if (reference != other.reference) return false
        if (bookingDate != other.bookingDate) return false
        if (otherPartyName != other.otherPartyName) return false
        if (otherPartyBankId != other.otherPartyBankId) return false
        if (otherPartyAccountId != other.otherPartyAccountId) return false
        if (postingText != other.postingText) return false
        if (valueDate != other.valueDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = account.hashCode()
        result = 31 * result + amount.hashCode()
        result = 31 * result + reference.hashCode()
        result = 31 * result + bookingDate.hashCode()
        result = 31 * result + (otherPartyName?.hashCode() ?: 0)
        result = 31 * result + (otherPartyBankId?.hashCode() ?: 0)
        result = 31 * result + (otherPartyAccountId?.hashCode() ?: 0)
        result = 31 * result + (postingText?.hashCode() ?: 0)
        result = 31 * result + valueDate.hashCode()
        return result
    }


    override fun toString(): String {
        return "$valueDate $amount $otherPartyName: $reference"
    }

}