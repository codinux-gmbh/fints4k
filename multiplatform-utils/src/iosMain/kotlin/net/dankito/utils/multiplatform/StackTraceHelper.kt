package net.dankito.utils.multiplatform


actual class StackTraceHelper() {

    actual fun getStackTrace(e: Throwable, maxCountStackTraceElements: Int?): String {
        var stackTrace = e.getStackTrace()

        maxCountStackTraceElements?.let {
            stackTrace = stackTrace.take(maxCountStackTraceElements)
        }

        return stackTrace.joinToString("\r\n")
    }

}