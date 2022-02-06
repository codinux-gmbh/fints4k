package net.dankito.banking.fints.transactions.mt940

import net.dankito.banking.fints.log.IMessageLogAppender
import net.dankito.banking.fints.model.Amount
import net.dankito.banking.fints.transactions.mt940.model.*
import net.dankito.utils.multiplatform.Date
import net.dankito.utils.multiplatform.DateFormatter
import net.dankito.utils.multiplatform.Month
import net.dankito.utils.multiplatform.isUpperCase
import net.dankito.utils.multiplatform.log.LoggerFactory


/*
4.1. SWIFT Supported Characters

a until z
A until Z
0 until 9
/ ‐ ? : ( ) . , ' + { }
CR LF Space
Although part of the character set, the curly brackets are permitted as delimiters and cannot be used within the text of
user‐to‐user messages.
Character ”‐” is not permitted as the first character of the line.
None of lines include only Space.
 */
open class Mt940Parser : IMt940Parser {

    companion object {
        val AccountStatementsSeparatorRegex = Regex("^\\s*-\\s*\$", RegexOption.MULTILINE) // a line only with '-' and may other white space characters

        // (?<!T\d\d(:\d\d)?) to filter that date time with format (yyyy-MM-dd)Thh:mm:ss(:SSS) is considered to be a field identifier
        val AccountStatementFieldSeparatorRegex = Regex("(?<!T\\d\\d(:\\d\\d)?):\\d\\d\\w?:")


        const val TransactionReferenceNumberCode = "20"

        const val RelatedReferenceNumberCode = "21"

        const val AccountIdentificationCode = "25"

        const val StatementNumberCode = "28C"

        const val OpeningBalanceCode = "60"

        const val StatementLineCode = "61"

        const val InformationToAccountOwnerCode = "86"

        const val ClosingBalanceCode = "62"


        val DateFormatter = DateFormatter("yyMMdd")

        val CurrentYearTwoDigit = Date().year() - 2000

        val CreditDebitCancellationRegex = Regex("C|D|RC|RD")

        val AmountRegex = Regex("\\d+,\\d*")

        val ReferenceTypeRegex = Regex("[A-Z]{4}\\+")

        val InformationToAccountOwnerSubFieldRegex = Regex("\\?\\d\\d")


        const val EndToEndReferenceKey = "EREF+"
        const val CustomerReferenceKey = "KREF+"
        const val MandateReferenceKey = "MREF+"
        const val CreditorIdentifierKey = "CRED+"
        const val OriginatorsIdentificationCodeKey = "DEBT+"
        const val CompensationAmountKey = "COAM+"
        const val OriginalAmountKey = "OAMT+"
        const val SepaReferenceKey = "SVWZ+"
        const val DeviantOriginatorKey = "ABWA+"
        const val DeviantRecipientKey = "ABWE+"


        private val log = LoggerFactory.getLogger(Mt940Parser::class)
    }


    override var logAppender: IMessageLogAppender? = null


    /**
     * Parses a whole MT 940 statements string, that is one that ends with a "-" line.
     */
    override fun parseMt940String(mt940String: String): List<AccountStatement> {
        return parseMt940Chunk(mt940String).first
    }

    /**
     * Parses incomplete MT 940 statements string, that is ones that not end with a "-" line,
     * as the they are returned e.g. if a HKKAZ response is dispersed over multiple messages.
     *
     * Tries to parse all statements in the string except an incomplete last one and returns an
     * incomplete last MT 940 statement (if any) as remainder.
     *
     * So each single HKKAZ partial response can be parsed immediately, its statements/transactions
     * be displayed immediately to user and the remainder then be passed together with next partial
     * HKKAZ response to this method till this whole MT 940 statement is parsed.
     */
    override fun parseMt940Chunk(mt940Chunk: String): Pair<List<AccountStatement>, String> {
        try {
            val singleAccountStatementsStrings = splitIntoSingleAccountStatements(mt940Chunk).toMutableList()

            var remainder = ""
            if (singleAccountStatementsStrings.isNotEmpty() && singleAccountStatementsStrings.last().isEmpty() == false) {
                remainder = singleAccountStatementsStrings.removeAt(singleAccountStatementsStrings.lastIndex)
            }

            val transactions = singleAccountStatementsStrings.mapNotNull { parseAccountStatement(it) }

            return Pair(transactions, remainder)
        } catch (e: Exception) {
            logError("Could not parse account statements from MT940 string:\n$mt940Chunk", e)
        }

        return Pair(listOf(), "")
    }


    protected open fun splitIntoSingleAccountStatements(mt940String: String): List<String> {
        return mt940String.split(AccountStatementsSeparatorRegex)
                        .map { it.replace("\n", "").replace("\r", "") }
    }


    protected open fun parseAccountStatement(accountStatementString: String): AccountStatement? {
        if (accountStatementString.isBlank()) {
            return null
        }

        try {
            val fieldsByCode = splitIntoFields(accountStatementString)

            return parseAccountStatement(fieldsByCode)
        } catch (e: Exception) {
            logError("Could not parse account statement:\n$accountStatementString", e)
        }

        return null
    }

    protected open fun splitIntoFields(accountStatementString: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        var lastMatchEnd = 0
        var lastMatchedCode = ""

        AccountStatementFieldSeparatorRegex.findAll(accountStatementString).forEach { matchResult ->
            if (lastMatchEnd > 0) {
                val previousStatement = accountStatementString.substring(lastMatchEnd, matchResult.range.first)
                result.add(Pair(lastMatchedCode, previousStatement))
            }

            lastMatchedCode = matchResult.value.replace(":", "")
            lastMatchEnd = matchResult.range.last + 1
        }

        if (lastMatchEnd > 0) {
            val previousStatement = accountStatementString.substring(lastMatchEnd, accountStatementString.length)
            result.add(Pair(lastMatchedCode, previousStatement))
        }

        return result
    }

    protected open fun parseAccountStatement(fieldsByCode: List<Pair<String, String>>): AccountStatement? {
        val statementAndMaySequenceNumber = getFieldValue(fieldsByCode, StatementNumberCode).split('/')
        val accountIdentification = getFieldValue(fieldsByCode, AccountIdentificationCode).split('/')
        val openingBalancePair = fieldsByCode.first { it.first.startsWith(OpeningBalanceCode) }
        val closingBalancePair = fieldsByCode.first { it.first.startsWith(ClosingBalanceCode) }

        return AccountStatement(
            getFieldValue(fieldsByCode, TransactionReferenceNumberCode),
            getOptionalFieldValue(fieldsByCode, RelatedReferenceNumberCode),
            accountIdentification[0],
            if (accountIdentification.size > 1) accountIdentification[1] else null,
            statementAndMaySequenceNumber[0].toInt(),
            if (statementAndMaySequenceNumber.size > 1) statementAndMaySequenceNumber[1].toInt() else null,
            parseBalance(openingBalancePair.first, openingBalancePair.second),
            parseAccountStatementTransactions(fieldsByCode),
            parseBalance(closingBalancePair.first, closingBalancePair.second)
        )
    }

    protected open fun getFieldValue(fieldsByCode: List<Pair<String, String>>, code: String): String {
        return fieldsByCode.first { it.first == code }.second
    }

    protected open fun getOptionalFieldValue(fieldsByCode: List<Pair<String, String>>, code: String): String? {
        return fieldsByCode.firstOrNull { it.first == code }?.second
    }

    protected open fun parseBalance(code: String, fieldValue: String): Balance {
        val isIntermediate = code.endsWith("M")

        val isDebit = fieldValue.startsWith("D")
        val bookingDateString = fieldValue.substring(1, 7)
        val statementDate = parseMt940Date(bookingDateString)
        val currency = fieldValue.substring(7, 10)
        val amountString = fieldValue.substring(10)
        val amount = parseAmount(amountString)

        return Balance(isIntermediate, !!!isDebit, statementDate, currency, amount)
    }

    protected open fun parseAccountStatementTransactions(fieldsByCode: List<Pair<String, String>>): List<Transaction> {
        val transactions = mutableListOf<Transaction>()

        fieldsByCode.forEachIndexed { index, pair ->
            if (pair.first == StatementLineCode) {
                val statementLine = parseStatementLine(pair.second)

                val nextPair = if (index < fieldsByCode.size - 1) fieldsByCode.get(index + 1) else null
                val information = if (nextPair?.first == InformationToAccountOwnerCode) parseNullableInformationToAccountOwner(nextPair.second) else null

                transactions.add(Transaction(statementLine, information))
            }
        }

        return transactions
    }

    /**
     * FORMAT
     * 6!n[4!n]2a[1!a]15d1!a3!c16x[//16x]
     * [34x]
     *
     * where subfields are:
     * Subfield   Format    Name
     *   1          6!n     (Value Date)
     *   2          [4!n]   (Entry Date)
     *   3          2a      (Debit/Credit Mark)
     *   4          [1!a]   (Funds Code)
     *   5          15d     (Amount)
     *   6          1!a3!c  (Transaction Type)(Identification Code)
     *   7          16x     (Reference for the Account Owner)
     *   8          [//16x] (Reference of the Account Servicing Institution)
     *   9          [34x]   (Supplementary Details)
     */
    protected open fun parseStatementLine(fieldValue: String): StatementLine {
        val valueDateString = fieldValue.substring(0, 6)
        val valueDate = parseMt940Date(valueDateString)

        val creditMarkMatchResult = CreditDebitCancellationRegex.find(fieldValue)
        val isDebit = creditMarkMatchResult?.value?.endsWith('D') == true
        val isCancellation = creditMarkMatchResult?.value?.startsWith('R') == true

        val creditMarkEnd = (creditMarkMatchResult?.range?.last ?: -1) + 1

        // booking date is the second field and is optional. It is normally only used when different from the value date.
        val bookingDateString = if ((creditMarkMatchResult?.range?.start ?: 0) > 6) fieldValue.substring(6, 10) else null
        val bookingDate = bookingDateString?.let { // bookingDateString has format MMdd -> add year from valueDateString
            parseMt940BookingDate(bookingDateString, valueDateString, valueDate)
        } ?: valueDate

        val amountMatchResult = AmountRegex.find(fieldValue)!!
        val amountString = amountMatchResult.value
        val amount = parseAmount(amountString)

        val amountEndIndex = amountMatchResult.range.last + 1

        val fundsCode = if (amountMatchResult.range.start - creditMarkEnd > 1) fieldValue.substring(creditMarkEnd + 1, creditMarkEnd + 2) else null

        /**
         * S    SWIFT transfer  For entries related to SWIFT transfer instructions and subsequent charge messages.
         *
         * N    Non-SWIFT       For entries related to payment and transfer instructions, including transfer related charges messages, not sent through SWIFT or where an alpha description is preferred.
         *
         * F    First advice    For entries being first advised by the statement (items originated by the account servicing institution).
         */
        val transactionType = fieldValue.substring(amountEndIndex, amountEndIndex + 1) // transaction type is 'N', 'S' or 'F'

        val bookingKeyStart = amountEndIndex + 1
        val bookingKey = fieldValue.substring(bookingKeyStart, bookingKeyStart + 3) // TODO: parse codes, p. 178

        val customerAndBankReference = fieldValue.substring(bookingKeyStart + 3).split("//")
        val customerReference = customerAndBankReference[0]

        /**
         * The content of this subfield is the account servicing institution's own reference for the transaction.
         * When the transaction has been initiated by the account servicing institution, this
         * reference may be identical to subfield 7, Reference for the Account Owner. If this is
         * the case, Reference of the Account Servicing Institution, subfield 8 may be omitted.
         */
        var bankReference = if (customerAndBankReference.size > 1) customerAndBankReference[1] else customerReference // TODO: or use null?
        var supplementaryDetails: String? = null

        val bankReferenceAndSupplementaryDetails = bankReference.split("\n")
        if (bankReferenceAndSupplementaryDetails.size > 1) {
            bankReference = bankReferenceAndSupplementaryDetails[0].trim()
            // TODO: parse /OCMT/ and /CHGS/, see page 518
            supplementaryDetails = bankReferenceAndSupplementaryDetails[1].trim()
        }

        return StatementLine(!!!isDebit, isCancellation, valueDate, bookingDate, null, amount, bookingKey,
            customerReference, bankReference, supplementaryDetails)
    }

    protected open fun parseNullableInformationToAccountOwner(informationToAccountOwnerString: String): InformationToAccountOwner? {
        try {
            val information = parseInformationToAccountOwner(informationToAccountOwnerString)

            mapReference(information)

            return information
        } catch (e: Exception) {
            logError("Could not parse InformationToAccountOwner from field value '$informationToAccountOwnerString'", e)
        }

        return null
    }

    protected open fun parseInformationToAccountOwner(informationToAccountOwnerString: String): InformationToAccountOwner {
        // e. g. starts with 0 -> Inlandszahlungsverkehr, starts with '3' -> Wertpapiergeschäft
        // see Finanzdatenformate p. 209 - 215
        val geschaeftsvorfallCode = informationToAccountOwnerString.substring(0, 2) // TODO: may map

        val referenceParts = mutableListOf<String>()
        val otherPartyName = StringBuilder()
        var otherPartyBankCode: String? = null
        var otherPartyAccountId: String? = null
        var bookingText: String? = null
        var primaNotaNumber: String? = null
        var textKeySupplement: String? = null

        val subFieldMatches = InformationToAccountOwnerSubFieldRegex.findAll(informationToAccountOwnerString).toList()
        subFieldMatches.forEachIndexed { index, matchResult ->
            val fieldCode = matchResult.value.substring(1, 3).toInt()
            val endIndex = if (index + 1 < subFieldMatches.size) subFieldMatches[index + 1].range.start else informationToAccountOwnerString.length
            val fieldValue = informationToAccountOwnerString.substring(matchResult.range.last + 1, endIndex)

            when (fieldCode) {
                0 -> bookingText = fieldValue
                10 -> primaNotaNumber = fieldValue
                in 20..29 -> referenceParts.add(fieldValue)
                30 -> otherPartyBankCode = fieldValue
                31 -> otherPartyAccountId = fieldValue
                32, 33 -> otherPartyName.append(fieldValue)
                34 -> textKeySupplement = fieldValue
                in 60..63 -> referenceParts.add(fieldValue)
            }
        }

        val reference = if (isFormattedReference(referenceParts)) joinReferenceParts(referenceParts)
                    else referenceParts.joinToString(" ")

        val otherPartyNameString = if (otherPartyName.isEmpty()) null else otherPartyName.toString()

        return InformationToAccountOwner(
            reference, otherPartyNameString, otherPartyBankCode, otherPartyAccountId,
            bookingText, primaNotaNumber, textKeySupplement
        )
    }

    protected open fun joinReferenceParts(referenceParts: List<String>): String {
        val reference = StringBuilder()

        referenceParts.firstOrNull()?.let {
            reference.append(it)
        }

        for (i in 1..referenceParts.size - 1) {
            val part = referenceParts[i]
            if (part.isNotEmpty() && part.first().isUpperCase && referenceParts[i - 1].last().isUpperCase == false) {
                reference.append(" ")
            }

            reference.append(part)
        }

        return reference.toString()
    }

    protected open fun isFormattedReference(referenceParts: List<String>): Boolean {
        return referenceParts.any { ReferenceTypeRegex.find(it) != null }
    }

    /**
     * Jeder Bezeichner [z.B. EREF+] muss am Anfang eines Subfeldes [z. B. ?21] stehen.
     * Bei Längenüberschreitung wird im nachfolgenden Subfeld ohne Wiederholung des Bezeichners fortgesetzt. Bei Wechsel des Bezeichners ist ein neues Subfeld zu beginnen.
     * Belegung in der nachfolgenden Reihenfolge, wenn vorhanden:
     * EREF+[ Ende-zu-Ende Referenz ] (DD-AT10; CT-AT41 - Angabe verpflichtend; NOTPROVIDED wird nicht eingestellt.)
     * KREF+[Kundenreferenz]
     * MREF+[Mandatsreferenz] (DD-AT01 - Angabe verpflichtend)
     * CRED+[Creditor Identifier] (DD-AT02 - Angabe verpflichtend bei SEPA-Lastschriften, nicht jedoch bei SEPA-Rücklastschriften)
     * DEBT+[Originators Identification Code](CT-AT10- Angabe verpflichtend,)
     * Entweder CRED oder DEBT
     *
     * optional zusätzlich zur Einstellung in Feld 61, Subfeld 9:
     *
     * COAM+ [Compensation Amount / Summe aus Auslagenersatz und Bearbeitungsprovision bei einer nationalen Rücklastschrift sowie optionalem Zinsausgleich.]
     * OAMT+[Original Amount] Betrag der ursprünglichen Lastschrift
     *
     * SVWZ+[SEPA-Verwendungszweck] (DD-AT22; CT-AT05 -Angabe verpflichtend, nicht jedoch bei R-Transaktionen)
     * ABWA+[Abweichender Überweisender] (CT-AT08) / Abweichender Zahlungsempfänger (DD-AT38) ] (optional)
     * ABWE+[Abweichender Zahlungsemp-fänger (CT-AT28) / Abweichender Zahlungspflichtiger ((DD-AT15)] (optional)
     *
     * Weitere 4 Verwendungszwecke können zu den Feldschlüsseln 60 bis 63 eingestellt werden.
     */
    protected open fun mapReference(information: InformationToAccountOwner) {
        val referenceParts = getReferenceParts(information.unparsedReference)

        referenceParts.forEach { entry ->
            setReferenceLineValue(information, entry.key, entry.value)
        }
    }

    open fun getReferenceParts(unparsedReference: String): Map<String, String> {
        var previousMatchType = ""
        var previousMatchEnd = 0

        val referenceParts = mutableMapOf<String, String>()

        ReferenceTypeRegex.findAll(unparsedReference).forEach { matchResult ->
            if (previousMatchEnd > 0) {
                val typeValue = unparsedReference.substring(previousMatchEnd, matchResult.range.first)

                referenceParts[previousMatchType] = typeValue
            }

            previousMatchType = unparsedReference.substring(matchResult.range)
            previousMatchEnd = matchResult.range.last + 1
        }

        if (previousMatchEnd > 0) {
            val typeValue = unparsedReference.substring(previousMatchEnd, unparsedReference.length)

            referenceParts[previousMatchType] = typeValue
        }

        return referenceParts
    }

    // TODO: there are more. See .pdf from Deutsche Bank
    protected open fun setReferenceLineValue(information: InformationToAccountOwner, referenceType: String, typeValue: String) {
        when (referenceType) {
            EndToEndReferenceKey -> information.endToEndReference = typeValue
            CustomerReferenceKey -> information.customerReference = typeValue
            MandateReferenceKey -> information.mandateReference = typeValue
            CreditorIdentifierKey -> information.creditorIdentifier = typeValue
            OriginatorsIdentificationCodeKey -> information.originatorsIdentificationCode = typeValue
            CompensationAmountKey -> information.compensationAmount = typeValue
            OriginalAmountKey -> information.originalAmount = typeValue
            SepaReferenceKey -> information.sepaReference = typeValue
            DeviantOriginatorKey -> information.deviantOriginator = typeValue
            DeviantRecipientKey -> information.deviantRecipient = typeValue
            else -> information.referenceWithNoSpecialType = typeValue
        }
    }


    protected open fun parseMt940Date(dateString: String): Date {
        // TODO: this should be necessary anymore, isn't it?

        // SimpleDateFormat is not thread-safe. Before adding another library i decided to parse
        // this really simple date format on my own
        if (dateString.length == 6) {
            try {
                var year = dateString.substring(0, 2).toInt()
                val month = dateString.substring(2, 4).toInt()
                val day = dateString.substring(4, 6).toInt()

                if (year > CurrentYearTwoDigit + 1) { // should be rarely the case: years before 2000
                    year -= 100
                }

                return Date(year + 2000, month, day) // java.util.Date years start at 1900 at month at 0 not at 1
            } catch (e: Exception) {
                logError("Could not parse dateString '$dateString'", e)
            }
        }

        return DateFormatter.parse(dateString)!! // fallback to not thread-safe SimpleDateFormat. Works in most cases but not all
    }

    /**
     * Booking date string consists only of MMDD -> we need to take the year from value date string.
     */
    protected open fun parseMt940BookingDate(bookingDateString: String, valueDateString: String, valueDate: Date): Date {
        val bookingDate = parseMt940Date(valueDateString.substring(0, 2) + bookingDateString)

        // there are rare cases that booking date is e.g. on 31.12.2019 and value date on 01.01.2020 -> booking date would be on 31.12.2020 (and therefore in the future)
        val bookingDateMonth = bookingDate.month()
        if (bookingDateMonth != valueDate.month() && bookingDateMonth == Month.December) {
            return parseMt940Date("" + (valueDate.year() - 1 - 2000) + bookingDateString)
        }

        return bookingDate
    }

    protected open fun parseAmount(amountString: String): Amount {
        return Amount(amountString)
    }


    protected open fun logError(message: String, e: Exception?) {
        logAppender?.let { logAppender ->
            logAppender.logError(message, e, log)
        }
        ?: run {
            log.error(e) { message }
        }
    }

}