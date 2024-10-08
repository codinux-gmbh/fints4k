package net.codinux.banking.fints.messages.segmente.implementierte.umsaetze

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.abgeleiteteformate.Datum
import net.codinux.banking.fints.messages.datenelemente.implementierte.Aufsetzpunkt
import net.codinux.banking.fints.messages.datenelemente.implementierte.account.AlleKonten
import net.codinux.banking.fints.messages.datenelemente.implementierte.account.MaximaleAnzahlEintraege
import net.codinux.banking.fints.messages.datenelementgruppen.Datenelementgruppe
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.Segmentkopf
import net.codinux.banking.fints.messages.segmente.Segment
import net.codinux.banking.fints.messages.segmente.id.CustomerSegmentId
import net.codinux.banking.fints.model.GetAccountTransactionsParameter


/**
 * Die Lösung bietet dem Kunden die Möglichkeit, auf seinem System verlorengegangene Buchungen erneut zu erhalten.
 *
 * Die maximale Anzahl der rückzumeldenden Buchungspositionen kann begrenzt werden. Eine Buchungsposition besteht
 * aus einem :61:/:86:-Block eines MT 940-Formats. Es muss davon unabhängig immer ein gültiges MT 940-Format
 * zurückgemeldet werden, d.h. die Felder :20: bis :60: und :62: bis :86: sind obligatorischer Bestandteil der Rückmeldung.
 *
 * Der maximale Zeitraum, für den rückwirkend Buchungen beim Kreditinstitut gespeichert sind, wird in den
 * Bankparameterdaten übermittelt.
 */
abstract class KontoumsaetzeZeitraumMt940Base(
    segmentVersion: Int,
    segmentNumber: Int,
    account: Datenelementgruppe,
    parameter: GetAccountTransactionsParameter
)
    : Segment(listOf(
        Segmentkopf(CustomerSegmentId.AccountTransactionsMt940, segmentVersion, segmentNumber),
        account,
        AlleKonten(false, Existenzstatus.Mandatory), // currently no supported, we retrieve account transactions account by account (most banks don't support AlleKonten anyway)
        Datum(parameter.fromDate, Existenzstatus.Optional),
        Datum(parameter.toDate, Existenzstatus.Optional),
        MaximaleAnzahlEintraege(parameter),
        Aufsetzpunkt(null, Existenzstatus.Optional) // will be set dynamically, see MessageBuilder.rebuildMessageWithContinuationId(); M: vom Institut wurde ein Aufsetzpunkt rückgemeldet. N: sonst
))