package net.codinux.banking.fints.response

import net.codinux.banking.fints.FinTsTestBaseJvm
import net.codinux.banking.fints.messages.HbciCharset
import net.codinux.banking.fints.tan.TanImageDecoder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.Charset


class ResponseParserTestJvm : FinTsTestBaseJvm() {

    private val underTest = ResponseParser()


    @Test
    fun `decode TanChallenge HHD_UC`() {

        // given
        val response = loadTestFile("Decode_TanChallengeHhdUc_WithMaskedCharacter.txt", Charset.forName(HbciCharset.DefaultCharset.displayName()))


        // when
        val result = underTest.parse(response)


        // then
        assertThat(result.successful).isTrue()

        assertThat(result.tanResponse).isNotNull

        val decodedChallengeHhdUc = TanImageDecoder().decodeChallenge(result.tanResponse?.challengeHHD_UC ?: "")
        assertThat(decodedChallengeHhdUc.decodingSuccessful).isTrue()
        assertThat(decodedChallengeHhdUc.imageBytes.size).isEqualTo(3664)
    }

}