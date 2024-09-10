package net.codinux.banking.fints.util

import net.codinux.banking.fints.model.TanMethod
import net.codinux.banking.fints.model.TanMethodType


open class TanMethodSelector {

  companion object {

    val NonVisual = listOf(TanMethodType.DecoupledTan, TanMethodType.DecoupledPushTan, TanMethodType.AppTan, TanMethodType.SmsTan, TanMethodType.ChipTanManuell, TanMethodType.EnterTan)

    val ImageBased = listOf(TanMethodType.QrCode, TanMethodType.ChipTanQrCode, TanMethodType.photoTan, TanMethodType.ChipTanPhotoTanMatrixCode)

    /**
     * NonVisualOrImageBased is a good default for most users as it lists the most simplistic ones (which also work with
     * the command line) first and then continues with image based TAN methods, which for UI applications are easily to display.
     */
    val NonVisualOrImageBased = buildList {
      // decoupled TAN method is the most simplistic TAN method, user only has to confirm the action in her TAN app, no manual TAN entering required
      // AppTan is the second most simplistic TAN method: user has to confirm action in her TAN app and then enter the displayed TAN
      addAll(listOf(TanMethodType.DecoupledTan, TanMethodType.DecoupledPushTan, TanMethodType.AppTan, TanMethodType.SmsTan, TanMethodType.EnterTan))
      addAll(ImageBased)
      addAll(listOf(TanMethodType.ChipTanManuell)) // this is quite inconvenient for user, so i added it as last
    }

  }


  open fun getSuggestedTanMethod(tanMethods: List<TanMethod>): TanMethod? {
    return findPreferredTanMethod(tanMethods, NonVisualOrImageBased) // we use NonVisualOrImageBased as it provides a good default for most users
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

}