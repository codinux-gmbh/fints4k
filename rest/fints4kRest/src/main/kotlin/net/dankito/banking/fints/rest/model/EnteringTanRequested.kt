package net.dankito.banking.fints.rest.model

import net.dankito.banking.fints.model.TanChallenge


class EnteringTanRequested(
    val tanRequestId: String,
    val tanChallenge: TanChallenge
)