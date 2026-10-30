package com.example.utils

import android.content.Context
import android.util.Log
import java.util.TimeZone

/** Reads current and legacy location keys once for all Aamal reminder calculations. */
object SelectedLocationTimeZone {
    fun fromPreferences(
        context: Context,
        deviceTimeZone: TimeZone = TimeZone.getDefault()
    ): ResolvedLocationTimeZone {
        val prayerPrefs = context.getSharedPreferences("shia_tracker_prefs", Context.MODE_PRIVATE)
        val pulsePrefs = context.getSharedPreferences("shiapulse_prefs", Context.MODE_PRIVATE)
        val zoneId = prayerPrefs.getString("selected_location_zone_id", null)
            ?: pulsePrefs.getString("loc_zone_id", null)
        val hasSelectedLocation = listOf(
            "selected_location_lat",
            "selected_location_lon",
            "selected_location_tz",
            "selected_location_zone_id",
            "selected_location_city",
            "city_lat",
            "city_lon",
            "city_tz"
        ).any(prayerPrefs::contains) || listOf(
            "loc_lat",
            "loc_lon",
            "loc_tz",
            "loc_zone_id",
            "loc_city"
        ).any(pulsePrefs::contains)
        val offset = when {
            prayerPrefs.contains("selected_location_tz") ->
                prayerPrefs.getFloat("selected_location_tz", 0f).toDouble()
            prayerPrefs.contains("city_tz") ->
                prayerPrefs.getFloat("city_tz", 0f).toDouble()
            pulsePrefs.contains("loc_tz") ->
                pulsePrefs.getFloat("loc_tz", 0f).toDouble()
            else -> null
        }
        return LocationTimeZone.resolveDetailed(
            timeZoneId = zoneId,
            fallbackOffsetHours = offset,
            hasSelectedLocation = hasSelectedLocation,
            deviceTimeZone = deviceTimeZone
        ).also { resolved ->
            if (resolved.source == LocationTimeZoneSource.DEVICE_INVALID_SELECTION) {
                Log.w(
                    "LocationTimeZone",
                    "Invalid selected timezone metadata; using the device zone consistently for reminders."
                )
            }
        }
    }
}
