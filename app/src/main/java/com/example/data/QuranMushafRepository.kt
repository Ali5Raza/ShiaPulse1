package com.example.data

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

data class QuranMushafReference(
    val surahNumber: Int,
    val ayahNumber: Int,
    val juzNumber: Int
) {
    val reference: String get() = "$surahNumber:$ayahNumber"
}

data class QuranMushafPage(
    val pageNumber: Int,
    val ayahs: List<QuranMushafReference>
)

/** Page membership only; Quran text always comes from the bundled, validated assets. */
object QuranMushafRepository {
    const val SOURCE_NAME = "Quran Foundation"
    const val TOTAL_PAGES = 604
    private const val API_ROOT = "https://api.quran.com/api/v4/verses/by_page/"
    private const val CACHE_DIRECTORY = "quran_mushaf_pages"
    private const val CACHE_VERSION = 1

    /** Blocking by design: invoke from Dispatchers.IO. */
    fun loadPage(context: Context, pageNumber: Int): QuranMushafPage {
        require(pageNumber in 1..TOTAL_PAGES) { "Invalid Mushaf page" }
        readCache(context, pageNumber)?.let { return it }
        val root = fetchJson(
            "$API_ROOT$pageNumber?fields=verse_key,juz_number,page_number&per_page=300"
        )
        val verses = root.optJSONArray("verses") ?: throw IOException("Mushaf page is missing")
        val total = root.optJSONObject("pagination")?.optInt("total_records", verses.length())
            ?: verses.length()
        if (total != verses.length() || verses.length() == 0) {
            throw IOException("Incomplete Mushaf page")
        }
        val references = parseReferences(verses, pageNumber)
        return QuranMushafPage(pageNumber, references).also { writeCache(context, it) }
    }

    private fun parseReferences(verses: JSONArray, pageNumber: Int): List<QuranMushafReference> {
        var previousId = 0
        val references = buildList {
            for (index in 0 until verses.length()) {
                val verse = verses.getJSONObject(index)
                val id = verse.optInt("id", 0)
                if (id <= previousId || verse.optInt("page_number", 0) != pageNumber) {
                    throw IOException("Wrong page or ayah order")
                }
                previousId = id
                val parts = verse.optString("verse_key").split(":")
                val surah = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val ayah = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val juz = verse.optInt("juz_number", 0)
                if (surah !in 1..QuranAssetLoader.TOTAL_SURAHS || ayah <= 0 || juz !in 1..30) {
                    throw IOException("Invalid Mushaf reference")
                }
                add(QuranMushafReference(surah, ayah, juz))
            }
        }
        if (references.map { it.reference }.toSet().size != references.size) {
            throw IOException("Duplicate ayah on Mushaf page")
        }
        return references
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
            if (status !in 200..299) throw IOException("Mushaf metadata HTTP $status")
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun cacheFile(context: Context, pageNumber: Int): File = File(
        File(context.filesDir, CACHE_DIRECTORY),
        "$pageNumber.json"
    )

    private fun readCache(context: Context, pageNumber: Int): QuranMushafPage? = runCatching {
        val file = cacheFile(context, pageNumber)
        if (!file.isFile) return@runCatching null
        val root = JSONObject(file.readText(Charsets.UTF_8))
        if (root.optInt("version") != CACHE_VERSION || root.optInt("pageNumber") != pageNumber) {
            return@runCatching null
        }
        val array = root.getJSONArray("ayahs")
        val ayahs = buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    QuranMushafReference(
                        item.getInt("surahNumber"),
                        item.getInt("ayahNumber"),
                        item.getInt("juzNumber")
                    )
                )
            }
        }
        if (
            ayahs.isEmpty() ||
            ayahs.any {
                it.surahNumber !in 1..QuranAssetLoader.TOTAL_SURAHS ||
                    it.ayahNumber <= 0 || it.juzNumber !in 1..30
            } ||
            ayahs.map { it.reference }.toSet().size != ayahs.size
        ) {
            return@runCatching null
        }
        QuranMushafPage(pageNumber, ayahs)
    }.getOrNull()

    private fun writeCache(context: Context, page: QuranMushafPage) {
        runCatching {
            val file = cacheFile(context, page.pageNumber)
            file.parentFile?.mkdirs()
            val array = JSONArray()
            page.ayahs.forEach { ayah ->
                array.put(
                    JSONObject()
                        .put("surahNumber", ayah.surahNumber)
                        .put("ayahNumber", ayah.ayahNumber)
                        .put("juzNumber", ayah.juzNumber)
                )
            }
            val root = JSONObject()
                .put("version", CACHE_VERSION)
                .put("pageNumber", page.pageNumber)
                .put("ayahs", array)
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
