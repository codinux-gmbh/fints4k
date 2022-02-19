package net.dankito.utils.multiplatform.extensions

import kotlinx.cinterop.*
import platform.Foundation.*


fun ByteArray.toNSData(): NSData = NSMutableData().apply {
    if (isEmpty()) return@apply
    this@toNSData.usePinned {
        appendBytes(it.addressOf(0), size.convert())
    }
}