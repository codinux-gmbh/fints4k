package net.dankito.utils.multiplatform.extensions

import net.dankito.utils.multiplatform.StringHelper


fun Int.format(format: String): String {
  return StringHelper.format(format, this)
}