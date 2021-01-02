package net.dankito.banking.fints.rest

import javax.annotation.Priority
import javax.ws.rs.Priorities
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.ext.Provider


@Provider
@Priority(Priorities.HEADER_DECORATOR)
open class AccessControlResponseFilter : ContainerResponseFilter {


    override fun filter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
        val headers: MultivaluedMap<String, Any> = responseContext.headers

        headers.add("Access-Control-Allow-Origin", "*")
        headers.add("Access-Control-Allow-Headers", "Authorization, Origin, X-Requested-With, Content-Type")
        headers.add("Access-Control-Expose-Headers", "Location, Content-Disposition")
        headers.add("Access-Control-Allow-Methods", "POST, PUT, GET, DELETE, HEAD, OPTIONS")
    }

}