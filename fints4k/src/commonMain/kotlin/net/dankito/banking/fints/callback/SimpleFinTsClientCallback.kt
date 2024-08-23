package net.dankito.banking.fints.callback

import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanGeneratorTanMedium
import net.dankito.banking.fints.model.*


open class SimpleFinTsClientCallback(
    protected open val askUserForTanMethod: ((supportedTanMethods: List<TanMethod>, suggestedTanMethod: TanMethod?) -> TanMethod?)? = null,
    protected open val messageLogAdded: ((MessageLogEntry) -> Unit)? = null,
    protected open val enterTanGeneratorAtc: ((bank: BankData, tanMedium: TanGeneratorTanMedium) -> EnterTanGeneratorAtcResult)? = null,
    protected open val enterTan: ((tanChallenge: TanChallenge) -> Unit)? = null
) : FinTsClientCallback {

    constructor() : this(null as ((tanChallenge: TanChallenge) -> Unit)?) // Swift does not support default parameter values -> create constructor overloads

    constructor(enterTan: ((tanChallenge: TanChallenge) -> Unit)?) : this(null, null, null, enterTan)


    override suspend fun askUserForTanMethod(supportedTanMethods: List<TanMethod>, suggestedTanMethod: TanMethod?): TanMethod? {

        return askUserForTanMethod?.invoke(supportedTanMethods, suggestedTanMethod) ?: suggestedTanMethod
    }

    override suspend fun enterTan(tanChallenge: TanChallenge) {
        enterTan?.invoke(tanChallenge) ?: run { tanChallenge.userDidNotEnterTan() }
    }

    override suspend fun enterTanGeneratorAtc(bank: BankData, tanMedium: TanGeneratorTanMedium): EnterTanGeneratorAtcResult {
        return enterTanGeneratorAtc?.invoke(bank, tanMedium) ?: EnterTanGeneratorAtcResult.userDidNotEnterAtc()
    }

    override fun messageLogAdded(messageLogEntry: MessageLogEntry) {
        messageLogAdded?.invoke(messageLogEntry)
    }

}