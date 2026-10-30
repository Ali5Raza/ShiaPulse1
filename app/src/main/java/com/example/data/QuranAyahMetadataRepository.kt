package com.example.data

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/** Location and Sajdah metadata, fetched on demand and cached per ayah. */
data class QuranAyahMetadata(
    val surahNumber: Int,
    val ayahNumber: Int,
    val juzNumber: Int? = null,
    val hizbNumber: Int? = null,
    val rubElHizbNumber: Int? = null,
    val rukuNumber: Int? = null,
    val manzilNumber: Int? = null,
    val pageNumber: Int? = null,
    val sajdahType: String? = null,
    val sajdahNumber: Int? = null
)

object QuranAyahMetadataRepository {
    const val SOURCE_NAME = "Quran Foundation"
    private const val API_ROOT = "https://api.quran.com/api/v4/verses/by_key/"
    private const val CACHE_DIRECTORY = "quran_metadata"

    /** Blocking by design: invoke from Dispatchers.IO. */
    fun load(context: Context, surahNumber: Int, ayahNumber: Int): QuranAyahMetadata {
        require(surahNumber in 1..QuranAssetLoader.TOTAL_SURAHS) { "Invalid Surah" }
        require(ayahNumber > 0) { "Invalid Ayah" }
        readCache(context, surahNumber, ayahNumber)?.let { return it }

        val fields = "verse_key,juz_number,hizb_number,rub_el_hizb_number,ruku_number," +
            "manzil_number,page_number,sajdah_type,sajdah_number"
        val root = fetchJson("$API_ROOT$surahNumber:$ayahNumber?fields=$fields")
        val verse = root.optJSONObject("verse") ?: throw IOException("Ayah metadata is missing")
        val metadata = QuranAyahMetadata(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            juzNumber = verse.optionalInt("juz_number"),
            hizbNumber = verse.optionalInt("hizb_number"),
            rubElHizbNumber = verse.optionalInt("rub_el_hizb_number"),
            rukuNumber = verse.optionalInt("ruku_number"),
            manzilNumber = verse.optionalInt("manzil_number"),
            pageNumber = verse.optionalInt("page_number"),
            sajdahType = verse.optionalString("sajdah_type"),
            sajdahNumber = verse.optionalInt("sajdah_number")
        )
        writeCache(context, metadata)
        return metadata
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
            if (status !in 200..299) throw IOException("Ayah metadata HTTP $status")
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun cacheFile(context: Context, surahNumber: Int, ayahNumber: Int): File =
        File(
            File(context.filesDir, CACHE_DIRECTORY),
            "${surahNumber.toString().padStart(3, '0')}_${ayahNumber.toString().padStart(3, '0')}.json"
        )

    private fun readCache(context: Context, surahNumber: Int, ayahNumber: Int): QuranAyahMetadata? =
        runCatching {
            val file = cacheFile(context, surahNumber, ayahNumber)
            if (!file.isFile) return@runCatching null
            val root = JSONObject(file.readText(Charsets.UTF_8))
            if (root.optInt("surahNumber") != surahNumber || root.optInt("ayahNumber") != ayahNumber) {
                return@runCatching null
            }
            QuranAyahMetadata(
                surahNumber = surahNumber,
                ayahNumber = ayahNumber,
                juzNumber = root.optionalInt("juzNumber"),
                hizbNumber = root.optionalInt("hizbNumber"),
                rubElHizbNumber = root.optionalInt("rubElHizbNumber"),
                rukuNumber = root.optionalInt("rukuNumber"),
                manzilNumber = root.optionalInt("manzilNumber"),
                pageNumber = root.optionalInt("pageNumber"),
                sajdahType = root.optionalString("sajdahType"),
                sajdahNumber = root.optionalInt("sajdahNumber")
            )
        }.getOrNull()

    private fun writeCache(context: Context, metadata: QuranAyahMetadata) {
        runCatching {
            val file = cacheFile(context, metadata.surahNumber, metadata.ayahNumber)
            file.parentFile?.mkdirs()
            val root = JSONObject()
                .put("surahNumber", metadata.surahNumber)
                .put("ayahNumber", metadata.ayahNumber)
                .put("juzNumber", metadata.juzNumber ?: JSONObject.NULL)
                .put("hizbNumber", metadata.hizbNumber ?: JSONObject.NULL)
                .put("rubElHizbNumber", metadata.rubElHizbNumber ?: JSONObject.NULL)
                .put("rukuNumber", metadata.rukuNumber ?: JSONObject.NULL)
                .put("manzilNumber", metadata.manzilNumber ?: JSONObject.NULL)
                .put("pageNumber", metadata.pageNumber ?: JSONObject.NULL)
                .put("sajdahType", metadata.sajdahType ?: JSONObject.NULL)
                .put("sajdahNumber", metadata.sajdahNumber ?: JSONObject.NULL)
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

private fun JSONObject.optionalInt(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null

private fun JSONObject.optionalString(key: String): String? =
    if (has(key) && !isNull(key)) optString(key).trim().takeIf { it.isNotBlank() } else null
