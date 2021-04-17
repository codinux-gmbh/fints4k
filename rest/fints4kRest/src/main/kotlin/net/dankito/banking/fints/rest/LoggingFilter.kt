package net.dankito.banking.fints.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider


@Provider
class LoggingFilter : ContainerResponseFilter {

    companion object {
        private val log = LoggerFactory.getLogger(LoggingFilter::class.java)
    }


    @Inject
    internal lateinit var mapper: ObjectMapper


    override fun filter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
        if (responseContext.statusInfo.family != Response.Status.Family.SUCCESSFUL) {
            log.warn("Request ${geRequestUrl(requestContext)} failed: ${getResponseStatus(responseContext)}"
                    + System.lineSeparator() + getHeadersAsString(responseContext)
                    + System.lineSeparator() + getBodyAsString(responseContext))
        }
        else if (log.isInfoEnabled) {
            log.info("Result of request ${geRequestUrl(requestContext)}: ${getResponseStatus(responseContext)}"
                    + System.lineSeparator() + getHeadersAsString(responseContext)
                    + System.lineSeparator() + getBodyAsString(responseContext))
        }
    }

    private fun geRequestUrl(requestContext: ContainerRequestContext): String {
        return "${requestContext.request.method} ${requestContext.uriInfo.requestUri}"
    }

    private fun getResponseStatus(responseContext: ContainerResponseContext): String {
        return "${responseContext.status} ${responseContext.statusInfo.reasonPhrase}"
    }

    private fun getHeadersAsString(responseContext: ContainerResponseContext): String {
        return responseContext.stringHeaders.map { header -> "${header.key}: ${header.value}" }.joinToString("\n", "Headers:\n")
    }

    private fun getBodyAsString(responseContext: ContainerResponseContext): String {
        if (responseContext.hasEntity()) {
            return "Body ${responseContext.entityClass.name}:\n" + mapper.writeValueAsString(responseContext.entity)
        }

        return "<No response body>"
    }

}