package net.dankito.utils.multiplatform.extensions


val Char.isLowerCase: Boolean
    get() = lowercaseChar() == this

val Char.isUpperCase: Boolean
    get() = isLowerCase == false


fun CharArray.asString(): String {
    return this.joinToString("")
}