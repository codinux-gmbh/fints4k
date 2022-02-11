package net.dankito.utils.multiplatform.os


actual class DeviceInfoRetriever {

    actual fun getDeviceInfo(): DeviceInfo {
        // TODO: retrieve manufacturer and device model
        return DeviceInfo("", "", System.getProperty("os.name", ""), System.getProperty("os.version", ""), System.getProperty("os.arch", ""))
    }

}