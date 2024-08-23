package net.codinux.banking.fints.tan

import io.ktor.utils.io.core.toByteArray
import net.codinux.log.logger
import net.codinux.banking.fints.messages.HbciCharset


open class TanImageDecoder {

    private val log by logger()


    open fun decodeChallenge(challengeHHD_UC: String): TanImage {
        try {
            val bytes = challengeHHD_UC.toByteArray(HbciCharset.DefaultCharset)

            val mimeTypeLength = getLength(bytes[0], bytes[1])
            val mimeTypeEnd = 2 + mimeTypeLength

            val mimeType = challengeHHD_UC.substring(2, mimeTypeEnd)

            val imageStart = mimeTypeEnd + 2

            // sometimes it happened that imageStart + getLength(bytes[mimeTypeEnd], bytes[mimeTypeEnd + 1])
            // was greater than challengeHHD_UC.length + 1 -> ignore image length and simply return all bytes starting with imageStart
            val imageBytes = bytes.copyOfRange(imageStart, bytes.size)

            return TanImage(mimeType, imageBytes)
        } catch (e: Exception) {
            log.error(e) { "Could not decode challenge HHD_UC to TanImage: $challengeHHD_UC" }

            return TanImage("", ByteArray(0), e)
        }
    }

    protected open fun getLength(higherOrderByte: Byte, lowerOrderByte: Byte): Int {
        return 256 * byteToUnsignedInt(higherOrderByte) + byteToUnsignedInt(lowerOrderByte)
    }

    protected open fun byteToUnsignedInt(byte: Byte): Int {
        return byte.toUByte().toInt()
    }

}