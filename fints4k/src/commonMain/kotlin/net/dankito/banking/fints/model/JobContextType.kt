package net.dankito.banking.fints.model


enum class JobContextType {

    AnonymousBankInfo,

    GetTanMedia,

    ChangeTanMedium,

    GetAccountInfo,

    AddAccount, // TODO: may split actions and create two JobContexts, one for GetAccountInfo and one for GetTransactions

    GetTransactions,

    TransferMoney

}