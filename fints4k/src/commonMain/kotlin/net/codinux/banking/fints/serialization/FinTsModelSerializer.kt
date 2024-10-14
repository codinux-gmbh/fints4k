package net.codinux.banking.fints.serialization

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.codinux.banking.fints.model.BankData
import net.codinux.log.logger

open class FinTsModelSerializer {

    private val json: Json by lazy {
        Json { this.ignoreUnknownKeys = true }
    }

    private val prettyPrintJson by lazy {
        Json {
            this.ignoreUnknownKeys = true
            this.prettyPrint = true
        }
    }

    private val mapper = SerializedFinTsDataMapper()

    private val log by logger()


    open fun serializeToJson(bank: BankData, prettyPrint: Boolean = false): String? {
        return try {
            val serializableData = mapper.map(bank)

            val json = if (prettyPrint) prettyPrintJson else json

            json.encodeToString(serializableData)
        } catch (e: Throwable) {
            log.error(e) { "Could not map fints4k model to JSON" }
            null
        }
    }

    open fun deserializeFromJson(serializedFinTsData: String): BankData? = try {
        val serializedData = json.decodeFromString<SerializedFinTsData>(serializedFinTsData)

        mapper.map(serializedData)
    } catch (e: Throwable) {
        log.error(e) { "Could not deserialize BankData from JSON:\n$serializedFinTsData"}
        null
    }

}