package net.dankito.utils.multiplatform

actual class StackTraceHelper {

    actual fun getStackTrace(e: Throwable, maxCountStackTraceElements: Int?): String {
        // TODO: is this a nice string?
        return e.getStackTrace().take(maxCountStackTraceElements ?: Int.MAX_VALUE).joinToString("\n")
    }

}