package net.dankito.banking.fints.callback

import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanGeneratorTanMedium
import net.dankito.banking.fints.model.*


open class NoOpFinTsClientCallback : FinTsClientCallback {

    override suspend fun askUserForTanMethod(supportedTanMethods: List<TanMethod>, suggestedTanMethod: TanMethod?): TanMethod? {
        return suggestedTanMethod
    }

    override suspend fun enterTan(tanChallenge: TanChallenge) {
        return tanChallenge.userDidNotEnterTan()
    }

    override suspend fun enterTanGeneratorAtc(bank: BankData, tanMedium: TanGeneratorTanMedium): EnterTanGeneratorAtcResult {
        return EnterTanGeneratorAtcResult.userDidNotEnterAtc()
    }

    override fun messageLogAdded(messageLogEntry: MessageLogEntry) {

    }

}