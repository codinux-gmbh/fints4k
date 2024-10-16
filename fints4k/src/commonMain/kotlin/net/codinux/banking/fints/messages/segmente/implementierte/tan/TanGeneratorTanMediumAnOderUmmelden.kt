package net.codinux.banking.fints.messages.segmente.implementierte.tan

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Code
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Datum
import net.codinux.banking.fints.messages.datenelemente.basisformate.AlphanumerischesDatenelement
import net.codinux.banking.fints.messages.datenelemente.basisformate.NumerischesDatenelement
import net.codinux.banking.fints.messages.datenelemente.implementierte.DoNotPrintDatenelement
import net.codinux.banking.fints.messages.datenelemente.implementierte.NotAllowedDatenelement
import net.codinux.banking.fints.messages.datenelemente.implementierte.allCodes
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMedium
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMediumKlasse
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.Segmentkopf
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.account.Kontoverbindung
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.account.KontoverbindungInternational
import net.codinux.banking.fints.messages.segmente.Segment
import net.codinux.banking.fints.messages.segmente.id.CustomerSegmentId
import net.codinux.banking.fints.model.BankData
import net.codinux.banking.fints.response.segments.ChangeTanMediaParameters


/**
 * The actual job is called "TAN-Medium an- bzw. ummelden", but as TAN lists aren't supported anymore I've implemented
 * it only for TAN Generators.
 *
 * Mit Hilfe dieses Geschäftsvorfalls kann der Kunde seinem Institut mitteilen, welches Medium (Chipkarte,
 * TAN-Generator oder bilateral vereinbart) er für die Autorisierung der Aufträge per TAN verwenden wird.
 *
 * Welches Medium gerade aktiv ist, kann mit Hilfe des Geschäftsvorfalls „TAN-Medium anzeigen Bestand (HKTAB)“ bzw.
 * für Detailinformationen zur Karte auch „Kartenanzeige anfordern (HKAZK)“ durch den Kunden erfragt werden.
 *
 * Der Kunde entscheidet selbst, welches seiner verfügbaren TAN-Medien er verwenden möchte.
 *
 * chipTAN-Verfahren:
 * Steht beim chipTAN-Verfahren ein Kartenwechsel an, so kann der Kunde mit diesem Geschäftsvorfall seine Karte bzw.
 * Folgekarte aktivieren. Kann der Kunde mehrere Karten verwenden, dann kann mit diesem GV die Ummeldung auf eine
 * andere Karte erfolgen. Das Kreditinstitut entscheidet selbst, ob dieser GV TAN-pflichtig ist oder nicht.
 */
open class TanGeneratorTanMediumAnOderUmmelden(
    segmentVersion: Int,
    segmentNumber: Int,
    bank: BankData,
    newActiveTanMedium: TanMedium,
    /**
     * Has to be set if „Eingabe von ATC und TAN erforderlich“ (BPD)=“J“
     */
    tan: String? = null,
    /**
     * Has to be set if „Eingabe von ATC und TAN erforderlich“ (BPD)=“J“
     */
    atc: Int? = null,
    /**
     * An optional field and only used in version 3
     */
    iccsn: String? = null,
    parameters: ChangeTanMediaParameters = bank.changeTanMediumParameters!!
)
    : Segment(listOf(
    Segmentkopf(CustomerSegmentId.ChangeTanMedium, segmentVersion, segmentNumber),
    Code(newActiveTanMedium.mediumClass, allCodes<TanMediumKlasse>(), Existenzstatus.Mandatory),
    AlphanumerischesDatenelement(newActiveTanMedium.tanGenerator?.cardNumber, if (newActiveTanMedium.mediumClass == TanMediumKlasse.TanGenerator) Existenzstatus.Mandatory else Existenzstatus.NotAllowed),
    AlphanumerischesDatenelement(newActiveTanMedium.tanGenerator?.cardSequenceNumber, if (newActiveTanMedium.mediumClass == TanMediumKlasse.TanGenerator && parameters.enteringCardSequenceNumberRequired) Existenzstatus.Mandatory else Existenzstatus.NotAllowed),
    if (segmentVersion > 1) NumerischesDatenelement(newActiveTanMedium.tanGenerator?.cardType, 2, if (newActiveTanMedium.mediumClass == TanMediumKlasse.TanGenerator && parameters.enteringCardTypeAllowed) Existenzstatus.Optional else Existenzstatus.NotAllowed) else DoNotPrintDatenelement(),
    if (segmentVersion == 2) Kontoverbindung(bank.accounts.first()) else DoNotPrintDatenelement(),
    if (segmentVersion >= 3 && parameters.accountInfoRequired) KontoverbindungInternational(bank.accounts.first(), bank) else DoNotPrintDatenelement(),
    if (segmentVersion >= 2 && newActiveTanMedium.mediumClass == TanMediumKlasse.TanGenerator) Datum(newActiveTanMedium.tanGenerator?.validFrom, Existenzstatus.Optional) else DoNotPrintDatenelement(),
    if (segmentVersion >= 2 && newActiveTanMedium.mediumClass == TanMediumKlasse.TanGenerator) Datum(newActiveTanMedium.tanGenerator?.validTo, Existenzstatus.Optional) else DoNotPrintDatenelement(),
    if (segmentVersion >= 3 && newActiveTanMedium.mediumClass == TanMediumKlasse.TanGenerator) AlphanumerischesDatenelement(iccsn, Existenzstatus.Optional, 19) else DoNotPrintDatenelement(),
    NotAllowedDatenelement(), // TAN-Listennummer not supported anymore
    NumerischesDatenelement(atc, 5, if (newActiveTanMedium.mediumClass == TanMediumKlasse.TanGenerator && parameters.enteringAtcAndTanRequired) Existenzstatus.Mandatory else Existenzstatus.NotAllowed),
    AlphanumerischesDatenelement(tan, if (parameters.enteringAtcAndTanRequired) Existenzstatus.Mandatory else Existenzstatus.NotAllowed, 99)
)) {

    init {
        if (parameters.enteringAtcAndTanRequired) {
            if (atc == null || tan == null) {
                throw UnsupportedOperationException("As „Eingabe von ATC und TAN erforderlich“ is set to \"J\" " +
                        "(ChangeTanMediaParameters.enteringAtcAndTanRequired is set to true) parameters atc and tan have to be set.")
            }
        }
    }

}