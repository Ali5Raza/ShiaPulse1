package com.example.data

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

enum class QuranNavigationType(
    val apiSegment: String,
    val metadataField: String,
    val maximum: Int
) {
    JUZ("juz", "juz_number", 30),
    HIZB("hizb", "hizb_number", 60),
    RUB_HIZB("rub_el_hizb", "rub_el_hizb_number", 240),
    RUKU("ruku", "ruku_number", 558),
    MANZIL("manzil", "manzil_number", 7),
    PAGE("page", "page_number", 604)
}

data class QuranNavigationTarget(
    val type: QuranNavigationType,
    val number: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val firstVerseId: Int,
    val totalVerses: Int
)

/** Resolves navigation units to their first ayah and caches only numeric metadata, never Quran text. */
object QuranNavigationRepository {
    const val SOURCE_NAME = "Quran Foundation"
    private const val API_ROOT = "https://api.quran.com/api/v4/verses/by_"
    private const val CACHE_DIRECTORY = "quran_navigation"
    private const val CACHE_VERSION = 1
    private const val FIELDS = "verse_key,juz_number,hizb_number,rub_el_hizb_number,ruku_number," +
        "manzil_number,page_number"

    /** Blocking by design: invoke from Dispatchers.IO. */
    fun resolve(context: Context, type: QuranNavigationType, number: Int): QuranNavigationTarget {
        require(number in 1..type.maximum) { "Invalid navigation unit" }
        readCache(context, type, number)?.let { return it }

        val root = fetchJson("$API_ROOT${type.apiSegment}/$number?fields=$FIELDS&per_page=1")
        val verses = root.optJSONArray("verses") ?: throw IOException("Navigation verses are missing")
        if (verses.length() == 0) throw IOException("Navigation unit is empty")
        val verse = verses.getJSONObject(0)
        if (verse.optInt(type.metadataField, 0) != number) {
            throw IOException("Navigation metadata does not match the requested unit")
        }
        val reference = verse.optString("verse_key").split(":")
        val surahNumber = reference.getOrNull(0)?.toIntOrNull() ?: 0
        val ayahNumber = reference.getOrNull(1)?.toIntOrNull() ?: 0
        val firstVerseId = verse.optInt("id", 0)
        if (surahNumber !in 1..QuranAssetLoader.TOTAL_SURAHS || ayahNumber <= 0 || firstVerseId <= 0) {
            throw IOException("Navigation target is invalid")
        }
        val totalVerses = root.optJSONObject("pagination")?.optInt("total_records", 0)
            ?.takeIf { it > 0 } ?: verses.length()
        return QuranNavigationTarget(
            type = type,
            number = number,
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            firstVerseId = firstVerseId,
            totalVerses = totalVerses
        ).also { writeCache(context, it) }
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
            if (status !in 200..299) throw IOException("Quran navigation HTTP $status")
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun cacheFile(context: Context, type: QuranNavigationType, number: Int): File = File(
        File(context.filesDir, CACHE_DIRECTORY),
        "${type.apiSegment}_$number.json"
    )

    private fun readCache(
        context: Context,
        type: QuranNavigationType,
        number: Int
    ): QuranNavigationTarget? = runCatching {
        val file = cacheFile(context, type, number)
        if (!file.isFile) return@runCatching null
        val root = JSONObject(file.readText(Charsets.UTF_8))
        if (
            root.optInt("version") != CACHE_VERSION ||
            root.optString("type") != type.name ||
            root.optInt("number") != number
        ) {
            return@runCatching null
        }
        QuranNavigationTarget(
            type = type,
            number = number,
            surahNumber = root.optInt("surahNumber"),
            ayahNumber = root.optInt("ayahNumber"),
            firstVerseId = root.optInt("firstVerseId"),
            totalVerses = root.optInt("totalVerses")
        ).takeIf {
            it.surahNumber in 1..QuranAssetLoader.TOTAL_SURAHS &&
                it.ayahNumber > 0 && it.firstVerseId > 0 && it.totalVerses > 0
        }
    }.getOrNull()

    private fun writeCache(context: Context, target: QuranNavigationTarget) {
        runCatching {
            val file = cacheFile(context, target.type, target.number)
            file.parentFile?.mkdirs()
            val root = JSONObject()
                .put("version", CACHE_VERSION)
                .put("type", target.type.name)
                .put("number", target.number)
                .put("surahNumber", target.surahNumber)
                .put("ayahNumber", target.ayahNumber)
                .put("firstVerseId", target.firstVerseId)
                .put("totalVerses", target.totalVerses)
                .put("source", SOURCE_NAME)
            val temporary = File(file.parentFile, file.name + ".tmp")
            temporary.writeText(root.toString(), Charsets.UTF_8)
            if (!temporary.renameTo(file)) {
                file.writeText(root.toString(), Charsets.UTF_8)
                temporary.delete()
            }
        }
    }
}
