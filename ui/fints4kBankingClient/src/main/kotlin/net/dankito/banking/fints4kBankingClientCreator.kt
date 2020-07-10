package net.dankito.banking

import net.dankito.banking.ui.BankingClientCallback
import net.dankito.banking.ui.IBankingClient
import net.dankito.banking.ui.IBankingClientCreator
import net.dankito.banking.bankfinder.BankInfo
import net.dankito.banking.util.IAsyncRunner
import java.io.File


open class fints4kBankingClientCreator : IBankingClientCreator {

    override fun createClient(
        bankInfo: BankInfo,
        customerId: String,
        pin: String,
        dataFolder: File,
        asyncRunner: IAsyncRunner,
        callback: BankingClientCallback
    ): IBankingClient {

        return fints4kBankingClient(bankInfo, customerId, pin, dataFolder, callback = callback)
    }

}