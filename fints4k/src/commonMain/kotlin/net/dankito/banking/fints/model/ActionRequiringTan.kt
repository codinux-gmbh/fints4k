package net.dankito.banking.fints.model

enum class ActionRequiringTan {
    GetAnonymousBankInfo,

    GetTanMedia,

    ChangeTanMedium,

    GetAccountInfo,

    GetTransactions,

    TransferMoney
}