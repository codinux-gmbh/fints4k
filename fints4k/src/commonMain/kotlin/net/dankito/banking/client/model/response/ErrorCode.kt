package net.dankito.banking.client.model.response


enum class ErrorCode {

  BankDoesNotSupportFinTs3,

  NetworkError,

  InternalError,

  BankReturnedError,

  WrongCredentials,

  AccountLocked,

  JobNotSupported,

  UserCancelledAction,

  TanRequiredButShouldAbortIfRequiresTan,

  NoneOfTheAccountsSupportsRetrievingData,

  DidNotRetrieveAllAccountData,

  CanNotDetermineBicForIban,

  NoAccountSupportsMoneyTransfer,

  MoreThanOneAccountSupportsMoneyTransfer

}