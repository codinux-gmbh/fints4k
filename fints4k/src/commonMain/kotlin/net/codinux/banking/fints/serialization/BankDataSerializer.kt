package net.codinux.banking.fints.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.codinux.banking.fints.model.BankData

object BankDataSerializer : KSerializer<BankData> {

    private val serializer = SerializedFinTsData.serializer()

    private val mapper = SerializedFinTsDataMapper()


    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: BankData) {
        val surrogate = mapper.map(value)

        encoder.encodeSerializableValue(serializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): BankData {
        val surrogate = decoder.decodeSerializableValue(serializer)

        return mapper.map(surrogate)
    }

}