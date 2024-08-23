package net.codinux.banking.fints.util

import net.codinux.banking.fints.model.TanMethod
import net.codinux.banking.fints.model.TanMethodType


open class TanMethodSelector {

  companion object {

    val NonVisual = listOf(TanMethodType.AppTan, TanMethodType.SmsTan, TanMethodType.ChipTanManuell, TanMethodType.EnterTan)

    val ImageBased = listOf(TanMethodType.QrCode, TanMethodType.ChipTanQrCode, TanMethodType.photoTan, TanMethodType.ChipTanPhotoTanMatrixCode)

  }


  open fun getSuggestedTanMethod(tanMethods: List<TanMethod>): TanMethod? {
    return tanMethods.firstOrNull { it.type != TanMethodType.ChipTanUsb && it.type != TanMethodType.SmsTan && it.type != TanMethodType.ChipTanManuell }
      ?: tanMethods.firstOrNull { it.type != TanMethodType.ChipTanUsb && it.type != TanMethodType.SmsTan }
      ?: tanMethods.firstOrNull { it.type != TanMethodType.ChipTanUsb }
      ?: first(tanMethods)
  }

  open fun findPreferredTanMethod(tanMethods: List<TanMethod>, preferredTanMethods: List<TanMethodType>?): TanMethod? {
    preferredTanMethods?.forEach { preferredTanMethodType ->
      tanMethods.firstOrNull { it.type == preferredTanMethodType }?.let {
        return it
      }
    }

    return null
  }


  open fun nonVisual(tanMethods: List<TanMethod>): TanMethod? {
    return findPreferredTanMethod(tanMethods, NonVisual)
      ?: tanMethods.firstOrNull { it.displayName.contains("manuell", true) }
  }

  open fun nonVisualOrFirst(tanMethods: List<TanMethod>): TanMethod? {
    return nonVisual(tanMethods)
      ?: first(tanMethods)
  }


  open fun imageBased(tanMethods: List<TanMethod>): TanMethod? {
    return findPreferredTanMethod(tanMethods, ImageBased)
  }

  open fun imageBasedOrFirst(tanMethods: List<TanMethod>): TanMethod? {
    return imageBased(tanMethods)
      ?: first(tanMethods)
  }


  open fun nonVisualOrImageBased(tanMethods: List<TanMethod>): TanMethod? {
    return nonVisual(tanMethods)
      ?: imageBased(tanMethods)
  }

  open fun nonVisualOrImageBasedOrFirst(tanMethods: List<TanMethod>): TanMethod? {
    return nonVisual(tanMethods)
      ?: imageBased(tanMethods)
      ?: first(tanMethods)
  }


  open fun first(tanMethods: List<TanMethod>): TanMethod? {
    return tanMethods.firstOrNull()
  }

}