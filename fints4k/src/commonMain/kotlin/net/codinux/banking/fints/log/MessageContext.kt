package net.codinux.banking.fints.log

import net.codinux.banking.fints.model.AccountData
import net.codinux.banking.fints.model.BankData
import net.codinux.banking.fints.model.MessageType
import net.codinux.banking.fints.model.JobContextType


class MessageContext(
    val jobType: JobContextType,
    val dialogType: MessageType,
    val jobNumber: Int,
    val dialogNumber: Int,
    val messageNumber: Int,
    val bank: BankData,
    val account: AccountData?
)