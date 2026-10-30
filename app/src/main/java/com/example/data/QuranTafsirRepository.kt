package com.example.data

import android.content.Context
import android.text.Html
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import org.json.JSONObject

/** A named, attributed commentary source. No AI-produced translation is registered here. */
data class QuranTafsirSource(
    val id: String,
    val name: String,
    val author: String,
    val language: String,
    val languageLabel: String,
    val provider: String,
    val termsLabel: String,
    val termsUrl: String,
    val originalSourceUrl: String,
    internal val format: QuranTafsirFormat
)

enum class QuranTafsirFormat {
    AYAH_HTML,
    SURAH_JSON
}

data class QuranTafsirEntry(
    val sourceId: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val mappedAyahs: List<Int>,
    val text: String,
    val sourceUrl: String,
    val retrievedAt: Long
)

data class QuranTafsirSearchResult(
    val sourceId: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val text: String
)

data class QuranTafsirSearchPage(
    val results: List<QuranTafsirSearchResult>,
    val totalMatches: Int,
    val surahCount: Int
)

/**
 * On-demand, source-attributed Tafsir with a private offline cache.
 *
 * English commentary is extracted from its direct ayah page. Classical Arabic editions use the
 * upstream ayah-to-block references; blocks are never assigned to an ayah by position or guesswork.
 * Source-sensitive text is not bundled in the repository or APK.
 */
object QuranTafsirRepository {
    private const val CACHE_DIRECTORY = "quran_tafsir"
    private const val CACHE_VERSION = 1
    private const val THAQALAYN_ROOT = "https://thaqalayn.com/quran/"
    private const val TAFSIR_DATA_ROOT = "https://thaqalayntafsir.netlify.app/"
    private const val TAFSIR_TERMS = "https://github.com/narmafraz/ThaqalaynTafsirData#license"
    private const val CLASSICAL_SOURCE_HOME = "https://www.altafsir.com/"

    val sources: List<QuranTafsirSource> = listOf(
        QuranTafsirSource(
            id = "en.nur",
            name = "Tafsir Nur al-Qur'an / An Enlightening Commentary",
            author = "Compiled by a Shia scholar group under Sayyid Kamal Faqih Imani · principally based on Tafsir Nemuneh by Ayatullah Nasir Makarim Shirazi and scholars · English translator Sayyid Abbas Sadr-'ameli",
            language = "en",
            languageLabel = "English",
            provider = "Thaqalayn · original work published by Al-Islam.org",
            termsLabel = "Thaqalayn CC BY 4.0 only where it holds rights; Al-Islam original is non-commercial; on-demand loading, no bundled corpus",
            termsUrl = "https://thaqalayn.com/terms",
            originalSourceUrl = "https://al-islam.org/library/quran-commentaries",
            format = QuranTafsirFormat.AYAH_HTML
        ),
        classicalSource(
            id = "ar.qummi",
            name = "Tafsir al-Qummi",
            author = "Ali ibn Ibrahim al-Qummi"
        ),
        classicalSource(
            id = "ar.tibyan",
            name = "Al-Tibyan fi Tafsir al-Qur'an",
            author = "Sheikh Abu Ja'far al-Tusi"
        ),
        classicalSource(
            id = "ar.majma",
            name = "Majma' al-Bayan",
            author = "Al-Fadl ibn al-Hasan al-Tabarsi"
        ),
        classicalSource(
            id = "ar.burhan",
            name = "Al-Burhan fi Tafsir al-Qur'an",
            author = "Hashim ibn Sulayman al-Bahrani"
        )
    )

    val defaultSourceId: String = sources.first().id

    fun source(sourceId: String): QuranTafsirSource =
        sources.firstOrNull { it.id == sourceId } ?: sources.first()

    /** Blocking by design: invoke from Dispatchers.IO. A null result means no mapped commentary. */
    fun load(
        context: Context,
        sourceId: String,
        surahNumber: Int,
        ayahNumber: Int
    ): QuranTafsirEntry? {
        require(surahNumber in 1..QuranAssetLoader.TOTAL_SURAHS) { "Invalid Surah" }
        require(ayahNumber > 0) { "Invalid Ayah" }
        val selectedSource = source(sourceId)
        return when (selectedSource.format) {
            QuranTafsirFormat.AYAH_HTML -> loadEnglishAyah(context, selectedSource, surahNumber, ayahNumber)
            QuranTafsirFormat.SURAH_JSON -> {
                val root = loadArabicSurah(context, selectedSource, surahNumber)
                entryFromArabicSurah(selectedSource, root, surahNumber, ayahNumber)
            }
        }
    }

    /** Searches only commentary already cached on this device; it never downloads a corpus. */
    fun searchCached(
        context: Context,
        query: String,
        sourceId: String? = null,
        language: String? = null,
        resultLimit: Int = 100
    ): QuranTafsirSearchPage {
        val normalizedQuery = normalizeForSearch(query)
        if (normalizedQuery.length < 2) return QuranTafsirSearchPage(emptyList(), 0, 0)
        val limit = resultLimit.coerceIn(1, 200)
        val matches = mutableListOf<QuranTafsirSearchResult>()
        val matchingSurahs = mutableSetOf<Int>()
        var totalMatches = 0
        val selectedSources = sources.filter { candidate ->
            (sourceId.isNullOrBlank() || candidate.id == sourceId) &&
                (language.isNullOrBlank() || candidate.language == language)
        }

        selectedSources.forEach { selectedSource ->
            val directory = File(File(context.filesDir, CACHE_DIRECTORY), selectedSource.id)
            val files = directory.listFiles { file -> file.isFile && file.name.endsWith(".json") }
                .orEmpty()
                .sortedBy { it.name }
            files.forEach { file ->
                runCatching {
                    when (selectedSource.format) {
                        QuranTafsirFormat.AYAH_HTML -> {
                            val entry = entryFromEnglishCache(
                                selectedSource,
                                JSONObject(file.readText(Charsets.UTF_8))
                            ) ?: return@runCatching
                            if (normalizeForSearch(entry.text).contains(normalizedQuery)) {
                                totalMatches++
                                matchingSurahs += entry.surahNumber
                                if (matches.size < limit) {
                                    matches += QuranTafsirSearchResult(
                                        selectedSource.id,
                                        entry.surahNumber,
                                        entry.ayahNumber,
                                        entry.text
                                    )
                                }
                            }
                        }
                        QuranTafsirFormat.SURAH_JSON -> {
                            val root = JSONObject(file.readText(Charsets.UTF_8))
                            val surahNumber = root.optInt("surah", 0)
                            if (surahNumber !in 1..QuranAssetLoader.TOTAL_SURAHS) return@runCatching
                            val blocks = root.optJSONArray("blocks") ?: return@runCatching
                            val ayahs = root.optJSONArray("ayahs") ?: return@runCatching
                            for (index in 0 until ayahs.length()) {
                                val mapping = ayahs.optJSONObject(index) ?: continue
                                val ayahNumber = mapping.optInt("ayah", 0)
                                val blockIndex = mapping.optInt("block", -1)
                                if (ayahNumber <= 0 || blockIndex !in 0 until blocks.length()) continue
                                val text = plainText(blocks.optString(blockIndex))
                                if (text.isBlank() || !normalizeForSearch(text).contains(normalizedQuery)) continue
                                totalMatches++
                                matchingSurahs += surahNumber
                                if (matches.size < limit) {
                                    matches += QuranTafsirSearchResult(
                                        selectedSource.id,
                                        surahNumber,
                                        ayahNumber,
                                        text
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return QuranTafsirSearchPage(matches, totalMatches, matchingSurahs.size)
    }

    private fun loadEnglishAyah(
        context: Context,
        selectedSource: QuranTafsirSource,
        surahNumber: Int,
        ayahNumber: Int
    ): QuranTafsirEntry? {
        val file = englishCacheFile(context, selectedSource.id, surahNumber, ayahNumber)
        if (file.isFile) {
            runCatching {
                entryFromEnglishCache(selectedSource, JSONObject(file.readText(Charsets.UTF_8)))
            }.getOrNull()?.let { return it }
        }
        val url = "$THAQALAYN_ROOT$surahNumber/$ayahNumber"
        val html = fetchText(url, "text/html,application/xhtml+xml")
        val text = extractThaqalaynCommentary(html, ayahNumber) ?: return null
        val entry = QuranTafsirEntry(
            sourceId = selectedSource.id,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            mappedAyahs = listOf(ayahNumber),
            text = text,
            sourceUrl = url,
            retrievedAt = System.currentTimeMillis()
        )
        writeAtomically(
            file,
            JSONObject()
                .put("version", CACHE_VERSION)
                .put("sourceId", selectedSource.id)
                .put("surahNumber", surahNumber)
                .put("ayahNumber", ayahNumber)
                .put("text", entry.text)
                .put("sourceUrl", url)
                .put("retrievedAt", entry.retrievedAt)
                .put("presentationChange", "HTML formatting removed; source wording retained")
                .toString()
        )
        return entry
    }

    private fun loadArabicSurah(
        context: Context,
        selectedSource: QuranTafsirSource,
        surahNumber: Int
    ): JSONObject {
        val file = arabicCacheFile(context, selectedSource.id, surahNumber)
        if (file.isFile) {
            runCatching {
                JSONObject(file.readText(Charsets.UTF_8)).also { root ->
                    validateArabicSurah(selectedSource, root, surahNumber)
                }
            }.getOrNull()?.let { return it }
        }
        val url = "$TAFSIR_DATA_ROOT${selectedSource.id}/$surahNumber.json"
        val body = fetchText(url, "application/json")
        val root = JSONObject(body)
        validateArabicSurah(selectedSource, root, surahNumber)
        writeAtomically(file, body)
        return root
    }

    private fun validateArabicSurah(
        selectedSource: QuranTafsirSource,
        root: JSONObject,
        surahNumber: Int
    ) {
        if (root.optString("edition") != selectedSource.id || root.optInt("surah") != surahNumber) {
            throw IOException("Tafsir Surah identity does not match the requested source")
        }
        if (root.optJSONArray("blocks") == null || root.optJSONArray("ayahs") == null) {
            throw IOException("Tafsir ayah mappings are missing")
        }
    }

    private fun entryFromArabicSurah(
        selectedSource: QuranTafsirSource,
        root: JSONObject,
        surahNumber: Int,
        ayahNumber: Int
    ): QuranTafsirEntry? {
        val blocks = root.optJSONArray("blocks") ?: return null
        val ayahs = root.optJSONArray("ayahs") ?: return null
        var selectedBlock = -1
        for (index in 0 until ayahs.length()) {
            val mapping = ayahs.optJSONObject(index) ?: continue
            if (mapping.optInt("ayah") == ayahNumber) {
                selectedBlock = mapping.optInt("block", -1)
                break
            }
        }
        if (selectedBlock !in 0 until blocks.length()) return null
        val text = plainText(blocks.optString(selectedBlock))
        if (text.isBlank()) return null
        val mappedAyahs = buildList {
            for (index in 0 until ayahs.length()) {
                val mapping = ayahs.optJSONObject(index) ?: continue
                if (mapping.optInt("block", -1) == selectedBlock) {
                    mapping.optInt("ayah", 0).takeIf { it > 0 }?.let { add(it) }
                }
            }
        }.distinct().sorted()
        return QuranTafsirEntry(
            sourceId = selectedSource.id,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            mappedAyahs = mappedAyahs.ifEmpty { listOf(ayahNumber) },
            text = text,
            sourceUrl = selectedSource.originalSourceUrl,
            retrievedAt = System.currentTimeMillis()
        )
    }

    private fun entryFromEnglishCache(
        selectedSource: QuranTafsirSource,
        root: JSONObject
    ): QuranTafsirEntry? {
        if (root.optInt("version") != CACHE_VERSION || root.optString("sourceId") != selectedSource.id) {
            return null
        }
        val surahNumber = root.optInt("surahNumber", 0)
        val ayahNumber = root.optInt("ayahNumber", 0)
        val text = root.optString("text").trim()
        if (surahNumber !in 1..QuranAssetLoader.TOTAL_SURAHS || ayahNumber <= 0 || text.isBlank()) {
            return null
        }
        return QuranTafsirEntry(
            sourceId = selectedSource.id,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            mappedAyahs = listOf(ayahNumber),
            text = text,
            sourceUrl = root.optString("sourceUrl").ifBlank {
                "$THAQALAYN_ROOT$surahNumber/$ayahNumber"
            },
            retrievedAt = root.optLong("retrievedAt", 0L)
        )
    }

    private fun extractThaqalaynCommentary(html: String, ayahNumber: Int): String? {
        val cleaned = SCRIPT_OR_STYLE.replace(html, "")
        val visibleText = plainText(cleaned)
        val marker = TAFSIR_MARKER.find(visibleText) ?: return null
        var text = visibleText.substring(marker.range.first)
        val cookieIndex = text.indexOf("Cookie preferences", ignoreCase = true)
        if (cookieIndex >= 0) text = text.substring(0, cookieIndex)
        val lines = text.lines().toMutableList()
        while (lines.lastOrNull()?.isBlank() == true) lines.removeAt(lines.lastIndex)
        if (lines.lastOrNull()?.trim()?.matches(footerPattern(ayahNumber)) == true) {
            lines.removeAt(lines.lastIndex)
        }
        text = lines.joinToString("\n").trim()
        if (!Regex("\\bVerse\\s+$ayahNumber\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            return null
        }
        return text.takeIf { it.length >= 120 }
    }

    private fun footerPattern(ayahNumber: Int) =
        Regex(".*[·|]\\s*Verse\\s+$ayahNumber\\s*", RegexOption.IGNORE_CASE)

    private fun plainText(value: String): String {
        if (value.isBlank()) return ""
        return Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace('\u00A0', ' ')
            .replace(Regex("[\\t ]+"), " ")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun normalizeForSearch(value: String): String =
        value.lowercase(Locale.ROOT)
            .replace(ARABIC_DIACRITICS, "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun fetchText(url: String, accept: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("Accept", accept)
            setRequestProperty("User-Agent", "ShiaPulse-Android/1.0")
        }
        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IOException("Tafsir HTTP $status")
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun englishCacheFile(
        context: Context,
        sourceId: String,
        surahNumber: Int,
        ayahNumber: Int
    ): File = File(
        File(File(context.filesDir, CACHE_DIRECTORY), sourceId),
        "${surahNumber.toString().padStart(3, '0')}_${ayahNumber.toString().padStart(3, '0')}.json"
    )

    private fun arabicCacheFile(context: Context, sourceId: String, surahNumber: Int): File = File(
        File(File(context.filesDir, CACHE_DIRECTORY), sourceId),
        "${surahNumber.toString().padStart(3, '0')}.json"
    )

    private fun writeAtomically(file: File, content: String) {
        runCatching {
            file.parentFile?.mkdirs()
            val temporary = File(file.parentFile, file.name + ".tmp")
            temporary.writeText(content, Charsets.UTF_8)
            if (!temporary.renameTo(file)) {
                file.writeText(content, Charsets.UTF_8)
                temporary.delete()
            }
        }
    }

    private fun classicalSource(id: String, name: String, author: String) = QuranTafsirSource(
        id = id,
        name = name,
        author = author,
        language = "ar",
        languageLabel = "Arabic",
        provider = "ThaqalaynTafsirData · altafsir.com / Royal Aal al-Bayt Institute",
        termsLabel = "Academic / non-commercial use under the original altafsir.com terms; loaded on demand",
        termsUrl = TAFSIR_TERMS,
        originalSourceUrl = CLASSICAL_SOURCE_HOME,
        format = QuranTafsirFormat.SURAH_JSON
    )

    private val SCRIPT_OR_STYLE = Regex(
        "<(script|style)\\b[^>]*>.*?</\\1>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val TAFSIR_MARKER = Regex("Tafsir\\s+Nur\\s+al-Qur", RegexOption.IGNORE_CASE)
    private val ARABIC_DIACRITICS = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
}
