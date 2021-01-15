package net.dankito.utils.multiplatform

import java.io.PrintWriter
import java.io.StringWriter


actual class StackTraceHelper actual constructor() {

    actual fun getStackTrace(e: Throwable, maxCountStackTraceElements: Int?): String {
        val stringWriter = StringWriter()
        e.printStackTrace(PrintWriter(stringWriter))

        val stackTrace = stringWriter.toString()

        maxCountStackTraceElements?.let {
            val elements = stackTrace.split(System.lineSeparator()).take(maxCountStackTraceElements)

            return elements.joinToString(System.lineSeparator())
        }

        return stackTrace
    }

}