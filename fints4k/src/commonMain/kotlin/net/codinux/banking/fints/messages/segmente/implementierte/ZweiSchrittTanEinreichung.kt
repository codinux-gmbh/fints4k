package net.codinux.banking.fints.messages.segmente.implementierte

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.JaNein
import net.codinux.banking.fints.messages.datenelemente.implementierte.NotAllowedDatenelement
import net.codinux.banking.fints.messages.datenelemente.implementierte.Segmentkennung
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.*
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.Segmentkopf
import net.codinux.banking.fints.messages.segmente.Segment
import net.codinux.banking.fints.messages.segmente.id.CustomerSegmentId
import net.codinux.banking.fints.messages.segmente.id.ISegmentId


open class ZweiSchrittTanEinreichung(
    segmentNumber: Int,
    process: TanProcess,
    segmentIdForWhichTanShouldGetGenerated: ISegmentId? = null,
    jobHashValue: String? = null,
    jobReference: String? = null,
    furtherTanFollows: Boolean? = false,
    cancelJob: Boolean? = null,
    tanMediaIdentifier: String? = null,
    segmentVersion: Int = 6

) : Segment(listOf(
        Segmentkopf(CustomerSegmentId.Tan, segmentVersion, segmentNumber),
        TANProzessDatenelement(process),
        Segmentkennung(segmentIdForWhichTanShouldGetGenerated?.id ?: ""), // M: bei TAN-Prozess=1. M: bei TAN-Prozess=4 und starker Authentifizierung. N: sonst
        NotAllowedDatenelement(), // Kontoverbindung // M: bei TAN-Prozess=1 und "Auftraggeberkonto erforderlich"=2 und Kontoverbindung im Auftrag enthalten. N: sonst
        AuftragsHashwert(jobHashValue ?: "", Existenzstatus.NotAllowed), // M: bei AuftragsHashwertverfahren<>0 und TAN-Prozess=1. N: sonst
        Auftragsreferenz(jobReference ?: "", if (process == TanProcess.TanProcess2 || process == TanProcess.TanProcess3 || process == TanProcess.AppTan) Existenzstatus.Mandatory else Existenzstatus.Optional), // M: bei TAN-Prozess=2, 3, 4. O: bei TAN-Prozess=1
        JaNein(furtherTanFollows, if (process == TanProcess.TanProcess1 || process == TanProcess.TanProcess2 || process == TanProcess.AppTan) Existenzstatus.Mandatory else Existenzstatus.NotAllowed), // M: bei TAN-Prozess=1, 2. N: bei TAN-Prozess=3, 4
        JaNein(cancelJob, if (process == TanProcess.TanProcess2 && cancelJob != null) Existenzstatus.Optional else Existenzstatus.NotAllowed), // O: bei TAN-Prozess=2 und „Auftragsstorno erlaubt“=J. N: sonst
        NotAllowedDatenelement(), // TODO: SMS-Abbuchungskonto // M: Bei TAN-Process=1, 3, 4 und „SMS-Abbuchungskonto erforderlich“=2. O: sonst
        NotAllowedDatenelement(), // TODO: Challenge-Klasse // M: bei TAN-Prozess=1 und „Challenge-Klasse erforderlich“=J. N: sonst
        NotAllowedDatenelement(), // TODO: Parameter Challenge-Klasse // O: Bei TAN-Process=1 „Challenge-Klasse erforderlich“=J. N: sonst
        BezeichnungDesTANMediums(tanMediaIdentifier, Existenzstatus.Optional), // M: bei TAN-Prozess=1, 3, 4 und „Anzahl unterstützter aktiver TAN-Medien“ nicht vorhanden. O: sonst
        NotAllowedDatenelement() // TODO: Antwort HHD_UC // M: bei TAN-Prozess=2 und „Antwort HHD_UC erforderlich“=“J“. O: sonst
))