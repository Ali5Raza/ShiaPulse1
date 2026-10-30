package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QuranDailyReading(
    val day: Long,
    val ayahReferences: Set<String> = emptySet(),
    val activeSeconds: Long = 0L
)

data class QuranReadingSession(
    val startedAt: Long,
    val endedAt: Long,
    val activeSeconds: Long,
    val ayahCount: Int
)

data class QuranReadingAnalyticsState(
    val isLoaded: Boolean = false,
    val daily: Map<Long, QuranDailyReading> = emptyMap(),
    val sessions: List<QuranReadingSession> = emptyList(),
    val totalSessions: Int = 0
) {
    val totalActiveSeconds: Long get() = daily.values.sumOf { it.activeSeconds }
    val totalUniqueAyahs: Int get() = daily.values.flatMap { it.ayahReferences }.toSet().size
}

/** Private on-device reading history. It never uploads or ranks worship activity. */
object QuranReadingAnalyticsStore {
    private const val PREFERENCES = "quran_reading_analytics"
    private const val AYAH_PREFIX = "ayahs_"
    private const val SECONDS_PREFIX = "seconds_"
    private const val SESSIONS = "sessions_v1"
    private const val TOTAL_SESSIONS = "total_sessions"
    private const val MAX_RECENT_SESSIONS = 100

    private val _state = MutableStateFlow(QuranReadingAnalyticsState())
    val state: StateFlow<QuranReadingAnalyticsState> = _state.asStateFlow()
    private var preferences: SharedPreferences? = null
    private var activeSession: ActiveSession? = null

    private data class ActiveSession(
        val startedAt: Long,
        val references: MutableSet<String> = mutableSetOf(),
        var activeSeconds: Long = 0L
    )

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
        val days = prefs.all.keys.mapNotNull { key ->
            when {
                key.startsWith(AYAH_PREFIX) -> key.removePrefix(AYAH_PREFIX).toLongOrNull()
                key.startsWith(SECONDS_PREFIX) -> key.removePrefix(SECONDS_PREFIX).toLongOrNull()
                else -> null
            }
        }.toSet()
        val daily = days.mapNotNull { day ->
            val references = prefs.getStringSet(AYAH_PREFIX + day, emptySet()).orEmpty()
                .filter(::isValidReference).toSet()
            val seconds = prefs.getLong(SECONDS_PREFIX + day, 0L).coerceAtLeast(0L)
            if (references.isEmpty() && seconds == 0L) {
                null
            } else {
                day to QuranDailyReading(day, references, seconds)
            }
        }.toMap()
        val sessions = readSessions(prefs)
        _state.value = QuranReadingAnalyticsState(
            isLoaded = true,
            daily = daily,
            sessions = sessions,
            totalSessions = prefs.getInt(TOTAL_SESSIONS, sessions.size).coerceAtLeast(sessions.size)
        )
    }

    @Synchronized
    fun startSession(context: Context) {
        initialize(context)
        if (activeSession != null) return
        activeSession = ActiveSession(System.currentTimeMillis())
        val updatedTotal = _state.value.totalSessions + 1
        preferences?.edit()?.putInt(TOTAL_SESSIONS, updatedTotal)?.apply()
        _state.value = _state.value.copy(totalSessions = updatedTotal)
    }

    @Synchronized
    fun endSession(context: Context) {
        initialize(context)
        val finished = activeSession ?: return
        activeSession = null
        if (finished.references.isEmpty() && finished.activeSeconds == 0L) return
        val session = QuranReadingSession(
            startedAt = finished.startedAt,
            endedAt = System.currentTimeMillis(),
            activeSeconds = finished.activeSeconds,
            ayahCount = finished.references.size
        )
        val updated = (listOf(session) + _state.value.sessions)
            .sortedByDescending { it.startedAt }
            .take(MAX_RECENT_SESSIONS)
        preferences?.edit()?.putStringSet(
            SESSIONS,
            updated.mapTo(mutableSetOf()) { encodeSession(it) }
        )?.apply()
        _state.value = _state.value.copy(sessions = updated)
    }

    fun recordAyah(context: Context, surahNumber: Int, ayahNumber: Int) {
        recordAyahs(context, listOf(QuranProgressStore.quranReferenceKey(surahNumber, ayahNumber)))
    }

    fun recordAyahs(context: Context, references: Iterable<String>) {
        val validReferences = references.filter(::isValidReference).toSet()
        if (validReferences.isEmpty()) return
        synchronized(this) {
            initialize(context)
            val prefs = preferences ?: return
            val day = QuranProgressStore.currentLocalEpochDay()
            activeSession?.references?.addAll(validReferences)
            val key = AYAH_PREFIX + day
            val previous = prefs.getStringSet(key, emptySet()).orEmpty()
            val updatedReferences = previous + validReferences
            if (updatedReferences.size == previous.size) return
            prefs.edit().putStringSet(key, updatedReferences).apply()
            val current = _state.value.daily[day] ?: QuranDailyReading(day)
            _state.value = _state.value.copy(
                daily = _state.value.daily + (
                    day to current.copy(ayahReferences = updatedReferences)
                )
            )
        }
    }

    fun recordActiveTime(context: Context, seconds: Long) {
        if (seconds <= 0L) return
        synchronized(this) {
            initialize(context)
            val session = activeSession ?: return
            val safeSeconds = seconds.coerceAtMost(60L)
            val prefs = preferences ?: return
            val day = QuranProgressStore.currentLocalEpochDay()
            session.activeSeconds += safeSeconds
            val key = SECONDS_PREFIX + day
            val updatedSeconds = prefs.getLong(key, 0L).coerceAtLeast(0L) + safeSeconds
            prefs.edit().putLong(key, updatedSeconds).apply()
            val current = _state.value.daily[day] ?: QuranDailyReading(day)
            _state.value = _state.value.copy(
                daily = _state.value.daily + (
                    day to current.copy(activeSeconds = updatedSeconds)
                )
            )
        }
    }

    private fun readSessions(prefs: SharedPreferences): List<QuranReadingSession> =
        prefs.getStringSet(SESSIONS, emptySet()).orEmpty().mapNotNull { encoded ->
            val parts = encoded.split('|')
            if (parts.size != 4) return@mapNotNull null
            val startedAt = parts[0].toLongOrNull() ?: return@mapNotNull null
            val endedAt = parts[1].toLongOrNull() ?: return@mapNotNull null
            val seconds = parts[2].toLongOrNull()?.coerceAtLeast(0L) ?: return@mapNotNull null
            val ayahCount = parts[3].toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null
            if (endedAt < startedAt) return@mapNotNull null
            QuranReadingSession(startedAt, endedAt, seconds, ayahCount)
        }.sortedByDescending { it.startedAt }.take(MAX_RECENT_SESSIONS)

    private fun encodeSession(session: QuranReadingSession): String = listOf(
        session.startedAt,
        session.endedAt,
        session.activeSeconds,
        session.ayahCount
    ).joinToString("|")

    private fun isValidReference(reference: String): Boolean {
        val surah = reference.substringBefore(':').toIntOrNull() ?: return false
        val ayah = reference.substringAfter(':').toIntOrNull() ?: return false
        return ':' in reference && surah in 1..QuranAssetLoader.TOTAL_SURAHS && ayah > 0
    }
}

internal fun quranReadingStreaks(readingDays: Set<Long>, today: Long): Pair<Int, Int> {
    if (readingDays.isEmpty()) return 0 to 0
    var current = 0
    var cursor = when {
        today in readingDays -> today
        today - 1 in readingDays -> today - 1
        else -> null
    }
    while (cursor != null && cursor in readingDays) {
        current++
        cursor--
    }
    var longest = 0
    var running = 0
    var previous: Long? = null
    readingDays.sorted().forEach { day ->
        running = if (previous != null && day == previous + 1) running + 1 else 1
        longest = maxOf(longest, running)
        previous = day
    }
    return current to longest
}
