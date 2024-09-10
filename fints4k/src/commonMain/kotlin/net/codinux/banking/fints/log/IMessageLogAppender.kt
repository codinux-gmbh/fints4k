package net.codinux.banking.fints.log

import kotlin.reflect.KClass


interface IMessageLogAppender {

    fun logError(loggingClass: KClass<*>, message: String, e: Throwable? = null)

}