package net.dankito.banking.fints.rest.model.dto.response

import net.dankito.banking.fints.rest.model.EnteringTanRequested


class RestResponse<T>(
    val status: ResponseType,
    val errorMessage: String?,
    val successResponse: T?,
    val enteringTanRequested: EnteringTanRequested? = null
) {

    companion object {

        fun <T> success(result: T): RestResponse<T> {
            return RestResponse(ResponseType.Success, null, result, null)
        }

        fun <T> error(errorMessage: String): RestResponse<T> {
            return RestResponse(ResponseType.Error, errorMessage, null, null)
        }

        fun <T> requiresTan(enteringTanRequested: EnteringTanRequested): RestResponse<T> {
            return RestResponse(ResponseType.TanRequired, null, null, enteringTanRequested)
        }

    }

}