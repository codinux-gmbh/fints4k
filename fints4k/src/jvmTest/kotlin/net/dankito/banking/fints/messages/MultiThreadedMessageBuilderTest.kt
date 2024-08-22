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

    private val underTest = MessageBuilder()


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
                context.startNewDialog()
                val result = createRandomMessage(index, context, underTest)

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
}