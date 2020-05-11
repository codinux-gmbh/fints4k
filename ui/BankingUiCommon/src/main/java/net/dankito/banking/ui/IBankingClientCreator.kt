package net.dankito.banking.ui

import net.dankito.fints.model.BankInfo
import net.dankito.utils.IThreadPool
import java.io.File


interface IBankingClientCreator {

    fun createClient(
        bankInfo: BankInfo, // TODO: create own value object to get rid off fints4java dependency
        customerId: String,
        pin: String,
        dataFolder: File,
        threadPool: IThreadPool, // TODO: wrap away JavaUtils IWebClient
        callback: BankingClientCallback
    ): IBankingClient

}