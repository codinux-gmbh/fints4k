package net.codinux.banking.fints.callback

import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMedium
import net.codinux.banking.fints.model.*


open class NoOpFinTsClientCallback : FinTsClientCallback {

    override suspend fun askUserForTanMethod(supportedTanMethods: List<TanMethod>, suggestedTanMethod: TanMethod?): TanMethod? {
        return suggestedTanMethod
    }

    override suspend fun enterTan(tanChallenge: TanChallenge) {
        return tanChallenge.userDidNotEnterTan()
    }

    override suspend fun enterTanGeneratorAtc(bank: BankData, tanMedium: TanMedium): EnterTanGeneratorAtcResult {
        return EnterTanGeneratorAtcResult.userDidNotEnterAtc()
    }

    override fun messageLogAdded(messageLogEntry: MessageLogEntry) {

    }

}