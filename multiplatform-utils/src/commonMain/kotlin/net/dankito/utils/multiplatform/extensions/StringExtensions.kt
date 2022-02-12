package net.dankito.utils.multiplatform.extensions

import net.dankito.utils.multiplatform.StringHelper


fun String.format(vararg args: Any?): String {
  return StringHelper.format(this, args)
}