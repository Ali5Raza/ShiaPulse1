package com.example.utils

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*

object PrayerTimeCalculator {

    // Existing Shia calculation conventions are intentionally unchanged.
    const val FAJR_ANGLE_DEGREES = 16.0
    const val MAGHRIB_ANGLE_DEGREES = 4.0
    const val ISHA_ANGLE_DEGREES = 14.0
    private const val SUNRISE_SUNSET_ANGLE_DEGREES = 0.833

    data class PrayerTimes(
        val imsakString: String,
        val fajrString: String,
        val sunriseString: String,
        val dhuhrString: String,
        val asrString: String,
        val sunsetString: String,
        val maghribString: String,
        val ishaString: String,
        val midnightString: String
    )

    enum class PrayerEvent {
        IMSAK,
        FAJR,
        SUNRISE,
        DHUHR,
        ASR,
        SUNSET,
        MAGHRIB,
        ISHA,
        MIDNIGHT
    }

    /** A Gregorian civil date, independent of the device's default time zone. */
    data class PrayerDate(val year: Int, val month: Int, val day: Int) {
        val key: String
            get() = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

        fun plusDays(days: Int): PrayerDate {
            val calendar = utcCalendar(this)
            calendar.add(Calendar.DAY_OF_YEAR, days)
            return fromCalendar(calendar)
        }

        fun dayOfYear(): Int = utcCalendar(this).get(Calendar.DAY_OF_YEAR)

        fun epochDay(): Long = utcCalendar(this).timeInMillis / MILLIS_PER_DAY

        fun atStartOfDay(timeZone: TimeZone): Calendar = GregorianCalendar(timeZone).apply {
            clear()
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        companion object {
            fun fromCalendar(calendar: Calendar): PrayerDate = PrayerDate(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
    }

    data class PrayerTimeline(
        val date: PrayerDate,
        val timeZoneId: String,
        val times: PrayerTimes,
        val instants: Map<PrayerEvent, Long>
    ) {
        fun instant(event: PrayerEvent): Long = instants.getValue(event)
    }

    data class NextPrayerEvent(
        val event: PrayerEvent,
        val instant: Long,
        val localTime: String,
        val isTomorrow: Boolean
    )

    data class SolarUtcHours(
        val transit: Double,
        val fajrHourAngle: Double,
        val sunriseHourAngle: Double,
        val sunsetHourAngle: Double,
        val asrHourAngle: Double,
        val maghribHourAngle: Double,
        val ishaHourAngle: Double
    )

    val countdownEvents: List<PrayerEvent> = listOf(
        PrayerEvent.FAJR,
        PrayerEvent.SUNRISE,
        PrayerEvent.DHUHR,
        PrayerEvent.ASR,
        PrayerEvent.SUNSET,
        PrayerEvent.MAGHRIB,
        PrayerEvent.ISHA,
        PrayerEvent.MIDNIGHT
    )

    val obligatoryPrayerEvents: List<PrayerEvent> = listOf(
        PrayerEvent.FAJR,
        PrayerEvent.DHUHR,
        PrayerEvent.ASR,
        PrayerEvent.MAGHRIB,
        PrayerEvent.ISHA
    )

    /** Backwards-compatible fixed-offset entry point used by existing non-Home screens. */
    fun calculateTimes(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        timezone: Double
    ): PrayerTimes = calculateTimeline(
        year = year,
        month = month,
        day = day,
        latitude = latitude,
        longitude = longitude,
        timeZone = LocationTimeZone.fixedOffset(timezone)
    ).times

    fun calculateTimes(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        timeZone: TimeZone
    ): PrayerTimes = calculateTimeline(year, month, day, latitude, longitude, timeZone).times

    /**
     * Calculates each event as an absolute instant. Time-zone rules are evaluated by
     * [GregorianCalendar] at the event itself, so DST transitions do not require a fixed offset.
     */
    fun calculateTimeline(
        year: Int,
        month: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        timeZone: TimeZone
    ): PrayerTimeline {
        val date = PrayerDate(year, month, day)
        return calculateTimeline(date, latitude, longitude, timeZone)
    }

    fun calculateTimeline(
        date: PrayerDate,
        latitude: Double,
        longitude: Double,
        timeZone: TimeZone
    ): PrayerTimeline {
        val solar = calculateSolarUtcHours(date, latitude, longitude)
        val midnightHours = (solar.transit + solar.sunsetHourAngle).let { sunset ->
            val nextFajr = solar.transit - solar.fajrHourAngle + 24.0
            sunset + (nextFajr - sunset) / 2.0
        }
        val eventHours = mapOf(
            PrayerEvent.IMSAK to (solar.transit - solar.fajrHourAngle - 10.0 / 60.0),
            PrayerEvent.FAJR to (solar.transit - solar.fajrHourAngle),
            PrayerEvent.SUNRISE to (solar.transit - solar.sunriseHourAngle),
            PrayerEvent.DHUHR to solar.transit,
            PrayerEvent.ASR to (solar.transit + solar.asrHourAngle),
            PrayerEvent.SUNSET to (solar.transit + solar.sunsetHourAngle),
            PrayerEvent.MAGHRIB to (solar.transit + solar.maghribHourAngle),
            PrayerEvent.ISHA to (solar.transit + solar.ishaHourAngle),
            PrayerEvent.MIDNIGHT to midnightHours
        )
        val utcStart = date.atStartOfDay(TimeZone.getTimeZone("UTC")).timeInMillis
        val instants = eventHours.mapValues { (_, hours) ->
            roundToNearestMinute(utcStart + hours * MILLIS_PER_HOUR)
        }

        val times = PrayerTimes(
            imsakString = formatInstant(instants.getValue(PrayerEvent.IMSAK), timeZone),
            fajrString = formatInstant(instants.getValue(PrayerEvent.FAJR), timeZone),
            sunriseString = formatInstant(instants.getValue(PrayerEvent.SUNRISE), timeZone),
            dhuhrString = formatInstant(instants.getValue(PrayerEvent.DHUHR), timeZone),
            asrString = formatInstant(instants.getValue(PrayerEvent.ASR), timeZone),
            sunsetString = formatInstant(instants.getValue(PrayerEvent.SUNSET), timeZone),
            maghribString = formatInstant(instants.getValue(PrayerEvent.MAGHRIB), timeZone),
            ishaString = formatInstant(instants.getValue(PrayerEvent.ISHA), timeZone),
            midnightString = formatInstant(instants.getValue(PrayerEvent.MIDNIGHT), timeZone)
        )
        return PrayerTimeline(date, timeZone.id, times, instants)
    }

    /** Selects by epoch instant, never by the device wall clock. */
    fun findNextEvent(
        today: PrayerTimeline,
        tomorrow: PrayerTimeline,
        nowMillis: Long
    ): NextPrayerEvent {
        val nextToday = countdownEvents
            .map { event -> event to today.instant(event) }
            .filter { (_, instant) -> instant > nowMillis }
            .minByOrNull { (_, instant) -> instant }
        if (nextToday != null) {
            val (event, instant) = nextToday
            return NextPrayerEvent(event, instant, localTime(today, event), isTomorrow = false)
        }

        val event = PrayerEvent.FAJR
        return NextPrayerEvent(
            event = event,
            instant = tomorrow.instant(event),
            localTime = localTime(tomorrow, event),
            isTomorrow = true
        )
    }

    fun localTime(timeline: PrayerTimeline, event: PrayerEvent): String = when (event) {
        PrayerEvent.IMSAK -> timeline.times.imsakString
        PrayerEvent.FAJR -> timeline.times.fajrString
        PrayerEvent.SUNRISE -> timeline.times.sunriseString
        PrayerEvent.DHUHR -> timeline.times.dhuhrString
        PrayerEvent.ASR -> timeline.times.asrString
        PrayerEvent.SUNSET -> timeline.times.sunsetString
        PrayerEvent.MAGHRIB -> timeline.times.maghribString
        PrayerEvent.ISHA -> timeline.times.ishaString
        PrayerEvent.MIDNIGHT -> timeline.times.midnightString
    }

    private fun calculateSolarUtcHours(
        date: PrayerDate,
        latitude: Double,
        longitude: Double
    ): SolarUtcHours {
        val m = date.month.toDouble()
        val y = date.year.toDouble()
        val d = date.day.toDouble()

        // Julian Date relative to epoch J2000.0 (existing algorithm).
        val jd = 367.0 * y - (7.0 * (y + (m + 9.0) / 12.0).toInt()) / 4.0 +
            (275.0 * m) / 9.0 + d - 730531.5

        var g = normalizeDegrees(357.529 + 0.98560028 * jd)
        var q = normalizeDegrees(280.459 + 0.98564736 * jd)
        var eclipticLongitude = q + 1.915 * sin(Math.toRadians(g)) +
            0.020 * sin(Math.toRadians(2.0 * g))
        eclipticLongitude = normalizeDegrees(eclipticLongitude)

        val obliquity = 23.439 - 0.00000036 * jd
        val declination = asin(
            sin(Math.toRadians(obliquity)) * sin(Math.toRadians(eclipticLongitude))
        )
        var rightAscension = Math.toDegrees(
            atan2(
                cos(Math.toRadians(obliquity)) * sin(Math.toRadians(eclipticLongitude)),
                cos(Math.toRadians(eclipticLongitude))
            )
        )
        rightAscension = normalizeDegrees(rightAscension) / 15.0

        var equationOfTime = q / 15.0 - rightAscension
        if (equationOfTime > 20.0) equationOfTime -= 24.0
        if (equationOfTime < -20.0) equationOfTime += 24.0

        // UTC hours are independent of location offset. The selected time zone is
        // applied only when each absolute event instant is created and displayed.
        val transitUtc = 12.0 - longitude / 15.0 - equationOfTime
        val latitudeRadians = Math.toRadians(latitude)
        val asrAltitude = Math.toDegrees(
            atan(1.0 / (1.0 + tan(abs(latitudeRadians - declination))))
        )

        return SolarUtcHours(
            transit = transitUtc,
            fajrHourAngle = computeHourAngle(latitude, declination, -FAJR_ANGLE_DEGREES),
            sunriseHourAngle = computeHourAngle(
                latitude,
                declination,
                -SUNRISE_SUNSET_ANGLE_DEGREES
            ),
            sunsetHourAngle = computeHourAngle(
                latitude,
                declination,
                -SUNRISE_SUNSET_ANGLE_DEGREES
            ),
            asrHourAngle = computeHourAngle(latitude, declination, asrAltitude),
            maghribHourAngle = computeHourAngle(latitude, declination, -MAGHRIB_ANGLE_DEGREES),
            ishaHourAngle = computeHourAngle(latitude, declination, -ISHA_ANGLE_DEGREES)
        )
    }

    private fun computeHourAngle(
        latitudeDegrees: Double,
        declinationRadians: Double,
        altitudeDegrees: Double
    ): Double {
        val latitudeRadians = Math.toRadians(latitudeDegrees)
        val altitudeRadians = Math.toRadians(altitudeDegrees)
        val cosHourAngle = (
            sin(altitudeRadians) - sin(latitudeRadians) * sin(declinationRadians)
            ) / (cos(latitudeRadians) * cos(declinationRadians))
        // Existing high-latitude behavior is retained pending religious/product review.
        if (cosHourAngle < -1.0) return 12.0
        if (cosHourAngle > 1.0) return 0.0
        return Math.toDegrees(acos(cosHourAngle)) / 15.0
    }

    private fun normalizeDegrees(degrees: Double): Double {
        var normalized = degrees % 360.0
        if (normalized < 0.0) normalized += 360.0
        return normalized
    }

    private fun formatInstant(instant: Long, timeZone: TimeZone): String {
        val calendar = GregorianCalendar(timeZone).apply { timeInMillis = instant }
        return String.format(
            Locale.US,
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }

    private fun utcCalendar(date: PrayerDate): GregorianCalendar =
        GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(date.year, date.month - 1, date.day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private fun roundToNearestMinute(valueMillis: Double): Long =
        ((valueMillis + 30_000.0) / 60_000.0).toLong() * 60_000L

    private const val MILLIS_PER_HOUR = 60.0 * 60.0 * 1000.0
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
}
