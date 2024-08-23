package net.dankito.banking.client.model.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.codinux.banking.fints.model.Amount


class AmountSerializer : KSerializer<Amount> {

  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Amount", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Amount) {
    encoder.encodeString(value.string)
  }

  override fun deserialize(decoder: Decoder): Amount {
    return Amount(decoder.decodeString())
  }

}