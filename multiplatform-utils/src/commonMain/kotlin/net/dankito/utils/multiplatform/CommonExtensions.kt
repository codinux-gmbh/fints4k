package net.dankito.utils.multiplatform


val Char.isLowerCase: Boolean
    get() = lowercaseChar() == this

val Char.isUpperCase: Boolean
    get() = isLowerCase == false


fun CharArray.asString(): String {
    return this.joinToString("")
}


fun Date.isBefore(other: Date): Boolean {
    return compareTo(other) < 0
}

fun Date.isBeforeOrEquals(other: Date): Boolean {
    return compareTo(other) <= 0
}


fun Throwable.getAllExceptionMessagesJoined(maxDepth: Int = 5): String {
    return getAllExceptionMessages(maxDepth).joinToString("\n")
}

fun Throwable.getAllExceptionMessages(maxDepth: Int = 5): List<String> {
    val exceptionMessages = mutableSetOf<String>()
    var innerException: Throwable? = this
    var depth = 0

    do {
        innerException?.message?.let { message ->
            exceptionMessages.add("${innerException!!::class.simpleName}: $message")
        }

        innerException = innerException?.cause
        depth++
    } while (innerException != null && depth < maxDepth)

    return exceptionMessages.toList()
}

fun Throwable.getInnerExceptionMessage(maxDepth: Int = 3): String {
    return this.getInnerException(maxDepth).message ?: ""
}

fun Throwable.getInnerException(maxDepth: Int = 3): Throwable {
    var innerException = this
    var depth = 0

    while (innerException.cause is Throwable && depth < maxDepth) {
        innerException = innerException.cause as Throwable
        depth++
    }

    return innerException
}