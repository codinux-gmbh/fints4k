package net.dankito.utils.multiplatform.log

actual class DefaultLoggerFactory actual constructor() {

  actual fun createDefaultLoggerFactory(): ILoggerFactory {
    return LogToConsoleLoggerFactory()
  }

}