package com.example.data

/**
 * A provenance record shared by Knowledge, Calendar and Karbala content.
 *
 * [direct] is meaningful for Qur'an references: false means that the verse is
 * related to the attribute or context, but does not quote the exact list form.
 */
data class KnowledgeReference(
    val type: KnowledgeReferenceType,
    val citation: String,
    val url: String = "",
    val direct: Boolean = true,
    val noteEn: String = "",
    val noteUr: String = "",
    val noteAr: String = "",
    val surahNumber: Int? = null,
    val ayahNumber: Int? = null
) {
    fun localizedNote(languageCode: String): String = when (languageCode) {
        "ur" -> noteUr.ifBlank { noteEn }
        "ar" -> noteAr.ifBlank { noteEn }
        else -> noteEn
    }
}

enum class KnowledgeReferenceType {
    QURAN,
    HADITH,
    AQWAL,
    HISTORY,
    DUA,
    GENERAL
}

fun quranReference(
    surah: Int,
    ayah: Int,
    direct: Boolean = true
): KnowledgeReference = KnowledgeReference(
    type = KnowledgeReferenceType.QURAN,
    citation = "Qur’an $surah:$ayah",
    direct = direct,
    surahNumber = surah,
    ayahNumber = ayah
)
