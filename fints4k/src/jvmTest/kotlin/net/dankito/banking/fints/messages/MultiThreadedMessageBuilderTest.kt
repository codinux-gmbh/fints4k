package net.dankito.banking.fints.messages

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.dankito.banking.fints.FinTsTestBase
import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.*
import net.dankito.banking.fints.messages.segmente.id.CustomerSegmentId
import net.dankito.banking.fints.model.*
import net.dankito.banking.fints.response.segments.*
import net.dankito.banking.fints.util.FinTsUtils
import java.util.concurrent.Executors
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiThreadedMessageBuilderTest : FinTsTestBase() {

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


    private val bank = createTestBank()

    @AfterTest
    fun tearDown() {
        bank.supportedJobs = listOf()
    }


    @Test
    fun testSegmentNumberOrderForParallelMessageCreation(): Unit = runTest {
        // in previous version when FinTsClient has been used in multi-threaded environments, e.g. on web servers were
        // messages are created for multiple parallel users, the segment numbers were wrong and not incrementally ordered

        val bank = createBankWithAllFeatures()

        val context = createContext(bank)

        val dispatcher = Executors.newFixedThreadPool(24).asCoroutineDispatcher()
        val coroutineScope = CoroutineScope(dispatcher)

        IntRange(0, 10_000).map { index ->
            coroutineScope.async {
//                context.startNewDialog()
                val result = createRandomMessage(index, context)

                val (segments, segmentNumbers) = extractSegmentNumbers(result)

                // assert that segment numbers are in ascending order in steps of one
                segmentNumbers.dropLast(1).forEachIndexed { index, segmentNumber ->
                    val nextSegmentNumber = segmentNumbers[index + 1]

                    assertEquals(segmentNumber + 1, nextSegmentNumber,
                        "Message numbers should be in ascending order with step one:\n${segments[index]}\n${segments[index + 1]}")
                }

                assertEquals(1, segmentNumbers.first())
                assertEquals(segmentNumbers.size, segmentNumbers.last())

                segments
            }
        }.awaitAll()
    }

    private fun extractSegmentNumbers(result: MessageBuilderResult): Pair<List<String>, List<Int>> {
        val segments = result.createdMessage!!.split("'").filter { it.isNotBlank() && it.startsWith("HNVSK") == false }.map { segment ->
            if (segment.startsWith("HNVSD")) segment.substring(segment.indexOf("HNSHK"))
            else segment
        }
        val segmentNumbers = segments.map { segment ->
            val indexOfFirstSeparator = segment.indexOf(':')
            val indexOfSecondSeparator = segment.indexOf(':', indexOfFirstSeparator + 1)
            segment.substring(indexOfFirstSeparator + 1, indexOfSecondSeparator).toInt()
        }

        return Pair(segments, segmentNumbers)
    }

    private fun createRandomMessage(index: Int, context: JobContext, account: AccountData = bank.accounts.first()): MessageBuilderResult = when (index % 14) {
        0 -> underTest.createAnonymousDialogInitMessage(context)
        2 -> underTest.createInitDialogMessage(context)
        3 -> underTest.createInitDialogMessageWithoutStrongCustomerAuthentication(context, null)
        4 -> underTest.createSynchronizeCustomerSystemIdMessage(context)
        5 -> underTest.createGetTanMediaListMessage(context)
        6 -> underTest.createChangeTanMediumMessage(context, TanGeneratorTanMedium(TanMediumKlasse.TanGenerator, TanMediumStatus.Aktiv, "", null, null, null, null, null), null, null)
        7 -> underTest.createGetBalanceMessage(context, account)
        8 -> underTest.createGetTransactionsMessage(context, GetAccountTransactionsParameter(bank, account, true))
        9 -> underTest.createGetTransactionsMessage(context, GetAccountTransactionsParameter(bank, bank.accounts[1], true))
        10 -> underTest.createBankTransferMessage(context, BankTransferData("", "", "", Money.Zero, null), account)
        11 -> underTest.createBankTransferMessage(context, BankTransferData("", "", "", Money.Zero, null, true), account)
        12 -> underTest.createSendEnteredTanMessage(context, "", TanResponse(TanProcess.TanProcess2, null, null, null, null, null, null, "HITAN:5:6:4+4++4937-10-13-02.30.03.700259+Sie möchten eine \"Umsatzabfrage\" freigeben?: Bitte bestätigen Sie den \"Startcode 80085335\" mit der Taste \"OK\".+@12@100880085335++Kartennummer ******0892"))
        13 -> underTest.createDialogEndMessage(context)
        else -> underTest.createAnonymousDialogEndMessage(context)
    }


    private fun createBankWithAllFeatures(): BankData {
        val getTransactionsJob = RetrieveAccountTransactionsParameters(JobParameters(CustomerSegmentId.AccountTransactionsMt940.id, 1, 1, null, "HIKAZS:73:5"), 180, true, false)
        val changeTanMediumJob = createAllowedJob(CustomerSegmentId.ChangeTanMedium, 3)
        bank.supportedJobs = listOf(
            getTransactionsJob,
            createAllowedJob(CustomerSegmentId.TanMediaList, 5), changeTanMediumJob,
            createAllowedJob(CustomerSegmentId.Balance, 7),
            createAllowedJob(CustomerSegmentId.CreditCardTransactions, 2),
            SepaAccountInfoParameters(createAllowedJob(CustomerSegmentId.SepaBankTransfer, 1), true, true, true, true, 35, listOf("pain.001.001.03")),
            SepaAccountInfoParameters(createAllowedJob(CustomerSegmentId.SepaRealTimeTransfer, 1), true, true, true, true, 35, listOf("pain.001.001.03")),
        )
        bank.pinInfo = PinInfo(getTransactionsJob, null, null, null, null, null, listOf(
            JobTanConfiguration(CustomerSegmentId.Balance.id, true),
            JobTanConfiguration(CustomerSegmentId.AccountTransactionsMt940.id, true),
            JobTanConfiguration(CustomerSegmentId.CreditCardTransactions.id, true),
            JobTanConfiguration(CustomerSegmentId.SepaBankTransfer.id, true),
            JobTanConfiguration(CustomerSegmentId.SepaRealTimeTransfer.id, true)
        ))
        bank.changeTanMediumParameters = ChangeTanMediaParameters(changeTanMediumJob, false, false, false, false, false, listOf())

        val checkingAccount = AccountData(CustomerId, null, BankCountryCode, BankCode, "ABCDDEBBXXX", CustomerId, AccountType.Girokonto, "EUR", "", null, null, bank.supportedJobs.map { it.jobName }, bank.supportedJobs)
        bank.addAccount(checkingAccount)

        val creditCardAccountJobs = bank.supportedJobs.filterNot { it.jobName == CustomerSegmentId.AccountTransactionsMt940.id }
        val creditCardAccount = AccountData(CustomerId + "_CreditCard", null, BankCountryCode, BankCode, "ABCDDEBBXXX", CustomerId, AccountType.Kreditkartenkonto, "EUR", "", null, null, creditCardAccountJobs.map { it.jobName }, creditCardAccountJobs)
        bank.addAccount(creditCardAccount)

        return bank
    }
}