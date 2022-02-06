package net.dankito.banking.fints.model


enum class MessageType {

    AnonymousDialogInit,

    DialogInit,

    DialogEnd,

    GetTanMedia,

    ChangeTanMedium,

    SynchronizeCustomerSystemId,

    Tan,

    AddAccount,

    GetBalance,

    GetTransactions,

    GetCreditCardTransactions,

    TransferMoney

}