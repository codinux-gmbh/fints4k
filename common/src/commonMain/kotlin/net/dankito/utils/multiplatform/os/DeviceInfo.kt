package net.dankito.utils.multiplatform.os


open class DeviceInfo(
    open val manufacturer: String,
    open val deviceModel: String,
    open val osName: String,
    open val osVersion: String,
    open val osArch: String
) {

    override fun toString(): String {
        return "$manufacturer $deviceModel: $osName $osVersion"
    }

}