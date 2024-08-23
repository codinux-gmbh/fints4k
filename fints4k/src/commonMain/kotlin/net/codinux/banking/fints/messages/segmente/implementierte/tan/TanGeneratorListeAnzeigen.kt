package net.codinux.banking.fints.messages.segmente.implementierte.tan

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Code
import net.codinux.banking.fints.messages.datenelemente.implementierte.allCodes
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMedienArtVersion
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMediumKlasse
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.Segmentkopf
import net.codinux.banking.fints.messages.segmente.Segment
import net.codinux.banking.fints.messages.segmente.id.CustomerSegmentId


open class TanGeneratorListeAnzeigen(
    segmentVersion: Int,
    segmentNumber: Int,
    tanMediaKind: TanMedienArtVersion = TanMedienArtVersion.Alle,
    tanMediumClass: TanMediumKlasse = TanMediumKlasse.AlleMedien
)
    : Segment(listOf(
    Segmentkopf(CustomerSegmentId.TanMediaList, segmentVersion, segmentNumber),
    Code(tanMediaKind, allCodes<TanMedienArtVersion>(), Existenzstatus.Mandatory),
    Code(tanMediumClass, allCodes<TanMediumKlasse>(), Existenzstatus.Mandatory)
)) {

    init {
        val supportedMediaClasses = TanMediumKlasse.values().filter { it.supportedHkTabVersions.contains(segmentVersion) }

        if (supportedMediaClasses.contains(tanMediumClass) == false) {
            throw UnsupportedOperationException("Value $tanMediumClass for TAN medium class is not valid for HKTAB version $segmentVersion. " +
                    "Supported values are: " + supportedMediaClasses)
        }
    }

}