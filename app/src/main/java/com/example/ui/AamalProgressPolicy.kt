package com.example.ui

import com.example.data.AamalCompletion
import com.example.data.AamalCustomActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * One completion rule for the tracker, Home progress, and streaks.
 * Room rows remain append/replace records; this policy only interprets them.
 */
object AamalProgressPolicy {
    private val isoDate = Regex("\\d{4}-\\d{2}-\\d{2}")

    private fun dateKey(calendar: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = calendar.timeZone
        }.format(calendar.time)

    private fun isVisible(
        category: AamalCategory,
        visibleCategories: Map<AamalCategory, Boolean>
    ): Boolean = visibleCategories[category] != false

    private fun wasAddedBy(date: String, activity: AamalCustomActivity): Boolean {
        val added = activity.dateAdded.takeIf { isoDate.matches(it) } ?: return true
        return date >= added
    }

    /**
     * Current requirements use enabled visible activities. For historical dates,
     * dateAdded prevents new customs from becoming retroactive requirements;
     * disabled/deleted customs are retained only when that date has evidence.
     */
    fun requiredActivityIds(
        date: String,
        isToday: Boolean,
        defaultList: List<AamalDef>,
        customAamal: List<AamalCustomActivity>,
        visibleCategories: Map<AamalCategory, Boolean>,
        allCompletions: List<AamalCompletion>
    ): Set<String> {
        val required = defaultList
            .filter { isVisible(it.category, visibleCategories) }
            .mapTo(linkedSetOf()) { it.id }
        if (!isVisible(AamalCategory.CUSTOM, visibleCategories)) return required

        val evidenceIds = allCompletions
            .asSequence()
            .filter { it.dateString == date }
            .map { it.activityId }
            .toSet()
        val knownCustomIds = customAamal.mapTo(hashSetOf()) { it.id }

        customAamal.forEach { activity ->
            if (!wasAddedBy(date, activity)) return@forEach
            val applicable = if (isToday) {
                activity.isEnabled
            } else {
                activity.isEnabled || activity.id in evidenceIds
            }
            if (applicable) required += activity.id
        }

        if (!isToday) {
            // Deletion does not delete completion rows. Keep those historical
            // requirements visible to the streak calculation when evidenced.
            evidenceIds
                .filter { it.startsWith("custom_") && it !in knownCustomIds }
                .forEach { required += it }
        }
        return required
    }

    fun isPerfectDay(
        date: String,
        isToday: Boolean,
        defaultList: List<AamalDef>,
        customAamal: List<AamalCustomActivity>,
        visibleCategories: Map<AamalCategory, Boolean>,
        allCompletions: List<AamalCompletion>
    ): Boolean {
        val required = requiredActivityIds(
            date,
            isToday,
            defaultList,
            customAamal,
            visibleCategories,
            allCompletions
        )
        if (required.isEmpty()) return false
        val completed = allCompletions
            .asSequence()
            .filter { it.dateString == date && it.isCompleted }
            .map { it.activityId }
            .toSet()
        return completed.containsAll(required)
    }

    /** Counts each consecutive civil date at most once, including today. */
    fun calculateStreak(
        allCompletions: List<AamalCompletion>,
        customAamal: List<AamalCustomActivity>,
        defaultList: List<AamalDef>,
        visibleCategories: Map<AamalCategory, Boolean> = AamalCategory.values().associateWith { true },
        today: Calendar = Calendar.getInstance()
    ): Int {
        if (allCompletions.isEmpty()) return 0
        val todayKey = dateKey(today)
        val cursor = today.clone() as Calendar
        if (!isPerfectDay(
                todayKey,
                isToday = true,
                defaultList = defaultList,
                customAamal = customAamal,
                visibleCategories = visibleCategories,
                allCompletions = allCompletions
            )
        ) {
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }

        var streak = 0
        // A finite evidence bound also protects malformed/imported databases.
        val maxIterations = allCompletions.map { it.dateString }.distinct().size + 1
        repeat(maxIterations) {
            val key = dateKey(cursor)
            val perfect = isPerfectDay(
                date = key,
                isToday = key == todayKey,
                defaultList = defaultList,
                customAamal = customAamal,
                visibleCategories = visibleCategories,
                allCompletions = allCompletions
            )
            if (!perfect) return streak
            streak++
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }
}
