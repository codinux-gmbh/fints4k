package net.codinux.banking.fints.transactions.swift

import kotlinx.datetime.LocalDate
import net.codinux.banking.fints.test.assertEquals
import net.codinux.banking.fints.test.assertNull
import net.codinux.banking.fints.test.assertSize
import net.codinux.banking.fints.transactions.swift.model.ContinuationIndicator
import net.codinux.banking.fints.transactions.swift.model.Holding
import net.codinux.banking.fints.transactions.swift.model.StatementOfHoldings
import kotlin.test.Test
import kotlin.test.assertNotNull

class Mt535ParserTest {

    private val underTest = Mt535Parser()


    @Test
    fun parseSimpleExample() {
        val result = underTest.parseMt535String(SimpleExampleString)

        val statement = assertStatement(result, "70033100", "0123456789", "41377,72", LocalDate(2024, 8, 30), LocalDate(2024, 8, 30))

        assertSize(2, statement.holdings)

        assertHolding(statement.holdings.first(), "MUL AMUN MSCI WLD ETF ACC MUL Amundi MSCI World V", "LU1781541179", null, LocalDate(2024, 6, 3), 1693, "16,828250257", "18531,08")
        assertHolding(statement.holdings[1], "NVIDIA CORP. DL-,001 NVIDIA Corp.", "US67066G1040", null, LocalDate(2024, 8, 5), 214, "92,86", "22846,64")
    }

    @Test
    fun parseDkExample() {
        val result = underTest.parseMt535String(DkMt535Example)

        val statement = assertStatement(result, "10020030", "1234567", "17026,37", null, LocalDate(1999, 5, 30))

        assertEquals("10020030", statement.bankCode)
        assertEquals("1234567", statement.accountIdentifier)

        assertEquals("17026,37", statement.totalBalance?.string)
        assertEquals("EUR", statement.totalBalanceCurrency)

        assertEquals(1, statement.pageNumber)
        assertEquals(ContinuationIndicator.SinglePage, statement.continuationIndicator)

        assertNull(statement.statementDate)
        assertEquals(LocalDate(1999, 5, 30), statement.preparationDate)

        assertSize(3, statement.holdings)

        assertHolding(statement.holdings.first(), "/DE/123456 Mustermann AG, Stammaktien", "DE0123456789", null, LocalDate(1999, 8, 15), 100, "68,5", "5270,")
        assertHolding(statement.holdings[1], "/DE/123457 Mustermann AG, Vorzugsaktien", "DE0123456790", null, LocalDate(1998, 10, 13), 100, "42,75", "5460,")
        // TODO: these values are not correct. Implement taking foreign currencies into account to fix this
        assertHolding(statement.holdings[2], "Australian Domestic Bonds 1993 (2003) Ser. 10", "AU9876543210", null, LocalDate(1999, 3, 15), null, "31", "6294,65")
    }


    private fun assertStatement(result: List<StatementOfHoldings>, bankCode: String, accountId: String, totalBalance: String?, statementDate: LocalDate?, preparationDate: LocalDate?, totalBalanceCurrency: String? = "EUR", pageNumber: Int? = 1, continuationIndicator: ContinuationIndicator = ContinuationIndicator.SinglePage): StatementOfHoldings {
        val statement = result.first()

        assertEquals(bankCode, statement.bankCode)
        assertEquals(accountId, statement.accountIdentifier)

        assertEquals(totalBalance, statement.totalBalance?.string)
        assertEquals(totalBalanceCurrency, statement.totalBalanceCurrency)

        assertEquals(pageNumber, statement.pageNumber)
        assertEquals(continuationIndicator, statement.continuationIndicator)

        assertEquals(statementDate, statement.statementDate)
        assertEquals(preparationDate, statement.preparationDate)

        return statement
    }

    private fun assertHolding(holding: Holding, name: String, isin: String?, wkn: String?, buyingDate: LocalDate?, quantity: Int?, averagePrice: String?, balance: String?, currency: String? = "EUR") {
        assertEquals(name, holding.name)

        assertEquals(isin, holding.isin)
        assertEquals(wkn, holding.wkn)

        assertEquals(buyingDate, holding.buyingDate)

        assertEquals(quantity, holding.quantity)
        assertEquals(averagePrice, holding.averageCostPrice?.string)

        assertEquals(balance, holding.totalBalance?.string)
        assertEquals(currency, holding.currency)
    }


    private val SimpleExampleString = """
        :16R:GENL
        :28E:1/ONLY
        :20C::SEME//NONREF
        :23G:NEWM
        :98A::PREP//20240830
        :98A::STAT//20240830
        :22F::STTY//CUST
        :97A::SAFE//70033100/0123456789
        :17B::ACTI//Y
        :16S:GENL
        
        :16R:FIN
        :35B:ISIN LU1781541179
        MUL AMUN MSCI WLD ETF ACC
        MUL Amundi MSCI World V
        :93B::AGGR//UNIT/1693,
        
        :16R:SUBBAL
        :93C::TAVI//UNIT/AVAI/1693,
        :70C::SUBB//1 MUL AMUN MSCI WLD ETF ACC
        2
        3 EDE 17.332000000EUR 2024-08-30T18:50:35.76
        4 17994.44EUR LU1781541179, 1/SO
        :16S:SUBBAL
        :19A::HOLD//EUR18531,08
        :70E::HOLD//1STK++++20240603+
        216,828250257+EUR
        :16S:FIN
        
        :16R:FIN
        :35B:ISIN US67066G1040
        NVIDIA CORP. DL-,001
        NVIDIA Corp.
        :93B::AGGR//UNIT/214,
        :16R:SUBBAL
        :93C::TAVI//UNIT/AVAI/214,
        :70C::SUBB//1 NVIDIA CORP. DL-,001
        2
        3 EDE 106.760000000EUR 2024-08-30T18:53:05.04
        4 19872.04EUR US67066G1040, 1/SHS
        :16S:SUBBAL
        :19A::HOLD//EUR22846,64
        :70E::HOLD//1STK++++20240805+
        292,86+EUR
        :16S:FIN
        
        :16R:ADDINFO
        :19A::HOLP//EUR41377,72
        :16S:ADDINFO
        -
    """.trimIndent()

    /**
     * See Anlage_3_Datenformate_V3.8, S. 317ff
     *
     * Bei der ersten Depotposition (Mustermann AG Stammaktien) liegt ein Bestand von 100 Stück
     * vor. Die zweite Position (Mustermann AG Vorzugsaktien) setzt sich aus einem Guthaben von
     * 130 Stück und einem schwebenden Abgang von 30 Stück zu einem Saldo von 100 Stück
     * zusammen. Bei der dritten Position (Australian Domestic Bonds) ist im Gesamtsaldo von
     * 10.000 Australischen Dollar ein Bestand von 2.500 Dollar als gesperrt gekennzeichnet.
     */
    private val DkMt535Example = """
        :16R:GENL
        :28E:1/ONLY
        :13A::STAT//004
        :20C::SEME//NONREF
        :23G:NEWM
        :98C::PREP//19990530120538
        :98A::STAT//19990529
        :22F::STTY//CUST
        :97A::SAFE//10020030/1234567
        :17B::ACTI//Y
        :16S:GENL
        
        :16R:FIN
        :35B:ISIN DE0123456789
        /DE/123456
        Mustermann AG, Stammaktien
        :90B::MRKT//ACTU/EUR52,7
        :94B::PRIC//LMAR/XFRA
        :98A::PRIC//19990529
        :93B::AGGR//UNIT/100,
        
        :16R:SUBBAL
        :93C::TAVI//UNIT/AVAI/100,
        :94C::SAFE//DE
        :70C::SUBB//12345678901234567890
        1
        :16S:SUBBAL
        
        :19A::HOLD//EUR5270,
        :70E::HOLD//STK+511+00081+DE+19990815
        68,5+EUR
        :16S:FIN
        
        :16R:FIN
        :35B:ISIN DE0123456790
        /DE/123457
        Mustermann AG, Vorzugsaktien
        :90B::MRKT//ACTU/EUR54,6
        :94B::PRIC//LMAR/XFRA
        :98A::PRIC//19990529
        :93B::AGGR//UNIT/100,
        
        :16R:SUBBAL
        :93C::TAVI//UNIT/AVAI/130,
        :94C::SAFE//DE
        :70C::SUBB//123456799123456799
        1
        :16S:SUBBAL
        
        :16R:SUBBAL
        :93C::PEND//UNIT/NAVL/N30,
        :94C::SAFE//DE
        :70C::SUBB//123456799123456799
        1
        :16S:SUBBAL
        
        :19A::HOLD//EUR5460,
        :70E::HOLD//STK+512+00081+DE+19981013
        42,75+EUR
        :16S:FIN
        
        :16R:FIN
        :35B:ISIN AU9876543210
        Australian Domestic Bonds
        1993 (2003) Ser. 10
        :90A::MRKT//PRCT/105,
        :94B::PRIC//LMAR/XASX
        :98A::PRIC//19990528
        :93B::AGGR//FAMT/10000,
        
        :16R:SUBBAL
        :93C::TAVI//FAMT/AVAI/7500,
        :94C::SAFE//AU
        :70C::SUBB//98765432109876543210
        4+Sydney
        :16S:SUBBAL
        
        :16R:SUBBAL
        :93C::BLOK//FAMT/NAVL/2500,
        :94C::SAFE//AU
        :70C::SUBB//98765432109876543210
        4+Sydney+20021231
        :16S:SUBBAL
        
        :99A::DAAC//004
        :19A::HOLD//EUR6294,65
        :19A::HOLD//AUD10500,
        :19A::ACRU//EUR1,72
        :19A::ACRU//AUD2,87
        :92B::EXCH//AUD/EUR/0,59949
        :70E::HOLD//AUD+525+00611+AU+19990315+200312
        31
        99,75++6,25
        :16S:FIN
        
        :16R:ADDINFO
        :19A::HOLP//EUR17026,37
        :16S:ADDINFO
        -
    """.trimIndent()

}