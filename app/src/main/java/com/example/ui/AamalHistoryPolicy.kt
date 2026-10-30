package com.example.ui

import com.example.data.AamalCompletion
import com.example.data.AamalCustomActivity

/** Rendering-only history merge; progress policy inputs remain unchanged. */
object AamalHistoryPolicy {
    fun mergeHistoricalActivities(
        currentActivities: List<AamalCustomActivity>,
        allCompletions: List<AamalCompletion>,
        metadata: (String) -> AamalCustomActivity?
    ): List<AamalCustomActivity> {
        val currentIds = currentActivities.mapTo(hashSetOf()) { it.id }
        val earliestEvidence = allCompletions
            .asSequence()
            .filter { it.activityId.startsWith("custom_") && it.activityId !in currentIds }
            .groupBy { it.activityId }
            .mapValues { (_, records) -> records.minOf { it.dateString } }

        return currentActivities + earliestEvidence.entries
            .sortedBy { it.key }
            .map { (id, firstEvidenceDate) ->
                val saved = metadata(id)
                AamalCustomActivity(
                    id = id,
                    title = saved?.title.orEmpty(),
                    category = saved?.category ?: "Custom",
                    isEnabled = false,
                    dateAdded = saved?.dateAdded?.takeIf { it.isNotBlank() } ?: firstEvidenceDate
                )
            }
    }

    fun displayTitle(activity: AamalCustomActivity, languageCode: String): String =
        activity.title.ifBlank {
            when (languageCode) {
                "ur" -> "حذف شدہ کسٹم عمل"
                "ar" -> "عمل مخصص محذوف"
                "fa" -> "عمل سفارشی حذف‌شده"
                "hi" -> "हटाया गया कस्टम कार्य"
                else -> "Deleted custom act"
            }
        }
}
