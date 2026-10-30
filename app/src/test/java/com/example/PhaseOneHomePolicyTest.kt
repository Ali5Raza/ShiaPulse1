package com.example

import com.example.data.AamalCompletion
import com.example.data.AamalCustomActivity
import com.example.receiver.AdhanScheduler
import com.example.ui.AamalCategory
import com.example.ui.AamalProgressPolicy
import com.example.ui.getDefaultAamalList
import com.example.ui.parseAamalActs
import com.example.utils.CountryLocationResolver
import com.example.utils.DeviceLocationAccuracy
import com.example.utils.HijriCalendarHelper
import com.example.utils.HomeDayContextFactory
import com.example.utils.LocationTimeZone
import com.example.utils.PrayerTimeCalculator
import com.example.utils.resolveDeviceLocationAccuracy
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class PhaseOneHomePolicyTest {

    @Test
    fun countryMetadataDoesNotUseSubstringMatching() {
        assertEquals("US", CountryLocationResolver.resolve(null, "Dearborn, USA"))
        assertEquals("AU", CountryLocationResolver.resolve(null, "Sydney, Australia"))
        assertEquals("", CountryLocationResolver.resolve(null, "Madinah"))
        assertEquals("SA", CountryLocationResolver.resolve(null, "Madinah, Saudi Arabia"))
        assertEquals("", CountryLocationResolver.resolve(null, "United States and Canada"))
        assertEquals("", CountryLocationResolver.resolve("not-a-country", "Madinah"))
    }

    @Test
    fun coarsePermissionHasAnApproximateFallback() {
        assertEquals(DeviceLocationAccuracy.HIGH_ACCURACY, resolveDeviceLocationAccuracy(true, true))
        assertEquals(DeviceLocationAccuracy.HIGH_ACCURACY, resolveDeviceLocationAccuracy(true, false))
        assertEquals(DeviceLocationAccuracy.BALANCED_APPROXIMATE, resolveDeviceLocationAccuracy(false, true))
        assertEquals(null, resolveDeviceLocationAccuracy(false, false))
    }

    @Test
    fun prayerInstantsUseLocationZoneRulesAcrossDst() {
        val zone = TimeZone.getTimeZone("America/Detroit")
        val winter = PrayerTimeCalculator.calculateTimeline(
            PrayerTimeCalculator.PrayerDate(2026, 1, 15),
            42.3223,
            -83.1763,
            zone
        )
        val summer = PrayerTimeCalculator.calculateTimeline(
            PrayerTimeCalculator.PrayerDate(2026, 7, 15),
            42.3223,
            -83.1763,
            zone
        )
        assertEquals(-5 * 60 * 60 * 1000, zone.getOffset(winter.instant(PrayerTimeCalculator.PrayerEvent.DHUHR)))
        assertEquals(-4 * 60 * 60 * 1000, zone.getOffset(summer.instant(PrayerTimeCalculator.PrayerEvent.DHUHR)))
        assertNotEquals(winter.times.dhuhrString, summer.times.dhuhrString)
    }

    @Test
    fun nextEventAfterIshaUsesTomorrowsActualFajrInstant() {
        val zone = TimeZone.getTimeZone("Australia/Sydney")
        val todayDate = PrayerTimeCalculator.PrayerDate(2026, 10, 28)
        val tomorrowDate = todayDate.plusDays(1)
        val today = PrayerTimeCalculator.calculateTimeline(todayDate, -33.8688, 151.2093, zone)
        val tomorrow = PrayerTimeCalculator.calculateTimeline(tomorrowDate, -33.8688, 151.2093, zone)
        val next = PrayerTimeCalculator.findNextEvent(
            today,
            tomorrow,
            today.instant(PrayerTimeCalculator.PrayerEvent.MIDNIGHT) + 60_000L
        )
        assertTrue(next.isTomorrow)
        assertEquals(PrayerTimeCalculator.PrayerEvent.FAJR, next.event)
        assertEquals(tomorrow.instant(PrayerTimeCalculator.PrayerEvent.FAJR), next.instant)
    }

    @Test
    fun maghribTransitionAppliesOnlyToActualSelectedToday() {
        val zone = TimeZone.getTimeZone("UTC")
        val currentDate = PrayerTimeCalculator.PrayerDate(2026, 10, 28)
        val timelines = HomeDayContextFactory.calculateTimelines(
            currentDate,
            currentDate,
            24.4672,
            39.6024,
            zone
        )
        val before = HomeDayContextFactory.create(
            nowMillis = timelines.current.instant(PrayerTimeCalculator.PrayerEvent.MAGHRIB) - 60_000L,
            selectedDate = currentDate,
            latitude = 24.4672,
            longitude = 39.6024,
            timeZone = zone,
            hijriOffset = 0,
            timelines = timelines,
            hijriConverter = ::fakeHijri
        )
        val after = HomeDayContextFactory.create(
            nowMillis = timelines.current.instant(PrayerTimeCalculator.PrayerEvent.MAGHRIB) + 60_000L,
            selectedDate = currentDate,
            latitude = 24.4672,
            longitude = 39.6024,
            timeZone = zone,
            hijriOffset = 0,
            timelines = timelines,
            hijriConverter = ::fakeHijri
        )
        val historical = HomeDayContextFactory.create(
            nowMillis = timelines.current.instant(PrayerTimeCalculator.PrayerEvent.MAGHRIB) + 60_000L,
            selectedDate = currentDate.plusDays(-1),
            latitude = 24.4672,
            longitude = 39.6024,
            timeZone = zone,
            hijriOffset = 0,
            hijriConverter = ::fakeHijri
        )
        assertFalse(before.isPastMaghrib)
        assertEquals(currentDate, before.effectiveGregorianDate)
        assertTrue(after.isPastMaghrib)
        assertEquals(currentDate.plusDays(1), after.effectiveGregorianDate)
        assertEquals(after.effectiveGregorianDate.year * 1000 + after.effectiveGregorianDate.dayOfYear(), after.dailyContentDayStamp)
        val afterNow = timelines.current.instant(PrayerTimeCalculator.PrayerEvent.MAGHRIB) + 60_000L
        assertEquals(29, after.guidanceCalendar(afterNow).get(Calendar.DAY_OF_MONTH))
        assertEquals(28, after.weeklyGuidanceCalendar(afterNow).get(Calendar.DAY_OF_MONTH))
        assertFalse(historical.isPastMaghrib)
        assertEquals(currentDate.plusDays(-1), historical.effectiveGregorianDate)
    }

    @Test
    fun locationChangeRebuildsTheSharedContext() {
        val date = PrayerTimeCalculator.PrayerDate(2026, 10, 28)
        val now = date.atStartOfDay(TimeZone.getTimeZone("UTC")).timeInMillis + 12 * 60 * 60 * 1000L
        val dearborn = HomeDayContextFactory.create(
            nowMillis = now,
            selectedDate = date,
            latitude = 42.3223,
            longitude = -83.1763,
            timeZone = TimeZone.getTimeZone("America/Detroit"),
            hijriOffset = 0,
            locationKey = "Dearborn, USA",
            countryCode = "US",
            hijriConverter = ::fakeHijri
        )
        val sydney = HomeDayContextFactory.create(
            nowMillis = now,
            selectedDate = date,
            latitude = -33.8688,
            longitude = 151.2093,
            timeZone = TimeZone.getTimeZone("Australia/Sydney"),
            hijriOffset = 0,
            locationKey = "Sydney, Australia",
            countryCode = "AU",
            hijriConverter = ::fakeHijri
        )
        assertEquals("US", dearborn.countryCode)
        assertEquals("AU", sydney.countryCode)
        assertEquals("America/Detroit", dearborn.timeZoneId)
        assertEquals("Australia/Sydney", sydney.timeZoneId)
        assertNotEquals(
            dearborn.timelines.current.instant(PrayerTimeCalculator.PrayerEvent.FAJR),
            sydney.timelines.current.instant(PrayerTimeCalculator.PrayerEvent.FAJR)
        )
    }

    @Test
    fun allFiveAdhanPreferencesAreIndependent() {
        val enabled = setOf(
            PrayerTimeCalculator.PrayerEvent.FAJR,
            PrayerTimeCalculator.PrayerEvent.ASR,
            PrayerTimeCalculator.PrayerEvent.ISHA
        )
        val selected = AdhanScheduler.enabledAdhanEvents { key ->
            enabled.any { AdhanScheduler.preferenceKey(it) == key }
        }
        assertEquals(enabled.toList(), selected)
        assertEquals(5, PrayerTimeCalculator.obligatoryPrayerEvents.size)
    }

    @Test
    fun perfectTodayIsCountedOnceAndNewCustomIsNotRetroactive() {
        val defaults = getDefaultAamalList()
        val today = utcCalendar(2026, 10, 28)
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val todayKey = dateKey(today)
        val yesterdayKey = dateKey(yesterday)
        val custom = AamalCustomActivity(
            id = "custom_new",
            title = "New custom act",
            dateAdded = todayKey
        )
        val records = defaults.flatMap { act ->
            listOf(
                AamalCompletion(todayKey, act.id, true),
                AamalCompletion(yesterdayKey, act.id, true)
            )
        } + AamalCompletion(todayKey, custom.id, true)

        assertEquals(
            2,
            AamalProgressPolicy.calculateStreak(
                allCompletions = records,
                customAamal = listOf(custom),
                defaultList = defaults,
                today = today
            )
        )
        assertEquals(
            1,
            AamalProgressPolicy.calculateStreak(
                allCompletions = records.filter { it.activityId != custom.id },
                customAamal = listOf(custom),
                defaultList = defaults,
                today = today
            )
        )
    }

    @Test
    fun hiddenCategoriesUseTheSameRequiredSetForPerfectDays() {
        val defaults = getDefaultAamalList()
        val visible = AamalCategory.values().associateWith { it != AamalCategory.ADHKAR }
        val today = utcCalendar(2026, 10, 28)
        val key = dateKey(today)
        val required = AamalProgressPolicy.requiredActivityIds(
            key,
            true,
            defaults,
            emptyList(),
            visible,
            emptyList()
        )
        val records = required.map { AamalCompletion(key, it, true) }
        assertTrue(required.isNotEmpty())
        assertTrue(AamalProgressPolicy.isPerfectDay(key, true, defaults, emptyList(), visible, records))
        assertEquals(1, AamalProgressPolicy.calculateStreak(records, emptyList(), defaults, visible, today))
    }

    @Test
    fun legacyQadrLinesBecomeNonemptyActsWithoutChangingEitherId() {
        val legacyFile = assetFile("Supplications/Aamal/aamal_shab_e_qadr.json")
        val populatedFile = assetFile("Supplications/Aamal/aamal_shabe_qadr.json")
        val legacyJson = JSONObject(legacyFile.readText())
        val populatedJson = JSONObject(populatedFile.readText())
        val legacyActs = parseAamalActs(legacyJson)
        val populatedActs = parseAamalActs(populatedJson)
        assertEquals("aamal_shab_e_qadr", legacyJson.getString("id"))
        assertEquals("aamal_shabe_qadr", populatedJson.getString("id"))
        assertEquals(9, legacyActs.size)
        assertEquals(6, populatedActs.size)
        assertTrue(legacyActs.all { !it.arabicRecitation.isNullOrBlank() })
        assertTrue(populatedActs.all { it.title.isNotBlank() })
    }

    private fun fakeHijri(
        date: PrayerTimeCalculator.PrayerDate,
        offset: Int
    ): HijriCalendarHelper.HijriDate = HijriCalendarHelper.HijriDate(
        day = date.day,
        month = 1,
        monthName = "Muharram",
        year = 1448 + offset
    )

    private fun utcCalendar(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
            clear()
            set(year, month - 1, day, 12, 0, 0)
        }

    private fun dateKey(calendar: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(calendar.time)

    private fun assetFile(relative: String): File = listOf(
        File("src/main/assets", relative),
        File("app/src/main/assets", relative)
    ).firstOrNull { it.isFile } ?: error("Missing test asset: $relative")
}
