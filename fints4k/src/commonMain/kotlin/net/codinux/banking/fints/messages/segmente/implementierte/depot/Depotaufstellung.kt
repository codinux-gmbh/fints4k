package net.codinux.banking.fints.messages.segmente.implementierte.depot

import net.codinux.banking.fints.messages.Existenzstatus
import net.codinux.banking.fints.messages.datenelemente.implementierte.Aufsetzpunkt
import net.codinux.banking.fints.messages.datenelemente.implementierte.account.MaximaleAnzahlEintraege
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.Segmentkopf
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.account.Kontoverbindung
import net.codinux.banking.fints.messages.segmente.Segment
import net.codinux.banking.fints.messages.segmente.id.CustomerSegmentId
import net.codinux.banking.fints.model.AccountData

/**
 * Nr. Name Version Typ Format Länge Status Anzahl Restriktionen
1 Segmentkopf 1 DEG M 1
2 Depot 3 DEG ktv # M 1
3 Währung der Depotaufstellung 1 DE cur # C 1 O: „Währung der Depotaufstellung wählbar“ (BPD) = „J“; N: sonst
4 Kursqualität 2 DE code 1 C 1 1,2 O: „Kursqualität wählbar“ (BPD) = „J“; N: sonst
5 Maximale Anzahl Einträge 1 DE num ..4 C 1 >0 O: „Eingabe Anzahl Einträge erlaubt“ (BPD) = „J“; N: sonst
6 Aufsetzpunkt 1 DE an ..35 C 1 M: vom Institut wurde ein Aufsetzpunkt rückgemeldet N: sonst
 */
class Depotaufstellung(
    segmentNumber: Int,
    account: AccountData,
//    parameter: GetAccountTransactionsParameter
): Segment(listOf(
    Segmentkopf(CustomerSegmentId.SecuritiesAccountBalance, 6, segmentNumber),
    Kontoverbindung(account),
    // TODO:
    //   3. Währung der Depotaufstellung
    //   4. Kursqualität
    //   5. Maximale Anzahl Einträge
    //   6. Aufsetzpunkt

//    MaximaleAnzahlEintraege(parameter), // TODO: this is wrong, it only works for HKKAZ
    MaximaleAnzahlEintraege(null, Existenzstatus.Optional),
    Aufsetzpunkt(null, Existenzstatus.Optional) // will be set dynamically, see MessageBuilder.rebuildMessageWithContinuationId(); M: vom Institut wurde ein Aufsetzpunkt rückgemeldet. N: sonst
))