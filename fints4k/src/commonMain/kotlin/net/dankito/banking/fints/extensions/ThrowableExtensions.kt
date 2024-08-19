package net.dankito.banking.fints.extensions


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

fun Throwable.getInnerException(maxDepth: Int = 3): Throwable {
    var innerException = this
    var depth = 0

    while (innerException.cause is Throwable && depth < maxDepth) {
        innerException = innerException.cause as Throwable
        depth++
    }

    return innerException
}