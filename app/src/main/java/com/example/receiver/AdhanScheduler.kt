package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.utils.LocationTimeZone
import com.example.utils.PrayerTimeCalculator
import java.util.Calendar

object AdhanScheduler {
    private const val PREFS = "shia_tracker_prefs"

    data class SavedAdhanLocation(
        val latitude: Double,
        val longitude: Double,
        val fallbackOffsetHours: Double,
        val timeZoneId: String?
    )

    fun prayerName(event: PrayerTimeCalculator.PrayerEvent): String = when (event) {
        PrayerTimeCalculator.PrayerEvent.FAJR -> "Fajr"
        PrayerTimeCalculator.PrayerEvent.DHUHR -> "Dhuhr"
        PrayerTimeCalculator.PrayerEvent.ASR -> "Asr"
        PrayerTimeCalculator.PrayerEvent.MAGHRIB -> "Maghrib"
        PrayerTimeCalculator.PrayerEvent.ISHA -> "Isha"
        else -> event.name
    }

    fun preferenceKey(event: PrayerTimeCalculator.PrayerEvent): String =
        "adhan_${prayerName(event).lowercase()}_enabled"

    fun prayerNamesForSettings(): List<String> =
        PrayerTimeCalculator.obligatoryPrayerEvents.map { prayerName(it) }

    /** Shared by production scheduling and focused preference tests. */
    fun enabledAdhanEvents(
        isPreferenceEnabled: (String) -> Boolean
    ): List<PrayerTimeCalculator.PrayerEvent> =
        PrayerTimeCalculator.obligatoryPrayerEvents.filter { event ->
            isPreferenceEnabled(preferenceKey(event))
        }

    /** Uses the ViewModel's keys, with legacy city_* fallback for existing installations. */
    fun savedLocation(context: Context): SavedAdhanLocation? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val hasCurrent = prefs.contains("selected_location_lat") &&
            prefs.contains("selected_location_lon")
        val hasLegacy = prefs.contains("city_lat") && prefs.contains("city_lon")
        if (!hasCurrent && !hasLegacy) return null

        val latitude = if (hasCurrent) {
            prefs.getFloat("selected_location_lat", 0f).toDouble()
        } else {
            prefs.getFloat("city_lat", 0f).toDouble()
        }
        val longitude = if (hasCurrent) {
            prefs.getFloat("selected_location_lon", 0f).toDouble()
        } else {
            prefs.getFloat("city_lon", 0f).toDouble()
        }
        if (!latitude.isFinite() || !longitude.isFinite() ||
            latitude !in -90.0..90.0 || longitude !in -180.0..180.0
        ) {
            return null
        }

        val fallbackOffset = when {
            prefs.contains("selected_location_tz") -> prefs.getFloat("selected_location_tz", 0f).toDouble()
            prefs.contains("city_tz") -> prefs.getFloat("city_tz", 0f).toDouble()
            else -> 0.0
        }
        return SavedAdhanLocation(
            latitude = latitude,
            longitude = longitude,
            fallbackOffsetHours = fallbackOffset,
            timeZoneId = prefs.getString("selected_location_zone_id", null)
        )
    }

    fun scheduleSavedAdhanAlarms(context: Context): Boolean {
        val location = savedLocation(context)
        if (location == null) {
            cancelAllAdhanAlarms(context)
            return false
        }
        scheduleAdhanAlarms(
            context = context,
            lat = location.latitude,
            lon = location.longitude,
            tzOffset = location.fallbackOffsetHours,
            timeZoneId = location.timeZoneId
        )
        return true
    }

    fun scheduleAdhanAlarms(
        context: Context,
        lat: Double,
        lon: Double,
        tzOffset: Double,
        timeZoneId: String? = null
    ) {
        if (!lat.isFinite() || !lon.isFinite() ||
            lat !in -90.0..90.0 || lon !in -180.0..180.0
        ) {
            return
        }

        val timeZone = LocationTimeZone.resolve(timeZoneId, tzOffset)
        val nowMillis = System.currentTimeMillis()
        val localNow = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val today = PrayerTimeCalculator.PrayerDate.fromCalendar(localNow)
        val todayTimeline = PrayerTimeCalculator.calculateTimeline(today, lat, lon, timeZone)
        val tomorrowTimeline = PrayerTimeCalculator.calculateTimeline(
            today.plusDays(1),
            lat,
            lon,
            timeZone
        )
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val enabledEvents = enabledAdhanEvents { key -> prefs.getBoolean(key, true) }.toSet()

        PrayerTimeCalculator.obligatoryPrayerEvents.forEach { event ->
            cancelMatchingAdhanAlarm(context, event, AdhanReceiver.ACTION_ADHAN_ALARM)
            cancelLegacyAdhanAlarm(context, event)
            if (event !in enabledEvents) {
                cancelAdhanAlarm(context, event)
                return@forEach
            }
            val todayInstant = todayTimeline.instant(event)
            val triggerInstant = if (todayInstant > nowMillis) {
                todayInstant
            } else {
                tomorrowTimeline.instant(event)
            }
            try {
                scheduleAlarm(context, event, triggerInstant)
            } catch (error: Exception) {
                // One unavailable alarm must not prevent the other prayers being scheduled.
                error.printStackTrace()
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        event: PrayerTimeCalculator.PrayerEvent,
        triggerInstant: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AdhanReceiver::class.java).apply {
            action = AdhanReceiver.alarmAction(prayerName(event))
            putExtra("PRAYER_NAME", prayerName(event))
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayerName(event).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerInstant,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerInstant, pendingIntent)
            }
        } catch (error: SecurityException) {
            // Existing fallback for devices where exact-alarm permission is unavailable.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerInstant,
                    pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerInstant, pendingIntent)
            }
        } catch (error: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerInstant, pendingIntent)
            } catch (fallbackError: Exception) {
                fallbackError.printStackTrace()
            }
        }
    }

    private fun cancelAdhanAlarm(context: Context, event: PrayerTimeCalculator.PrayerEvent) {
        cancelMatchingAdhanAlarm(context, event, AdhanReceiver.alarmAction(prayerName(event)))
        cancelMatchingAdhanAlarm(context, event, AdhanReceiver.ACTION_ADHAN_ALARM)
        cancelLegacyAdhanAlarm(context, event)
    }

    private fun cancelLegacyAdhanAlarm(context: Context, event: PrayerTimeCalculator.PrayerEvent) {
        cancelMatchingAdhanAlarm(context, event, null)
    }

    private fun cancelMatchingAdhanAlarm(
        context: Context,
        event: PrayerTimeCalculator.PrayerEvent,
        action: String?
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AdhanReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayerName(event).hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelAllAdhanAlarms(context: Context) {
        PrayerTimeCalculator.obligatoryPrayerEvents.forEach { event ->
            cancelAdhanAlarm(context, event)
        }
    }
}
