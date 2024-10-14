package net.codinux.banking.fints.messages

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import net.codinux.banking.fints.FinTsTestBase
import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.JobTanConfiguration
import net.codinux.banking.fints.messages.segmente.id.CustomerSegmentId
import net.codinux.banking.fints.model.*
import net.codinux.banking.fints.response.segments.AccountType
import net.codinux.banking.fints.response.segments.JobParameters
import net.codinux.banking.fints.response.segments.PinInfo
import net.codinux.banking.fints.response.segments.RetrieveAccountTransactionsParameters
import net.codinux.banking.fints.util.FinTsUtils
import kotlin.test.*


class MessageBuilderTest : FinTsTestBase() {

    private val underTest = object : MessageBuilder(object : FinTsUtils() {
        override fun formatDate(date: LocalDate): String {
            return Date.toString()
        }

        override fun formatTime(time: LocalTime): String {
            return Time.toString()
        }
    }) {

        override fun createControlReference(): String {
            return ControlReference
        }

    }


    // we need to create our own copy as otherwise Kotlin/Native throws an InvalidMutabilityException
    private val bank = createTestBank()

    @AfterTest
    fun tearDown() {
        bank.supportedJobs = listOf()
    }


    @Test
    fun createAnonymousDialogInitMessage() {

        // given
        val context = createContext(bank)

        // when
        val result = underTest.createAnonymousDialogInitMessage(context).createdMessage

        // then
        assertEquals(result,
            "HNHBK:1:3+000000000125+300+0+1'" +
            "HKIDN:2:2+280:12345678+${CustomerId}+0+0'" +
            "HKVVB:3:3+0+0+${Language.code}+$ProductName+$ProductVersion'" +
            "HNHBS:4:1+1'"
        )
    }

    @Test
    fun createAnonymousDialogEndMessage() {

        // given
        val dialogId = createDialogId()
        val context = createContext(bank, dialogId)

        // when
        val result = underTest.createAnonymousDialogEndMessage(context).createdMessage ?: ""

        // then
        assertEquals(normalizeBinaryData(result), normalizeBinaryData(
            "HNHBK:1:3+000000000067+300+$dialogId+1'" +
            "HKEND:2:1+$dialogId'" +
            "HNHBS:3:1+1'"
        ))
    }


    @Test
    fun createDialogInitMessage() {

        // given
        val context = createContext(bank)

        // when
        val result = underTest.createSynchronizeCustomerSystemIdMessage(context).createdMessage ?: ""

        // then
        assertEquals(normalizeBinaryData(result), normalizeBinaryData(
            "HNHBK:1:3+000000000397+300+0+1'" +
            "HNVSK:998:3+PIN:2+998+1+1::0+1:$Date:$Time+2:2:13:@8@        :5:1+280:$BankCode:$CustomerId:V:0:0+0'" +
            "HNVSD:999:1+@234@" + "HNSHK:2:4+PIN:2+${SecurityFunction.code}+$ControlReference+1+1+1::0+1+1:$Date:$Time+1:999:1+6:10:16+280:$BankCode:$CustomerId:S:0:0'" +
            "HKIDN:3:2+280:$BankCode+$CustomerId+0+0'" +
            "HKVVB:4:3+0+0+${Language.code}+$ProductName+$ProductVersion'" +
            "HKTAN:5:6+4+HKIDN'" +
            "HKSYN:6:3+0'" +
            "HNSHA:7:2+$ControlReference++$Pin''" +
            "HNHBS:8:1+1'"
        ))
    }

    @Test
    fun createDialogEndMessage() {

        // given
        val dialogId = createDialogId()
        val context = createContext(bank, dialogId)

        // when
        val result = underTest.createDialogEndMessage(context).createdMessage ?: ""

        // then
        assertEquals(normalizeBinaryData(result), normalizeBinaryData(
            "HNHBK:1:3+000000000309+300+$dialogId+1'" +
            "HNVSK:998:3+PIN:2+998+1+1::0+1:$Date:$Time+2:2:13:@8@        :5:1+280:$BankCode:$CustomerId:V:0:0+0'" +
            "HNVSD:999:1+@140@" + "HNSHK:2:4+PIN:2+${SecurityFunction.code}+$ControlReference+1+1+1::0+1+1:$Date:$Time+1:999:1+6:10:16+280:$BankCode:$CustomerId:S:0:0'" +
            "HKEND:3:1+$dialogId'" +
            "HNSHA:4:2+$ControlReference++$Pin''" +
            "HNHBS:5:1+1'"
        ))
    }


    @Test
    fun createGetTransactionsMessage_JobIsNotAllowed() {

        // given
        val context = createContext(bank)

        // when
        val result = underTest.createGetTransactionsMessage(context, GetAccountTransactionsParameter(bank, Account))

        // then
        assertFalse(result.isJobAllowed)
    }

    @Test
    fun createGetTransactionsMessage_JobVersionIsNotSupported() {

        // given
        val getTransactionsJob = JobParameters("HKKAZ", 1, 1, null, "HKKAZ:73:5")
        val getTransactionsJobWithPreviousVersion = JobParameters("HKKAZ", 1, 1, null, "HKKAZ:72:4")
        bank.supportedJobs = listOf(getTransactionsJob)
        val account = AccountData(CustomerId, null, BankCountryCode, BankCode, null, CustomerId, AccountType.Girokonto, "EUR", "", null, null, listOf(getTransactionsJob.jobName), listOf(getTransactionsJobWithPreviousVersion))
        bank.addAccount(account)

        val context = createContext(bank)

        // when
        val result = underTest.createGetTransactionsMessage(context, GetAccountTransactionsParameter(bank, account))

        // then
        assertTrue(result.isJobAllowed)
        assertFalse(result.isJobVersionSupported)
    }

    @Test
    fun createGetTransactionsMessage() {

        // given
        val getTransactionsJob = RetrieveAccountTransactionsParameters(JobParameters(CustomerSegmentId.AccountTransactionsMt940.id, 1, 1, null, "HIKAZS:73:5"), 180, true, false)
        bank.supportedJobs = listOf(getTransactionsJob)
        bank.jobsRequiringTan = setOf(CustomerSegmentId.AccountTransactionsMt940.id)
        val account = AccountData(CustomerId, null, BankCountryCode, BankCode, null, CustomerId, AccountType.Girokonto, "EUR", "", null, null, listOf(getTransactionsJob.jobName), listOf(getTransactionsJob))
        bank.addAccount(account)

        val context = createContext(bank)

        val fromDate = LocalDate(2019, Month.AUGUST, 6)
        val toDate = LocalDate(2019, Month.OCTOBER, 21)
        val maxCountEntries = 99

        // when
        val result = underTest.createGetTransactionsMessage(context, GetAccountTransactionsParameter(bank, account, false, fromDate, toDate, maxCountEntries))

        // then
        assertNotNull(result.createdMessage)

        assertEquals(normalizeBinaryData(result.createdMessage!!), normalizeBinaryData(
            "HNHBK:1:3+000000000361+300+0+1'" +
            "HNVSK:998:3+PIN:2+998+1+1::0+1:$Date:$Time+2:2:13:@8@        :5:1+280:$BankCode:$CustomerId:V:0:0+0'" +
            "HNVSD:999:1+@198@" + "HNSHK:2:4+PIN:2+${SecurityFunction.code}+$ControlReference+1+1+1::0+1+1:$Date:$Time+1:999:1+6:10:16+280:$BankCode:$CustomerId:S:0:0'" +
            "HKKAZ:3:${getTransactionsJob.segmentVersion}+$CustomerId::280:$BankCode+N+${convertDate(fromDate)}+${convertDate(toDate)}+$maxCountEntries'" +
            "HKTAN:4:6+4+HKKAZ'" +
            "HNSHA:5:2+$ControlReference++$Pin''" +
            "HNHBS:6:1+1'"
        ))
    }

    @Ignore
    @Test
    fun createGetTransactionsMessage_WithContinuationIdSet() {

        // given
        val getTransactionsJob = RetrieveAccountTransactionsParameters(JobParameters(CustomerSegmentId.AccountTransactionsMt940.id, 1, 1, null, "HIKAZS:73:5"), 180, true, false)
        bank.supportedJobs = listOf(getTransactionsJob)
        bank.jobsRequiringTan = setOf(CustomerSegmentId.AccountTransactionsMt940.id)
        val account = AccountData(CustomerId, null, BankCountryCode, BankCode, null, CustomerId, AccountType.Girokonto, "EUR", "", null, null, listOf(getTransactionsJob.jobName), listOf(getTransactionsJob))
        bank.addAccount(account)

        val context = createContext(bank)

        val fromDate = LocalDate(2019, Month.AUGUST, 6)
        val toDate = LocalDate(2019, Month.OCTOBER, 21)
        val maxCountEntries = 99
        val continuationId = "9345-10-26-11.52.15.693455"

        // when
        val result = underTest.createGetTransactionsMessage(context, // TODO: test Aufsetzpunkt / continuationId
            GetAccountTransactionsParameter(bank, account, false, fromDate, toDate, maxCountEntries, false))

        // then
        assertNotNull(result.createdMessage)

        assertEquals(normalizeBinaryData(result.createdMessage!!), normalizeBinaryData(
            "HNHBK:1:3+000000000340+300+0+1'" +
            "HNVSK:998:3+PIN:2+998+1+1::0+1:$Date:$Time+2:2:13:@8@        :5:1+280:$BankCode:$CustomerId:V:0:0+0'" +
            "HNVSD:999:1+@225@" + "HNSHK:2:4+PIN:2+${SecurityFunction.code}+$ControlReference+1+1+1::0+1+1:$Date:$Time+1:999:1+6:10:16+280:$BankCode:$CustomerId:S:0:0'" +
            "HKKAZ:3:${getTransactionsJob.segmentVersion}+$CustomerId::280:$BankCode+N+${convertDate(fromDate)}+${convertDate(toDate)}+$maxCountEntries+$continuationId'" +
            "HKTAN:4:6+4+HKKAZ'" +
            "HNSHA:5:2+$ControlReference++$Pin''" +
            "HNHBS:6:1+1'"
        ))
    }

}