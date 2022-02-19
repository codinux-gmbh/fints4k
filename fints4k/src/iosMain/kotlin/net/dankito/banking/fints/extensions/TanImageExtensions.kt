package net.dankito.banking.fints.extensions

import net.dankito.banking.fints.tan.TanImage
import net.dankito.utils.multiplatform.extensions.toNSData
import kotlinx.cinterop.*
import platform.Foundation.*


fun TanImage.imageBytesAsNSData(): NSData {
    return imageBytes.toNSData()
}