package net.dankito.banking.banklistcreator.prettifier


enum class BankListPrettifierOption {

    /**
     * Maps e.g. 'DB Privat- und Firmenkundenbank ...' to 'Deutsche Bank' or 'Deutsche Kreditbank' to 'DKB'
     */
    MapBankNamesToWellKnownNames,

    /**
     * Often the same bank is contained multiple times but with a '(Gf 2)' etc. suffix in name. Filters these out that have the same bank code and postal code.
     */
    RemoveBanksWithSameBankCodeAndPostalCode,

    /**
     * Often the same bank is contained multiple times but with a '(Gf 2)' etc. suffix in name. Filters these out that have the same bank code and city.
     */
    RemoveBanksWithSameBankCodeAndCity,

    /**
     * By default each branch has its own BankInfo. With this option only main branch keeps its BankInfo and all branches get added to branchesInOtherCities.
     */
    MergeAllBranchesOfBankIntoOne,

    /**
     * Removes banks like 'Deutsche Bank (Gf intern)' or 'UniCredit Bank - HVB Settlement EAC01' etc.
     */
    RemoveInstitutionInternalBank

}