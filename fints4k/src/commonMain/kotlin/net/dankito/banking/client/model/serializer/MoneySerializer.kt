package net.dankito.banking.client.model.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.dankito.banking.fints.model.Amount
import net.dankito.banking.fints.model.Currency
import net.dankito.banking.fints.model.Money


class MoneySerializer : KSerializer<Money> {

  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Money", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Money) {
    encoder.encodeString(value.amount.string + " " + value.currency.code)
  }

  override fun deserialize(decoder: Decoder): Money {
    val value = decoder.decodeString()
    val parts = value.split(" ")

    if (parts.size > 1) {
      return Money(Amount(parts[0]), parts[1])
    }

    return Money(Amount(value), Currency.DefaultCurrencyCode)
  }

}