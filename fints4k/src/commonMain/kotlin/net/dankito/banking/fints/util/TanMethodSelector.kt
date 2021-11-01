package net.dankito.banking.fints.util

import net.dankito.banking.fints.model.TanMethod
import net.dankito.banking.fints.model.TanMethodType


open class TanMethodSelector {

  companion object {
    val NonVisual = listOf(TanMethodType.AppTan, TanMethodType.SmsTan, TanMethodType.ChipTanManuell, TanMethodType.EnterTan)
  }


  open fun getSuggestedTanMethod(tanMethods: List<TanMethod>): TanMethod? {
    return tanMethods.firstOrNull { it.type != TanMethodType.ChipTanUsb && it.type != TanMethodType.SmsTan && it.type != TanMethodType.ChipTanManuell }
      ?: tanMethods.firstOrNull { it.type != TanMethodType.ChipTanUsb && it.type != TanMethodType.SmsTan }
      ?: tanMethods.firstOrNull { it.type != TanMethodType.ChipTanUsb }
      ?: tanMethods.firstOrNull()
  }

  open fun findPreferredTanMethod(tanMethods: List<TanMethod>, preferredTanMethods: List<TanMethodType>?): TanMethod? {
    preferredTanMethods?.forEach { preferredTanMethodType ->
      tanMethods.firstOrNull { it.type == preferredTanMethodType }?.let {
        return it
      }
    }

    return null
  }

  open fun selectNonVisual(tanMethods: List<TanMethod>): TanMethod? {
    return findPreferredTanMethod(tanMethods, NonVisual)
      ?: tanMethods.firstOrNull { it.displayName.contains("manuell", true) }
      ?: tanMethods.firstOrNull()
  }

}