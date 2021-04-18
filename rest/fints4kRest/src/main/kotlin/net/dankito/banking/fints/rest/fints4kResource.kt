package net.dankito.banking.fints.rest

import net.dankito.banking.fints.response.client.AddAccountResponse
import net.dankito.banking.fints.response.client.GetTransactionsResponse
import net.dankito.banking.fints.rest.model.dto.request.AddAccountRequestDto
import net.dankito.banking.fints.rest.mapper.DtoMapper
import net.dankito.banking.fints.rest.model.dto.request.GetAccountsTransactionsRequestDto
import net.dankito.banking.fints.rest.model.dto.request.TanResponseDto
import net.dankito.banking.fints.rest.model.dto.response.AddAccountResponseDto
import net.dankito.banking.fints.rest.model.dto.response.GetAccountsTransactionsResponseDto
import net.dankito.banking.fints.rest.model.dto.response.RestResponse
import net.dankito.banking.fints.rest.service.fints4kService
import net.dankito.banking.fints.rest.service.model.GetAccountsTransactionsResponse
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType


@Path("/fints/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class fints4kResource {

    @Inject
    protected val service = fints4kService()

    protected val mapper = DtoMapper()


    @POST
    @Path("addaccount")
    fun addAccount(request: AddAccountRequestDto): RestResponse<AddAccountResponseDto> {
        val response = service.getAddAccountResponse(request)

        return mapper.createRestResponse(response) { successResponse -> mapper.map(successResponse) }
    }


    @POST
    @Path("transactions")
    fun getAccountTransactions(request: GetAccountsTransactionsRequestDto): GetAccountsTransactionsResponseDto {
        val response = service.getAccountTransactions(request)

        return mapper.map(response)
    }


    @POST
    @Path("tanresponse")
    fun tanResponse(dto: TanResponseDto): RestResponse<Any> {
        val response = service.handleTanResponse(dto)

        // couldn't make it that compiler access ResponseHolder<*> for mapper.createRestResponse(), resulted in very cryptic "{"arity":0}" response -> handle it manually
        response.response?.let { successResponse ->
            return RestResponse.success(mapSuccessResponse(successResponse))
        }

        // all other cases map here, the responseMapper callback has no function
        return mapper.createRestResponse(response) { it!! }
    }

    private fun mapSuccessResponse(successResponse: Any): Any {
        return when (successResponse) {
            is AddAccountResponse -> mapper.map(successResponse)
            is GetAccountsTransactionsResponse -> mapper.map(successResponse)
            is GetTransactionsResponse -> mapper.mapTransactions(successResponse)
            else -> successResponse // add others / new ones here
        }
    }

}