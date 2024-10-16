package net.codinux.banking.fints.callback

import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMedium
import net.codinux.banking.fints.model.*


interface FinTsClientCallback {

    /**
     * When user did not select a TAN method, this method gets called so that user selects one.
     *
     * As almost all FinTS messages need the selected TAN method, this method gets called quite early.
     *
     * As a simplification fints4k already suggests which TAN method may is the best one for user.
     *
     * If you do not support an enter tan dialog or if your enter tan dialog supports selecting a TAN method, it's
     * best returning [suggestedTanMethod] and to not show an extra select TAN method dialog.
     */
    suspend fun askUserForTanMethod(supportedTanMethods: List<TanMethod>, suggestedTanMethod: TanMethod?): TanMethod?

    suspend fun enterTan(tanChallenge: TanChallenge)

    /**
     * This method gets called for chipTan TAN generators when the bank asks the customer to synchronize her/his TAN generator.
     *
     * If you do not support entering TAN generator ATC, return [EnterTanGeneratorAtcResult.userDidNotEnterAtc]
     */
    suspend fun enterTanGeneratorAtc(bank: BankData, tanMedium: TanMedium): EnterTanGeneratorAtcResult

    /**
     * Gets fired when a FinTS message get sent to bank server, a FinTS message is received from bank server or an error occurred.
     *
     * Be aware, in order that this message gets fired [net.codinux.banking.fints.config.FinTsClientOptions.fireCallbackOnMessageLogs] has to be set to true.
     */
    fun messageLogAdded(messageLogEntry: MessageLogEntry)

}