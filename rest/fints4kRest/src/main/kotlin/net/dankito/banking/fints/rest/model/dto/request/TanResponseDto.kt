package net.dankito.banking.fints.rest.model.dto.request

import net.dankito.banking.fints.model.EnterTanResult


class TanResponseDto(
    val tanRequestId: String,
    val enterTanResult: EnterTanResult
)