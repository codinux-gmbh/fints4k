package net.dankito.banking.fints.log

import net.dankito.banking.fints.model.AccountData
import net.dankito.banking.fints.model.BankData
import net.dankito.banking.fints.model.MessageType
import net.dankito.banking.fints.model.JobContextType


class MessageContext(
    val jobType: JobContextType,
    val dialogType: MessageType,
    val jobNumber: Int,
    val dialogNumber: Int,
    val messageNumber: Int,
    val bank: BankData,
    val account: AccountData?
)