package com.example.receiver

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.MainActivity

/** Stable notification/widget identities. Extras are payloads, never identity discriminators. */
object AppPendingIntents {
    const val ACTION_OPEN_ADHAN_TRACKER = "com.example.action.OPEN_ADHAN_TRACKER"
    const val ACTION_OPEN_AAMAL_TRACKER = "com.example.action.OPEN_AAMAL_TRACKER"
    const val ACTION_OPEN_HIJRI_HOME = "com.example.action.OPEN_HIJRI_HOME"
    const val ACTION_OPEN_CALENDAR = "com.example.action.OPEN_CALENDAR"
    const val ACTION_OPEN_MASOOM_CALENDAR = "com.example.action.OPEN_MASOOM_CALENDAR"
    const val ACTION_OPEN_QURAN_SHARED_KHATAM = "com.example.action.OPEN_QURAN_SHARED_KHATAM"

    private const val AAMAL_OPEN_REQUEST_CODE = 3004

    private fun activity(
        context: Context,
        requestCode: Int,
        action: String,
        configure: Intent.() -> Unit
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            configure()
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun adhanContent(context: Context, prayerName: String): PendingIntent =
        activity(context, prayerName.hashCode(), ACTION_OPEN_ADHAN_TRACKER) {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_screen", "tracker")
        }

    fun adhanStop(context: Context, prayerName: String): PendingIntent {
        val intent = Intent(context, AdhanReceiver::class.java).apply {
            action = AdhanReceiver.stopAction(prayerName)
            putExtra("PRAYER_NAME", prayerName)
        }
        return PendingIntent.getBroadcast(
            context,
            prayerName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun aamalContent(context: Context): PendingIntent =
        activity(context, AAMAL_OPEN_REQUEST_CODE, ACTION_OPEN_AAMAL_TRACKER) {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_screen", "aamal_tracker")
        }

    fun hijriWidgetContent(context: Context): PendingIntent =
        activity(context, 0, ACTION_OPEN_HIJRI_HOME) { }

    fun calendarContent(context: Context): PendingIntent =
        activity(context, CalendarNotificationManager.NOTIFICATION_ID, ACTION_OPEN_CALENDAR) {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_screen", "calendar")
        }

    fun masoomEventContent(context: Context): PendingIntent =
        activity(context, "events".hashCode(), ACTION_OPEN_MASOOM_CALENDAR) {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_screen", "calendar")
        }

    fun quranSharedKhatamContent(context: Context, code: String): PendingIntent =
        activity(context, code.hashCode(), "$ACTION_OPEN_QURAN_SHARED_KHATAM.$code") {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_screen", "quran_shared_khatam")
            putExtra(QuranSharedKhatamReminderScheduler.EXTRA_CODE, code)
        }
}
