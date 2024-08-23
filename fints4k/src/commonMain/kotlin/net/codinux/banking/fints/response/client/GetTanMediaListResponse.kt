package net.codinux.banking.fints.response.client

import net.codinux.banking.fints.model.JobContext
import net.codinux.banking.fints.response.BankResponse
import net.codinux.banking.fints.response.segments.TanMediaList


open class GetTanMediaListResponse(
    context: JobContext,
    response: BankResponse,
    val tanMediaList: TanMediaList?
)
    : FinTsClientResponse(context, response)