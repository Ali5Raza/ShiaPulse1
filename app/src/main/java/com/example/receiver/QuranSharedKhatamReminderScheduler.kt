package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object QuranSharedKhatamReminderScheduler {
    const val PREFS_NAME = "quran_shared_khatam"
    const val EXTRA_CODE = "quran_khatam_code"
    const val EXTRA_TITLE = "quran_khatam_title"
    private const val ENABLED_PREFIX = "reminder_enabled_"
    private const val HOUR_PREFIX = "reminder_hour_"
    private const val MINUTE_PREFIX = "reminder_minute_"
    private const val TITLE_PREFIX = "reminder_title_"
    const val ACTION_QURAN_SHARED_KHATAM_REMINDER = "com.example.action.QURAN_SHARED_KHATAM_REMINDER"

    fun reminderAction(code: String): String = "$ACTION_QURAN_SHARED_KHATAM_REMINDER.$code"

    fun isEnabled(context: Context, code: String): Boolean = prefs(context)
        .getBoolean(ENABLED_PREFIX + code, false)

    fun reminderHour(context: Context, code: String): Int = prefs(context)
        .getInt(HOUR_PREFIX + code, 20)

    fun reminderMinute(context: Context, code: String): Int = prefs(context)
        .getInt(MINUTE_PREFIX + code, 0)

    fun setReminder(
        context: Context,
        code: String,
        title: String,
        hour: Int,
        minute: Int,
        enabled: Boolean
    ) {
        val normalizedCode = code.trim().uppercase()
        if (normalizedCode.isBlank()) return
        prefs(context).edit()
            .putBoolean(ENABLED_PREFIX + normalizedCode, enabled)
            .putInt(HOUR_PREFIX + normalizedCode, hour.coerceIn(0, 23))
            .putInt(MINUTE_PREFIX + normalizedCode, minute.coerceIn(0, 59))
            .putString(TITLE_PREFIX + normalizedCode, title)
            .apply()
        if (enabled) schedule(context, normalizedCode, title, hour, minute)
        else cancel(context, normalizedCode)
    }

    fun updateTime(context: Context, code: String, hour: Int, minute: Int) {
        val title = prefs(context).getString(TITLE_PREFIX + code, null) ?: "Shared Quran Khatam"
        setReminder(context, code, title, hour, minute, isEnabled(context, code))
    }

    fun updateTitle(context: Context, code: String, title: String) {
        val prefs = prefs(context)
        prefs.edit().putString(TITLE_PREFIX + code, title).apply()
        if (prefs.getBoolean(ENABLED_PREFIX + code, false)) {
            schedule(context, code, title, reminderHour(context, code), reminderMinute(context, code))
        }
    }

    fun cancel(context: Context, code: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderIntent(context, code, null)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        legacyReminderIntent(context, code)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    fun rescheduleAll(context: Context) {
        val prefs = prefs(context)
        prefs.all.keys.filter { it.startsWith(ENABLED_PREFIX) }.forEach { key ->
            val code = key.removePrefix(ENABLED_PREFIX)
            if (prefs.getBoolean(key, false) && code.isNotBlank()) {
                schedule(
                    context = context,
                    code = code,
                    title = prefs.getString(TITLE_PREFIX + code, null) ?: "Shared Quran Khatam",
                    hour = prefs.getInt(HOUR_PREFIX + code, 20),
                    minute = prefs.getInt(MINUTE_PREFIX + code, 0)
                )
            }
        }
    }

    private fun schedule(context: Context, code: String, title: String, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val firstReminder = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        legacyReminderIntent(context, code)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            firstReminder.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            reminderIntent(context, code, title)
        )
    }

    private fun reminderIntent(context: Context, code: String, title: String?): PendingIntent {
        val intent = Intent(context, QuranSharedKhatamReminderReceiver::class.java).apply {
            action = reminderAction(code)
            putExtra(EXTRA_CODE, code)
            putExtra(EXTRA_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            code.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun legacyReminderIntent(context: Context, code: String): PendingIntent? {
        val intent = Intent(context, QuranSharedKhatamReminderReceiver::class.java).apply {
            action = ACTION_QURAN_SHARED_KHATAM_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            code.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
