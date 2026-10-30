package com.example.data

import android.content.Context
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

enum class QuranSearchMatch {
    ARABIC,
    ENGLISH,
    URDU
}

data class QuranSearchResult(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val surahNameAr: String,
    val text: String,
    val matchedIn: QuranSearchMatch
)

object QuranSearchEngine {
    private val arabicMarks = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val repeatedWhitespace = Regex("\\s+")

    const val MIN_QUERY_LENGTH = 2
    const val MAX_RESULTS = 200

    suspend fun search(
        context: Context,
        surahs: List<QuranSurahInfo>,
        query: String,
        maxResults: Int = MAX_RESULTS
    ): List<QuranSearchResult> = withContext(Dispatchers.IO) {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.length < MIN_QUERY_LENGTH || maxResults <= 0) {
            return@withContext emptyList()
        }

        val results = mutableListOf<QuranSearchResult>()
        for (info in surahs) {
            coroutineContext.ensureActive()
            val surah = QuranAssetLoader.loadSurah(context, info)
            for (ayah in surah.verses) {
                val match = when {
                    normalize(ayah.arabic).contains(normalizedQuery) -> {
                        QuranSearchMatch.ARABIC to ayah.arabic
                    }
                    normalize(ayah.english).contains(normalizedQuery) -> {
                        QuranSearchMatch.ENGLISH to ayah.english
                    }
                    normalize(ayah.urdu).contains(normalizedQuery) -> {
                        QuranSearchMatch.URDU to ayah.urdu
                    }
                    else -> null
                }
                if (match != null) {
                    results += QuranSearchResult(
                        surahNumber = info.number,
                        ayahNumber = ayah.number,
                        surahName = info.name,
                        surahNameAr = info.nameAr,
                        text = match.second,
                        matchedIn = match.first
                    )
                    if (results.size >= maxResults) return@withContext results
                }
            }
        }
        results
    }

    internal fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(arabicMarks, "")
        .replace('ٱ', 'ا')
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ى', 'ي')
        .replace('ؤ', 'و')
        .replace('ئ', 'ي')
        .replace(repeatedWhitespace, " ")
        .trim()
}
