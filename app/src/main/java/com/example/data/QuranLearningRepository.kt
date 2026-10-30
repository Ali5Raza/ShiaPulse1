package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** A word-level learning aid. The primary ayah translations remain Qarai and Jawadi. */
data class QuranWord(
    val position: Int,
    val arabic: String,
    val translation: String,
    val transliteration: String,
    val audioUrl: String?
)

data class QuranLearningSurah(
    val surahNumber: Int,
    val wordsByAyah: Map<Int, List<QuranWord>>
)

/**
 * Loads word data only when the learning view is requested. Responses are normalized and cached
 * on the device for offline reading; no third-party word dataset is bundled in the APK or repo.
 */
object QuranLearningRepository {
    const val SOURCE_NAME = "Quran Foundation"
    const val SOURCE_URL = "https://quran.com/"
    const val RECITER_NAME = "Mishari Rashid al-Afasy"
    const val RECITER_URL = "https://quran.com/en/reciters"

    private const val API_ROOT = "https://api.quran.com/api/v4/verses/by_chapter/"
    private const val WORD_AUDIO_ROOT = "https://audio.qurancdn.com/"
    private const val AYAH_AUDIO_ROOT = "https://verses.quran.foundation/Alafasy/mp3/"
    private const val CACHE_DIRECTORY = "quran_learning"
    private const val CACHE_VERSION = 1
    private const val PAGE_SIZE = 50

    fun ayahAudioUrl(surahNumber: Int, ayahNumber: Int): String {
        require(surahNumber in 1..QuranAssetLoader.TOTAL_SURAHS) { "Invalid Surah" }
        require(ayahNumber > 0) { "Invalid Ayah" }
        return AYAH_AUDIO_ROOT +
            surahNumber.toString().padStart(3, '0') +
            ayahNumber.toString().padStart(3, '0') + ".mp3"
    }

    fun wordAudioUrl(rawPath: String?): String? {
        val path = rawPath?.trim().orEmpty()
        if (path.isEmpty()) return null
        return if (path.startsWith("https://") || path.startsWith("http://")) {
            path
        } else {
            WORD_AUDIO_ROOT + path.trimStart('/')
        }
    }

    /** Blocking by design: call from Dispatchers.IO, as the Quran reader already does for assets. */
    fun loadSurah(context: Context, info: QuranSurahInfo): QuranLearningSurah {
        readCache(context, info)?.let { return it }

        val wordsByAyah = linkedMapOf<Int, List<QuranWord>>()
        var page = 1
        var pageCount = 0
        while (page > 0) {
            check(++pageCount <= 20) { "Too many Quran word-data pages" }
            val root = fetchJson(
                API_ROOT + info.number +
                    "?language=en&words=true" +
                    "&word_fields=text_uthmani,translation,transliteration,audio_url" +
                    "&per_page=$PAGE_SIZE&page=$page"
            )
            val verses = root.optJSONArray("verses")
                ?: throw IOException("Quran word data is missing verses")
            for (index in 0 until verses.length()) {
                val verse = verses.getJSONObject(index)
                val ayahNumber = verse.optInt("verse_number", 0)
                if (ayahNumber <= 0) continue
                val words = parseWords(verse.optJSONArray("words"))
                wordsByAyah[ayahNumber] = words
            }
            page = root.optJSONObject("pagination")?.optInt("next_page", 0) ?: 0
        }

        val expected = (1..info.verseCount).toSet()
        check(wordsByAyah.keys == expected) { "Incomplete word data for Surah ${info.number}" }
        check(wordsByAyah.values.all { it.isNotEmpty() }) { "Empty word data for Surah ${info.number}" }
        return QuranLearningSurah(info.number, wordsByAyah).also { writeCache(context, it) }
    }

    private fun parseWords(array: JSONArray?): List<QuranWord> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                if (item.optString("char_type_name") != "word") continue
                val arabic = item.optString("text_uthmani").ifBlank { item.optString("text") }
                if (arabic.isBlank()) continue
                add(
                    QuranWord(
                        position = item.optInt("position", size + 1),
                        arabic = arabic,
                        translation = item.optJSONObject("translation")?.optString("text").orEmpty(),
                        transliteration = item.optJSONObject("transliteration")?.optString("text").orEmpty(),
                        audioUrl = wordAudioUrl(
                            item.optString("audio_url").takeUnless { item.isNull("audio_url") }
                        )
                    )
                )
            }
        }
    }

    private fun fetchJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "ShiaPulse-Android/1.0")
        }
        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IOException("Quran word data HTTP $status")
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun cacheFile(context: Context, surahNumber: Int): File =
        File(File(context.filesDir, CACHE_DIRECTORY), "surah_${surahNumber}.json")

    private fun readCache(context: Context, info: QuranSurahInfo): QuranLearningSurah? = runCatching {
        val surahNumber = info.number
        val file = cacheFile(context, surahNumber)
        if (!file.isFile) return@runCatching null
        val root = JSONObject(file.readText(Charsets.UTF_8))
        if (root.optInt("version") != CACHE_VERSION || root.optInt("surahNumber") != surahNumber) {
            return@runCatching null
        }
        val ayahs = root.getJSONArray("ayahs")
        val wordsByAyah = linkedMapOf<Int, List<QuranWord>>()
        for (index in 0 until ayahs.length()) {
            val ayah = ayahs.getJSONObject(index)
            val ayahNumber = ayah.getInt("number")
            val words = ayah.getJSONArray("words")
            wordsByAyah[ayahNumber] = buildList {
                for (wordIndex in 0 until words.length()) {
                    val word = words.getJSONObject(wordIndex)
                    add(
                        QuranWord(
                            position = word.getInt("position"),
                            arabic = word.getString("arabic"),
                            translation = word.optString("translation"),
                            transliteration = word.optString("transliteration"),
                            audioUrl = word.optString("audioUrl").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }
        if (wordsByAyah.keys != (1..info.verseCount).toSet() || wordsByAyah.values.any { it.isEmpty() }) {
            return@runCatching null
        }
        QuranLearningSurah(surahNumber, wordsByAyah)
    }.getOrNull()

    private fun writeCache(context: Context, surah: QuranLearningSurah) {
        runCatching {
            val file = cacheFile(context, surah.surahNumber)
            file.parentFile?.mkdirs()
            val root = JSONObject()
                .put("version", CACHE_VERSION)
                .put("surahNumber", surah.surahNumber)
                .put("source", SOURCE_NAME)
            val ayahs = JSONArray()
            surah.wordsByAyah.toSortedMap().forEach { (number, words) ->
                val wordArray = JSONArray()
                words.forEach { word ->
                    wordArray.put(
                        JSONObject()
                            .put("position", word.position)
                            .put("arabic", word.arabic)
                            .put("translation", word.translation)
                            .put("transliteration", word.transliteration)
                            .put("audioUrl", word.audioUrl ?: "")
                    )
                }
                ayahs.put(JSONObject().put("number", number).put("words", wordArray))
            }
            root.put("ayahs", ayahs)
            val temporary = File(file.parentFile, file.name + ".tmp")
            temporary.writeText(root.toString(), Charsets.UTF_8)
            if (!temporary.renameTo(file)) {
                file.writeText(root.toString(), Charsets.UTF_8)
                temporary.delete()
            }
        }
    }
}

/** Splits existing translation text for reading only; no translation or meaning is generated. */
internal fun quranPhraseSegments(text: String): List<String> {
    val terminators = setOf(',', ';', ':', '.', '!', '?', '،', '؛', '؟', '۔', '—', '–')
    val phrases = mutableListOf<String>()
    val current = StringBuilder()
    text.trim().split(Regex("\\s+")).forEach { token ->
        if (token.isBlank()) return@forEach
        if (current.isNotEmpty()) current.append(' ')
        current.append(token)
        if (token.lastOrNull() in terminators) {
            phrases += current.toString()
            current.clear()
        }
    }
    if (current.isNotEmpty()) phrases += current.toString()
    return phrases
}
