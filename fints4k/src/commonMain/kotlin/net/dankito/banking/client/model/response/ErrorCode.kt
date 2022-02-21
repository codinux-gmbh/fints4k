package net.dankito.banking.client.model.response


enum class ErrorCode {

  BankDoesNotSupportFinTs3,

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