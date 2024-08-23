package net.dankito.banking.client.model.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.codinux.banking.fints.model.Currency


class CurrencySerializer : KSerializer<Currency> {

  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Currency", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Currency) {
    encoder.encodeString(value.code)
  }

  override fun deserialize(decoder: Decoder): Currency {
    return Currency(decoder.decodeString())
  }

}