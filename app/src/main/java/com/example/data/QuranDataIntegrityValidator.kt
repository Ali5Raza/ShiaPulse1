package com.example.data

import android.content.Context
import java.io.File
import org.json.JSONObject

/** Internal structural validation; it never rewrites Quran, translation, Tafsir, or metadata content. */
data class QuranDataIntegrityReport(
    val checkedAyahs: Int,
    val checkedRecords: Int,
    val issues: List<String>
) {
    val isValid: Boolean get() = issues.isEmpty()

    fun requireValid() {
        check(isValid) {
            "Quran data integrity failed: " + issues.take(20).joinToString("; ")
        }
    }
}

private class QuranIntegritySink {
    val issues = mutableListOf<String>()
    var checkedRecords = 0
    fun error(message: String) {
        issues += message
    }
}

object QuranDataIntegrityValidator {
    private val expectedAyahCounts = listOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128,
        111, 110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73,
        54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49,
        62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28,
        28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
    )
    private val expectedJuzStartIds = listOf(
        1, 149, 260, 386, 517, 641, 751, 900, 1042, 1201, 1328, 1479, 1649, 1803,
        2030, 2215, 2484, 2674, 2876, 3215, 3386, 3564, 3733, 4090, 4265, 4511,
        4706, 5105, 5242, 5673
    )

    /** Blocking by design: invoke from tests or Dispatchers.IO, never on the UI thread. */
    fun validateBundled(context: Context): QuranDataIntegrityReport {
        val sink = QuranIntegritySink()
        val references = mutableSetOf<String>()
        val index = runCatching { QuranAssetLoader.loadIndex(context) }.getOrElse {
            sink.error("Bundled index cannot be loaded: ${it.message}")
            return QuranDataIntegrityReport(0, 0, sink.issues)
        }
        if (index.map { it.number } != (1..114).toList()) sink.error("Surah IDs are not 1–114")
        if (index.map { it.verseCount } != expectedAyahCounts) sink.error("Canonical ayah counts differ")
        index.forEach { info ->
            sink.checkedRecords++
            val surah = runCatching { QuranAssetLoader.loadSurah(context, info) }.getOrElse {
                sink.error("Surah ${info.number} cannot be loaded: ${it.message}")
                return@forEach
            }
            if (surah.number != info.number) sink.error("Wrong Surah ID in ${info.assetPath}")
            if (surah.verses.size != info.verseCount) sink.error("Wrong ayah count in Surah ${info.number}")
            surah.verses.forEachIndexed { position, ayah ->
                val reference = "${info.number}:${ayah.number}"
                if (ayah.number != position + 1) sink.error("Non-sequential ayah ID at $reference")
                if (!references.add(reference)) sink.error("Duplicate ayah $reference")
                if (ayah.arabic.isBlank()) sink.error("Missing Arabic at $reference")
                if (ayah.english.isBlank()) sink.error("Missing Qarai translation at $reference")
                if (ayah.urdu.isBlank()) sink.error("Missing Jawadi translation at $reference")
                val expectedAudio = "https://verses.quran.foundation/Alafasy/mp3/" +
                    info.number.toString().padStart(3, '0') +
                    ayah.number.toString().padStart(3, '0') + ".mp3"
                if (QuranLearningRepository.ayahAudioUrl(info.number, ayah.number) != expectedAudio) {
                    sink.error("Wrong ayah audio mapping at $reference")
                }
            }
        }
        if (references.size != QuranAssetLoader.TOTAL_VERSES) {
            sink.error("Expected 6,236 unique ayahs, found ${references.size}")
        }
        val juzStarts = runCatching { QuranJuzIndex.startGlobalIds(index) }.getOrElse {
            sink.error("Juz boundaries cannot be resolved: ${it.message}")
            emptyList()
        }
        if (juzStarts != expectedJuzStartIds) sink.error("Juz boundary IDs differ from verified metadata")
        return QuranDataIntegrityReport(references.size, sink.checkedRecords, sink.issues)
    }

    /** Validates currently cached external records without downloading or trusting their identities. */
    fun validateRuntimeCaches(context: Context): QuranDataIntegrityReport {
        val sink = QuranIntegritySink()
        val index = runCatching { QuranAssetLoader.loadIndex(context) }.getOrElse {
            sink.error("Index unavailable for cache validation")
            return QuranDataIntegrityReport(0, 0, sink.issues)
        }
        val pageByReference = validateMushafCaches(context, index, sink)
        validateMetadataCaches(context, index, pageByReference, sink)
        validateNavigationCaches(context, index, pageByReference, sink)
        validateLearningCaches(context, index, sink)
        validateTafsirCaches(context, index, sink)
        return QuranDataIntegrityReport(0, sink.checkedRecords, sink.issues)
    }

    fun validateAll(context: Context): QuranDataIntegrityReport {
        val bundled = validateBundled(context)
        val runtime = validateRuntimeCaches(context)
        return QuranDataIntegrityReport(
            bundled.checkedAyahs,
            bundled.checkedRecords + runtime.checkedRecords,
            bundled.issues + runtime.issues
        )
    }

    private fun validateMetadataCaches(
        context: Context,
        index: List<QuranSurahInfo>,
        pageByReference: Map<String, Int>,
        sink: QuranIntegritySink
    ) {
        jsonFiles(File(context.filesDir, "quran_metadata")).forEach { file ->
            sink.checkedRecords++
            val root = readJson(file, sink) ?: return@forEach
            val match = Regex("(\\d{1,3})_(\\d{1,3})\\.json").matchEntire(file.name)
            val surah = root.optInt("surahNumber", 0)
            val ayah = root.optInt("ayahNumber", 0)
            if (match == null || match.groupValues[1].toInt() != surah ||
                match.groupValues[2].toInt() != ayah
            ) {
                sink.error("Metadata filename/ID mismatch: ${file.name}")
            }
            if (!isValidReference(surah, ayah, index)) {
                sink.error("Orphan metadata at $surah:$ayah")
                return@forEach
            }
            val reference = "$surah:$ayah"
            root.optionalInt("juzNumber")?.let { cachedJuz ->
                if (cachedJuz != QuranJuzIndex.juzNumber(surah, ayah, index)) {
                    sink.error("Wrong Juz mapping at $reference")
                }
            }
            root.optionalInt("hizbNumber")?.takeUnless { it in 1..60 }?.let {
                sink.error("Wrong Hizb at $reference")
            }
            root.optionalInt("rubElHizbNumber")?.takeUnless { it in 1..240 }?.let {
                sink.error("Wrong Rubʿ at $reference")
            }
            root.optionalInt("rukuNumber")?.takeUnless { it in 1..558 }?.let {
                sink.error("Wrong Ruku at $reference")
            }
            root.optionalInt("manzilNumber")?.takeUnless { it in 1..7 }?.let {
                sink.error("Wrong Manzil at $reference")
            }
            root.optionalInt("pageNumber")?.let { page ->
                if (page !in 1..604 || pageByReference[reference]?.let { it != page } == true) {
                    sink.error("Wrong Page mapping at $reference")
                }
            }
        }
    }

    private fun validateMushafCaches(
        context: Context,
        index: List<QuranSurahInfo>,
        sink: QuranIntegritySink
    ): Map<String, Int> {
        val pageByReference = mutableMapOf<String, Int>()
        jsonFiles(File(context.filesDir, "quran_mushaf_pages")).forEach { file ->
            sink.checkedRecords++
            val root = readJson(file, sink) ?: return@forEach
            val page = root.optInt("pageNumber", 0)
            if (page !in 1..604 || file.name != "$page.json") {
                sink.error("Mushaf page identity mismatch: ${file.name}")
                return@forEach
            }
            val array = root.optJSONArray("ayahs")
            if (array == null || array.length() == 0) {
                sink.error("Empty Mushaf page $page")
                return@forEach
            }
            val references = mutableSetOf<String>()
            var previousGlobalId = 0
            for (position in 0 until array.length()) {
                val item = array.optJSONObject(position)
                if (item == null) {
                    sink.error("Invalid ayah record on page $page")
                    continue
                }
                val surah = item.optInt("surahNumber", 0)
                val ayah = item.optInt("ayahNumber", 0)
                val reference = "$surah:$ayah"
                if (!isValidReference(surah, ayah, index)) {
                    sink.error("Orphan Mushaf ayah $reference")
                    continue
                }
                val globalId = QuranJuzIndex.globalAyahId(surah, ayah, index)
                if (globalId <= previousGlobalId) sink.error("Wrong Mushaf ayah order on page $page")
                previousGlobalId = globalId
                if (!references.add(reference) || pageByReference.put(reference, page)?.let { it != page } == true) {
                    sink.error("Duplicate/wrong page relationship at $reference")
                }
                if (item.optInt("juzNumber", 0) != QuranJuzIndex.juzNumber(surah, ayah, index)) {
                    sink.error("Wrong Mushaf Juz at $reference")
                }
            }
        }
        return pageByReference
    }

    private fun validateNavigationCaches(
        context: Context,
        index: List<QuranSurahInfo>,
        pageByReference: Map<String, Int>,
        sink: QuranIntegritySink
    ) {
        jsonFiles(File(context.filesDir, "quran_navigation")).forEach { file ->
            sink.checkedRecords++
            val root = readJson(file, sink) ?: return@forEach
            val type = runCatching { QuranNavigationType.valueOf(root.optString("type")) }.getOrNull()
            val number = root.optInt("number", 0)
            val surah = root.optInt("surahNumber", 0)
            val ayah = root.optInt("ayahNumber", 0)
            if (type == null || number !in 1..type.maximum || !isValidReference(surah, ayah, index)) {
                sink.error("Invalid navigation target in ${file.name}")
                return@forEach
            }
            val reference = "$surah:$ayah"
            val expectedId = QuranJuzIndex.globalAyahId(surah, ayah, index)
            if (root.optInt("firstVerseId", 0) != expectedId || root.optInt("totalVerses", 0) <= 0) {
                sink.error("Wrong navigation ID/count at $reference")
            }
            if (type == QuranNavigationType.PAGE && pageByReference[reference]?.let { it != number } == true) {
                sink.error("Wrong cached Page target at $reference")
            }
            if (type == QuranNavigationType.JUZ) {
                val boundary = QuranJuzIndex.boundaries[number - 1]
                if (boundary.surahNumber != surah || boundary.ayahNumber != ayah) {
                    sink.error("Wrong Juz $number target")
                }
            }
        }
    }

    private fun validateLearningCaches(
        context: Context,
        index: List<QuranSurahInfo>,
        sink: QuranIntegritySink
    ) {
        jsonFiles(File(context.filesDir, "quran_learning")).forEach { file ->
            sink.checkedRecords++
            val root = readJson(file, sink) ?: return@forEach
            val surahNumber = root.optInt("surahNumber", 0)
            val info = index.firstOrNull { it.number == surahNumber }
            if (info == null || file.name != "surah_$surahNumber.json") {
                sink.error("Word-cache Surah identity mismatch: ${file.name}")
                return@forEach
            }
            val ayahs = root.optJSONArray("ayahs")
            if (ayahs == null || ayahs.length() != info.verseCount) {
                sink.error("Missing word ayahs in Surah $surahNumber")
                return@forEach
            }
            val seenAyahs = mutableSetOf<Int>()
            for (position in 0 until ayahs.length()) {
                val ayah = ayahs.optJSONObject(position) ?: continue
                val ayahNumber = ayah.optInt("number", 0)
                if (ayahNumber !in 1..info.verseCount || !seenAyahs.add(ayahNumber)) {
                    sink.error("Duplicate/orphan word ayah $surahNumber:$ayahNumber")
                }
                val words = ayah.optJSONArray("words")
                if (words == null || words.length() == 0) {
                    sink.error("Missing words at $surahNumber:$ayahNumber")
                    continue
                }
                var previousPosition = 0
                for (wordIndex in 0 until words.length()) {
                    val word = words.optJSONObject(wordIndex) ?: continue
                    val wordPosition = word.optInt("position", 0)
                    if (wordPosition <= previousPosition || word.optString("arabic").isBlank()) {
                        sink.error("Wrong word ID/order at $surahNumber:$ayahNumber")
                    }
                    previousPosition = wordPosition
                    val audio = word.optString("audioUrl")
                    if (audio.isNotBlank() &&
                        !Regex("https://audio\\.qurancdn\\.com/.+\\.mp3").matches(audio)
                    ) {
                        sink.error("Wrong word audio mapping at $surahNumber:$ayahNumber")
                    }
                }
            }
            if (seenAyahs != (1..info.verseCount).toSet()) {
                sink.error("Orphan/missing word records in Surah $surahNumber")
            }
        }
    }

    private fun validateTafsirCaches(
        context: Context,
        index: List<QuranSurahInfo>,
        sink: QuranIntegritySink
    ) {
        QuranTafsirRepository.sources.forEach { source ->
            val directory = File(File(context.filesDir, "quran_tafsir"), source.id)
            jsonFiles(directory).forEach { file ->
                sink.checkedRecords++
                val root = readJson(file, sink) ?: return@forEach
                if (source.format == QuranTafsirFormat.AYAH_HTML) {
                    val surah = root.optInt("surahNumber", 0)
                    val ayah = root.optInt("ayahNumber", 0)
                    val expectedName = surah.toString().padStart(3, '0') + "_" +
                        ayah.toString().padStart(3, '0') + ".json"
                    if (root.optString("sourceId") != source.id || file.name != expectedName ||
                        !isValidReference(surah, ayah, index) || root.optString("text").isBlank()
                    ) {
                        sink.error("Wrong Tafsir identity/orphan in ${source.id}/${file.name}")
                    }
                } else {
                    val surah = root.optInt("surah", 0)
                    val info = index.firstOrNull { it.number == surah }
                    val blocks = root.optJSONArray("blocks")
                    val ayahs = root.optJSONArray("ayahs")
                    if (root.optString("edition") != source.id || info == null ||
                        file.name != "$surah.json" || blocks == null || ayahs == null
                    ) {
                        sink.error("Wrong Tafsir Surah identity in ${source.id}/${file.name}")
                        return@forEach
                    }
                    val mappings = mutableSetOf<String>()
                    for (position in 0 until ayahs.length()) {
                        val mapping = ayahs.optJSONObject(position) ?: continue
                        val ayah = mapping.optInt("ayah", 0)
                        val block = mapping.optInt("block", -1)
                        if (ayah !in 1..info.verseCount || block !in 0 until blocks.length() ||
                            !mappings.add("$ayah:$block") || blocks.optString(block).isBlank()
                        ) {
                            sink.error("Orphan Tafsir mapping $surah:$ayah in ${source.id}")
                        }
                    }
                }
            }
        }
    }

    private fun isValidReference(
        surahNumber: Int,
        ayahNumber: Int,
        index: List<QuranSurahInfo>
    ): Boolean = index.firstOrNull { it.number == surahNumber }?.let { ayahNumber in 1..it.verseCount }
        ?: false

    private fun jsonFiles(directory: File): List<File> = directory.listFiles()
        ?.filter { it.isFile && it.name.endsWith(".json") }
        ?.sortedBy { it.name }
        .orEmpty()

    private fun readJson(file: File, sink: QuranIntegritySink): JSONObject? = runCatching {
        JSONObject(file.readText(Charsets.UTF_8))
    }.getOrElse {
        sink.error("Unreadable JSON: ${file.name}")
        null
    }
}

private fun JSONObject.optionalInt(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null
