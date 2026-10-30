package com.example.utils

import java.util.Calendar
import java.util.TimeZone

data class HomePrayerTimelines(
    val selected: PrayerTimeCalculator.PrayerTimeline,
    val current: PrayerTimeCalculator.PrayerTimeline,
    val tomorrow: PrayerTimeCalculator.PrayerTimeline,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String
)

/**
 * The single Islamic-day snapshot consumed by Home, guidance, Daily content,
 * occasion theming, and upcoming events.
 */
data class HomeDayContext(
    val locationKey: String,
    val countryCode: String?,
    val timeZoneId: String,
    val currentLocationDate: PrayerTimeCalculator.PrayerDate,
    val selectedDate: PrayerTimeCalculator.PrayerDate,
    val effectiveGregorianDate: PrayerTimeCalculator.PrayerDate,
    val isPastMaghrib: Boolean,
    val hijriOffset: Int,
    val hijriDate: HijriCalendarHelper.HijriDate,
    val timelines: HomePrayerTimelines
) {
    val dailyContentDayStamp: Int
        get() = effectiveGregorianDate.year * 1000 + effectiveGregorianDate.dayOfYear()

    private fun actualCalendar(nowMillis: Long): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)).apply { timeInMillis = nowMillis }

    /** Uses the effective Islamic day for date-based guidance and rotation. */
    fun guidanceCalendar(nowMillis: Long): Calendar = actualCalendar(nowMillis).apply {
        set(Calendar.YEAR, effectiveGregorianDate.year)
        set(Calendar.MONTH, effectiveGregorianDate.month - 1)
        set(Calendar.DAY_OF_MONTH, effectiveGregorianDate.day)
    }

    /** Preserves the established Thursday-night rule for actual today. */
    fun weeklyGuidanceCalendar(nowMillis: Long): Calendar =
        if (selectedDate == currentLocationDate) actualCalendar(nowMillis) else guidanceCalendar(nowMillis)
}

object HomeDayContextFactory {
    fun calculateTimelines(
        selectedDate: PrayerTimeCalculator.PrayerDate,
        currentLocationDate: PrayerTimeCalculator.PrayerDate,
        latitude: Double,
        longitude: Double,
        timeZone: TimeZone
    ): HomePrayerTimelines {
        val current = PrayerTimeCalculator.calculateTimeline(
            currentLocationDate,
            latitude,
            longitude,
            timeZone
        )
        val selected = if (selectedDate == currentLocationDate) {
            current
        } else {
            PrayerTimeCalculator.calculateTimeline(selectedDate, latitude, longitude, timeZone)
        }
        return HomePrayerTimelines(
            selected = selected,
            current = current,
            tomorrow = PrayerTimeCalculator.calculateTimeline(
                currentLocationDate.plusDays(1),
                latitude,
                longitude,
                timeZone
            ),
            latitude = latitude,
            longitude = longitude,
            timeZoneId = timeZone.id
        )
    }

    fun create(
        nowMillis: Long,
        selectedDate: PrayerTimeCalculator.PrayerDate,
        latitude: Double,
        longitude: Double,
        timeZone: TimeZone,
        hijriOffset: Int,
        locationKey: String = "",
        countryCode: String? = null,
        timelines: HomePrayerTimelines? = null,
        hijriConverter: (PrayerTimeCalculator.PrayerDate, Int) -> HijriCalendarHelper.HijriDate =
            { date, offset ->
                HijriCalendarHelper.convertGregorianToHijri(
                    date.year,
                    date.month,
                    date.day,
                    forceOffset = offset
                )
            }
    ): HomeDayContext {
        val currentCalendar = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val currentLocationDate = PrayerTimeCalculator.PrayerDate.fromCalendar(currentCalendar)
        val resolvedTimelines = timelines?.takeIf {
            it.current.date == currentLocationDate &&
                it.selected.date == selectedDate &&
                it.tomorrow.date == currentLocationDate.plusDays(1) &&
                it.latitude == latitude &&
                it.longitude == longitude &&
                it.timeZoneId == timeZone.id
        } ?: calculateTimelines(
            selectedDate,
            currentLocationDate,
            latitude,
            longitude,
            timeZone
        )

        // The extra Islamic-day transition applies only to actual today, never to
        // a historical/future date explicitly selected by the user.
        val isPastMaghrib = selectedDate == currentLocationDate &&
            nowMillis >= resolvedTimelines.selected.instant(PrayerTimeCalculator.PrayerEvent.MAGHRIB)
        val effectiveGregorianDate = selectedDate.plusDays(if (isPastMaghrib) 1 else 0)

        return HomeDayContext(
            locationKey = locationKey,
            countryCode = CountryLocationResolver.validatedCode(countryCode),
            timeZoneId = timeZone.id,
            currentLocationDate = currentLocationDate,
            selectedDate = selectedDate,
            effectiveGregorianDate = effectiveGregorianDate,
            isPastMaghrib = isPastMaghrib,
            hijriOffset = hijriOffset,
            hijriDate = hijriConverter(effectiveGregorianDate, hijriOffset),
            timelines = resolvedTimelines
        )
    }
}
