package net.codinux.banking.fints.transactions.swift

import kotlinx.datetime.*
import net.codinux.banking.fints.extensions.EuropeBerlin
import net.codinux.banking.fints.log.IMessageLogAppender
import net.codinux.banking.fints.model.Amount
import net.codinux.banking.fints.transactions.swift.model.ContinuationIndicator
import net.codinux.banking.fints.transactions.swift.model.Holding
import net.codinux.banking.fints.transactions.swift.model.StatementOfHoldings
import net.codinux.banking.fints.transactions.swift.model.SwiftMessageBlock

open class Mt535Parser(
    logAppender: IMessageLogAppender? = null
) : MtParserBase(logAppender) {

    open fun parseMt535String(mt535String: String): List<StatementOfHoldings> {
        val blocks = parseMtString(mt535String, true)

        // should actually always be only one block, just to be on the safe side
        return blocks.mapNotNull { parseStatementOfHoldings(it) }
    }

    protected open fun parseStatementOfHoldings(mt535Block: SwiftMessageBlock): StatementOfHoldings? {
        try {
            val containsHoldings = mt535Block.getMandatoryField("17B").endsWith("//Y")
            val holdings = if (containsHoldings) parseHoldings(mt535Block) else emptyList()

            return parseStatementOfHoldings(holdings, mt535Block)
        } catch (e: Throwable) {
            logError("Could not parse MT 535 block:\n$mt535Block", e)
        }

        return null
    }

    protected open fun parseStatementOfHoldings(holdings: List<Holding>, mt535Block: SwiftMessageBlock): StatementOfHoldings {
        val totalBalance = parseBalance(mt535Block.getMandatoryRepeatableField("19A").last())

        val accountStatement = mt535Block.getMandatoryField("97A")
        val bankCode = accountStatement.substringAfter("//").substringBefore('/')
        val accountIdentifier = accountStatement.substringAfterLast('/')

        val (pageNumber, continuationIndicator) = parsePageNumber(mt535Block)

        val (statementDate, preparationDate) = parseStatementAndPreparationDate(mt535Block)

        return StatementOfHoldings(bankCode, accountIdentifier, holdings, totalBalance?.first, totalBalance?.second, pageNumber, continuationIndicator, statementDate, preparationDate)
    }

    // this is a MT5(35) specific balance format
    protected open fun parseBalance(balanceString: String?): Pair<Amount, String>? {
        if (balanceString != null) {
            val balancePart = balanceString.substringAfterLast('/')
            val amount = balancePart.substring(3)
            val isNegative = amount.startsWith("N")
            return Pair(Amount(if (isNegative) "-${amount.substring(1)}" else amount), balancePart.substring(0, 3))
        }

        return null
    }

    protected open fun parsePageNumber(mt535Block: SwiftMessageBlock): Pair<Int?, ContinuationIndicator> {
        return try {
            val pageNumberStatement = mt535Block.getMandatoryField("28E")
            val pageNumber = pageNumberStatement.substringBefore('/').toInt()
            val continuationIndicator = pageNumberStatement.substringAfter('/').let { indicatorString ->
                ContinuationIndicator.entries.firstOrNull { it.mtValue == indicatorString } ?: ContinuationIndicator.Unknown
            }

            Pair(pageNumber, continuationIndicator)
        } catch (e: Throwable) {
            logError("Could not parse statement and preparation date of block:\n$mt535Block", e)

            Pair(null, ContinuationIndicator.Unknown)
        }
    }

    protected open fun parseStatementAndPreparationDate(mt535Block: SwiftMessageBlock): Pair<LocalDate?, LocalDate?> {
        return try {
            // TODO: differ between 98A (without time) and 98C (with time)
            // TODO: ignore (before parsing?) 98A/C of holdings which start with ":PRIC//
            val dates = mt535Block.getMandatoryRepeatableField("98").map { it.substringBefore("//") to parse4DigitYearDate(it.substringAfter("//").substring(0, 8)) } // if given we ignore time
            val statementDate = dates.firstOrNull { it.first == ":STAT" }?.second // specifications and their implementations: the statement date is actually mandatory, but not all banks actually set it
            val preparationDate = dates.firstOrNull { it.first == ":PREP" }?.second

            Pair(statementDate, preparationDate)
        } catch (e: Throwable) {
            logError("Could not parse statement and preparation date of block:\n$mt535Block", e)
            Pair(null, null)
        }
    }

    protected open fun parseHoldings(mt535Block: SwiftMessageBlock): List<Holding> {
        val blockLines = mt535Block.getFieldsInOrder()
        val holdingBlocksStartIndices = blockLines.indices.filter { blockLines[it].first == "16R" && blockLines[it].second == "FIN" }
        val holdingBlocksEndIndices = blockLines.indices.filter { blockLines[it].first == "16S" && blockLines[it].second == "FIN" }

        val holdingBlocks = holdingBlocksStartIndices.mapIndexed { blockIndex, startIndex ->
            val endIndex = holdingBlocksEndIndices[blockIndex]
            val holdingBlockLines = blockLines.subList(startIndex + 1, endIndex)
            SwiftMessageBlock(holdingBlockLines)
        }

        return holdingBlocks.mapNotNull { parseHolding(it) }
    }

    protected open fun parseHolding(holdingBlock: SwiftMessageBlock): Holding? =
        try {
            val nameStatementLines = holdingBlock.getMandatoryField("35B").split("\n")
            val isinOrWkn = nameStatementLines.first()
            val isin = if (isinOrWkn.startsWith("ISIN ")) {
                isinOrWkn.substringAfter(' ')
            } else {
                null
            }
            val wkn = if (isin == null) {
                isinOrWkn
            } else if (nameStatementLines[1].startsWith("DE")) {
                nameStatementLines[1]
            } else {
                null
            }

            val name = nameStatementLines.subList(if (isin == null || wkn == null) 1 else 2, nameStatementLines.size).joinToString(" ")

            // TODO: check for optional code :90a: Preis
            // TODO: check for optional code :94B: Herkunft von Preis / Kurs
            // TODO: check for optional code :98A: Herkunft von Preis / Kurs
            // TODO: check for optional code :99A: Anzahl der aufgelaufenen Tage
            // TODO: check for optional code :92B: Exchange rate

            val holdingTotalBalance = holdingBlock.getMandatoryField("93B")
            val balanceIsQuantity = holdingTotalBalance.startsWith(":AGGR//UNIT") // == Die Stückzahl wird als Zahl (Zähler) ausgedrückt
            // else it starts with "AGGR/FAMT" = Die Stückzahl wird als Nennbetrag ausgedrückt. Bei Nennbeträgen wird die Währung durch die „Depotwährung“ in Feld B:70E: bestimmt
            val totalBalanceWithOptionalSign = holdingTotalBalance.substring(":AGGR//UNIT/".length)
            val totalBalanceIsNegative = totalBalanceWithOptionalSign.first() == 'N'
            val totalBalance = if (totalBalanceIsNegative) "-" + totalBalanceWithOptionalSign.substring(1) else totalBalanceWithOptionalSign

            // there's a second ":HOLD//" entry if the currency if the security differs from portfolio's currency // TODO: the 3rd holding of the DK example has this, so implement it to display the correct value
            val portfolioValueStatement = holdingBlock.getOptionalRepeatableField("19A")?.firstOrNull { it.startsWith(":HOLD//") }
            val portfolioValue = parseBalance(portfolioValueStatement?.substringAfter(":HOLD//")) // Value for total balance from B:93B: in the same currency as C:19A:

            val (buyingDate, averageCostPrice, averageCostPriceCurrency) = parseHoldingAdditionalInformation(holdingBlock)

            val (marketValue, pricingTime, totalCostPrice) = parseMarketValue(holdingBlock)

            val balance = portfolioValue?.first ?: (if (balanceIsQuantity == false) Amount(totalBalance) else null)
            val quantity = if (balanceIsQuantity) totalBalance.replace(",", ".").toDoubleOrNull() else null

            Holding(name, isin, wkn, buyingDate, quantity, averageCostPrice, balance, portfolioValue?.second ?: averageCostPriceCurrency, marketValue, pricingTime, totalCostPrice)
        } catch (e: Throwable) {
            logError("Could not parse MT 535 holding block:\n$holdingBlock", e)

            null
        }

    protected open fun parseHoldingAdditionalInformation(holdingBlock: SwiftMessageBlock): Triple<LocalDate?, Amount?, String?> {
        try {
            val additionalInformationLines = holdingBlock.getOptionalField("70E")?.split('\n')
            if (additionalInformationLines != null) {
                val firstLine = additionalInformationLines.first().substring(":HOLD//".length).let {
                    if (it.startsWith("1")) it.substring(1) else it // specifications and their implementations: line obligatory has to start with '1' but that's not always the case
                }
                val currencyOfSafekeepingAccountIsUnit = firstLine.startsWith("STK") // otherwise it's "KON“ = Contracts or ISO currency code of the category currency in the case of securities quoted in percentages

                val firstLineParts = firstLine.split('+')
                val buyingDate = if (firstLineParts.size > 4) parse4DigitYearDate(firstLineParts[4]) else null

                val secondLine = if (additionalInformationLines.size > 1) additionalInformationLines[1].let { if (it.startsWith("2")) it.substring(1) else it } else "" // cut off "2"; the second line is actually mandatory, but to be on the safe side
                val secondLineParts = secondLine.split('+')
                val averageCostPriceAmount = if (secondLineParts.size > 0) secondLineParts[0] else null
                val averageCostPriceCurrency = if (secondLineParts.size > 1) secondLineParts[1] else null

                // third and fourth line are only filled in in the case of futures contracts

                return Triple(buyingDate, averageCostPriceAmount?.let { Amount(it) }, averageCostPriceCurrency)
            }
        } catch (e: Throwable) {
            logError("Could not parse additional information for holding:\n$holdingBlock", e)
        }

        return Triple(null, null, null)
    }

    private fun parseMarketValue(holdingBlock: SwiftMessageBlock): Triple<Amount?, Instant?, Amount?> {
        try {
            val subBalanceDetailsLines = holdingBlock.getOptionalField("70C")?.split('\n')
            if (subBalanceDetailsLines != null) {
                val thirdLine = if (subBalanceDetailsLines.size > 2) subBalanceDetailsLines[2].let { if (it.startsWith("3")) it.substring(1) else it }.trim() else null
                val (marketValue, pricingTime) = if (thirdLine != null) {
                    val thirdLineParts = thirdLine.split(' ')
                    val marketValueAmountAndCurrency = if (thirdLineParts.size > 1) thirdLineParts[1].takeIf { it.isNotBlank() } else null
                    val marketValue = marketValueAmountAndCurrency?.let { Amount(it.replace('.', ',').replace("EUR", "")) } // TODO: also check for other currencies
                    val pricingTime = try {
                        if (thirdLineParts.size > 2) thirdLineParts[2].let { if (it.length > 18) LocalDateTime.parse(it.substring(0, 19)).toInstant(TimeZone.EuropeBerlin) else null } else null
                    } catch (e: Throwable) {
                        logError("Could not parse pricing time from line: $thirdLine", e)
                        null
                    }

                    marketValue to pricingTime
                } else {
                    null to null
                }

                val fourthLine = if (subBalanceDetailsLines.size > 3) subBalanceDetailsLines[3].let { if (it.startsWith("4")) it.substring(1) else it }.trim() else null

                val totalCostPrice = if (fourthLine != null) {
                    val fourthLineParts = fourthLine.split(' ')
                    val totalCostPriceAmountAndCurrency = if (fourthLineParts.size > 0) fourthLineParts[0] else null

                    totalCostPriceAmountAndCurrency?.let { Amount(it.replace('.', ',').replace("EUR", "")) } // TODO: also check for other currencies
                } else {
                    null
                }

                return Triple(marketValue, pricingTime, totalCostPrice)
            }
        } catch (e: Throwable) {
            logError("Could not map market value and total cost price, but is a non-standard anyway", e)
        }

        return Triple(null, null, null)
    }

}