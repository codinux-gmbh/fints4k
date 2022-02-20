package net.dankito.banking.client.model.parameter


enum class RetrieveTransactions {

  No,

  All,

  /**
   * Some banks support that according to PSD2 account transactions of last 90 days may be retrieved without
   * a TAN (= no strong customer authorization needed). So try this options if you don't want to enter a TAN.
   */
  OfLast90Days,

  /**
   * Retrieves account transactions in the boundaries of [GetAccountDataParameter.retrieveTransactionsFrom] to [GetAccountDataParameter.retrieveTransactionsTo].
   */
  AccordingToRetrieveFromAndTo

}