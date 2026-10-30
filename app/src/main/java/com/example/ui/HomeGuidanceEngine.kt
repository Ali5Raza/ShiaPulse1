package com.example.ui

import com.example.data.EventType
import com.example.data.MasoomEvent
import com.example.data.MasoomWisdom
import com.example.data.QuranSurahInfo
import com.example.data.ZiyaratItem
import java.util.Calendar

/** The source and reason remain visible so recommendations are explainable, not random. */
enum class GuidanceReason {
    EXACT_EVENT,
    HIJRI_DATE,
    HOLY_MONTH,
    WEEKLY,
    TIME_OF_DAY,
    DAILY
}

enum class HomeRecommendationKind {
    AAMAL,
    DUA,
    ZIYARAT,
    WISDOM,
    QURAN
}

data class HomeRecommendation(
    val id: String,
    val route: String,
    val title: String,
    val titleUr: String? = null,
    val titleAr: String? = null,
    val description: String,
    val descriptionUr: String? = null,
    val descriptionAr: String? = null,
    val category: String,
    val relevance: String,
    val reason: GuidanceReason,
    val score: Int,
    val categoryUr: String? = null,
    val categoryAr: String? = null,
    val relevanceUr: String? = null,
    val relevanceAr: String? = null,
    val kind: HomeRecommendationKind = HomeRecommendationKind.DUA
) {
    fun localizedTitle(languageCode: String): String = when (languageCode) {
        "ur" -> titleUr?.takeIf { it.isNotBlank() } ?: title
        "ar" -> titleAr?.takeIf { it.isNotBlank() } ?: title
        else -> title
    }

    fun localizedDescription(languageCode: String): String = when (languageCode) {
        "ur" -> descriptionUr?.takeIf { it.isNotBlank() } ?: description
        "ar" -> descriptionAr?.takeIf { it.isNotBlank() } ?: description
        else -> description
    }

    fun localizedCategory(languageCode: String): String = when (languageCode) {
        "ur" -> categoryUr?.takeIf { it.isNotBlank() } ?: category
        "ar" -> categoryAr?.takeIf { it.isNotBlank() } ?: category
        else -> category
    }

    fun localizedRelevance(languageCode: String): String = when (languageCode) {
        "ur" -> relevanceUr?.takeIf { it.isNotBlank() } ?: relevance
        "ar" -> relevanceAr?.takeIf { it.isNotBlank() } ?: relevance
        else -> relevance
    }
}

data class HomeGuidancePlan(
    val recommendations: List<HomeRecommendation>,
    val occasionEvents: List<MasoomEvent>,
    val contentTopic: String,
    val isJumuah: Boolean,
    val isRamadan: Boolean,
    val primaryMasoomIndex: Int = 0
)

/**
 * Deterministic, event-aware guidance built only from the app's verified content.
 * Exact dates outrank weekly and time suggestions; equal priorities rotate daily.
 */
object HomeGuidanceEngine {
    private const val ROUTE_DUA = "duas"
    private const val ROUTE_AAMAL = "aamal_detail"
    private const val ROUTE_ZIYARAT = "ziyarats"
    private const val ROUTE_WISDOM = "masoomeen"
    private const val ROUTE_QURAN = "quran"

    // A deterministic two-week rotation for ordinary days; no religious obligation is implied.
    private val dailyQuranRotation = listOf(1, 93, 94, 97, 103, 108, 112, 113, 114, 18, 36, 55, 56, 67)

    private data class Candidate(
        val id: String,
        val route: String,
        val score: Int,
        val reason: GuidanceReason
    )

    private class Candidates {
        val items = mutableListOf<Candidate>()

        fun dua(id: String, score: Int, reason: GuidanceReason) {
            items += Candidate(id, ROUTE_DUA, score, reason)
        }

        fun aamal(id: String, score: Int, reason: GuidanceReason) {
            items += Candidate(id, ROUTE_AAMAL, score, reason)
        }

        fun ziyarat(id: String, score: Int, reason: GuidanceReason) {
            items += Candidate(id, ROUTE_ZIYARAT, score, reason)
        }

        fun wisdom(masoomIndex: Int, score: Int, reason: GuidanceReason) {
            items += Candidate(masoomIndex.toString(), ROUTE_WISDOM, score, reason)
        }

        fun quran(surahNumber: Int, score: Int, reason: GuidanceReason) {
            items += Candidate(surahNumber.toString(), ROUTE_QURAN, score, reason)
        }
    }

    fun build(
        now: Calendar,
        hijriDay: Int,
        hijriMonth: Int,
        hijriMonthName: String,
        events: List<MasoomEvent>,
        duas: List<ShiaDua>,
        aamal: List<AamalItem>,
        ziyarats: List<ZiyaratItem> = emptyList(),
        wisdom: List<MasoomWisdom> = emptyList(),
        quranSurahs: List<QuranSurahInfo> = emptyList(),
        weeklyNow: Calendar = now
    ): HomeGuidancePlan {
        val dayOfWeek = weeklyNow.get(Calendar.DAY_OF_WEEK)
        val hour = weeklyNow.get(Calendar.HOUR_OF_DAY)
        val dayOfYear = now.get(Calendar.DAY_OF_YEAR)
        val isThursdayNight = (dayOfWeek == Calendar.THURSDAY && hour >= 17) ||
            (dayOfWeek == Calendar.FRIDAY && hour < 5)
        val isJumuah = isThursdayNight || dayOfWeek == Calendar.FRIDAY
        val isTuesdayNight = (dayOfWeek == Calendar.TUESDAY && hour >= 17) ||
            (dayOfWeek == Calendar.WEDNESDAY && hour < 5)
        val isMorning = hour in 5..11
        val isNight = hour >= 18 || hour < 5
        val occasionEvents = events.filter { event ->
            event.occursOn(hijriDay, hijriMonth, hijriMonthName)
        }
        val duaById = duas.associateBy { it.id }
        val aamalById = aamal.associateBy { it.id }
        val ziyaratById = ziyarats.associateBy { it.id }
        val wisdomByMasoom = wisdom.associateBy { it.masoomIndex }
        val quranByNumber = quranSurahs.associateBy { it.number }

        val candidates = Candidates()
        val specialReason = if (occasionEvents.isNotEmpty()) {
            GuidanceReason.EXACT_EVENT
        } else {
            GuidanceReason.HIJRI_DATE
        }

        // Fixed observances remain available even if an admin has not created an event record.
        when {
            hijriMonth == 1 && hijriDay == 10 -> {
                candidates.aamal("aamal_ashura", 130, specialReason)
                candidates.dua("ashura", 128, specialReason)
                candidates.dua("waritha", 122, specialReason)
                candidates.dua("ziyarat_nahiya", 118, specialReason)
            }
            hijriMonth == 2 && hijriDay == 20 -> {
                candidates.dua("ziyarat_arbaeen", 130, specialReason)
                candidates.dua("waritha", 124, specialReason)
                candidates.dua("ashura", 120, specialReason)
            }
            hijriMonth == 12 && hijriDay == 18 -> {
                candidates.aamal("aamal_eid_ghadir", 130, specialReason)
                candidates.dua("nudba", 122, specialReason)
                candidates.dua("hadith_kisa", 120, specialReason)
                candidates.dua("ziyarat_aminullah", 116, specialReason)
            }
            hijriMonth == 12 && hijriDay == 24 -> {
                candidates.dua("hadith_kisa", 130, specialReason)
                candidates.dua("ziyarat_aminullah", 120, specialReason)
            }
            hijriMonth == 10 && hijriDay == 1 -> {
                candidates.aamal("aamal_eid_fitr", 130, specialReason)
                candidates.dua("nudba", 122, specialReason)
                candidates.dua("ziyarat_aminullah", 114, specialReason)
            }
            hijriMonth == 7 && hijriDay == 15 -> {
                candidates.aamal("aamal_umme_dawood", 130, specialReason)
                candidates.dua("ya_man_arjuhu", 124, specialReason)
                candidates.aamal("aamal_rajab", 112, specialReason)
            }
            hijriMonth == 8 && hijriDay == 15 -> {
                candidates.dua("hujjat", 130, specialReason)
                candidates.dua("ziyarat_aal_yaseen", 126, specialReason)
                candidates.dua("faraj", 122, specialReason)
                candidates.aamal("aamal_shaban", 116, specialReason)
            }
            hijriMonth == 9 && hijriDay in listOf(19, 21, 23) -> {
                candidates.aamal("aamal_shab_e_qadr", 130, specialReason)
                candidates.quran(97, 126, specialReason)
                candidates.dua("iftitah", 124, specialReason)
                candidates.dua("hazeen", 120, specialReason)
                candidates.dua("munajat_taibin", 116, specialReason)
            }
            hijriMonth == 9 && hijriDay in 13..15 -> {
                candidates.dua("dua_mujeer", 126, specialReason)
                candidates.dua("iftitah", 116, specialReason)
                candidates.aamal("aamal_ramadan", 112, specialReason)
            }
        }

        // Match admin-managed events as well as the built-in dates above. Direct
        // Infallible occasions also bring their own Ziyarat and Aqwal, when present.
        occasionEvents.forEach { event ->
            addEventCandidates(candidates, event)
            addMasoomCandidates(
                candidates = candidates,
                event = event,
                ziyarats = ziyarats,
                ziyaratById = ziyaratById,
                wisdomByMasoom = wisdomByMasoom
            )
        }

        // Month-long devotions have lower priority than the exact day.
        when (hijriMonth) {
            1, 2 -> {
                candidates.dua("ashura", 72, GuidanceReason.HOLY_MONTH)
                candidates.dua("waritha", 68, GuidanceReason.HOLY_MONTH)
            }
            7 -> {
                candidates.aamal("aamal_rajab", 86, GuidanceReason.HOLY_MONTH)
                candidates.dua("ya_man_arjuhu", 82, GuidanceReason.HOLY_MONTH)
            }
            8 -> {
                candidates.aamal("aamal_shaban", 86, GuidanceReason.HOLY_MONTH)
                candidates.dua("salawat_shabaniyah", 84, GuidanceReason.HOLY_MONTH)
                candidates.dua("munajat_shabaniyah", 80, GuidanceReason.HOLY_MONTH)
            }
            9 -> {
                candidates.aamal("aamal_ramadan", 88, GuidanceReason.HOLY_MONTH)
                candidates.dua("iftitah", 86, GuidanceReason.HOLY_MONTH)
                candidates.quran(97, 84, GuidanceReason.HOLY_MONTH)
                candidates.dua("dua_baha", 80, GuidanceReason.HOLY_MONTH)
            }
            12 -> if (hijriDay <= 10) {
                candidates.aamal("aamal_zilhajj", 96, GuidanceReason.HOLY_MONTH)
            }
        }

        when {
            isThursdayNight -> {
                candidates.dua("kumayl", 108, GuidanceReason.WEEKLY)
                candidates.aamal("aamal_jumuah", 102, GuidanceReason.WEEKLY)
                candidates.quran(18, 96, GuidanceReason.WEEKLY)
            }
            dayOfWeek == Calendar.FRIDAY -> {
                candidates.aamal("aamal_jumuah", 106, GuidanceReason.WEEKLY)
                candidates.dua("nudba", 102, GuidanceReason.WEEKLY)
                candidates.quran(18, 100, GuidanceReason.WEEKLY)
                candidates.dua("dua_simaat", 98, GuidanceReason.WEEKLY)
            }
            isTuesdayNight -> candidates.dua("tawassul", 104, GuidanceReason.WEEKLY)
            dayOfWeek == Calendar.SUNDAY -> candidates.dua("hadith_kisa", 42, GuidanceReason.WEEKLY)
        }

        if (isMorning) {
            candidates.dua("sabah", 64, GuidanceReason.TIME_OF_DAY)
            candidates.dua("ahad", 60, GuidanceReason.TIME_OF_DAY)
        }
        if (isNight) {
            candidates.dua("hazeen", 58, GuidanceReason.TIME_OF_DAY)
            candidates.dua("munajat_taibin", 54, GuidanceReason.TIME_OF_DAY)
            candidates.dua("dua_mashlool", 48, GuidanceReason.TIME_OF_DAY)
        }

        // Reliable fallbacks make the section useful on ordinary days too.
        candidates.dua("ziyarat_aminullah", 34, GuidanceReason.DAILY)
        candidates.dua("dua_mashlool", 30, GuidanceReason.DAILY)
        candidates.dua("faraj", 28, GuidanceReason.DAILY)
        candidates.dua("hujjat", 26, GuidanceReason.DAILY)
        candidates.dua("nade_ali", 24, GuidanceReason.DAILY)
        candidates.dua("noor", 22, GuidanceReason.DAILY)
        candidates.quran(
            surahNumber = dailyQuranRotation[dayOfYear % dailyQuranRotation.size],
            score = 36,
            reason = GuidanceReason.DAILY
        )

        // During an exact occasion, do not fill the Home feed with unrelated daily
        // or weekly picks merely to reach four cards. Relevant variety is preferred.
        val prioritizedCandidates = if (occasionEvents.isNotEmpty()) {
            candidates.items.filter { it.reason == GuidanceReason.EXACT_EVENT }
        } else {
            candidates.items
        }
        val seen = mutableSetOf<String>()
        val availableRecommendations = prioritizedCandidates
            .sortedWith(
                compareByDescending<Candidate> { it.score }
                    .thenBy { candidate ->
                        ((candidate.id.hashCode() and 0x7fffffff) + dayOfYear * 37) % 1009
                    }
            )
            .mapNotNull { candidate ->
                val key = "${candidate.route}:${candidate.id}"
                if (!seen.add(key)) return@mapNotNull null
                when (candidate.route) {
                    ROUTE_DUA -> duaById[candidate.id]?.let { dua ->
                        HomeRecommendation(
                            id = dua.id,
                            route = ROUTE_DUA,
                            title = dua.title,
                            titleUr = dua.titleUr,
                            titleAr = dua.titleAr,
                            description = dua.description,
                            descriptionUr = dua.descriptionUr,
                            descriptionAr = dua.descriptionAr,
                            category = dua.category,
                            categoryUr = dua.categoryUr,
                            categoryAr = dua.categoryAr,
                            relevance = dua.relevance.ifBlank { dua.category },
                            relevanceUr = dua.relevanceUr ?: dua.categoryUr,
                            relevanceAr = dua.relevanceAr ?: dua.categoryAr,
                            reason = candidate.reason,
                            score = candidate.score,
                            kind = if (dua.isZiyarat()) HomeRecommendationKind.ZIYARAT else HomeRecommendationKind.DUA
                        )
                    }
                    ROUTE_AAMAL -> aamalById[candidate.id]?.let { item ->
                        HomeRecommendation(
                            id = item.id,
                            route = ROUTE_AAMAL,
                            title = item.title,
                            titleUr = item.titleUr,
                            titleAr = item.titleAr,
                            description = item.description,
                            descriptionUr = item.descriptionUr,
                            descriptionAr = item.descriptionAr,
                            category = item.category,
                            categoryUr = item.categoryUr,
                            categoryAr = item.categoryAr,
                            relevance = item.relevance.ifBlank { item.category },
                            relevanceUr = item.relevanceUr ?: item.categoryUr,
                            relevanceAr = item.relevanceAr ?: item.categoryAr,
                            reason = candidate.reason,
                            score = candidate.score,
                            kind = HomeRecommendationKind.AAMAL
                        )
                    }
                    ROUTE_ZIYARAT -> ziyaratById[candidate.id]?.let { ziyarat ->
                        createZiyaratRecommendation(candidate, ziyarat)
                    }
                    ROUTE_WISDOM -> wisdomByMasoom[candidate.id.toIntOrNull() ?: -1]?.let { masoomWisdom ->
                        createWisdomRecommendation(candidate, masoomWisdom, dayOfYear)
                    }
                    ROUTE_QURAN -> quranByNumber[candidate.id.toIntOrNull() ?: -1]?.let { surah ->
                        createQuranRecommendation(candidate, surah)
                    }
                    else -> null
                }
            }
        val recommendations = selectBalancedRecommendations(
            available = availableRecommendations,
            exactOccasion = occasionEvents.isNotEmpty()
        )

        val topic = selectContentTopic(
            occasionEvents = occasionEvents,
            hijriMonth = hijriMonth,
            hijriDay = hijriDay,
            isJumuah = isJumuah,
            isMorning = isMorning,
            isNight = isNight,
            dayOfYear = dayOfYear
        )

        return HomeGuidancePlan(
            recommendations = recommendations,
            occasionEvents = occasionEvents,
            contentTopic = topic,
            isJumuah = isJumuah,
            isRamadan = hijriMonth == 9,
            primaryMasoomIndex = occasionEvents.firstOrNull { isDirectMasoomEvent(it) }?.masoomIndex ?: 0
        )
    }

    private fun selectBalancedRecommendations(
        available: List<HomeRecommendation>,
        exactOccasion: Boolean
    ): List<HomeRecommendation> {
        val quranPick = available.firstOrNull { it.kind == HomeRecommendationKind.QURAN }
        if (quranPick == null) {
            return if (exactOccasion) {
                selectOccasionRecommendations(available).take(4)
            } else {
                available.take(4)
            }
        }

        // Reserve one of four slots for Quran without adding another Home section.
        // Other contextual picks keep their existing priority and occasion lanes.
        val nonQuran = available.filter { it.kind != HomeRecommendationKind.QURAN }
        val prioritized = if (exactOccasion) {
            selectOccasionRecommendations(nonQuran)
        } else {
            nonQuran
        }
        return (prioritized.take(3) + quranPick)
            .sortedByDescending { it.score }
            .take(4)
    }

    private fun selectOccasionRecommendations(
        available: List<HomeRecommendation>
    ): List<HomeRecommendation> {
        val lanePriority = listOf(
            HomeRecommendationKind.AAMAL,
            HomeRecommendationKind.ZIYARAT,
            HomeRecommendationKind.WISDOM,
            HomeRecommendationKind.DUA
        )
        val lanePicks = lanePriority.mapNotNull { kind ->
            available.firstOrNull { it.kind == kind }
        }
        val pickedKeys = lanePicks.mapTo(mutableSetOf()) { "${it.route}:${it.id}" }
        val remaining = available.filter { "${it.route}:${it.id}" !in pickedKeys }
        return lanePicks + remaining
    }

    private fun ShiaDua.isZiyarat(): Boolean {
        val normalizedCategory = category.lowercase()
        return id.startsWith("ziyarat_") || id == "ashura" || id == "waritha" ||
            "ziyarat" in normalizedCategory || "ziyarah" in normalizedCategory
    }

    private fun createQuranRecommendation(
        candidate: Candidate,
        surah: QuranSurahInfo
    ): HomeRecommendation {
        val relevance = when (candidate.reason) {
            GuidanceReason.WEEKLY -> Triple(
                "Jumu’ah Quran reading",
                "جمعہ کے لیے قرآن کی تلاوت",
                "قراءة القرآن ليوم الجمعة"
            )
            GuidanceReason.HOLY_MONTH -> Triple(
                "Quran reading for this sacred month",
                "اس مبارک مہینے میں قرآن کی تلاوت",
                "قراءة القرآن في هذا الشهر المبارك"
            )
            GuidanceReason.EXACT_EVENT, GuidanceReason.HIJRI_DATE -> Triple(
                "Quran for today’s reflection",
                "آج کے غور و فکر کے لیے قرآن",
                "القرآن لتأمل اليوم"
            )
            else -> Triple(
                "Today’s Quran pick",
                "آج کی قرآن تجویز",
                "اختيار القرآن لليوم"
            )
        }
        return HomeRecommendation(
            id = surah.number.toString(),
            route = ROUTE_QURAN,
            title = "Surah ${surah.name}",
            titleUr = "سورۃ ${surah.nameAr}",
            titleAr = "سورة ${surah.nameAr}",
            description = "${surah.verseCount} verses · Arabic with English and Urdu translations",
            descriptionUr = "${surah.verseCount} آیات · عربی متن، انگریزی اور اردو ترجمے کے ساتھ",
            descriptionAr = "${surah.verseCount} آيات · بالنص العربي والترجمتين الإنجليزية والأردية",
            category = "Quran",
            categoryUr = "قرآن مجید",
            categoryAr = "القرآن الكريم",
            relevance = relevance.first,
            relevanceUr = relevance.second,
            relevanceAr = relevance.third,
            reason = candidate.reason,
            score = candidate.score,
            kind = HomeRecommendationKind.QURAN
        )
    }

    private fun createZiyaratRecommendation(
        candidate: Candidate,
        ziyarat: ZiyaratItem
    ): HomeRecommendation = HomeRecommendation(
        id = ziyarat.id,
        route = ROUTE_ZIYARAT,
        title = ziyarat.titleEn,
        titleUr = ziyarat.titleUr,
        titleAr = ziyarat.titleAr,
        description = ziyarat.enText.homeExcerpt(),
        descriptionUr = ziyarat.urText.homeExcerpt(),
        descriptionAr = ziyarat.arText.homeExcerpt(),
        category = "Ziyarat",
        categoryUr = "زیارت",
        categoryAr = "زيارة",
        relevance = "Ziyarat for today’s occasion",
        relevanceUr = "آج کی مناسبت سے منسلک زیارت",
        relevanceAr = "زيارة مرتبطة بمناسبة اليوم",
        reason = candidate.reason,
        score = candidate.score,
        kind = HomeRecommendationKind.ZIYARAT
    )

    private fun createWisdomRecommendation(
        candidate: Candidate,
        wisdom: MasoomWisdom,
        dayOfYear: Int
    ): HomeRecommendation {
        val sayingIndex = dayOfYear % wisdom.sayingsEn.size
        return HomeRecommendation(
            id = candidate.id,
            route = ROUTE_WISDOM,
            title = "Aqwal of ${wisdom.nameEn}",
            titleUr = "${wisdom.nameUr} کے اقوال",
            titleAr = "من أقوال ${wisdom.nameAr}",
            description = wisdom.sayingsEn[sayingIndex],
            descriptionUr = wisdom.sayingsUr.getOrNull(sayingIndex) ?: wisdom.sayingsUr.firstOrNull(),
            descriptionAr = wisdom.sayingsAr.getOrNull(sayingIndex) ?: wisdom.sayingsAr.firstOrNull(),
            category = "Aqwal & Hadith",
            categoryUr = "اقوال و احادیث",
            categoryAr = "أقوال وحديث",
            relevance = "Sayings linked to this occasion",
            relevanceUr = "آج کی مناسبت سے منسلک اقوال",
            relevanceAr = "أقوال مرتبطة بمناسبة اليوم",
            reason = candidate.reason,
            score = candidate.score,
            kind = HomeRecommendationKind.WISDOM
        )
    }

    private fun addMasoomCandidates(
        candidates: Candidates,
        event: MasoomEvent,
        ziyarats: List<ZiyaratItem>,
        ziyaratById: Map<String, ZiyaratItem>,
        wisdomByMasoom: Map<Int, MasoomWisdom>
    ) {
        if (!isDirectMasoomEvent(event)) return

        val preferredId = when (event.masoomIndex) {
            1 -> "ziyarat_prophet"
            2 -> "ziyarat_aminullah"
            3 -> "ziyarat_fatimah"
            4 -> "ziyarat_hasan"
            5 -> "ziyarat_ashura"
            6 -> "ziyarat_sajjad"
            7 -> "ziyarat_baqir"
            8 -> "ziyarat_sadiq"
            9 -> "ziyarat_kadhim"
            10 -> "ziyarat_ridha"
            11 -> "ziyarat_jawad"
            12 -> "ziyarat_hadi"
            13 -> "ziyarat_askari"
            14 -> "ziyarat_yasin"
            else -> null
        }
        val ziyaratId = preferredId?.takeIf { ziyaratById.containsKey(it) }
            ?: ziyarats.firstOrNull { it.masoomIndex == event.masoomIndex }?.id
        if (ziyaratId != null) {
            candidates.ziyarat(ziyaratId, 126, GuidanceReason.EXACT_EVENT)
        }

        val personWisdom = wisdomByMasoom[event.masoomIndex]
        if (personWisdom != null && personWisdom.sayingsEn.isNotEmpty()) {
            candidates.wisdom(event.masoomIndex, 120, GuidanceReason.EXACT_EVENT)
        }
    }

    private fun isDirectMasoomEvent(event: MasoomEvent): Boolean {
        if (event.masoomIndex !in 1..14) return false
        val id = event.id.lowercase()
        val canonicalId = id.startsWith("event_imam_") ||
            id.startsWith("event_prophet_") ||
            id.startsWith("event_bibi_fatima_") ||
            id == "event_ashura" ||
            id == "event_arbaeen" ||
            id == "event_karbala_arrival" ||
            id == "event_karbala_water_cutoff" ||
            id == "event_eid_zahra" ||
            id == "event_eid_bithah" ||
            id == "event_eid_ghadir" ||
            id == "event_eid_mubahala" ||
            id == "event_ali_fatima_marriage"
        if (canonicalId) return true

        // Admin/Firebase records may use a custom ID; a principal-person title
        // still identifies the occasion without classifying related family members.
        val title = event.title.lowercase().trim()
        return when (event.masoomIndex) {
            1 -> title.startsWith("prophet muhammad")
            2 -> title.startsWith("imam ali") || title.startsWith("strike on imam ali") ||
                title.startsWith("marriage of imam ali")
            3 -> title.startsWith("lady fatima")
            4 -> title.startsWith("imam hasan")
            5 -> title.startsWith("imam hussain") || title.startsWith("imam hussein") ||
                "ashura" in title || "arbaeen" in title
            6 -> title.startsWith("imam sajjad") || title.startsWith("imam zayn")
            7 -> title.startsWith("imam baqir")
            8 -> title.startsWith("imam sadiq")
            9 -> title.startsWith("imam kadhim") || title.startsWith("imam kazim")
            10 -> title.startsWith("imam ridha") || title.startsWith("imam reza")
            11 -> title.startsWith("imam jawad")
            12 -> title.startsWith("imam hadi")
            13 -> title.startsWith("imam askari")
            14 -> title.startsWith("imam mahdi")
            else -> false
        }
    }

    private fun String.homeExcerpt(maxLength: Int = 180): String {
        val clean = trim().replace(Regex("\\s+"), " ")
        return if (clean.length <= maxLength) clean else clean.take(maxLength).trimEnd() + "…"
    }

    private fun addEventCandidates(candidates: Candidates, event: MasoomEvent) {
        val id = event.id.lowercase()
        val title = event.title.lowercase()
        val reason = GuidanceReason.EXACT_EVENT
        when {
            "qadr" in id || "qadr" in title -> {
                candidates.aamal("aamal_shab_e_qadr", 132, reason)
                candidates.quran(97, 126, reason)
                candidates.dua("iftitah", 124, reason)
                candidates.dua("hazeen", 122, reason)
                candidates.dua("munajat_taibin", 118, reason)
            }
            "eid_fitr" in id || "eid al-fitr" in title -> {
                candidates.aamal("aamal_eid_fitr", 132, reason)
                candidates.dua("nudba", 124, reason)
                candidates.dua("ziyarat_aminullah", 116, reason)
            }
            "arafah" in id || "arafah" in title -> {
                candidates.aamal("aamal_zilhajj", 132, reason)
                candidates.dua("waritha", 120, reason)
            }
            "arbaeen" in id || "arbaeen" in title -> {
                candidates.dua("ziyarat_arbaeen", 132, reason)
                candidates.dua("waritha", 124, reason)
            }
            "ashura" in id || "ashura" in title -> {
                candidates.aamal("aamal_ashura", 132, reason)
                candidates.dua("ashura", 128, reason)
                candidates.dua("waritha", 122, reason)
            }
            "ghadir" in id || "ghadir" in title -> {
                candidates.aamal("aamal_eid_ghadir", 132, reason)
                candidates.dua("hadith_kisa", 122, reason)
                candidates.dua("nudba", 120, reason)
            }
            "mubahala" in id || "mubahala" in title -> {
                candidates.dua("hadith_kisa", 132, reason)
                candidates.dua("ziyarat_aminullah", 118, reason)
            }
            "mahdi" in title -> {
                candidates.dua("hujjat", 122, reason)
                candidates.dua("ziyarat_aal_yaseen", 120, reason)
                candidates.dua("faraj", 116, reason)
            }
            "abbas" in title -> {
                candidates.dua("ziyarat_abbas", 122, reason)
                candidates.dua("ashura", 108, reason)
            }
            "fatima" in title -> {
                candidates.dua("ziyarat_fatima", 122, reason)
                candidates.dua("hadith_kisa", 116, reason)
            }
            "sajjad" in title -> {
                candidates.dua("munajat_taibin", 124, reason)
            }
            "hussain" in title -> {
                candidates.dua("waritha", 120, reason)
                candidates.dua("ashura", 116, reason)
            }
            "imam_ali" in id || "imam ali" in title -> {
                candidates.dua("munajat_imamali", 120, reason)
                candidates.dua("ziyarat_aminullah", 112, reason)
            }
            event.eventType == EventType.WILADAT -> {
                if (isDirectMasoomEvent(event)) {
                    candidates.dua("tawassul", 118, reason)
                } else {
                    candidates.dua("hadith_kisa", 124, reason)
                    candidates.dua("ziyarat_aminullah", 120, reason)
                }
            }
            else -> {
                if (isDirectMasoomEvent(event)) {
                    candidates.dua("tawassul", 118, reason)
                } else {
                    candidates.dua("munajat_taibin", 124, reason)
                    candidates.dua("ziyarat_aminullah", 120, reason)
                }
            }
        }
    }

    private fun selectContentTopic(
        occasionEvents: List<MasoomEvent>,
        hijriMonth: Int,
        hijriDay: Int,
        isJumuah: Boolean,
        isMorning: Boolean,
        isNight: Boolean,
        dayOfYear: Int
    ): String {
        val eventText = occasionEvents.joinToString(" ") { "${it.id} ${it.title}" }.lowercase()
        return when {
            "ghadir" in eventText || (hijriMonth == 12 && hijriDay == 18) -> "ghadir"
            "mahdi" in eventText || (hijriMonth == 8 && hijriDay == 15) -> "hope"
            "qadr" in eventText || (hijriMonth == 9 && hijriDay in listOf(19, 21, 23)) -> "forgiveness"
            "arafah" in eventText || (hijriMonth == 12 && hijriDay == 9) -> "forgiveness"
            hijriMonth == 10 && hijriDay == 1 -> "gratitude"
            "mubahala" in eventText || occasionEvents.any { it.eventType == EventType.WILADAT } -> "ahlulbayt"
            hijriMonth in 1..2 || occasionEvents.any { it.eventType == EventType.SHAHADAT } -> "patience"
            hijriMonth == 9 -> "forgiveness"
            hijriMonth == 7 -> "forgiveness"
            isJumuah -> "remembrance"
            isMorning -> "gratitude"
            isNight -> "trust"
            else -> listOf("prayer", "character", "knowledge", "kindness")[dayOfYear % 4]
        }
    }
}
