package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.utils.ShiaAdhanPlayer

class AdhanReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_STOP_ADHAN = "com.example.action.STOP_ADHAN"
        const val ACTION_ADHAN_ALARM = "com.example.action.ADHAN_ALARM"

        fun stopAction(prayerName: String): String = "$ACTION_STOP_ADHAN.$prayerName"
        fun alarmAction(prayerName: String): String = "$ACTION_ADHAN_ALARM.$prayerName"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_STOP_ADHAN || action?.startsWith("$ACTION_STOP_ADHAN.") == true) {
            ShiaAdhanPlayer.stop()
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val prayerName = intent.getStringExtra("PRAYER_NAME")
            if (prayerName != null) {
                manager.cancel(prayerName.hashCode())
            }
            // An old intent without a name must not cancel unrelated app notifications.
            return
        }

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED) {
            val prefs = context.getSharedPreferences("shia_tracker_prefs", Context.MODE_PRIVATE)
            val isGlobalEnabled = prefs.getBoolean("adhan_enabled", false)
            if (isGlobalEnabled) {
                AdhanScheduler.scheduleSavedAdhanAlarms(context)
            }
            return
        }

        val prefs = context.getSharedPreferences("shia_tracker_prefs", Context.MODE_PRIVATE)
        val isGlobalEnabled = prefs.getBoolean("adhan_enabled", false)
        if (!isGlobalEnabled) return

        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Namaz"
        
        // Specific toggle check (default to true)
        val isPrayerEnabled = prefs.getBoolean("adhan_${prayerName.lowercase()}_enabled", true)
        if (!isPrayerEnabled) return

        val prefSound = prefs.getString("adhan_sound_key", "ali_fani") ?: "ali_fani"
        // 1. Play Shia Adhan audio Stream with offline Fallback
        ShiaAdhanPlayer.play(context, prefSound)

        // 2. Show prayer notification with a Mute/Stop button
        showAdhanNotification(context, prayerName)

        // 3. Reschedule all enabled alarms for the future using the same saved location.
        AdhanScheduler.scheduleSavedAdhanAlarms(context)
    }

    internal fun showAdhanNotification(context: Context, prayerName: String) {
        val channelId = "adhan_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Adhan Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Adhan notifications for Shia Prayer Times"
                enableVibration(true)
                setBypassDnd(true)
            }
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = AppPendingIntents.adhanContent(context, prayerName)
        val stopPendingIntent = AppPendingIntents.adhanStop(context, prayerName)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Adhan: Time for $prayerName")
            .setContentText("Observe your premium Shia $prayerName prayer now. Recite Namaz on its prime time!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_lock_silent_mode,
                "Stop Shia Adhan",
                stopPendingIntent
            )
            .build()

        manager.notify(prayerName.hashCode(), notification)
    }
}
