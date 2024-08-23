package net.codinux.banking.fints.response.segments

import net.codinux.banking.fints.messages.datenelemente.implementierte.Synchronisierungsmodus


open class ReceivedSynchronization(

    segmentString: String,

    /**
     * Only set if [Synchronisierungsmodus] was set to [Synchronisierungsmodus.NeueKundensystemIdZurueckmelden]
     */
    val customerSystemId: String? = null,

    /**
     * Only set if [Synchronisierungsmodus] was set to [Synchronisierungsmodus.LetzteVerarbeiteteNachrichtennummerZurueckmelden]
     */
    val lastMessageNumber: String? = null,

    /**
     * Only set if [Synchronisierungsmodus] was set to [Synchronisierungsmodus.SignaturIdZurueckmelden]
     */
    val securityReferenceNumberForSigningKey: String? = null,

    /**
     * Only set if [Synchronisierungsmodus] was set to [Synchronisierungsmodus.SignaturIdZurueckmelden]
     */
    val securityReferenceNumberForDigitalSignature: String? = null

)
    : ReceivedSegment(segmentString)