package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.MasoomeenData
import com.example.utils.HijriCalendarHelper
import java.util.Calendar

class MasoomEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("shia_tracker_prefs", Context.MODE_PRIVATE)
        // Ensure notifications aren't globally disabled if such a preference is supported, otherwise proceed
        val isEventsEnabled = prefs.getBoolean("events_enabled", true)
        if (!isEventsEnabled) return

        // Check if there's any notable event today
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // For 8 AM notifications, we are not past Maghrib
        val hijriDate = HijriCalendarHelper.convertGregorianToHijri(calendar, 0)
        val allEvents = MasoomeenData.getAllEvents(context)
        
        val todaysEvents = allEvents.filter {
            it.occursOn(hijriDate.day, hijriDate.month, hijriDate.monthName)
        }

        if (todaysEvents.isNotEmpty()) {
            showEventsNotification(context, todaysEvents)
        }
    }

    private fun showEventsNotification(context: Context, events: List<com.example.data.MasoomEvent>) {
        val channelId = "masoomeen_events_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Islamic Events Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders for Shia Islamic events from the Lunar Calendar"
            }
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = AppPendingIntents.masoomEventContent(context)

        val languageCode = context.getSharedPreferences("shia_tracker_prefs", Context.MODE_PRIVATE)
            .getString("selected_language", "en") ?: "en"
        val titleText = if (events.size == 1) {
            events.first().localizedTitle(languageCode)
        } else {
            when (languageCode) {
                "ur" -> "آج متعدد اسلامی مناسبتیں"
                "ar" -> "مناسبات إسلامية متعددة اليوم"
                else -> "Multiple Islamic Events Today"
            }
        }
        val contentText = events.joinToString("\n") { it.localizedTitle(languageCode) }
        val bigTextContext = events.joinToString("\n\n") {
            "• ${it.localizedTitle(languageCode)}: ${it.localizedDescription(languageCode)}"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextContext))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify("masoomeen_events".hashCode(), notification)
    }
}
