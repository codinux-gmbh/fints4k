package net.dankito.utils.multiplatform.os


expect class DeviceInfoRetriever actual constructor() {

    fun getDeviceInfo(): DeviceInfo

}