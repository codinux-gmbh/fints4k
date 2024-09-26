package net.codinux.banking.fints.tan


open class TanImage(
    val mimeType: String? = null,
    val imageBytes: ByteArray? = null,
    val decodingError: Exception? = null
) {

    val decodingSuccessful: Boolean
        get() = mimeType != null && imageBytes != null


    override fun toString(): String {
        if (decodingSuccessful == false) {
            return "Decoding error: $decodingError"
        }

        return "$mimeType ${imageBytes?.size} bytes"
    }

}