package net.dankito.banking.fints.log

import net.dankito.banking.fints.model.BankData
import net.dankito.utils.multiplatform.log.Logger


interface IMessageLogAppender {

    fun logError(message: String, e: Exception? = null, logger: Logger? = null, bank: BankData? = null)

}