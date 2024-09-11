package net.codinux.banking.fints.model


enum class MessageType {

    AnonymousDialogInit,

    DialogInit,

    DialogEnd,

    GetTanMedia,

    ChangeTanMedium,

    SynchronizeCustomerSystemId,

    CheckDecoupledTanStatus,

    Tan,

    GetBalance,

    GetTransactions,

    GetCreditCardTransactions,

    GetSecuritiesAccountBalance,

    TransferMoney

}