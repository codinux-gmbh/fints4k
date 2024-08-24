package net.codinux.banking.fints.response.segments

import net.codinux.banking.fints.messages.datenelemente.implementierte.Dialogsprache
import net.codinux.banking.fints.messages.datenelemente.implementierte.HbciVersion


open class BankParameters(
    val bpdVersion: Int,

    val bankCountryCode: Int,

    val bankCode: String,

    val bankName: String,

    /**
     * Anzahl Geschäftsvorfallsarten:
     * Maximale Anzahl an Geschäftsvorfallsarten, die pro Nachricht zulässig ist.
     * Der Wert ‚0’ gibt an, dass keine Restriktionen bzgl. der Anzahl an Geschäftsvorfallsarten bestehen.
     *
     * In einer Nachricht sind Aufträge beliebiger unterschiedlicher Geschäftsvorfallsarten
     * zugelassen (z. B. drei Segmente HKCCS und ein Segment HKSAL). Eine Einschränkung ist mit Hilfe des Feldes „Anzahl Geschäftsvorfallsarten“ im Segment
     * „Bankparameter allgemein“ möglich.
     *
     * Maximale Anzahl aller Geschäftsvorfallsarten pro Nachricht: „Anzahl Geschäftsvorfallsarten“ (BPA, also dieser Wert hier).
     * Maximale Anzahl eines bestimmten Segments pro Nachricht: „Maximale Anzahl Aufträge“ (JobParameter des jeweiligen Segments).
     */
    val countMaxJobsPerMessage: Int,

    val supportedLanguages: List<Dialogsprache>,

    val supportedHbciVersions: List<HbciVersion>,

    /**
     * Obergrenze in Kilobyte (=1024 Byte) für die Nachrichtengröße. Dies kann kreditinstitutsindividuell
     * je nach technischen Restriktionen bzgl. der Verarbeitung umfangreicher Kundennachrichten vorgegeben werden.
     *
     * Der Wert ‚0’ gibt an, dass keine Restriktionen bzgl. der Nachrichtengröße bestehen.
     *
     * Eingehende Nachrichten, die dekomprimiert und entschlüsselt diese Grenze überschreiten, können dann
     * abgelehnt werden.
     */
    val maxMessageSize: Int?,

    /**
     * Zeitraum, nach dem frühestens eine weitere Life-Indikator-Nachricht gesendet werden darf.
     * Die Angabe erfolgt in Sekunden. Liegt keine Begrenzung vor, kann der Wert ‚0’ angegeben werden.
     */
    val minTimeout: Int?,

    /**
     * Zeitraum, nach dem das Kreditinstitut einen Dialog voraussichtlich beenden wird, sofern keine
     * weiteren Kundennachrichten gesendet wurden. Die Angabe erfolgt in Sekunden. Liegt keine
     * Begrenzung vor, kann der Wert ‚0’ angegeben werden.
     */
    val maxTimeout: Int?,

    segmentString: String

) : ReceivedSegment(segmentString)