package net.dankito.banking.fints.log

import net.dankito.banking.fints.model.AccountData
import net.dankito.banking.fints.model.BankData


class MessageContext(
    val jobNumber: Int,
    val dialogNumber: Int,
    val bank: BankData,
    val account: AccountData?
)