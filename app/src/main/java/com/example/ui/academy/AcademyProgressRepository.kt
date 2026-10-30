package com.example.ui.academy

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Immutable snapshot consumed by every Academy surface, including Parent Dashboard and Tree. */
data class AcademyProgress(
    val points: Int = 0,
    val lessonSteps: Map<String, Int> = emptyMap(),
    val completedLessons: Set<String> = emptySet(),
    val readDuas: Set<Int> = emptySet(),
    val favoriteDuas: Set<Int> = emptySet(),
    val completedGames: Set<String> = emptySet(),
    val bestQuizScore: Int = 0,
    val bestQuizTotal: Int = 0,
    val quizAttempts: Int = 0,
    val activityDates: Set<String> = emptySet(),
    val kindnessDates: Set<String> = emptySet(),
    val lastActivityDate: String? = null,
    val currentStreak: Int = 0,
    val daily: AcademyDailyMissions = AcademyDailyMissions()
)

data class AcademyDailyMissions(
    val dua: Boolean = false,
    val quiz: Boolean = false,
    val learning: Boolean = false,
    val kindness: Boolean = false
)

/**
 * On-device persistence. No child profile, activity or PIN is sent to Firebase or another service.
 * Reward methods are idempotent where appropriate, so revisiting a lesson cannot farm points.
 */
class AcademyProgressRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutableProgress = MutableStateFlow(readProgress())
    val progress: StateFlow<AcademyProgress> = mutableProgress.asStateFlow()

    @Synchronized
    fun recordLessonStep(lessonId: String, stepReached: Int) {
        if (lessonId.isBlank() || stepReached < 0) return
        val key = "step_$lessonId"
        if (prefs.getInt(key, 0) >= stepReached) return
        prefs.edit().putInt(key, stepReached).apply()
        refresh()
    }

    @Synchronized
    fun completeLesson(lessonId: String, totalSteps: Int, rewardPoints: Int): Int {
        val current = readProgress()
        val isNew = lessonId !in current.completedLessons
        val awarded = if (isNew) rewardPoints.coerceAtLeast(0) else 0
        val completed = current.completedLessons + lessonId
        val today = todayKey()
        val editor = prefs.edit()
            .putStringSet("completed_lessons", completed)
            .putInt("step_$lessonId", totalSteps.coerceAtLeast(0))
            .putInt("points", current.points + awarded)
            .putString("daily_date", today)
            .putBoolean("daily_learning", true)
        recordActivity(editor, current, today)
        editor.apply()
        refresh()
        return awarded
    }

    @Synchronized
    fun markDuaRead(duaId: Int): Int {
        val current = readProgress()
        val isNew = duaId !in current.readDuas
        val awarded = if (isNew) DUA_REWARD else 0
        val today = todayKey()
        val editor = prefs.edit()
            .putStringSet("read_duas", (current.readDuas + duaId).asStringSet())
            .putInt("points", current.points + awarded)
            .putString("daily_date", today)
            .putBoolean("daily_dua", true)
        recordActivity(editor, current, today)
        editor.apply()
        refresh()
        return awarded
    }

    @Synchronized
    fun toggleDuaFavorite(duaId: Int): Boolean {
        val current = readProgress()
        val nowFavorite = duaId !in current.favoriteDuas
        val updated = if (nowFavorite) current.favoriteDuas + duaId else current.favoriteDuas - duaId
        prefs.edit().putStringSet("favorite_duas", updated.asStringSet()).apply()
        refresh()
        return nowFavorite
    }

    /** Rewards only an improvement over the previous best percentage; every attempt is still reported. */
    @Synchronized
    fun recordQuiz(score: Int, total: Int): Int {
        if (total <= 0) return 0
        val safeScore = score.coerceIn(0, total)
        val current = readProgress()
        val previousBestPercentage = if (current.bestQuizTotal == 0) {
            0
        } else {
            current.bestQuizScore * 100 / current.bestQuizTotal
        }
        val attemptPercentage = safeScore * 100 / total
        val awarded = (attemptPercentage - previousBestPercentage).coerceAtLeast(0)
        val improvesBest = current.bestQuizTotal == 0 ||
            safeScore.toFloat() / total > current.bestQuizScore.toFloat() / current.bestQuizTotal
        val today = todayKey()
        val editor = prefs.edit()
            .putInt("quiz_attempts", current.quizAttempts + 1)
            .putInt("best_quiz_score", if (improvesBest) safeScore else current.bestQuizScore)
            .putInt("best_quiz_total", if (improvesBest) total else current.bestQuizTotal)
            .putInt("points", current.points + awarded)
            .putString("daily_date", today)
            .putBoolean("daily_quiz", true)
        recordActivity(editor, current, today)
        editor.apply()
        refresh()
        return awarded
    }

    @Synchronized
    fun recordGame(gameId: String, rewardPoints: Int = GAME_REWARD): Int {
        val current = readProgress()
        val isNew = gameId !in current.completedGames
        val awarded = if (isNew) rewardPoints.coerceAtLeast(0) else 0
        val today = todayKey()
        val editor = prefs.edit()
            .putStringSet("completed_games", current.completedGames + gameId)
            .putInt("points", current.points + awarded)
            .putString("daily_date", today)
            .putBoolean("daily_learning", true)
        recordActivity(editor, current, today)
        editor.apply()
        refresh()
        return awarded
    }

    @Synchronized
    fun markKindnessToday(): Boolean {
        val current = readProgress()
        val today = todayKey()
        if (today in current.kindnessDates) return false
        val editor = prefs.edit()
            .putStringSet("kindness_dates", current.kindnessDates + today)
            .putInt("points", current.points + KINDNESS_REWARD)
            .putString("daily_date", today)
            .putBoolean("daily_kindness", true)
        recordActivity(editor, current, today)
        editor.apply()
        refresh()
        return true
    }

    fun hasParentPin(): Boolean = !prefs.getString("parent_pin_hash", null).isNullOrBlank()

    @Synchronized
    fun setParentPin(pin: String): Boolean {
        if (!pin.matches(Regex("[0-9]{4,6}"))) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
        prefs.edit()
            .putString("parent_pin_salt", salt)
            .putString("parent_pin_hash", hashPin(salt, pin))
            .apply()
        return true
    }

    fun verifyParentPin(pin: String): Boolean {
        val salt = prefs.getString("parent_pin_salt", null) ?: return false
        val expected = prefs.getString("parent_pin_hash", null) ?: return false
        val actual = hashPin(salt, pin)
        return MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), actual.toByteArray(Charsets.UTF_8))
    }

    @Synchronized
    fun resetLearningProgress() {
        val salt = prefs.getString("parent_pin_salt", null)
        val hash = prefs.getString("parent_pin_hash", null)
        prefs.edit().clear().apply()
        val editor = prefs.edit()
        if (!salt.isNullOrBlank() && !hash.isNullOrBlank()) {
            editor.putString("parent_pin_salt", salt)
            editor.putString("parent_pin_hash", hash)
        }
        editor.apply()
        refresh()
    }

    private fun recordActivity(editor: SharedPreferences.Editor, current: AcademyProgress, today: String) {
        val yesterday = dateKeyOffset(-1)
        val newStreak = when (current.lastActivityDate) {
            today -> current.currentStreak.coerceAtLeast(1)
            yesterday -> current.currentStreak + 1
            else -> 1
        }
        editor
            .putStringSet("activity_dates", (current.activityDates + today).takeLastSorted(400))
            .putString("last_activity_date", today)
            .putInt("current_streak", newStreak)
    }

    private fun readProgress(): AcademyProgress {
        val today = todayKey()
        val dailyIsToday = prefs.getString("daily_date", null) == today
        val lastActivity = prefs.getString("last_activity_date", null)
        val storedStreak = prefs.getInt("current_streak", 0)
        val visibleStreak = if (lastActivity == today || lastActivity == dateKeyOffset(-1)) storedStreak else 0
        val steps = prefs.all.mapNotNull { (key, value) ->
            if (key.startsWith("step_") && value is Int) key.removePrefix("step_") to value else null
        }.toMap()
        return AcademyProgress(
            points = prefs.getInt("points", 0),
            lessonSteps = steps,
            completedLessons = prefs.getStringSet("completed_lessons", emptySet()).orEmpty().toSet(),
            readDuas = prefs.getStringSet("read_duas", emptySet()).asIntSet(),
            favoriteDuas = prefs.getStringSet("favorite_duas", emptySet()).asIntSet(),
            completedGames = prefs.getStringSet("completed_games", emptySet()).orEmpty().toSet(),
            bestQuizScore = prefs.getInt("best_quiz_score", 0),
            bestQuizTotal = prefs.getInt("best_quiz_total", 0),
            quizAttempts = prefs.getInt("quiz_attempts", 0),
            activityDates = prefs.getStringSet("activity_dates", emptySet()).orEmpty().toSet(),
            kindnessDates = prefs.getStringSet("kindness_dates", emptySet()).orEmpty().toSet(),
            lastActivityDate = lastActivity,
            currentStreak = visibleStreak,
            daily = AcademyDailyMissions(
                dua = dailyIsToday && prefs.getBoolean("daily_dua", false),
                quiz = dailyIsToday && prefs.getBoolean("daily_quiz", false),
                learning = dailyIsToday && prefs.getBoolean("daily_learning", false),
                kindness = dailyIsToday && prefs.getBoolean("daily_kindness", false)
            )
        )
    }

    @Synchronized
    fun refreshProgress() {
        refresh()
    }

    private fun refresh() {
        mutableProgress.value = readProgress()
    }

    private fun Set<Int>.asStringSet(): Set<String> = mapTo(linkedSetOf()) { it.toString() }

    private fun Set<String>?.asIntSet(): Set<Int> =
        this.orEmpty().mapNotNullTo(linkedSetOf()) { it.toIntOrNull() }

    private fun Set<String>.takeLastSorted(limit: Int): Set<String> =
        sorted().takeLast(limit).toSet()

    private fun todayKey(): String = dateFormat().format(Date())

    private fun dateKeyOffset(offsetDays: Int): String = Calendar.getInstance().run {
        add(Calendar.DAY_OF_YEAR, offsetDays)
        dateFormat().format(time)
    }

    private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun hashPin(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest("$salt:$pin".toByteArray(Charsets.UTF_8)).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val PREFS_NAME = "shia_academy_progress_v2"
        const val DUA_REWARD = 10
        const val GAME_REWARD = 25
        const val KINDNESS_REWARD = 5
        const val WUDU_REWARD = 50
        const val SALAH_REWARD = 100
        const val AQAID_REWARD = 50
        const val STORY_REWARD = 75
    }
}
