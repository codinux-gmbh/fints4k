package net.dankito.utils.multiplatform


expect class StackTraceHelper {

    fun getStackTrace(e: Throwable, maxCountStackTraceElements: Int? = null): String

}