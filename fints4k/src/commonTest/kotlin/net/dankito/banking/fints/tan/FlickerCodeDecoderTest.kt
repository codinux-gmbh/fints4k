package net.dankito.banking.fints.tan

import net.dankito.banking.fints.model.HHDVersion
import kotlin.test.Test
import kotlin.test.assertEquals


class FlickerCodeDecoderTest {

    private val underTest = FlickerCodeDecoder()


    @Test
    fun decodeChallenge_ExampleFromChapter_C_4_1_BCD_without_ControlByte_and_DataElements() {

        // given
        val challenge = "070A2082901998"


        // when
        val response = underTest.decodeChallenge(challenge, HHDVersion.HHD_1_3)


        // then
        assertEquals("070520829019981A", response.parsedDataSet)
    }


    @Test
    fun decodeChallenge_ExampleFromChapter_C_4_1_without_ControlByte() {

        // given
        val challenge = "070A208290199872IE99BOFI"


        // when
        val response = underTest.decodeChallenge(challenge, HHDVersion.HHD_1_3)


        // then
        assertEquals("100520829019981849453939424F46494B", response.parsedDataSet)
    }

    @Test
    fun decodeChallenge_AmountInASCII() {

        // given
        val challenge = "2908881696281098765432100532,00"

        // when
        val result = underTest.decodeChallenge(challenge, HHDVersion.HHD_1_3)

        // then
        assertEquals("1204881696280598765432101533322C30303A", result.parsedDataSet)
    }

    @Test
    fun decodeChallenge_CheckLuhnSumModulo10Is0() {

        // given
        val challenge = "100880040243"

        // when
        val result = underTest.decodeChallenge(challenge, HHDVersion.HHD_1_3)

        // then
        assertEquals("0604800402430B", result.parsedDataSet)
    }


    @Test
    fun decodeHHD1_4_GetTransactions() {

        // given
        val challenge = "0388A01239230124622DE26123456780987654321"

        // when
        val result = underTest.decodeChallenge(challenge, HHDVersion.HHD_1_4)

        // then
        assertEquals("1F85012392301246564445323631323334353637383039383736353433323175", result.parsedDataSet)
    }

    @Test
    fun decodeHHD1_4_TransferMoney() {

        // given
        val challenge = "0438701109374422DE2612345678098765432106100,00"

        // when
        val result = underTest.decodeChallenge(challenge, HHDVersion.HHD_1_4)

        // then
        assertEquals("2584011093744F5644453236313233343536373830393837363534333231463130302C303008", result.parsedDataSet)
    }

}