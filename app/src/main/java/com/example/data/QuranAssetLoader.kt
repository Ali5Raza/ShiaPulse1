package com.example.data

import android.content.Context
import java.util.Locale
import org.json.JSONObject

data class QuranSurahInfo(
    val number: Int,
    val name: String,
    val nameAr: String,
    val revelationType: String,
    val verseCount: Int
) {
    val assetPath: String
        get() = "Quran/surah_${number.toString().padStart(3, '0')}.json"
}

data class QuranAyah(
    val number: Int,
    val arabic: String,
    val english: String,
    val urdu: String
)

data class QuranSurah(
    val number: Int,
    val verses: List<QuranAyah>
)

data class QuranReference(
    val surahNumber: Int,
    val ayahNumber: Int? = null
)

/** Resolves existing Daily Ayat citations without changing or duplicating their content. */
object QuranReferenceParser {
    private val numericReference = Regex("""\b(\d{1,3})\s*:\s*(\d{1,3})\b""")
    private val namedReference = Regex(
        """\bSurah\s+(.+?)(?=\s*,|\s*\bVerse\b|\s*\bAyah\b|$)""",
        RegexOption.IGNORE_CASE
    )
    private val verseNumber = Regex("""\b(?:Verse|Ayah)\s+(\d{1,3})\b""", RegexOption.IGNORE_CASE)

    fun parse(source: String, surahs: List<QuranSurahInfo>): QuranReference? {
        val numericMatch = numericReference.find(source)
        if (numericMatch != null) {
            val surahNumber = numericMatch.groupValues[1].toIntOrNull()
            val ayahNumber = numericMatch.groupValues[2].toIntOrNull()
            if (surahNumber != null && ayahNumber != null) {
                return validatedReference(surahNumber, ayahNumber, surahs)
            }
        }

        val rawName = namedReference.find(source)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotEmpty() } ?: return null
        val normalizedName = normalizeSurahName(rawName)
        val surah = surahs.firstOrNull { normalizeSurahName(it.name) == normalizedName }
            ?: when (normalizedName) {
                // Common English spellings used by the existing verified Ayat collection.
                "alkauthar", "alkawthar", "alkausar" -> surahs.firstOrNull { it.number == 108 }
                else -> null
            }
            ?: return null
        val ayahNumber = verseNumber.find(source)?.groupValues?.get(1)?.toIntOrNull()
        return validatedReference(surah.number, ayahNumber, surahs)
    }

    private fun validatedReference(
        surahNumber: Int,
        ayahNumber: Int?,
        surahs: List<QuranSurahInfo>
    ): QuranReference? {
        val surah = surahs.firstOrNull { it.number == surahNumber } ?: return null
        if (ayahNumber != null && ayahNumber !in 1..surah.verseCount) return null
        return QuranReference(surahNumber, ayahNumber)
    }

    private fun normalizeSurahName(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace("sy", "sh")
        .replace("aa", "a")
        .replace("aw", "au")
        .replace(Regex("[^a-z0-9]"), "")
}

/** Loads one Surah at a time so the complete offline Quran does not occupy memory at once. */
object QuranAssetLoader {
    const val TOTAL_SURAHS = 114
    const val TOTAL_VERSES = 6236

    private val indexLock = Any()
    private val surahCacheLock = Any()

    @Volatile
    private var cachedIndex: List<QuranSurahInfo>? = null

    // At most three recently read Surahs are retained; the largest is only a few hundred KB.
    private val surahCache = LinkedHashMap<Int, QuranSurah>(3, 0.75f, true)

    fun loadIndex(context: Context): List<QuranSurahInfo> {
        cachedIndex?.let { return it }
        return synchronized(indexLock) {
            cachedIndex?.let { return@synchronized it }
            val root = JSONObject(readAsset(context, "Quran/quran_index.json"))
            val array = root.getJSONArray("surahs")
            val result = buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        QuranSurahInfo(
                            number = item.getInt("number"),
                            name = item.getString("name"),
                            nameAr = item.getString("nameAr"),
                            revelationType = item.getString("revelationType"),
                            verseCount = item.getInt("verseCount")
                        )
                    )
                }
            }
            require(result.size == TOTAL_SURAHS) { "Quran index must contain 114 Surahs" }
            require(result.sumOf { it.verseCount } == TOTAL_VERSES) { "Quran index must contain 6,236 verses" }
            cachedIndex = result
            result
        }
    }

    fun loadSurah(context: Context, info: QuranSurahInfo): QuranSurah {
        synchronized(surahCacheLock) {
            surahCache[info.number]?.let { return it }
        }
        val root = JSONObject(readAsset(context, info.assetPath))
        val array = root.getJSONArray("verses")
        val verses = buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    QuranAyah(
                        number = item.getInt("number"),
                        arabic = item.getString("arabic"),
                        english = item.getString("english"),
                        urdu = item.getString("urdu")
                    )
                )
            }
        }
        require(root.getInt("number") == info.number) { "Unexpected Surah asset" }
        require(verses.size == info.verseCount) { "Incomplete Surah ${info.number}" }
        require(verses.map { it.number } == (1..info.verseCount).toList()) {
            "Non-sequential verses in Surah ${info.number}"
        }
        return QuranSurah(info.number, verses).also { surah ->
            synchronized(surahCacheLock) {
                surahCache[info.number] = surah
                while (surahCache.size > 3) {
                    val eldest = surahCache.entries.firstOrNull()?.key ?: break
                    surahCache.remove(eldest)
                }
            }
        }
    }

    private fun readAsset(context: Context, path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
}
