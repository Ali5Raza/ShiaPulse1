package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QuranReadingPosition(
    val surahNumber: Int,
    val ayahNumber: Int,
    val updatedAt: Long = 0L
)

enum class QuranMemorisationStatus {
    NEEDS_PRACTICE,
    ALMOST_MEMORISED,
    MEMORISED
}

data class QuranProgressState(
    val isLoaded: Boolean = false,
    val lastRead: QuranReadingPosition? = null,
    val bookmarks: Set<String> = emptySet(),
    val completedSurahs: Set<Int> = emptySet(),
    val khatamStartDay: Long? = null,
    val khatamTargetDays: Int = 30,
    val memorisation: Map<String, QuranMemorisationStatus> = emptyMap(),
    val arabicFontScale: Float = 1f,
    val translationFontScale: Float = 1f,
    val keepScreenAwake: Boolean = false,
    val hifzMode: Boolean = false
)

/** Private, offline reading state. Public/shared Khatam deliberately does not use this store. */
object QuranProgressStore {
    private const val PREFS_NAME = "quran_progress"
    private const val LAST_SURAH = "last_surah"
    private const val LAST_AYAH = "last_ayah"
    private const val LAST_UPDATED = "last_updated"
    private const val BOOKMARKS = "bookmarks"
    private const val COMPLETED_SURAHS = "completed_surahs"
    private const val KHATAM_START_DAY = "khatam_start_day"
    private const val KHATAM_TARGET_DAYS = "khatam_target_days"
    private const val MEMORISATION = "memorisation"
    private const val ARABIC_SCALE = "arabic_scale"
    private const val TRANSLATION_SCALE = "translation_scale"
    private const val KEEP_AWAKE = "keep_awake"
    private const val HIFZ_MODE = "hifz_mode"

    private val mutableState = MutableStateFlow(QuranProgressState())
    val state: StateFlow<QuranProgressState> = mutableState.asStateFlow()

    @Volatile
    private var preferences: SharedPreferences? = null

    @Synchronized
    fun initialize(context: Context) {
        if (preferences != null) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences = prefs
        mutableState.value = readState(prefs).copy(isLoaded = true)
    }

    fun recordReading(context: Context, surahNumber: Int, ayahNumber: Int) {
        if (surahNumber !in 1..QuranAssetLoader.TOTAL_SURAHS || ayahNumber <= 0) return
        update(context) {
            it.copy(
                lastRead = QuranReadingPosition(
                    surahNumber = surahNumber,
                    ayahNumber = ayahNumber,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleBookmark(context: Context, surahNumber: Int, ayahNumber: Int) {
        val reference = quranReferenceKey(surahNumber, ayahNumber)
        update(context) { state ->
            state.copy(
                bookmarks = if (reference in state.bookmarks) {
                    state.bookmarks - reference
                } else {
                    state.bookmarks + reference
                }
            )
        }
    }

    fun removeBookmark(context: Context, reference: String) {
        update(context) { it.copy(bookmarks = it.bookmarks - reference) }
    }

    fun toggleSurahCompleted(context: Context, surahNumber: Int) {
        if (surahNumber !in 1..QuranAssetLoader.TOTAL_SURAHS) return
        update(context) { state ->
            state.copy(
                completedSurahs = if (surahNumber in state.completedSurahs) {
                    state.completedSurahs - surahNumber
                } else {
                    state.completedSurahs + surahNumber
                }
            )
        }
    }

    fun startKhatamPlan(context: Context, targetDays: Int) {
        update(context) {
            it.copy(
                khatamStartDay = currentLocalEpochDay(),
                khatamTargetDays = targetDays.coerceIn(1, 3650)
            )
        }
    }

    fun stopKhatamPlan(context: Context) {
        update(context) { it.copy(khatamStartDay = null) }
    }

    fun clearKhatamProgress(context: Context) {
        update(context) {
            it.copy(completedSurahs = emptySet(), khatamStartDay = null)
        }
    }

    fun setMemorisationStatus(
        context: Context,
        surahNumber: Int,
        ayahNumber: Int,
        status: QuranMemorisationStatus?
    ) {
        val reference = quranReferenceKey(surahNumber, ayahNumber)
        update(context) { state ->
            val updated = state.memorisation.toMutableMap()
            if (status == null) updated.remove(reference) else updated[reference] = status
            state.copy(memorisation = updated)
        }
    }

    fun setArabicFontScale(context: Context, value: Float) {
        update(context) { it.copy(arabicFontScale = value.coerceIn(0.8f, 1.5f)) }
    }

    fun setTranslationFontScale(context: Context, value: Float) {
        update(context) { it.copy(translationFontScale = value.coerceIn(0.8f, 1.5f)) }
    }

    fun setKeepScreenAwake(context: Context, enabled: Boolean) {
        update(context) { it.copy(keepScreenAwake = enabled) }
    }

    fun setHifzMode(context: Context, enabled: Boolean) {
        update(context) { it.copy(hifzMode = enabled) }
    }

    fun currentLocalEpochDay(): Long {
        val now = System.currentTimeMillis()
        return localEpochDay(now, TimeZone.getDefault().getOffset(now))
    }

    internal fun localEpochDay(timestampMillis: Long, offsetMillis: Int): Long =
        Math.floorDiv(timestampMillis + offsetMillis.toLong(), TimeUnit.DAYS.toMillis(1))

    fun quranReferenceKey(surahNumber: Int, ayahNumber: Int): String = "$surahNumber:$ayahNumber"

    @Synchronized
    private fun update(context: Context, transform: (QuranProgressState) -> QuranProgressState) {
        initialize(context)
        val updated = transform(mutableState.value).copy(isLoaded = true)
        persist(requireNotNull(preferences), updated)
        mutableState.value = updated
    }

    private fun readState(prefs: SharedPreferences): QuranProgressState {
        val lastSurah = prefs.getInt(LAST_SURAH, 0)
        val lastAyah = prefs.getInt(LAST_AYAH, 0)
        return QuranProgressState(
            lastRead = if (lastSurah > 0 && lastAyah > 0) {
                QuranReadingPosition(lastSurah, lastAyah, prefs.getLong(LAST_UPDATED, 0L))
            } else {
                null
            },
            bookmarks = prefs.getStringSet(BOOKMARKS, emptySet()).orEmpty().toSet(),
            completedSurahs = prefs.getStringSet(COMPLETED_SURAHS, emptySet()).orEmpty()
                .mapNotNull { it.toIntOrNull() }
                .filter { it in 1..QuranAssetLoader.TOTAL_SURAHS }
                .toSet(),
            khatamStartDay = prefs.getLong(KHATAM_START_DAY, -1L).takeIf { it >= 0L },
            khatamTargetDays = prefs.getInt(KHATAM_TARGET_DAYS, 30).coerceIn(1, 3650),
            memorisation = prefs.getStringSet(MEMORISATION, emptySet()).orEmpty().mapNotNull { entry ->
                val statusName = entry.substringAfterLast('|', "")
                val reference = entry.substringBeforeLast('|', "")
                val status = QuranMemorisationStatus.entries.firstOrNull { it.name == statusName }
                if (reference.isBlank() || status == null) null else reference to status
            }.toMap(),
            arabicFontScale = prefs.getFloat(ARABIC_SCALE, 1f).coerceIn(0.8f, 1.5f),
            translationFontScale = prefs.getFloat(TRANSLATION_SCALE, 1f).coerceIn(0.8f, 1.5f),
            keepScreenAwake = prefs.getBoolean(KEEP_AWAKE, false),
            hifzMode = prefs.getBoolean(HIFZ_MODE, false)
        )
    }

    private fun persist(prefs: SharedPreferences, state: QuranProgressState) {
        val lastRead = state.lastRead
        prefs.edit()
            .putInt(LAST_SURAH, lastRead?.surahNumber ?: 0)
            .putInt(LAST_AYAH, lastRead?.ayahNumber ?: 0)
            .putLong(LAST_UPDATED, lastRead?.updatedAt ?: 0L)
            .putStringSet(BOOKMARKS, state.bookmarks)
            .putStringSet(COMPLETED_SURAHS, state.completedSurahs.map { it.toString() }.toSet())
            .putLong(KHATAM_START_DAY, state.khatamStartDay ?: -1L)
            .putInt(KHATAM_TARGET_DAYS, state.khatamTargetDays)
            .putStringSet(
                MEMORISATION,
                state.memorisation.map { (reference, status) -> "$reference|${status.name}" }.toSet()
            )
            .putFloat(ARABIC_SCALE, state.arabicFontScale)
            .putFloat(TRANSLATION_SCALE, state.translationFontScale)
            .putBoolean(KEEP_AWAKE, state.keepScreenAwake)
            .putBoolean(HIFZ_MODE, state.hifzMode)
            .apply()
    }
}
