package net.dankito.fints.response.segments


open class AccountInfo(
    val accountNumber: String,
    val subAccountAttribute: String?,
    val bankCountryCode: Int,
    val bankCode: String,
    val iban: String?,
    val customerId: String,
    val accountType: AccountType?,
    val currency: String?, // TODO: may parse to a value object
    val accountHolderName1: String,
    val accountHolderName2: String?,
    val productName: String?,
    val accountLimit: String?, // TODO: parse
    val extension: String?, // TODO: parse

    segmentString: String

)
    : ReceivedSegment(segmentString)