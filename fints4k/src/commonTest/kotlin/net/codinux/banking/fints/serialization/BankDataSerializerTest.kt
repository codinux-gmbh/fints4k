package net.codinux.banking.fints.serialization

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.codinux.banking.fints.model.BankData
import net.codinux.banking.fints.test.TestDataGenerator
import net.codinux.banking.fints.test.assertContains
import net.codinux.banking.fints.test.assertSize
import net.codinux.banking.fints.test.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BankDataSerializerTest {

    private val serializedBankData = TestDataGenerator.serializedBankData

    private val json = Json {
        prettyPrint = true
    }


    @Test
    fun serializeToJson() {
        val bank = TestDataGenerator.generateBankDataForSerialization()

        val result = json.encodeToString(bank)

        assertEquals(serializedBankData, result)
    }

    @Test
    fun deserializeFromJson() {
        val result = json.decodeFromString<BankData>(serializedBankData)

        assertNotNull(result)

        assertSize(8, result.tanMethodsSupportedByBank)
        assertSize(4, result.tanMethodsAvailableForUser)
        assertContains(result.tanMethodsSupportedByBank, result.tanMethodsAvailableForUser) // check that it contains exactly the same object instances
        assertNotNull(result.selectedTanMethod)
        assertContains(result.tanMethodsSupportedByBank, result.selectedTanMethod) // check that it contains exactly the same object instance

        assertSize(3, result.tanMedia)
        assertNotNull(result.selectedTanMedium)
        assertContains(result.tanMedia, result.selectedTanMedium) // check that it contains exactly the same object instance

        assertSize(14, result.supportedJobs)
        assertSize(33, result.jobsRequiringTan)

        result.accounts.forEach { account ->
            assertTrue(account.allowedJobs.isNotEmpty())
            assertContains(result.supportedJobs, account.allowedJobs) // check that it contains exactly the same object instances
        }

        assertEquals(serializedBankData, json.encodeToString(result))
    }

}