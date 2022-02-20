package net.dankito.banking.client.model.response


enum class ErrorCode {

  InternalError,

  BankReturnedError,

  WrongCredentials,

  AccountLocked,

  JobNotSupported,

  UserCancelledAction,

  TanRequiredButShouldAbortIfRequiresTan,

  NoneOfTheAccountsSupportsRetrievingData,

  DidNotRetrieveAllAccountData

}