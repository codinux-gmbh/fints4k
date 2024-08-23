package net.codinux.banking.fints.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class FinTsUtilsTest {

    private val underTest = FinTsUtils()


    @Test
    fun formatDate() {

        // given
        val date = LocalDate(1988, 3, 27)

        // when
        val result = underTest.formatDate(date)

        // then
        assertEquals("19880327", result)
    }

    @Test
    fun formatDateAsInt() {

        // given
        val date = LocalDate(1988, 3, 27)

        // when
        val result = underTest.formatDateAsInt(date)

        // then
        assertEquals(19880327, result)
    }


    @Test
    fun formatTime_AM() {

        // given
        val time = LocalTime(8, 2, 1)

        // when
        val result = underTest.formatTime(time)

        // then
        assertEquals("080201", result)
    }

    @Test
    fun formatTime_PM() {

        // given
        val time = LocalTime(18, 22, 51)

        // when
        val result = underTest.formatTime(time)

        // then
        assertEquals("182251", result)
    }

    @Test
    fun formatTimeAsInt_AM() {

        // given
        val time = LocalTime(8, 2, 1)

        // when
        val result = underTest.formatTimeAsInt(time)

        // then
        assertEquals(80201, result)
    }

    @Test
    fun formatTimeAsInt_PM() {

        // given
        val time = LocalTime(18, 22, 51)

        // when
        val result = underTest.formatTimeAsInt(time)

        // then
        assertEquals(182251, result)
    }


    @Test
    fun prettyPrint() {
        val result = underTest.prettyPrintFinTsMessage("""HNHBK:1:3+000000000392+300+0+1'HNVSK:998:3+PIN:1+998+1+1::0+1:20240821:022352+2:2:13:@8@        :5:1+280:10010010:UserName:V:0:0+0'HNVSD:999:1+@230@HNSHK:2:4+PIN:1+999+1265303553+1+1+1::0+1+1:20240821:022352+1:999:1+6:10:16+280:10010010:UserName:S:0:0'HKIDN:3:2+280:10010010+UserName+0+0'HKVVB:4:3+0+0+0+15E53C26816138699C7B6A3E8+1.0.0'HKSYN:5:3+0'HNSHA:6:2+1265303553++MyPassword''HNHBS:7:1+1'""")

        assertEquals(result.replace("\r\n", "\n"), """
            HNHBK:1:3+000000000392+300+0+1'
            HNVSK:998:3+PIN:1+998+1+1::0+1:20240821:022352+2:2:13:@8@        :5:1+280:10010010:UserName:V:0:0+0'
            HNVSD:999:1+@230@
            HNSHK:2:4+PIN:1+999+1265303553+1+1+1::0+1+1:20240821:022352+1:999:1+6:10:16+280:10010010:UserName:S:0:0'
            HKIDN:3:2+280:10010010+UserName+0+0'
            HKVVB:4:3+0+0+0+15E53C26816138699C7B6A3E8+1.0.0'
            HKSYN:5:3+0'
            HNSHA:6:2+1265303553++MyPassword''
            HNHBS:7:1+1'
        """.trimIndent().replace("\r\n", "\n")
        )
    }

}