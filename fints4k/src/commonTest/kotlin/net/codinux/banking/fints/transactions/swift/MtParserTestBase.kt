package net.codinux.banking.fints.transactions.swift

import net.codinux.banking.fints.FinTsTestBase
import net.codinux.banking.fints.transactions.mt940.model.Balance
import net.codinux.banking.fints.transactions.mt940.model.RemittanceInformationField
import net.codinux.banking.fints.transactions.mt940.model.StatementLine
import kotlinx.datetime.LocalDate
import net.codinux.banking.fints.extensions.*
import net.codinux.banking.fints.model.Amount
import net.codinux.banking.fints.test.*


abstract class MtParserTestBase : FinTsTestBase() {

    companion object {

        const val Currency = "EUR"

        val AccountStatement1PreviousStatementBookingDate = LocalDate(1988, 2, 26)
        val AccountStatement1BookingDate = LocalDate(1988, 2, 27)

        val AccountStatement1OpeningBalanceAmount = Amount("12345,67")

        val AccountStatement1Transaction1Amount = Amount("1234,56")
        val AccountStatement1Transaction1OtherPartyName = "Sender1"
        val AccountStatement1Transaction1OtherPartyBankId = "AAAADE12"
        val AccountStatement1Transaction1OtherPartyAccountId = "DE99876543210987654321"

        val AccountStatement1Transaction2Amount = Amount("432,10")
        val AccountStatement1Transaction2OtherPartyName = "Receiver2"
        val AccountStatement1Transaction2OtherPartyBankId = "BBBBDE56"
        val AccountStatement1Transaction2OtherPartyAccountId = "DE77987654321234567890"

        val AccountStatement1ClosingBalanceAmount = Amount("13580,23")
        val AccountStatement1With2TransactionsClosingBalanceAmount = Amount("13148,13")
    }


    protected fun assertBalance(balance: Balance, isCredit: Boolean, bookingDate: LocalDate, amount: Amount) {
        assertEquals(isCredit, balance.isCredit)
        assertEquals(bookingDate, balance.bookingDate)
        assertEquals(amount, balance.amount)
        assertEquals(Currency, balance.currency)
    }

    protected fun assertTurnover(statementLine: StatementLine, valueDate: LocalDate, amount: Amount, isCredit: Boolean = true,
                               bookingDate: LocalDate? = valueDate) {

        assertEquals(isCredit, statementLine.isCredit)
        assertFalse(statementLine.isReversal)
        assertEquals(valueDate, statementLine.valueDate)
        assertEquals(bookingDate, statementLine.bookingDate)
        assertEquals(amount, statementLine.amount)
    }

    protected fun assertTransactionDetails(details: RemittanceInformationField?, otherPartyName: String,
                                         otherPartyBankId: String, otherPartyAccountId: String) {

        assertNotNull(details)

        assertEquals(otherPartyName, details.otherPartyName)
        assertEquals(otherPartyBankId, details.otherPartyBankId)
        assertEquals(otherPartyAccountId, details.otherPartyAccountId)
    }


    protected val AccountStatementWithSingleTransaction = """
        :20:STARTUMSE
        :25:$BankCode/$CustomerId
        :28C:00000/001
        :60F:C${convertMt940Date(AccountStatement1PreviousStatementBookingDate)}EUR$AccountStatement1OpeningBalanceAmount
        :61:${convertMt940Date(AccountStatement1BookingDate)}${convertToShortBookingDate(AccountStatement1BookingDate)}CR${AccountStatement1Transaction1Amount}N062NONREF
        :86:166?00GUTSCHR. UEBERWEISUNG?109249?20EREF+674?21SVWZ+1908301/
        EUR $AccountStatement1Transaction1Amount/20?2219-10-02/...?30$AccountStatement1Transaction1OtherPartyBankId?31$AccountStatement1Transaction1OtherPartyAccountId
        ?32$AccountStatement1Transaction1OtherPartyName
        :62F:C${convertMt940Date(AccountStatement1BookingDate)}EUR$AccountStatement1ClosingBalanceAmount
        -
    """.trimIndent()

    protected val AccountStatementWithTwoTransactions = """
        :20:STARTUMSE
        :25:$BankCode/$CustomerId
        :28C:00000/001
        :60F:C${convertMt940Date(AccountStatement1PreviousStatementBookingDate)}EUR$AccountStatement1OpeningBalanceAmount
        :61:${convertMt940Date(AccountStatement1BookingDate)}${convertToShortBookingDate(AccountStatement1BookingDate)}CR${AccountStatement1Transaction1Amount}N062NONREF
        :86:166?00GUTSCHR. UEBERWEISUNG?109249?20EREF+674?21SVWZ+1908301/
        EUR $AccountStatement1Transaction1Amount/20?2219-10-02/...?30$AccountStatement1Transaction1OtherPartyBankId?31$AccountStatement1Transaction1OtherPartyAccountId
        ?32$AccountStatement1Transaction1OtherPartyName
        :61:${convertMt940Date(AccountStatement1BookingDate)}${convertToShortBookingDate(AccountStatement1BookingDate)}DR${AccountStatement1Transaction2Amount}N062NONREF
        :86:166?00ONLINE-UEBERWEISUNG?109249?20EREF+674?21SVWZ+1908301/
        EUR $AccountStatement1Transaction2Amount/20?2219-10-02/...?30$AccountStatement1Transaction2OtherPartyBankId?31$AccountStatement1Transaction2OtherPartyAccountId
        ?32$AccountStatement1Transaction2OtherPartyName
        :62F:C${convertMt940Date(AccountStatement1BookingDate)}EUR$AccountStatement1With2TransactionsClosingBalanceAmount
        -
    """.trimIndent()

    protected val AccountStatementWithSingleTransaction_SheetNumberOmitted = """
        :20:STARTUMS
        :25:$BankCode/$CustomerId
        :28C:0
        :60F:C200511EUR0,00
        :61:200511D15,00NMSCNONREF
        :86:808?00Entgelt/Auslagen?10907?20Preis bezahlt bis 12.2020
        ?21Jahrespreis?22$AccountHolderName?23Folge-Nr.   0 Verfall 12.23
        ?30$BankCode?31$CustomerId?32Ausgabe einer Debitkarte
        :62F:D200511EUR15,00
        -
    """.trimIndent()

    protected val AccountStatementWithAnnualJumpFromBookingDateToValueDate =
        ":20:STARTUMSE\n" +
        ":25:$BankCode/$CustomerId\n" +
        ":28C:00000/001\n" +
        ":60F:C191227EUR104501,86\n" +
        ":61:2001011230DR3,99N024NONREF\n" +
        ":86:809?00ENTGELTABSCHLUSS?106666?20Entgeltabrechnung?21siehe Anl\n" +
        "age?30$BankCode\n" +
        ":61:2001011230CR0,00N066NONREF\n" +
        ":86:805?00ABSCHLUSS?106666?20Abrechnung 30.12.2019?21siehe Anlage\n" +
        "?30$BankCode\n" +
        ":62F:C191230EUR104490,88\n" +
        "-"

    protected val AccountStatementWithLineStartsWithDashThatIsNotABlockSeparator = "\n" +
            ":20:MT940-2005200849\n" +
            ":21:NONREF\n" +
            ":25:20041111/369300900EUR\n" +
            ":28C:0/1\n" +
            ":60F:C200512EUR0,00\n" +
            ":61:2005120512CR100,00NMSCNONREF//POS 7\n" +
            ":86:820?20ÜBERTRAG / ÜBERWEISUNG?21EROEFFNUNGSBETRAG?22END-TO-END-REF\n" +
            ".:?23NICHT ANGEGEBEN?24Ref. HW220133C3232360/15499?32DAN\n" +
            "NKITO\n" +
            ":62M:C200513EUR100,00\n" +
            "-\n" +
            ":20:MT940-2005200849\n" +
            ":21:NONREF\n" +
            ":25:20041111/369300900EUR\n" +
            ":28C:0/2\n" +
            ":60M:C200513EUR100,00\n" +
            ":61:2005130513CR0,10NMSCNONREF//POS 6\n" +
            ":86:820?20ÜBERTRAG / ÜBERWEISUNG?21TEST?22END-TO-END-REF.:?23NICHT AN\n" +
            "GEGEBEN?24Ref. 7T2C0YTD0BZL4V9S/1?32DANKITO\n" +
            ":61:2005130513CR0,15NMSCNONREF//POS 5\n" +
            ":86:820?20ÜBERTRAG / ÜBERWEISUNG?21ECHTZEITUEBERWEISUNGSTEST?22END-TO\n" +
            "-END-REF.:?23NICHT ANGEGEBEN?24Ref. 402C0YTD0GLPFDFV/1?32DANKI\n" +
            "TO\n" +
            ":61:2005130513CR0,30NMSCNONREF//POS 4\n" +
            ":86:820?20ÜBERTRAG / ÜBERWEISUNG?21UND NOCH EIN TEST FUER JAVA?22FX?2\n" +
            "3END-TO-END-REF.:?24NICHT ANGEGEBEN?25Ref. 5D2C0YTD0HVAB3X3/1?32D\n" +
            "ANKITO\n" +
            ":61:2005130513CR0,10NMSCNONREF//POS 3\n" +
            ":86:820?20ÜBERTRAG / ÜBERWEISUNG?21LASS DIE KOHLE RUEBER WACHS?22EN?2\n" +
            "3END-TO-END-REF.:?24NICHT ANGEGEBEN?25Ref. J3220134C3451151/6200?\n" +
            "32DANKITO\n" +
            ":61:2005130513CR0,01NMSCNONREF//POS 2\n" +
            ":86:820?20ÜBERTRAG / ÜBERWEISUNG?21TEST?22END-TO-END-REF.:?23NICHT AN\n" +
            "GEGEBEN?24Ref. J3220134C3451151/6201?32DANKITO\n" +
            ":62M:C200514EUR100,66\n" +
            "-\n" +
            ":20:MT940-2005200849\n" +
            ":21:NONREF\n" +
            ":25:20041111/369300900EUR\n" +
            ":28C:0/3\n" +
            ":60M:C200514EUR100,66\n" +
            ":61:2005140514DR0,01NMSCNONREF//POS 1\n" +
            ":86:820?20ÜBERTRAG / ÜBERWEISUNG?21END-TO-END-REF.:?22NICHT ANGEGEBEN\n" +
            "?23Ref. J022013510234936/2?30ABCDEFGHIJK?31DE1112345679876543210\n" +
            "?32DANKITO\n" +
            ":62F:C200520EUR100,65\n" +
            "-"

    protected val AccountStatementWithTimeThatGotDetectedAsFieldCode = "\n" +
            ":20:STARTUMS\n" +
            ":25:$BankCode/$CustomerId\n" +
            ":28C:0\n" +
            ":60F:D200514EUR15,00\n" +
            ":61:200514D0,02NMSCKREF+\n" +
            ":86:177?00SEPA Überweisung?10804?20KREF+2020-05-14T00:58:23:09\n" +
            "?2193 ?22SVWZ+Test TAN1:Auftrag nich\n" +
            "?23t TAN-pflichtig IBAN: DE111?23456780987654321 BIC: ABCD\n" +
            "?25DEMM123 ?30$Bic?31$Iban\n" +
            "?32DANKITO\n" +
            ":61:200514D0,05NMSCKREF+\n" +
            ":86:177?00SEPA Überweisung?10804?20KREF+2020-05-14T01:35:20.67\n" +
            "?216 ?22SVWZ+Lass es endlich ruber?23wachsen TAN1:Auftrag nicht \n" +
            "?24TAN-pflichtig IBAN: DE11123?25456780987654321 BIC: ABCDDE\n" +
            "?26MM123 ?30$Bic?31$Iban\n" +
            "?32DANKITO\n" +
            ":61:200514C0,01NMSC\n" +
            ":86:166?00SEPA Gutschrift?10804?20SVWZ+2020-05-14T13:10:34.09\n" +
            "?211 Test transaction b0a557f2?22 f962-4608-9201-f890e1fc037\n" +
            "?23b IBAN: DE11123456780987654?24321 BIC: $Bic \n" +
            "?30$Bic?31$Iban?32DANKITO\n" +
            ":62F:C200514EUR84,28\n" +
            "-"

    protected val QuotationMarkCommaGetsDetectedAsFieldValue = """
            :20:STARTUMS
            :25:$BankCode/$CustomerId
            :28C:0
            :60F:C200511EUR0,00
            :61:200511D15,00NMSCNONREF
            :86:105?00BASISLASTSCHRIFT?10931?20EREF+6MKL2OT30QENNLIU
            ?21MREF+?,3SQNdUbxm9z7dB)+gKYD?22JAKzCM0G?23CRED+DE94ZZZ00000123456
            ?24SVWZ+306-4991422-2405949 NI?25LE Mktp DE 6MKL2OT30QENNLIU?26
            EREF: 6MKL2OT30QENNLIU MRE?27F: ?,3SQNdUbxm9z7dB)+gKYDJA?28KzCM0G
            CRED: DE94ZZZ0000012?293456 IBAN: DE87300308801234?30TUBDDEDD?31DE87300308801234567890?32NILE PAYMENTS EUROPE S.C.?33A.?34992?60567890 BIC: TUBDDEDD 
            :62F:D200511EUR15,00
            -
        """.trimIndent()


    protected fun convertMt940Date(date: LocalDate): String {
        // don't use DateFormatter for this as it's not implemented in Kotlin/Native
        return (date.year % 100).toString() + date.monthNumber.toStringWithMinDigits(2) + date.dayOfMonth.toStringWithMinDigits(2)
    }

    protected fun convertToShortBookingDate(date: LocalDate): String {
        // don't use DateFormatter for this as it's not implemented in Kotlin/Native
        return date.monthNumber.toStringWithMinDigits(2) + date.dayOfMonth.toStringWithMinDigits(2)
    }

}