package com.example.utils

import android.util.Log

/**
 * A lightweight analytics manager to track template and sticker usage.
 * To integrate with a real backend (like Firebase Analytics or Mixpanel),
 * you can add the respective SDK calls inside these methods.
 */
object AnalyticsManager {
    private const val TAG = "AnalyticsManager"

    fun logTemplateUsed(templateName: String) {
        // Example: FirebaseAnalytics.getInstance(context).logEvent("template_used", bundle)
        Log.i(TAG, "Event Logged: Template Used -> $templateName")
    }

    fun logStickerUsed(stickerName: String) {
        Log.i(TAG, "Event Logged: Sticker Used -> $stickerName")
    }

    fun logFeatureUsed(featureName: String) {
        Log.i(TAG, "Event Logged: Feature Used -> $featureName")
    }
}
