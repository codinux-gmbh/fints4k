package net.dankito.banking.fints.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import net.dankito.utils.multiplatform.extensions.of
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
        val time = LocalDateTime.of(8, 2, 1)

        // when
        val result = underTest.formatTime(time)

        // then
        assertEquals("080201", result)
    }

    @Test
    fun formatTime_PM() {

        // given
        val time = LocalDateTime.of(18, 22, 51)

        // when
        val result = underTest.formatTime(time)

        // then
        assertEquals("182251", result)
    }

    @Test
    fun formatTimeAsInt_AM() {

        // given
        val time = LocalDateTime.of(8, 2, 1)

        // when
        val result = underTest.formatTimeAsInt(time)

        // then
        assertEquals(80201, result)
    }

    @Test
    fun formatTimeAsInt_PM() {

        // given
        val time = LocalDateTime.of(18, 22, 51)

        // when
        val result = underTest.formatTimeAsInt(time)

        // then
        assertEquals(182251, result)
    }

}