package com.example

import com.example.data.EventType
import com.example.data.MasoomEvent
import com.example.data.MasoomWisdom
import com.example.data.QuranSurahInfo
import com.example.data.ZiyaratItem
import com.example.ui.AamalItem
import com.example.ui.GuidanceReason
import com.example.ui.HomeGuidanceEngine
import com.example.ui.HomeRecommendationKind
import com.example.ui.ShiaDua
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeGuidanceEngineTest {

    @Test
    fun imamShahadatUsesOnlyRelatedAvailableContent() {
        val plan = HomeGuidanceEngine.build(
            now = noon(),
            hijriDay = 25,
            hijriMonth = 10,
            hijriMonthName = "Shawwal",
            events = listOf(event("event_imam_sadiq_shahadat", 25, 10, 8)),
            duas = listOf(dua("tawassul"), dua("kumayl"), dua("ziyarat_aminullah", "Ziyarat")),
            aamal = emptyList(),
            ziyarats = listOf(ziyarat("ziyarat_sadiq", 8), ziyarat("ziyarat_hasan", 4)),
            wisdom = listOf(wisdom(8), wisdom(4)),
            quranSurahs = quranSurahs()
        )

        assertEquals(8, plan.primaryMasoomIndex)
        assertEquals("patience", plan.contentTopic)
        assertEquals(
            setOf("ziyarats:ziyarat_sadiq", "masoomeen:8", "duas:tawassul"),
            plan.recommendations.map { "${it.route}:${it.id}" }.toSet()
        )
        assertFalse(plan.recommendations.any { it.id == "kumayl" || it.id == "ziyarat_hasan" })
        assertTrue(plan.recommendations.all { it.reason == GuidanceReason.EXACT_EVENT })
    }

    @Test
    fun unavailableOccasionCategoriesAreOmittedInsteadOfFilled() {
        val plan = HomeGuidanceEngine.build(
            now = noon(),
            hijriDay = 25,
            hijriMonth = 10,
            hijriMonthName = "Shawwal",
            events = listOf(event("event_imam_sadiq_shahadat", 25, 10, 8)),
            duas = emptyList(),
            aamal = emptyList(),
            ziyarats = emptyList(),
            wisdom = listOf(wisdom(8))
        )

        assertEquals(1, plan.recommendations.size)
        assertEquals("masoomeen", plan.recommendations.single().route)
        assertEquals("8", plan.recommendations.single().id)
        assertEquals(HomeRecommendationKind.WISDOM, plan.recommendations.single().kind)
    }

    @Test
    fun ashuraKeepsAamalZiyaratAndWisdomInDistinctLanes() {
        val plan = HomeGuidanceEngine.build(
            now = noon(),
            hijriDay = 10,
            hijriMonth = 1,
            hijriMonthName = "Muharram",
            events = listOf(event("event_ashura", 10, 1, 5)),
            duas = listOf(
                dua("ashura", "Holy Ziyarats"),
                dua("waritha", "Holy Ziyarats"),
                dua("ziyarat_nahiya", "Holy Ziyarats"),
                dua("kumayl")
            ),
            aamal = listOf(aamal("aamal_ashura"), aamal("aamal_jumuah")),
            ziyarats = listOf(ziyarat("ziyarat_ashura", 5)),
            wisdom = listOf(wisdom(5))
        )

        assertTrue(plan.recommendations.any {
            it.kind == HomeRecommendationKind.AAMAL && it.id == "aamal_ashura"
        })
        assertTrue(plan.recommendations.any { it.kind == HomeRecommendationKind.ZIYARAT })
        assertTrue(plan.recommendations.any { it.route == "masoomeen" && it.id == "5" })
        assertFalse(plan.recommendations.any { it.id == "aamal_jumuah" || it.id == "kumayl" })
        assertTrue(plan.recommendations.all { it.reason == GuidanceReason.EXACT_EVENT })
    }

    @Test
    fun ordinaryDayReservesOneQuranSlotWithinFourCards() {
        val wednesday = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 3, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val plan = HomeGuidanceEngine.build(
            now = wednesday,
            hijriDay = 17,
            hijriMonth = 12,
            hijriMonthName = "Dhul Hijjah",
            events = emptyList(),
            duas = listOf(
                dua("ziyarat_aminullah"),
                dua("dua_mashlool"),
                dua("faraj"),
                dua("hujjat"),
                dua("nade_ali"),
                dua("noor")
            ),
            aamal = emptyList(),
            quranSurahs = quranSurahs()
        )

        assertEquals(4, plan.recommendations.size)
        val quran = plan.recommendations.filter { it.kind == HomeRecommendationKind.QURAN }
        assertEquals(1, quran.size)
        assertEquals("quran", quran.single().route)
        assertTrue((quran.single().id.toIntOrNull() ?: 0) in 1..114)
    }

    @Test
    fun fridaySuggestsKahfAlongsideExistingJumuahGuidance() {
        val plan = HomeGuidanceEngine.build(
            now = noon(),
            hijriDay = 17,
            hijriMonth = 12,
            hijriMonthName = "Dhul Hijjah",
            events = emptyList(),
            duas = listOf(dua("nudba"), dua("dua_simaat")),
            aamal = listOf(aamal("aamal_jumuah")),
            quranSurahs = quranSurahs()
        )

        val quran = plan.recommendations.single { it.kind == HomeRecommendationKind.QURAN }
        assertEquals("18", quran.id)
        assertEquals(GuidanceReason.WEEKLY, quran.reason)
        assertTrue(plan.recommendations.any { it.id == "aamal_jumuah" })
        assertTrue(plan.recommendations.any { it.id == "nudba" })
        assertTrue(plan.recommendations.size <= 4)
    }

    @Test
    fun qadrIncludesSurahQadrWithoutMixingInGeneralDailyPicks() {
        val plan = HomeGuidanceEngine.build(
            now = noon(),
            hijriDay = 21,
            hijriMonth = 9,
            hijriMonthName = "Ramadan",
            events = listOf(event("event_laylatul_qadr_21", 21, 9, 0)),
            duas = listOf(dua("iftitah"), dua("hazeen"), dua("munajat_taibin"), dua("kumayl")),
            aamal = listOf(aamal("aamal_shab_e_qadr"), aamal("aamal_jumuah")),
            quranSurahs = quranSurahs()
        )

        val quran = plan.recommendations.single { it.kind == HomeRecommendationKind.QURAN }
        assertEquals("97", quran.id)
        assertEquals(GuidanceReason.EXACT_EVENT, quran.reason)
        assertTrue(plan.recommendations.all { it.reason == GuidanceReason.EXACT_EVENT })
        assertFalse(plan.recommendations.any { it.id == "kumayl" || it.id == "aamal_jumuah" })
    }

    private fun noon(): Calendar = Calendar.getInstance().apply {
        set(2026, Calendar.JUNE, 5, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun event(id: String, day: Int, month: Int, masoomIndex: Int) = MasoomEvent(
        id = id,
        title = "Imam occasion",
        dateStringHijri = "",
        eventType = EventType.SHAHADAT,
        description = "Occasion description",
        hijriDay = day,
        hijriMonth = month,
        masoomIndex = masoomIndex
    )

    private fun dua(id: String, category: String = "Supplication") = ShiaDua(
        id = id,
        title = id,
        category = category,
        description = "Description for $id",
        relevance = "Relevant to the occasion",
        audioUrl = "",
        lines = emptyList()
    )

    private fun aamal(id: String) = AamalItem(
        id = id,
        title = id,
        category = "Occasion Aamal",
        description = "Description for $id",
        relevance = "Relevant to the occasion",
        acts = emptyList()
    )

    private fun quranSurahs(): List<QuranSurahInfo> = (1..114).map { number ->
        QuranSurahInfo(
            number = number,
            name = "Surah $number",
            nameAr = "سورة $number",
            revelationType = "Meccan",
            verseCount = 7
        )
    }

    private fun ziyarat(id: String, masoomIndex: Int) = ZiyaratItem(
        id = id,
        masoomIndex = masoomIndex,
        titleEn = id,
        titleAr = id,
        titleUr = id,
        audioUrl = "",
        arText = "Arabic recitation",
        enText = "English recitation",
        urText = "Urdu recitation"
    )

    private fun wisdom(masoomIndex: Int) = MasoomWisdom(
        masoomIndex = masoomIndex,
        nameEn = "Masoom $masoomIndex",
        nameUr = "معصوم $masoomIndex",
        nameAr = "معصوم $masoomIndex",
        sayingsEn = listOf("Saying one", "Saying two", "Saying three"),
        sayingsUr = listOf("قول یک", "قول دو", "قول سه"),
        sayingsAr = listOf("قول واحد", "قول اثنان", "قول ثلاثة")
    )
}
