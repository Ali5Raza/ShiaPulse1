package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class QuranHifzReviewOutcome {
    AGAIN,
    HARD,
    GOOD,
    EASY
}

data class QuranHifzRecord(
    val reference: String,
    val masteryLevel: Int = 0,
    val dueDay: Long,
    val intervalDays: Int = 0,
    val easeFactor: Float = 2.3f,
    val reviewCount: Int = 0,
    val repeatCount: Int = 0,
    val mistakeCount: Int = 0,
    val lastReviewDay: Long = -1L
)

data class QuranHifzState(
    val isLoaded: Boolean = false,
    val records: Map<String, QuranHifzRecord> = emptyMap(),
    val dailyTarget: Int = 5,
    val dailyReviews: Map<Long, Int> = emptyMap()
) {
    val reviewsToday: Int
        get() = dailyReviews[QuranProgressStore.currentLocalEpochDay()] ?: 0

    val dueRecords: List<QuranHifzRecord>
        get() {
            val today = QuranProgressStore.currentLocalEpochDay()
            return records.values.filter { it.dueDay <= today }.sortedWith(
                compareBy<QuranHifzRecord> { it.dueDay }
                    .thenBy { it.reference.substringBefore(':').toIntOrNull() ?: Int.MAX_VALUE }
                    .thenBy { it.reference.substringAfter(':').toIntOrNull() ?: Int.MAX_VALUE }
            )
        }

    val totalMistakes: Int get() = records.values.sumOf { it.mistakeCount }
    val totalRepeats: Int get() = records.values.sumOf { it.repeatCount }
}

/** Private, on-device Hifz scheduling. Existing memorisation statuses remain the compatibility layer. */
object QuranHifzStore {
    private const val PREFERENCES = "quran_hifz_revision"
    private const val KEY_RECORDS = "records_v1"
    private const val KEY_DAILY_TARGET = "daily_target"
    private const val KEY_DAILY_REVIEWS = "daily_reviews_v1"

    private val _state = MutableStateFlow(QuranHifzState())
    val state: StateFlow<QuranHifzState> = _state.asStateFlow()
    private var preferences: SharedPreferences? = null

    @Synchronized
    fun initialize(context: Context) {
        if (preferences == null) {
            preferences = context.applicationContext.getSharedPreferences(
                PREFERENCES,
                Context.MODE_PRIVATE
            )
        }
        if (_state.value.isLoaded) return
        val prefs = preferences ?: return
        _state.value = QuranHifzState(
            isLoaded = true,
            records = readRecords(prefs),
            dailyTarget = prefs.getInt(KEY_DAILY_TARGET, 5).coerceIn(1, 50),
            dailyReviews = readDailyReviews(prefs)
        )
    }

    fun migrateLegacyStatuses(
        context: Context,
        statuses: Map<String, QuranMemorisationStatus>
    ) {
        if (!_state.value.isLoaded) initialize(context)
        val today = QuranProgressStore.currentLocalEpochDay()
        update(context) { current ->
            val additions = statuses.mapNotNull { (reference, status) ->
                if (!isValidReference(reference) || reference in current.records) {
                    null
                } else {
                    reference to legacyRecord(reference, status, today)
                }
            }.toMap()
            if (additions.isEmpty()) current else current.copy(records = current.records + additions)
        }
    }

    fun setMasteryFromLegacy(
        context: Context,
        surahNumber: Int,
        ayahNumber: Int,
        status: QuranMemorisationStatus?
    ) {
        val reference = QuranProgressStore.quranReferenceKey(surahNumber, ayahNumber)
        val today = QuranProgressStore.currentLocalEpochDay()
        update(context) { current ->
            if (status == null) {
                current.copy(records = current.records - reference)
            } else {
                val existing = current.records[reference]
                val updated = existing?.copy(masteryLevel = levelForLegacyStatus(status))
                    ?: legacyRecord(reference, status, today)
                current.copy(records = current.records + (reference to updated))
            }
        }
    }

    fun setDailyTarget(context: Context, target: Int) {
        update(context) { it.copy(dailyTarget = target.coerceIn(1, 50)) }
    }

    fun recordRepeat(context: Context, reference: String) {
        if (!isValidReference(reference)) return
        val today = QuranProgressStore.currentLocalEpochDay()
        update(context) { current ->
            val existing = current.records[reference] ?: QuranHifzRecord(reference, dueDay = today)
            current.copy(
                records = current.records + (
                    reference to existing.copy(repeatCount = existing.repeatCount + 1)
                )
            )
        }
    }

    /** Applies one spaced-repetition review and returns the compatible legacy status. */
    fun recordReview(
        context: Context,
        reference: String,
        outcome: QuranHifzReviewOutcome
    ): QuranMemorisationStatus {
        if (!isValidReference(reference)) return QuranMemorisationStatus.NEEDS_PRACTICE
        val today = QuranProgressStore.currentLocalEpochDay()
        var resultingStatus = QuranMemorisationStatus.NEEDS_PRACTICE
        update(context) { current ->
            val previous = current.records[reference] ?: QuranHifzRecord(reference, dueDay = today)
            val updated = nextReview(previous, outcome, today)
            resultingStatus = statusForMasteryLevel(updated.masteryLevel)
            val updatedDaily = (current.dailyReviews + (
                today to ((current.dailyReviews[today] ?: 0) + 1)
            )).filterKeys { it in (today - 120)..today }
            current.copy(
                records = current.records + (reference to updated),
                dailyReviews = updatedDaily
            )
        }
        return resultingStatus
    }

    fun statusForMasteryLevel(level: Int): QuranMemorisationStatus = when (level.coerceIn(0, 5)) {
        in 0..1 -> QuranMemorisationStatus.NEEDS_PRACTICE
        in 2..3 -> QuranMemorisationStatus.ALMOST_MEMORISED
        else -> QuranMemorisationStatus.MEMORISED
    }

    private fun nextReview(
        previous: QuranHifzRecord,
        outcome: QuranHifzReviewOutcome,
        today: Long
    ): QuranHifzRecord {
        val previousLevel = previous.masteryLevel.coerceIn(0, 5)
        val previousInterval = previous.intervalDays.coerceAtLeast(0)
        val previousEase = previous.easeFactor.coerceIn(1.3f, 3f)
        val newLevel: Int
        val newInterval: Int
        val newEase: Float
        val newMistakes: Int
        when (outcome) {
            QuranHifzReviewOutcome.AGAIN -> {
                newLevel = (previousLevel - 2).coerceAtLeast(0)
                newInterval = 0
                newEase = (previousEase - 0.2f).coerceAtLeast(1.3f)
                newMistakes = previous.mistakeCount + 1
            }
            QuranHifzReviewOutcome.HARD -> {
                newLevel = (previousLevel - 1).coerceAtLeast(0)
                newInterval = if (previousInterval == 0) {
                    1
                } else {
                    max(1, (previousInterval * 1.2f).roundToInt())
                }
                newEase = (previousEase - 0.15f).coerceAtLeast(1.3f)
                newMistakes = previous.mistakeCount
            }
            QuranHifzReviewOutcome.GOOD -> {
                newLevel = (previousLevel + 1).coerceAtMost(5)
                newInterval = scheduledInterval(newLevel, previousInterval, previousEase, 1f)
                newEase = previousEase
                newMistakes = previous.mistakeCount
            }
            QuranHifzReviewOutcome.EASY -> {
                newLevel = (previousLevel + 2).coerceAtMost(5)
                newInterval = scheduledInterval(newLevel, previousInterval, previousEase, 1.35f)
                newEase = (previousEase + 0.1f).coerceAtMost(3f)
                newMistakes = previous.mistakeCount
            }
        }
        return previous.copy(
            masteryLevel = newLevel,
            dueDay = today + newInterval.toLong(),
            intervalDays = newInterval,
            easeFactor = newEase,
            reviewCount = previous.reviewCount + 1,
            mistakeCount = newMistakes,
            lastReviewDay = today
        )
    }

    private fun scheduledInterval(
        newLevel: Int,
        previousInterval: Int,
        easeFactor: Float,
        outcomeMultiplier: Float
    ): Int {
        val base = when (newLevel.coerceIn(0, 5)) {
            0 -> 0
            1 -> 1
            2 -> 3
            3 -> 7
            4 -> 14
            else -> 30
        }
        if (previousInterval <= 0) return base
        return max(base, (previousInterval * easeFactor * outcomeMultiplier).roundToInt())
    }

    private fun legacyRecord(
        reference: String,
        status: QuranMemorisationStatus,
        today: Long
    ) = QuranHifzRecord(
        reference = reference,
        masteryLevel = levelForLegacyStatus(status),
        dueDay = today,
        intervalDays = when (status) {
            QuranMemorisationStatus.NEEDS_PRACTICE -> 0
            QuranMemorisationStatus.ALMOST_MEMORISED -> 3
            QuranMemorisationStatus.MEMORISED -> 14
        }
    )

    private fun levelForLegacyStatus(status: QuranMemorisationStatus): Int = when (status) {
        QuranMemorisationStatus.NEEDS_PRACTICE -> 1
        QuranMemorisationStatus.ALMOST_MEMORISED -> 3
        QuranMemorisationStatus.MEMORISED -> 5
    }

    private fun update(context: Context, transform: (QuranHifzState) -> QuranHifzState) {
        synchronized(this) {
            if (!_state.value.isLoaded) initialize(context)
            val current = _state.value
            val updated = transform(current)
            if (updated == current) return
            preferences?.edit()
                ?.putInt(KEY_DAILY_TARGET, updated.dailyTarget)
                ?.putStringSet(KEY_RECORDS, updated.records.values.mapTo(mutableSetOf()) { encodeRecord(it) })
                ?.putStringSet(KEY_DAILY_REVIEWS, encodeDailyReviews(updated.dailyReviews))
                ?.apply()
            _state.value = updated
        }
    }

    private fun readRecords(prefs: SharedPreferences): Map<String, QuranHifzRecord> =
        prefs.getStringSet(KEY_RECORDS, emptySet()).orEmpty().mapNotNull { encoded ->
            val parts = encoded.split('|')
            if (parts.size != 9 || !isValidReference(parts[0])) return@mapNotNull null
            val record = QuranHifzRecord(
                reference = parts[0],
                masteryLevel = parts[1].toIntOrNull()?.coerceIn(0, 5) ?: return@mapNotNull null,
                dueDay = parts[2].toLongOrNull() ?: return@mapNotNull null,
                intervalDays = parts[3].toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null,
                easeFactor = parts[4].toFloatOrNull()?.coerceIn(1.3f, 3f) ?: return@mapNotNull null,
                reviewCount = parts[5].toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null,
                repeatCount = parts[6].toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null,
                mistakeCount = parts[7].toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null,
                lastReviewDay = parts[8].toLongOrNull() ?: return@mapNotNull null
            )
            record.reference to record
        }.toMap()

    private fun encodeRecord(record: QuranHifzRecord): String = listOf(
        record.reference,
        record.masteryLevel.toString(),
        record.dueDay.toString(),
        record.intervalDays.toString(),
        String.format(Locale.US, "%.2f", record.easeFactor),
        record.reviewCount.toString(),
        record.repeatCount.toString(),
        record.mistakeCount.toString(),
        record.lastReviewDay.toString()
    ).joinToString("|")

    private fun readDailyReviews(prefs: SharedPreferences): Map<Long, Int> =
        prefs.getStringSet(KEY_DAILY_REVIEWS, emptySet()).orEmpty().mapNotNull { encoded ->
            val parts = encoded.split('=')
            val day = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val count = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
            day to count
        }.toMap()

    private fun encodeDailyReviews(reviews: Map<Long, Int>): Set<String> =
        reviews.mapTo(mutableSetOf()) { (day, count) -> "$day=$count" }

    private fun isValidReference(reference: String): Boolean {
        val surah = reference.substringBefore(':').toIntOrNull() ?: return false
        val ayah = reference.substringAfter(':').toIntOrNull() ?: return false
        return ':' in reference && surah in 1..QuranAssetLoader.TOTAL_SURAHS && ayah > 0
    }
}
