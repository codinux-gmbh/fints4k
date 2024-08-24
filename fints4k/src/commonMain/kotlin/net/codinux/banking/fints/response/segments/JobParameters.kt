package net.codinux.banking.fints.response.segments


open class JobParameters(
    open val jobName: String,
    /**
     * Höchstens zulässige Anzahl an Segmenten der jeweiligen Auftragsart je
     * Kundennachricht. Übersteigt die Anzahl der vom Kunden übermittelten Segmente pro Auftragsart die zugelassene Maximalanzahl, so wird die gesamte
     * Nachricht abgelehnt.
     */
    open val maxCountJobs: Int,

    /**
     * Anzahl der Signaturen, die zur Ausführung eines Geschäftsvorfalls als erforderlich definiert ist.
     * Falls 0 angegeben ist, handelt es sich um einen nicht signierungspflichtigen
     * Geschäftsvorfall, der auch über einen anonymen Zugang ohne Signierungsmöglichkeit ausgeführt werden kann.
     * Falls die Anzahl der benötigten Signaturen größer als 1 ist, bedeutet dies,
     * dass dieser Geschäftsvorfall zusätzlich von mindestens einem anderen berechtigten Benutzer signiert werden muss, über dessen Identität in den UPD
     * jedoch nichts ausgesagt wird.
     * In bestimmten Fällen ist die Anzahl der Signaturen durch die Art des Geschäftsvorfalls vorgegeben (z. B. sind bei Keymanagement-Aufträgen nicht
     * mehrere Signaturen möglich).
     *
     * (Ist meistens 1, da PinTan Nachrichten außer im Anonymen Dialog immer eigene Signatur brauchen.)
     */
    open val minimumCountSignatures: Int,
    open val securityClass: Int?,
    segmentString: String
)
    : ReceivedSegment(segmentString) {


    internal constructor() : this("", 0, 0, null, "0:0:0") // for object deserializers


    constructor(parameters: JobParameters)
            : this(parameters.jobName, parameters.maxCountJobs, parameters.minimumCountSignatures,
                    parameters.securityClass, parameters.segmentString)


    override fun toString(): String {
        return "$jobName $segmentVersion"
    }

}