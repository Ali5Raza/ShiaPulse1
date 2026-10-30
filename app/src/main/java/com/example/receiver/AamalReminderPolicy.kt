package com.example.receiver

import android.content.Intent
import java.util.Calendar
import java.util.TimeZone

internal enum class AamalReminderAction { DELIVER, RESCHEDULE_ONLY, IGNORE }

internal object AamalReminderPolicy {
    const val DEFAULT_HOUR = 20
    const val DEFAULT_MINUTE = 0

    fun actionFor(action: String?): AamalReminderAction = when (action) {
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        "android.intent.action.QUICKBOOT_POWERON" -> AamalReminderAction.RESCHEDULE_ONLY
        null,
        AamalEventScheduler.ACTION_AAMAL_REMINDER -> AamalReminderAction.DELIVER
        else -> AamalReminderAction.IGNORE
    }

    fun learnedReminderHour(timestamps: List<Long>, timeZone: TimeZone): Int {
        if (timestamps.isEmpty()) return DEFAULT_HOUR
        val hourCounts = IntArray(24)
        timestamps.forEach { timestamp ->
            val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = timestamp }
            hourCounts[calendar.get(Calendar.HOUR_OF_DAY)]++
        }
        return hourCounts.indices.maxByOrNull { hourCounts[it] } ?: DEFAULT_HOUR
    }

    fun dateKey(epochMillis: Long, timeZone: TimeZone): String {
        val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = epochMillis }
        return String.format(
            java.util.Locale.US,
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun nextTriggerMillis(
        nowMillis: Long,
        hour: Int,
        minute: Int,
        timeZone: TimeZone
    ): Long {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= nowMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}
