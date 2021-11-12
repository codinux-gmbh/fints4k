package net.dankito.banking.fints.response.client

import net.dankito.banking.fints.model.JobContext
import net.dankito.banking.fints.response.BankResponse
import net.dankito.banking.fints.response.segments.TanMediaList


open class GetTanMediaListResponse(
    context: JobContext,
    response: BankResponse,
    val tanMediaList: TanMediaList?
)
    : FinTsClientResponse(context, response)