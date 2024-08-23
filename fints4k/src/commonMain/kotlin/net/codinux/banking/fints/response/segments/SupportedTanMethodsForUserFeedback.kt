package net.codinux.banking.fints.response.segments

import net.codinux.banking.fints.messages.datenelemente.implementierte.signatur.Sicherheitsfunktion
import net.codinux.banking.fints.response.ResponseParser

open class SupportedTanMethodsForUserFeedback(
    val supportedTanMethods: List<Sicherheitsfunktion>,
    message: String
)
    : Feedback(ResponseParser.SupportedTanMethodsForUserResponseCode, message)