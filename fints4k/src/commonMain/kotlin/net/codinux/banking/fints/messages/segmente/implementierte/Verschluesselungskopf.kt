package net.codinux.banking.fints.messages.segmente.implementierte

import net.codinux.banking.fints.messages.datenelemente.implementierte.NotAllowedDatenelement
import net.codinux.banking.fints.messages.datenelemente.implementierte.encryption.Komprimierungsfunktion
import net.codinux.banking.fints.messages.datenelemente.implementierte.encryption.KomprimierungsfunktionDatenelement
import net.codinux.banking.fints.messages.datenelemente.implementierte.encryption.Verschluesselungsalgorithmus
import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.*
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.Segmentkopf
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.encryption.VerschluesselungsalgorithmusDatenelementgruppe
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.signatur.Schluesselname
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.signatur.SicherheitsdatumUndUhrzeit
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.signatur.SicherheitsidentifikationDetails
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.signatur.Sicherheitsprofil
import net.codinux.banking.fints.messages.segmente.Segment
import net.codinux.banking.fints.messages.segmente.id.MessageSegmentId
import net.codinux.banking.fints.model.BankData


/**
 * Der Verschlüsselungskopf enthält Informationen über die Art des Sicherheitsservice, die
 * Verschlüsselungsfunktion und die zu verwendenden Chiffrierschlüssel.
 *
 * Zum Abgleich mit dem in den BPD definierten RAH-Verschlüsselungsverfahren wird das Feld
 * „Bezeichner für Algorithmusparameter, Schlüssel“ in der DEG „Verschlüsselungsalgorithmus“ herangezogen.
 *
 *
 * Abweichende Belegung für PIN/TAN Verfahren (Dokument Sicherheitsverfahren PIN/TAN, B.9.8 Segment „Verschlüsselungskopf“, S. 59)
 *
 * Sicherheitsfunktion, kodiert
 *      Es wird der Wert „998“ (Klartext) verwendet.
 *
 * Zertifikat
 *      Dieses Feld darf nicht belegt werden.
 */
open class Verschluesselungskopf(
    bank: BankData,
    versionOfSecurityProcedure: VersionDesSicherheitsverfahrens,
    date: Int,
    time: Int,
    mode: Operationsmodus,
    encryptionAlgorithm: Verschluesselungsalgorithmus,
    key: Schluesselart,
    keyNumber: Int,
    keyVersion: Int,
    algorithm: Komprimierungsfunktion

) : Segment(listOf(
    Segmentkopf(MessageSegmentId.EncryptionHeader, 3, 998),
    Sicherheitsprofil(Sicherheitsverfahren.PIN_TAN_Verfahren, versionOfSecurityProcedure), // fints4k only supports Pin/Tan and PSD2 requires two step tan procedure; the only exception is the first dialog to get user's TAN procedures which allows to use one step tan procedure (as we don't know TAN procedures yet)
    SicherheitsfunktionKodiert(Sicherheitsfunktion.Klartext),
    RolleDesSicherheitslieferantenKodiert(), // allowed: 1, 4
    SicherheitsidentifikationDetails(bank.customerSystemId),
    SicherheitsdatumUndUhrzeit(date, time),
    VerschluesselungsalgorithmusDatenelementgruppe(mode, encryptionAlgorithm),
    Schluesselname(bank, key, keyNumber, keyVersion),
    KomprimierungsfunktionDatenelement(algorithm),
    NotAllowedDatenelement() // Certificate not applicapable for PIN/TAN
))