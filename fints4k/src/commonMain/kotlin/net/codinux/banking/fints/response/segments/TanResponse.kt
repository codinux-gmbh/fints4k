package net.codinux.banking.fints.response.segments

import kotlinx.datetime.Instant
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanProcess


open class TanResponse(
    val tanProcess: TanProcess,
    val jobHashValue: String?, // M: bei Auftrags-Hashwertverfahren<>0 und TAN-Prozess=1. N: sonst
    val jobReference: String?, // M: bei TAN-Prozess=2, 3, 4. O: bei TAN-Prozess=1

    /**
     * Dieses Datenelement enthält im Falle des Zwei-Schritt-TAN-Verfahrens die Challenge zu einem
     * eingereichten Auftrag. Aus der Challenge wird vom Kunden die eigentliche TAN ermittelt.
     * Die Challenge wird unabhängig vom Prozessvariante 1 oder 2 in der Kreditinstitutsantwort im
     * Segment HITAN übermittelt.
     *
     * Ist der BPD-Parameter „Challenge strukturiert“ mit „J“ belegt, so können im Text folgende
     * Formatsteuerzeichen enthalten sein, die kundenseitig entsprechend zu interpretieren sind.
     * Eine Kaskadierung von Steuerzeichen ist nicht erlaubt.
     *
     * <br>             Zeilenumbruch
     * <p>              Neuer Absatz
     * <b> ... </b>     Fettdruck
     * <i> ... </i>     Kursivdruck
     * <u> ... </u>     Unterstreichen
     * <ul> ... </ul>   Beginn / Ende Aufzählung
     * <ol> ... </ol>   Beginn / Ende Nummerierte Liste
     * <li> ... </li>   Listenelement einer Aufzählung / Nummerierten Liste
     */
    val challenge: String?, // M: bei TAN-Prozess=1, 3, 4. O: bei TAN-Prozess=2

    val challengeHHD_UC: String?,

    /**
     * Datum und Uhrzeit, bis zu welchem Zeitpunkt eine TAN auf Basis der gesendeten Challenge gültig ist. Nach Ablauf der Gültigkeitsdauer wird die entsprechende TAN entwertet.
     *
     * In server's time zone, that is Europe/Berlin.
     */
    val tanExpirationTime: Instant?,
    val tanMediaIdentifier: String? = null, // M: bei TAN-Prozess=1, 3, 4 und „Anzahl unterstützter aktiver TAN-Medien“ nicht vorhanden. O: sonst

    segmentString: String
) :
    ReceivedSegment(segmentString) {

    companion object {
        const val NoChallengeResponse = "nochallenge"
        const val NoJobReferenceResponse = "noref"
    }

    open val isStrongAuthenticationRequired: Boolean
        get() = challenge != null && challenge != NoChallengeResponse

}