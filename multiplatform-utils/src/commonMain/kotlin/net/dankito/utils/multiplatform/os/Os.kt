package net.dankito.utils.multiplatform.os


open class Os(open val osHelper: OsHelper = OsHelper()) {

    open val osType: OsType
        get() = osHelper.osType


    open val isRunningOnJvm: Boolean
        get() = osType == OsType.JVM

    open val isRunningOnAndroid: Boolean
        get() = osType == OsType.Android

    open val isRunningOniOS: Boolean
        get() = osType == OsType.iOS

}