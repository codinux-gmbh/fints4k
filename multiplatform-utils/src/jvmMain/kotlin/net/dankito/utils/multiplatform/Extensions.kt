package net.dankito.utils.multiplatform


fun String?.toFile(): java.io.File? {
    return this?.let { File(it) }
}

fun java.util.Date.toDate(): Date {
    return Date(this.time)
}