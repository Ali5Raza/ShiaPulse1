package com.example.data

/** Numeric Juz boundaries verified against Quran Foundation's by_juz metadata routes. */
data class QuranJuzBoundary(
    val number: Int,
    val surahNumber: Int,
    val ayahNumber: Int
)

object QuranJuzIndex {
    const val SOURCE_NAME = "Quran Foundation"

    val boundaries: List<QuranJuzBoundary> = listOf(
        QuranJuzBoundary(1, 1, 1),
        QuranJuzBoundary(2, 2, 142),
        QuranJuzBoundary(3, 2, 253),
        QuranJuzBoundary(4, 3, 93),
        QuranJuzBoundary(5, 4, 24),
        QuranJuzBoundary(6, 4, 148),
        QuranJuzBoundary(7, 5, 82),
        QuranJuzBoundary(8, 6, 111),
        QuranJuzBoundary(9, 7, 88),
        QuranJuzBoundary(10, 8, 41),
        QuranJuzBoundary(11, 9, 93),
        QuranJuzBoundary(12, 11, 6),
        QuranJuzBoundary(13, 12, 53),
        QuranJuzBoundary(14, 15, 1),
        QuranJuzBoundary(15, 17, 1),
        QuranJuzBoundary(16, 18, 75),
        QuranJuzBoundary(17, 21, 1),
        QuranJuzBoundary(18, 23, 1),
        QuranJuzBoundary(19, 25, 21),
        QuranJuzBoundary(20, 27, 56),
        QuranJuzBoundary(21, 29, 46),
        QuranJuzBoundary(22, 33, 31),
        QuranJuzBoundary(23, 36, 28),
        QuranJuzBoundary(24, 39, 32),
        QuranJuzBoundary(25, 41, 47),
        QuranJuzBoundary(26, 46, 1),
        QuranJuzBoundary(27, 51, 31),
        QuranJuzBoundary(28, 58, 1),
        QuranJuzBoundary(29, 67, 1),
        QuranJuzBoundary(30, 78, 1)
    )

    fun startGlobalIds(surahs: List<QuranSurahInfo>): List<Int> {
        require(surahs.size == QuranAssetLoader.TOTAL_SURAHS) { "Complete Surah index is required" }
        return boundaries.map { boundary ->
            globalAyahId(boundary.surahNumber, boundary.ayahNumber, surahs)
        }
    }

    fun globalAyahId(surahNumber: Int, ayahNumber: Int, surahs: List<QuranSurahInfo>): Int {
        val info = surahs.firstOrNull { it.number == surahNumber }
            ?: throw IllegalArgumentException("Invalid Surah")
        require(ayahNumber in 1..info.verseCount) { "Invalid Ayah" }
        return surahs.filter { it.number < surahNumber }.sumOf { it.verseCount } + ayahNumber
    }

    fun completedCount(readReferences: Set<String>, surahs: List<QuranSurahInfo>): Int {
        val starts = startGlobalIds(surahs)
        val readIds = readReferences.mapNotNull { reference ->
            val surah = reference.substringBefore(':').toIntOrNull() ?: return@mapNotNull null
            val ayah = reference.substringAfter(':').toIntOrNull() ?: return@mapNotNull null
            runCatching { globalAyahId(surah, ayah, surahs) }.getOrNull()
        }.toSet()
        return starts.indices.count { index ->
            val start = starts[index]
            val end = starts.getOrNull(index + 1)?.minus(1) ?: QuranAssetLoader.TOTAL_VERSES
            (start..end).all(readIds::contains)
        }
    }

    fun juzNumber(surahNumber: Int, ayahNumber: Int, surahs: List<QuranSurahInfo>): Int {
        val globalId = globalAyahId(surahNumber, ayahNumber, surahs)
        val starts = startGlobalIds(surahs)
        return starts.indexOfLast { it <= globalId }.plus(1).coerceIn(1, 30)
    }
}
