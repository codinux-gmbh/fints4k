package net.dankito.banking.fints.webclient

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KtorWebClientTest {

    private val underTest = KtorWebClient()

    @Test
    fun get() = runTest {
        val result = underTest.get("https://staging.dankito.net/bankfinder?maxItems=1&query=720")

        assertTrue(result.successful)
        assertEquals(200, result.responseCode)
        assertNotNull(result.body)
    }

}