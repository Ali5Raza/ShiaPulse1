package com.example

import com.example.api.DailyContentClient
import com.example.api.ShiaQuote
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyContentClientTest {

    @Test
    fun occasionSpeakerOutranksAnUnrelatedTopicMatch() {
        val sadiq = quote("A saying about knowledge.", "Imam Sadiq (as) - Tawhid")
        val unrelatedPatience = quote("Patience is a virtue.", "Imam Hasan (as) - Bihar al-Anwar")

        val selected = DailyContentClient.selectDailyContent(
            items = listOf(unrelatedPatience, sadiq),
            topic = "patience",
            dayStamp = 2026001,
            masoomIndex = 8
        )

        assertEquals(sadiq, selected)
    }

    @Test
    fun husseinSpellingIsRecognizedForImamHussain() {
        val hussein = quote("Dignity is essential.", "Imam Hussein (as) - Maqtal")
        val unrelated = quote("Patience is essential.", "Prophet Muhammad (saws) - Sirah")

        val selected = DailyContentClient.selectDailyContent(
            items = listOf(unrelated, hussein),
            topic = "patience",
            dayStamp = 2026002,
            masoomIndex = 5
        )

        assertEquals(hussein, selected)
    }

    private fun quote(english: String, source: String) = ShiaQuote(
        arabic = "قول",
        english = english,
        source = source,
        urdu = "قول"
    )
}
