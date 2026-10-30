package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.utils.HijriCalendarHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CalendarNotificationManager {
    const val CHANNEL_ID = "calendar_notification_bar_channel"
    const val NOTIFICATION_ID = 2026
    const val PREF_KEY = "show_calendar_in_notification_bar"

    fun isCalendarNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("shia_tracker_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_KEY, false)
    }

    fun setCalendarNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("shia_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(PREF_KEY, enabled)
            .putBoolean("show_calendar_in_toolbar", false) // Ensure it is removed from in-app toolbar
            .apply()

        if (enabled) {
            showOrUpdateCalendarNotification(context)
        } else {
            cancelCalendarNotification(context)
        }
    }

    fun showOrUpdateCalendarNotification(context: Context) {
        if (!isCalendarNotificationEnabled(context)) {
            cancelCalendarNotification(context)
            return
        }

        // On Android 13+, check POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create persistent channel with LOW importance so it stays in notification bar without sound/vibrate disturbance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Calendar in Notification Bar",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent Hijri and Gregorian dates with Shia Islamic events in the status and notification bar"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }

        // Calculate Dates
        HijriCalendarHelper.syncOffset(context)
        val isPastMaghrib = HijriCalendarHelper.isPastMaghribToday(context)
        val extraOffset = if (isPastMaghrib) 1 else 0

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val fullGregFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val shortGregFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        val gregFullStr = fullGregFormat.format(cal.time)
        val gregShortStr = shortGregFormat.format(cal.time)

        val hijriDate = HijriCalendarHelper.convertGregorianToHijri(
            year,
            month,
            day,
            HijriCalendarHelper.globalOffsetDays + extraOffset
        )
        val hijriDateStr = "${hijriDate.day} ${hijriDate.monthName} ${hijriDate.year} AH"

        // Closest / Today's Shia event
        val eventInfo = HijriCalendarHelper.getUpcomingShiaEvent(context, extraOffset)
        val eventText = eventInfo.displayText

        val pendingIntent = AppPendingIntents.calendarContent(context)

        val title = "🌙 $hijriDateStr  •  $gregShortStr"
        val bigText = buildString {
            append("📅 $gregFullStr\n")
            append("🌙 $hijriDateStr\n")
            append("✨ $eventText")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calendar_notification)
            .setContentTitle(title)
            .setContentText(eventText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_calendar_notification,
                "Open Calendar",
                pendingIntent
            )
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelCalendarNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
