package net.codinux.banking.fints.util

import io.ktor.utils.io.charsets.Charset
import net.codinux.banking.fints.messages.HbciCharset


interface IBase64Service {

    companion object {
        val DefaultCharset = HbciCharset.DefaultCharset
    }


    fun encode(text: String, charset: Charset = DefaultCharset): String


    fun decode(base64: String, charset: Charset = DefaultCharset): String

}