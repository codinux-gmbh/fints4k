package net.codinux.banking.fints.response.segments

import net.codinux.banking.fints.response.ResponseParser


open class AufsetzpunktFeedback(
    val aufsetzpunkt: String,
    message: String
)
    : Feedback(ResponseParser.AufsetzpunktResponseCode, message)