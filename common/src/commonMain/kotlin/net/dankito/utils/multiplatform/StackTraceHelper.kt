package net.dankito.utils.multiplatform


expect class StackTraceHelper actual constructor() {

    fun getStackTrace(e: Throwable, maxCountStackTraceElements: Int? = null): String

}