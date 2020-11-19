package net.dankito.banking.bankfinder


open class InMemoryBankFinder() : BankFinderBase(), IBankFinder {

    constructor(bankList: List<BankInfo>) : this() {
        this.bankListField = bankList
    }


    protected var bankListField: List<BankInfo>? = null


    override fun findBankByBankCode(query: String): List<BankInfo> {
        if (query.isEmpty()) {
            return getBankList()
        }

        return getBankList().filter { it.bankCode.startsWith(query) }
    }

    override fun findBankByNameBankCodeOrCityForNonEmptyQuery(query: String): List<BankInfo> {
        val queryPartsLowerCase = query.toLowerCase().split(" ", "-")

        return getBankList().filter { bankInfo ->
            checkIfAllQueryPartsMatchBankNameBankCodeOrCity(queryPartsLowerCase, bankInfo)
        }
    }

    protected open fun checkIfAllQueryPartsMatchBankNameBankCodeOrCity(queryPartsLowerCase: List<String>, bankInfo: BankInfo): Boolean {
        for (queryPartLowerCase in queryPartsLowerCase) {
            if (checkIfQueryMatchesBankNameBankCodeOrCity(bankInfo, queryPartLowerCase) == false) {
                return false
            }
        }

        return true
    }

    protected open fun checkIfQueryMatchesBankNameBankCodeOrCity(bankInfo: BankInfo, queryLowerCase: String): Boolean {
        return bankInfo.name.toLowerCase().contains(queryLowerCase)
                || bankInfo.bankCode.startsWith(queryLowerCase)
                || bankInfo.city.toLowerCase().startsWith(queryLowerCase)
                || bankInfo.branchesInOtherCities.any { it.toLowerCase().startsWith(queryLowerCase) }
    }


    override fun searchBankByBic(bic: String): BankInfo? {
        return getBankList().firstOrNull { it.bic == bic }
    }


    override fun preloadBankList() {
        findBankByBankCode("")
    }


    override fun getBankList(): List<BankInfo> {
        bankListField?.let {
            return it
        }

        val bankList = loadBankList()

        this.bankListField = bankList

        return bankList
    }

}