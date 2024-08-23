package net.codinux.banking.fints.model


enum class MessageType {

    AnonymousDialogInit,

    DialogInit,

    DialogEnd,

    GetTanMedia,

    ChangeTanMedium,

    SynchronizeCustomerSystemId,

    Tan,

    GetBalance,

    GetTransactions,

    GetCreditCardTransactions,

    TransferMoney

}