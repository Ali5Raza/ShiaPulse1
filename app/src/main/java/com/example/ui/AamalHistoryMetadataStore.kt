package com.example.ui

import android.content.Context
import com.example.data.AamalCustomActivity

/** Preferences tombstones avoid a Room schema change and never alter completion rows. */
object AamalHistoryMetadataStore {
    private const val PREFS_NAME = "aamal_history_metadata"
    private const val TITLE = "title:"
    private const val CATEGORY = "category:"
    private const val DATE_ADDED = "date_added:"
    private const val CAPTURED = "captured:"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun captureBeforeDeletion(context: Context, activity: AamalCustomActivity) {
        prefs(context).edit()
            .putString(TITLE + activity.id, activity.title)
            .putString(CATEGORY + activity.id, activity.category)
            .putString(DATE_ADDED + activity.id, activity.dateAdded)
            .putBoolean(CAPTURED + activity.id, true)
            .apply()
    }

    fun read(context: Context, id: String): AamalCustomActivity? {
        val preferences = prefs(context)
        if (!preferences.getBoolean(CAPTURED + id, false)) return null
        return AamalCustomActivity(
            id = id,
            title = preferences.getString(TITLE + id, null).orEmpty(),
            category = preferences.getString(CATEGORY + id, null) ?: "Custom",
            isEnabled = false,
            dateAdded = preferences.getString(DATE_ADDED + id, null).orEmpty()
        )
    }
}
