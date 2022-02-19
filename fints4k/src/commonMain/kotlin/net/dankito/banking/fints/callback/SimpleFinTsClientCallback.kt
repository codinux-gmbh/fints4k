package net.dankito.banking.fints.callback

import net.dankito.banking.fints.messages.datenelemente.implementierte.tan.TanGeneratorTanMedium
import net.dankito.banking.fints.model.*


open class SimpleFinTsClientCallback(
    protected open val enterTan: ((bank: BankData, tanChallenge: TanChallenge) -> EnterTanResult)? = null,
    protected open val enterTanGeneratorAtc: ((bank: BankData, tanMedium: TanGeneratorTanMedium) -> EnterTanGeneratorAtcResult)? = null,
    protected open val askUserForTanMethod: ((supportedTanMethods: List<TanMethod>, suggestedTanMethod: TanMethod?) -> TanMethod?)? = null
) : FinTsClientCallback {

    constructor() : this(null) // Swift does not support default parameter values -> create constructor overloads

    constructor(enterTan: ((bank: BankData, tanChallenge: TanChallenge) -> EnterTanResult)?) : this(enterTan, null)


    override suspend fun askUserForTanMethod(supportedTanMethods: List<TanMethod>, suggestedTanMethod: TanMethod?): TanMethod? {

        return askUserForTanMethod?.invoke(supportedTanMethods, suggestedTanMethod) ?: suggestedTanMethod
    }

    override suspend fun enterTan(bank: BankData, tanChallenge: TanChallenge): EnterTanResult {
        return enterTan?.invoke(bank, tanChallenge) ?: EnterTanResult.userDidNotEnterTan()
    }

    override suspend fun enterTanGeneratorAtc(bank: BankData, tanMedium: TanGeneratorTanMedium): EnterTanGeneratorAtcResult {
        return enterTanGeneratorAtc?.invoke(bank, tanMedium) ?: EnterTanGeneratorAtcResult.userDidNotEnterAtc()
    }

}