package com.example.utils
 
import java.util.Calendar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
 
object HijriCalendarHelper {
    var globalOffsetDays by mutableStateOf(0)

    fun syncOffset(context: android.content.Context) {
        val prefs = context.getSharedPreferences("shiapulse_prefs", android.content.Context.MODE_PRIVATE)
        val autoSync = prefs.getBoolean("hijri_auto_sync", true)
        globalOffsetDays = if (!autoSync) {
            prefs.getInt("user_manual_offset", prefs.getInt("hijri_offset_days", 0))
        } else {
            prefs.getInt("resolved_hijri_offset", prefs.getInt("api_calculated_offset", prefs.getInt("hijri_offset_days", 0)))
        }
    }

    fun isPastMaghribToday(context: android.content.Context): Boolean {
        return try {
            val prayerPrefs = context.getSharedPreferences("shia_tracker_prefs", android.content.Context.MODE_PRIVATE)
            val pulsePrefs = context.getSharedPreferences("shiapulse_prefs", android.content.Context.MODE_PRIVATE)
            val lat = prayerPrefs.getFloat("selected_location_lat", pulsePrefs.getFloat("loc_lat", 32.6160f)).toDouble()
            val lon = prayerPrefs.getFloat("selected_location_lon", pulsePrefs.getFloat("loc_lon", 44.0249f)).toDouble()
            val fallbackOffset = prayerPrefs.getFloat("selected_location_tz", pulsePrefs.getFloat("loc_tz", 3.0f)).toDouble()
            val timeZoneId = prayerPrefs.getString("selected_location_zone_id", null)
                ?: pulsePrefs.getString("loc_zone_id", null)
            val timeZone = LocationTimeZone.resolve(timeZoneId, fallbackOffset)
            val nowMillis = System.currentTimeMillis()
            val localNow = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
            val today = PrayerTimeCalculator.PrayerDate.fromCalendar(localNow)
            val timeline = PrayerTimeCalculator.calculateTimeline(today, lat, lon, timeZone)
            nowMillis >= timeline.instant(PrayerTimeCalculator.PrayerEvent.MAGHRIB)
        } catch (e: Exception) {
            android.util.Log.e("HijriCalendarHelper", "Failed to check Maghrib time", e)
            false
        }
    }

    data class HijriDate(val day: Int, val month: Int, val monthName: String, val year: Int)

    val MONTH_NAMES = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qa'dah", "Dhu al-Hijjah"
    )



    fun convertGregorianToHijri(year: Int, month: Int, day: Int, forceOffset: Int? = null): HijriDate {
        val icuCal = android.icu.util.IslamicCalendar(
            android.icu.util.TimeZone.getDefault(),
            android.icu.util.ULocale.getDefault()
        )
        // Use Umm al-Qura as the base, which is much closer to actual sightings in most regions
        icuCal.calculationType = android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        
        val gregCal = java.util.Calendar.getInstance()
        gregCal.set(year, month - 1, day, 12, 0, 0)
        
        // Apply manual/synced offset to the date before converting
        gregCal.add(java.util.Calendar.DAY_OF_YEAR, forceOffset ?: globalOffsetDays)
        
        icuCal.time = gregCal.time
        
        val hYear = icuCal.get(android.icu.util.IslamicCalendar.YEAR)
        // ICU month is 0-based
        val hMonth = icuCal.get(android.icu.util.IslamicCalendar.MONTH) + 1
        val hDay = icuCal.get(android.icu.util.IslamicCalendar.DAY_OF_MONTH)
        
        return HijriDate(
            day = hDay,
            month = hMonth,
            monthName = MONTH_NAMES.getOrElse(hMonth - 1) { "Muharram" },
            year = hYear
        )
    }

    suspend fun fetchAndSyncOffsetFromApi(context: android.content.Context) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val prefs = context.getSharedPreferences("shiapulse_prefs", android.content.Context.MODE_PRIVATE)
        val autoSync = prefs.getBoolean("hijri_auto_sync", true)
        if (!autoSync) {
            android.util.Log.d("HijriSync", "Hijri auto sync is disabled. Skipping API call.")
            return@withContext
        }
        
        val lastSyncDate = prefs.getString("last_hijri_sync_date", "")
        
        val todayCal = Calendar.getInstance()
        val todayStr = String.format(java.util.Locale.US, "%04d-%02d-%02d", todayCal.get(Calendar.YEAR), todayCal.get(Calendar.MONTH) + 1, todayCal.get(Calendar.DAY_OF_MONTH))
        
        if (lastSyncDate == todayStr) {
            android.util.Log.d("HijriSync", "Already synced today ($todayStr). Skipping API call.")
            return@withContext
        }

        val generation = HijriConfigurationGeneration.begin()
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            // Compare yesterday, today, and tomorrow
            val offsets = mutableListOf<Int>()
            
            for (dayOffset in -1..1) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, dayOffset)
                
                val d = cal.get(Calendar.DAY_OF_MONTH)
                val m = cal.get(Calendar.MONTH) + 1
                val y = cal.get(Calendar.YEAR)
                
                val formattedDate = String.format(java.util.Locale.US, "%02d-%02d-%04d", d, m, y)
                // specify calculation method explicitly (adjustment=0)
                val url = "https://api.aladhan.com/v1/gToH?date=$formattedDate&adjustment=0" 
                
                val request = okhttp3.Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseStr = response.body?.string()
                    if (responseStr != null) {
                        val json = org.json.JSONObject(responseStr)
                        val data = json.getJSONObject("data")
                        val hijri = data.getJSONObject("hijri")
                        
                        val apiDay = hijri.getString("day").toInt()
                        val apiMonth = hijri.getJSONObject("month").getInt("number")
                        val apiYear = hijri.getString("year").toInt()
                        
                        var bestOffset: Int? = null
                        for (offset in -3..3) {
                            val testHijri = convertGregorianToHijri(y, m, d, forceOffset = offset)
                            if (testHijri.day == apiDay && testHijri.month == apiMonth && testHijri.year == apiYear) {
                                bestOffset = offset
                                break
                            }
                        }
                        if (bestOffset != null) {
                            offsets.add(bestOffset)
                            val localHijri = convertGregorianToHijri(y, m, d, 0)
                            android.util.Log.d("HijriSync", "Offset for $formattedDate -> Local: ${localHijri.day}-${localHijri.month}-${localHijri.year}, API: $apiDay-$apiMonth-$apiYear, calcOffset: $bestOffset")
                        }
                    }
                }
            }
            
            if (offsets.isNotEmpty()) {
                if (!HijriConfigurationGeneration.isCurrent(generation)) {
                    android.util.Log.d("HijriSync", "Discarding stale API configuration result")
                    return@withContext
                }
                val confirmedOffset = offsets.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: offsets[0]
                val prevApiOffset = prefs.getInt("api_calculated_offset", 0)
                
                android.util.Log.d("HijriSync", "API calculation offset: $confirmedOffset (prev API offset: $prevApiOffset)")
                
                val stored = HijriConfigurationGeneration.runIfCurrent(generation) {
                    prefs.edit()
                        .putInt("api_calculated_offset", confirmedOffset)
                        .putInt("api_synced_offset", confirmedOffset)
                        .putString("last_hijri_sync_date", todayStr)
                        .apply()
                }
                if (!stored) return@withContext
                    
                // Re-resolve active offset under the correct priority order:
                // Priority 0: Manual (if autoSync false)
                // Priority 1: Content Management Country-wise (if configured for this country)
                // Priority 2: Automatic Library / API (this confirmedOffset)
                try {
                    val countryService = DefaultCountryDetectionService(context)
                    val remoteConfigService = DefaultRemoteConfigurationService(context)
                    val hijriConfigService = DefaultHijriConfigurationService(countryService, remoteConfigService)
                    hijriConfigService.resolveAndApplyOffset(context)
                } catch (e: Exception) {
                    android.util.Log.e("HijriSync", "Error re-resolving offset after API sync", e)
                    HijriConfigurationGeneration.runIfCurrent(generation) {
                        if (prefs.getBoolean("hijri_auto_sync", true)) {
                            globalOffsetDays = confirmedOffset
                            prefs.edit().putInt("resolved_hijri_offset", confirmedOffset).apply()
                        }
                    }
                }
                
                try {
                    val widgetIntent = android.content.Intent("com.example.action.UPDATE_HIJRI_WIDGET").apply {
                        component = android.content.ComponentName(context, com.example.receiver.HijriWidgetProvider::class.java)
                    }
                    context.sendBroadcast(widgetIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                android.util.Log.w("HijriSync", "Could not calculate API offset from response. Keeping current offset: $globalOffsetDays")
            }
        } catch(e: Exception) {
            android.util.Log.e("HijriSync", "API call failed. Keeping prev offset: $globalOffsetDays", e)
        }
    }

    fun convertGregorianToHijri(calendar: Calendar, dayOffset: Int = 0): HijriDate {
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        return convertGregorianToHijri(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Converts a Hijri date back to Gregorian (Calendar) via Meeus Julian Day conversion.
     * month is 1-indexed (1 = Muharram, ..., 12 = Dhu al-Hijjah)
     */
    fun convertHijriToGregorian(hYear: Int, hMonth: Int, hDay: Int, forceOffset: Int? = null): Calendar {
        val icuCal = android.icu.util.IslamicCalendar(
            android.icu.util.TimeZone.getDefault(),
            android.icu.util.ULocale.getDefault()
        )
        icuCal.calculationType = android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        icuCal.set(android.icu.util.IslamicCalendar.YEAR, hYear)
        icuCal.set(android.icu.util.IslamicCalendar.MONTH, (hMonth - 1).coerceIn(0, 11))
        icuCal.set(android.icu.util.IslamicCalendar.DAY_OF_MONTH, hDay)
        
        val gregCal = java.util.Calendar.getInstance()
        gregCal.time = icuCal.time
        
        // Reverse the offset applied in convertGregorianToHijri
        gregCal.add(java.util.Calendar.DAY_OF_YEAR, -(forceOffset ?: globalOffsetDays))
        
        return gregCal
    }

    data class UpcomingEventInfo(
        val displayText: String,
        val isWiladat: Boolean,
        val isShahadat: Boolean,
        val daysUntil: Int
    )

    fun getUpcomingShiaEvent(context: android.content.Context, extraOffset: Int = 0): UpcomingEventInfo {
        try {
            val allEvents = com.example.data.MasoomeenData.getAllEvents(context)
            val languageCode = context.getSharedPreferences("shia_tracker_prefs", android.content.Context.MODE_PRIVATE)
                .getString("selected_language", "en") ?: "en"
            val scanCal = Calendar.getInstance()
            for (offset in 0..30) {
                val sYear = scanCal.get(Calendar.YEAR)
                val sMonth = scanCal.get(Calendar.MONTH) + 1
                val sDay = scanCal.get(Calendar.DAY_OF_MONTH)
                
                val sHijri = convertGregorianToHijri(sYear, sMonth, sDay, globalOffsetDays + extraOffset)
                val matchedEvents = allEvents.filter { ev ->
                    ev.occursOn(sHijri.day, sHijri.month, sHijri.monthName)
                }

                if (matchedEvents.isNotEmpty()) {
                    // A shared date can contain several occasions; do not hide all but the first.
                    // Solemn presentation takes priority when joyful and solemn records coincide.
                    val isShahadat = matchedEvents.any { it.eventType == com.example.data.EventType.SHAHADAT }
                    val isWiladat = !isShahadat && matchedEvents.any { it.eventType == com.example.data.EventType.WILADAT }
                    val typePrefix = if (isWiladat) "🎉 Wiladat" else "🏴 Shahadat"
                    val eventTitles = matchedEvents.joinToString(" & ") { it.localizedTitle(languageCode) }

                    val text = when (offset) {
                        0 -> "TODAY: $typePrefix - $eventTitles"
                        1 -> "TOMORROW: $typePrefix - $eventTitles"
                        else -> "In $offset days (${sHijri.day} ${sHijri.monthName}): $typePrefix - $eventTitles"
                    }
                    return UpcomingEventInfo(text, isWiladat, isShahadat, offset)
                }
                scanCal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return UpcomingEventInfo("No upcoming major Shia events in the next 30 days.", isWiladat = false, isShahadat = false, daysUntil = -1)
    }
}
