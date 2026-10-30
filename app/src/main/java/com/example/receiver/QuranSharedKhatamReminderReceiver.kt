package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class QuranSharedKhatamReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                QuranSharedKhatamReminderScheduler.rescheduleAll(context)
                return
            }
        }

        val code = intent.getStringExtra(QuranSharedKhatamReminderScheduler.EXTRA_CODE)
            ?.trim()?.uppercase().orEmpty()
        if (code.isBlank() || !QuranSharedKhatamReminderScheduler.isEnabled(context, code)) return
        val storedTitle = context.getSharedPreferences(
            QuranSharedKhatamReminderScheduler.PREFS_NAME,
            Context.MODE_PRIVATE
        ).getString("reminder_title_$code", null)
        val title = storedTitle
            ?: intent.getStringExtra(QuranSharedKhatamReminderScheduler.EXTRA_TITLE)
            ?: "Shared Quran Khatam"
        showReminder(context, code, title)
    }

    private fun showReminder(context: Context, code: String, title: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Quran Khatam reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val language = context.getSharedPreferences("shia_tracker_prefs", Context.MODE_PRIVATE)
            .getString("selected_language", "en") ?: "en"
        val body = when (language) {
            "ur" -> "آج کا قرآن ورد مکمل کریں اور مشترکہ پیش رفت اپڈیٹ کریں۔"
            "ar" -> "أتمم ورد اليوم من القرآن وحدّث التقدم المشترك."
            else -> "Complete today's Quran reading and update your shared progress."
        }
        val contentIntent = AppPendingIntents.quranSharedKhatamContent(context, code)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(410000 + (code.hashCode() and 0xFFFF), notification)
    }

    private companion object {
        const val CHANNEL_ID = "quran_shared_khatam_reminders"
    }
}
