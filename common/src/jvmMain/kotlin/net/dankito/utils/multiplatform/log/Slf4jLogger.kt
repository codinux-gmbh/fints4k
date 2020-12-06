package net.dankito.utils.multiplatform.log

import net.dankito.utils.multiplatform.os.OsHelper


open class Slf4jLogger(protected val slf4jLogger: org.slf4j.Logger, protected val osHelper: OsHelper = OsHelper()) : Logger {

    companion object {

        const val MaxLogCatMessageLength = 4 * 1024

    }


    override val name: String
        get() = slf4jLogger.name


    override val isFatalEnabled: Boolean
        get() = isErrorEnabled

    override val isErrorEnabled: Boolean
        get() = slf4jLogger.isErrorEnabled

    override val isWarnEnabled: Boolean
        get() = slf4jLogger.isWarnEnabled

    override val isInfoEnabled: Boolean
        get() = slf4jLogger.isInfoEnabled

    override val isDebugEnabled: Boolean
        get() = slf4jLogger.isDebugEnabled

    override val isTraceEnabled: Boolean
        get() = slf4jLogger.isTraceEnabled


    override fun fatal(message: String, exception: Throwable?, vararg arguments: Any) {
        error(message, exception, arguments)
    }

    override fun fatal(exception: Throwable?, vararg arguments: Any, message: () -> String) {
        error(exception, *arguments, message = message)
    }


    override fun error(message: String, exception: Throwable?, vararg arguments: Any) {
        log(exception, arguments, { message }, { msg, args -> slf4jLogger.error(msg, *args) } )
    }

    override fun error(exception: Throwable?, vararg arguments: Any, message: () -> String) {
        log(exception, arguments, message, { msg, args -> slf4jLogger.error(msg, *args) } )
    }


    override fun warn(message: String, exception: Throwable?, vararg arguments: Any) {
        log(exception, arguments, { message }, { msg, args -> slf4jLogger.warn(msg, *args) } )
    }

    override fun warn(exception: Throwable?, vararg arguments: Any, message: () -> String) {
        log(exception, arguments, message, { msg, args -> slf4jLogger.warn(msg, *args) } )
    }


    override fun info(message: String, exception: Throwable?, vararg arguments: Any) {
        log(exception, arguments, { message }, { msg, args -> slf4jLogger.info(msg, *args) } )
    }

    override fun info(exception: Throwable?, vararg arguments: Any, message: () -> String) {
        log(exception, arguments, message, { msg, args -> slf4jLogger.info(msg, *args) } )
    }


    override fun debug(message: String, exception: Throwable?, vararg arguments: Any) {
        log(exception, arguments, { message }, { msg, args -> slf4jLogger.debug(msg, *args) } )
    }

    override fun debug(exception: Throwable?, vararg arguments: Any, message: () -> String) {
        log(exception, arguments, message, { msg, args -> slf4jLogger.debug(msg, *args) } )
    }


    override fun trace(message: String, exception: Throwable?, vararg arguments: Any) {
        log(exception, arguments, { message }, { msg, args -> slf4jLogger.trace(msg, *args) } )
    }

    override fun trace(exception: Throwable?, vararg arguments: Any, message: () -> String) {
        log(exception, arguments, message, { msg, args -> slf4jLogger.trace(msg, *args) } )
    }


    protected open fun log(exception: Throwable?, arguments: Array<out Any>, messageCreator: () -> String,
                 logOnLevel: (String, Array<out Any>) -> Unit) {

        val message = messageCreator()

        val args = determineArguments(exception, arguments)

        if (osHelper.isRunningOnAndroid && message.length > MaxLogCatMessageLength) {
            var index = 0
            // as LogCat only prints at maximum 4076 bytes per message, break up message in multiple strings
            message.chunked(MaxLogCatMessageLength - 5).forEach { chunk -> // -5 to also log index
                logOnLevel("[${index++}] $chunk", args)
            }
        }
        else {
            logOnLevel(message, args)
        }
    }

    protected open fun determineArguments(exception: Throwable?, arguments: Array<out Any>): Array<out Any> {
        return if (exception != null) {
            if (arguments.isEmpty()) {
                arrayOf(exception)
            } else {
                val argumentsIncludingException: MutableList<Any> = mutableListOf(exception)
                argumentsIncludingException.addAll(0, arguments.toList())

                argumentsIncludingException.toTypedArray()
            }
        } else {
            arguments
        }
    }

}