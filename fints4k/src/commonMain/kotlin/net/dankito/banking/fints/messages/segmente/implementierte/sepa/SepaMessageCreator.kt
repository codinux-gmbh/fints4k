package net.dankito.banking.fints.messages.segmente.implementierte.sepa

import net.dankito.utils.multiplatform.Date
import net.dankito.utils.multiplatform.DateFormatter


/**
 * It may sounds like beginners programming loading a XML file and doing string replacements to set actual values.
 * And yes I know how to use xjc :).
 *
 * But there's some reason behind it:
 * - Serializing to XML is always a bit problematic on Android.
 * - I don't need another dependency.
 * - And it should be a little bit faster (even though not much :) ).
 */
open class SepaMessageCreator : ISepaMessageCreator {

    companion object {
        const val AllowedSepaCharacters = "A-Za-z0-9\\?,\\-\\+\\.,:/\\(\\)\'\" (&\\w{2,4};)"

        const val ReservedXmlCharacters = "\'\"&<>"

        val AllowedSepaCharactersPattern = Regex("^[$AllowedSepaCharacters]*$")

        val AllowedSepaCharactersExceptReservedXmlCharactersPattern = Regex("^[$AllowedSepaCharacters$ReservedXmlCharacters]*$")

        const val MessageIdKey = "MessageId"

        const val CreationDateTimeKey = "CreationDateTime"

        const val PaymentInformationIdKey = "PaymentInformationId"

        const val NumberOfTransactionsKey = "NumberOfTransactions"

        val IsoDateFormat = DateFormatter("yyyy-MM-dd'T'HH:mm:ss.SSS")
    }


    override fun containsOnlyAllowedCharacters(stringToTest: String): Boolean {
        return AllowedSepaCharactersPattern.matches(stringToTest)
                && convertDiacriticsAndReservedXmlCharacters(stringToTest) == stringToTest
    }

    override fun containsOnlyAllowedCharactersExceptReservedXmlCharacters(stringToTest: String): Boolean {
        return AllowedSepaCharactersExceptReservedXmlCharactersPattern.matches(stringToTest)
                && convertDiacritics(stringToTest) == stringToTest
    }

    override fun convertReservedXmlCharacters(input: String): String {
        // TODO: add other replacement strings
        return input
            .replace("\"", "&quot;")
            .replace("\'", "&apos;")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    override fun convertDiacritics(input: String): String {
        return input

            .replace("??", "A")
            .replace("??", "A")
            .replace("??", "A")
            .replace("??", "A")
            .replace("??", "A")
            .replace("??", "A")

            .replace("??", "a")
            .replace("??", "a")
            .replace("??", "a")
            .replace("??", "a")
            .replace("??", "a")
            .replace("??", "a")

            .replace("??", "E")
            .replace("??", "E")
            .replace("??", "E")
            .replace("??", "E")

            .replace("??", "e")
            .replace("??", "e")
            .replace("??", "e")
            .replace("??", "e")

            .replace("??", "I")
            .replace("??", "I")
            .replace("??", "I")
            .replace("??", "I")

            .replace("??", "i")
            .replace("??", "i")
            .replace("??", "i")
            .replace("??", "i")

            .replace("??", "O")
            .replace("??", "O")
            .replace("??", "O")
            .replace("??", "O")
            .replace("??", "O")

            .replace("??", "o")
            .replace("??", "o")
            .replace("??", "o")
            .replace("??", "o")
            .replace("??", "o")

            .replace("??", "U")
            .replace("??", "U")
            .replace("??", "U")
            .replace("??", "U")
            .replace("??", "U")

            .replace("??", "u")
            .replace("??", "u")
            .replace("??", "u")
            .replace("??", "u")
            .replace("??", "u")

            .replace("??", "C")
            .replace("??", "C")
            .replace("??", "N")

            .replace("??", "c")
            .replace("??", "c")
            .replace("??", "n")
            .replace("??", "ss")
    }


    override fun createXmlFile(messageTemplate: PaymentInformationMessages, replacementStrings: Map<String, String>): String {
        var xmlFile = messageTemplate.xmlTemplate

        val now = Date()
        val nowInIsoDate = IsoDateFormat.format(now)

        if (replacementStrings.containsKey(MessageIdKey) == false) {
            xmlFile = replacePlaceholderWithValue(xmlFile, MessageIdKey, nowInIsoDate)
        }
        if (replacementStrings.containsKey(CreationDateTimeKey) == false) {
            xmlFile = replacePlaceholderWithValue(xmlFile, CreationDateTimeKey, nowInIsoDate)
        }
        if (replacementStrings.containsKey(PaymentInformationIdKey) == false) {
            xmlFile = replacePlaceholderWithValue(xmlFile, PaymentInformationIdKey, nowInIsoDate)
        }

        replacementStrings.forEach { entry ->
            xmlFile = replacePlaceholderWithValue(xmlFile, entry.key, entry.value)
        }

        return xmlFile
    }

    protected open fun replacePlaceholderWithValue(xmlFile: String, key: String, value: String): String {
        return xmlFile.replace("$$key$", value)
    }

}