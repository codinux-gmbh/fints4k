package net.dankito.utils.multiplatform.os


actual open class OsHelper {

    actual val osType: OsType
        get() {
            return if (isRunningOnAndroid) {
                OsType.Android
            }
            else {
                OsType.JVM
            }
        }


    open val isRunningOnAndroid: Boolean = determineIsRunningOnAndroid()


    protected open fun determineIsRunningOnAndroid(): Boolean {
        try {
            Class.forName("android.app.Activity")
            return true
        } catch (ex: Exception) { }

        return false
    }

}