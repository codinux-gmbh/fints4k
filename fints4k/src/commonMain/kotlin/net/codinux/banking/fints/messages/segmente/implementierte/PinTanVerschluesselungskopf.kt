package net.codinux.banking.fints.messages.segmente.implementierte

import net.codinux.banking.fints.messages.datenelemente.implementierte.encryption.Komprimierungsfunktion
import net.codinux.banking.fints.messages.datenelemente.implementierte.encryption.Verschluesselungsalgorithmus
import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.*
import net.codinux.banking.fints.model.MessageBaseData


open class PinTanVerschluesselungskopf(
    baseData: MessageBaseData,
    date: Int,
    time: Int

) : Verschluesselungskopf(
    baseData.bank,
    baseData.versionOfSecurityProcedure,
    date,
    time,
    Operationsmodus.Cipher_Block_Chaining,
    Verschluesselungsalgorithmus.Two_Key_Triple_DES,
    Schluesselart.Chiffrierschluessel,
    Schluesselnummer.PinTanDefaultValue,
    Schluesselversion.PinTanDefaultValue,
    Komprimierungsfunktion.Keine_Kompression
)