package net.dankito.banking.fints.log

import kotlin.reflect.KClass


interface IMessageLogAppender {

    fun logError(loggingClass: KClass<*>, message: String, e: Exception? = null)

}