package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AamalCompletion
import com.example.data.AamalCustomActivity
import com.example.receiver.AamalEventScheduler
import com.example.receiver.AamalReminderPolicy
import com.example.receiver.AdhanScheduler
import com.example.receiver.AppPendingIntents
import com.example.ui.AamalHistoryPolicy
import com.example.ui.AamalProgressPolicy
import com.example.ui.AamalCategory
import com.example.ui.NotificationPermissionPolicy
import com.example.ui.NotificationPermissionTarget
import com.example.ui.calculateNextPrayer
import com.example.ui.getDefaultAamalList
import com.example.utils.AppRemoteConfig
import com.example.utils.CountryDetectionService
import com.example.utils.DefaultHijriConfigurationService
import com.example.utils.HijriConfig
import com.example.utils.HomeDayContextFactory
import com.example.utils.LocationTimeZone
import com.example.utils.LocationTimeZoneSource
import com.example.utils.PrayerTimeCalculator
import com.example.utils.RemoteConfigurationService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 26, 33])
class PhaseOneOneBlockerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun permissionResultIsTargetOnlyAndDenialIsRecoverable() {
        val enabled = mutableListOf<String>()
        NotificationPermissionPolicy.handleResult(
            NotificationPermissionTarget.AAMAL,
            true,
            { enabled += "adhan" },
            { enabled += "aamal" },
            { enabled += "calendar" }
        )
        NotificationPermissionPolicy.handleResult(
            NotificationPermissionTarget.ADHAN,
            false,
            { enabled += "adhan-denied" },
            { enabled += "aamal-denied" },
            { enabled += "calendar-denied" }
        )
        NotificationPermissionPolicy.handleResult(
            NotificationPermissionTarget.CALENDAR,
            true,
            { enabled += "adhan" },
            { enabled += "aamal" },
            { enabled += "calendar" }
        )
        assertEquals(listOf("aamal", "calendar"), enabled)
        assertFalse(NotificationPermissionPolicy.shouldRequestRuntimePermission(32, false))
        assertTrue(NotificationPermissionPolicy.shouldRequestRuntimePermission(33, false))
        assertFalse(NotificationPermissionPolicy.shouldRequestRuntimePermission(33, true))
    }

    @Test
    fun allFivePrayerControlsUseSchedulerKeysAndIndependentSelection() {
        val events = PrayerTimeCalculator.obligatoryPrayerEvents
        assertEquals(listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"), AdhanScheduler.prayerNamesForSettings())
        assertEquals(5, events.map { AdhanScheduler.preferenceKey(it) }.distinct().size)
        val expected = setOf(
            PrayerTimeCalculator.PrayerEvent.FAJR,
            PrayerTimeCalculator.PrayerEvent.ASR,
            PrayerTimeCalculator.PrayerEvent.ISHA
        )
        val selected = AdhanScheduler.enabledAdhanEvents { key ->
            expected.any { AdhanScheduler.preferenceKey(it) == key }
        }
        assertEquals(expected.toList(), selected)
    }

    @Test
    fun notificationAndWidgetPendingIntentsRetainRoutesButHaveDistinctIdentities() {
        val fajrStop = AppPendingIntents.adhanStop(context, "Fajr")
        val dhuhrStop = AppPendingIntents.adhanStop(context, "Dhuhr")
        assertNotEquals(fajrStop, dhuhrStop)
        assertNotEquals(
            shadowOf(fajrStop).savedIntent.action,
            shadowOf(dhuhrStop).savedIntent.action
        )

        val aamal = AppPendingIntents.aamalContent(context)
        val widget = AppPendingIntents.hijriWidgetContent(context)
        assertNotEquals(aamal, widget)
        assertEquals("aamal_tracker", shadowOf(aamal).savedIntent.getStringExtra("open_screen"))
        assertEquals(null, shadowOf(widget).savedIntent.getStringExtra("open_screen"))

        // These strings have the same Java hashCode; action identity still separates them.
        val first = AppPendingIntents.quranSharedKhatamContent(context, "FB")
        val second = AppPendingIntents.quranSharedKhatamContent(context, "Ea")
        assertNotEquals(first, second)
        assertNotEquals(shadowOf(first).savedIntent.action, shadowOf(second).savedIntent.action)
    }

    @Test
    fun reminderPolicyUsesSelectedZoneForLearningDatesAndDst() {
        val deviceZone = TimeZone.getTimeZone("America/New_York")
        val invalid = LocationTimeZone.resolveDetailed("Invalid/Zone", null, true, deviceZone)
        assertEquals(LocationTimeZoneSource.DEVICE_INVALID_SELECTION, invalid.source)
        assertEquals(deviceZone.id, invalid.timeZone.id)
        val fixed = LocationTimeZone.resolveDetailed(null, 5.5, true, deviceZone)
        assertEquals(LocationTimeZoneSource.FIXED_OFFSET, fixed.source)
        assertEquals(5.5 * 60 * 60 * 1000, fixed.timeZone.getOffset(0L).toDouble(), 0.0)
        val noSelection = LocationTimeZone.resolveDetailed(null, null, false, deviceZone)
        assertEquals(LocationTimeZoneSource.DEVICE_NO_SELECTION, noSelection.source)

        val karachi = TimeZone.getTimeZone("Asia/Karachi")
        val first = localMillis(karachi, 2026, 10, 28, 20, 15)
        val second = localMillis(karachi, 2026, 10, 29, 20, 45)
        assertEquals(20, AamalReminderPolicy.learnedReminderHour(listOf(first, second), karachi))
        val midnightInstant = localMillis(TimeZone.getTimeZone("UTC"), 2026, 10, 28, 19, 30)
        assertEquals("2026-10-29", AamalReminderPolicy.dateKey(midnightInstant, karachi))
        assertEquals("2026-10-28", AamalReminderPolicy.dateKey(midnightInstant, TimeZone.getTimeZone("UTC")))

        val newYork = TimeZone.getTimeZone("America/New_York")
        val beforeSpringDst = localMillis(newYork, 2026, 3, 8, 1, 30)
        val nextSpring = AamalReminderPolicy.nextTriggerMillis(beforeSpringDst, 1, 30, newYork)
        assertEquals(23L * 60L * 60L * 1000L, nextSpring - beforeSpringDst)
        val beforeFallDst = localMillis(newYork, 2026, 10, 31, 3, 30)
        val nextFall = AamalReminderPolicy.nextTriggerMillis(beforeFallDst, 3, 30, newYork)
        assertEquals(25L * 60L * 60L * 1000L, nextFall - beforeFallDst)
    }

    @Test
    fun bootAndPackageRestoreOnlyRescheduleAamal() {
        assertEquals(
            com.example.receiver.AamalReminderAction.RESCHEDULE_ONLY,
            AamalReminderPolicy.actionFor(android.content.Intent.ACTION_BOOT_COMPLETED)
        )
        assertEquals(
            com.example.receiver.AamalReminderAction.RESCHEDULE_ONLY,
            AamalReminderPolicy.actionFor(android.content.Intent.ACTION_MY_PACKAGE_REPLACED)
        )
        assertEquals(
            com.example.receiver.AamalReminderAction.DELIVER,
            AamalReminderPolicy.actionFor(AamalEventScheduler.ACTION_AAMAL_REMINDER)
        )
        assertEquals(
            com.example.receiver.AamalReminderAction.IGNORE,
            AamalReminderPolicy.actionFor("unrelated.action")
        )
    }

    @Test
    fun staleHomeTimelineCannotBeReusedForAnotherLocationOrZone() {
        val date = PrayerTimeCalculator.PrayerDate(2026, 10, 28)
        val oldZone = TimeZone.getTimeZone("UTC")
        val newZone = TimeZone.getTimeZone("Asia/Karachi")
        val now = date.atStartOfDay(newZone).timeInMillis + 12 * 60 * 60 * 1000L
        val stale = HomeDayContextFactory.calculateTimelines(date, date, 0.0, 0.0, oldZone)
        val contextSnapshot = HomeDayContextFactory.create(
            nowMillis = now,
            selectedDate = date,
            latitude = 24.8607,
            longitude = 67.0011,
            timeZone = newZone,
            hijriOffset = 0,
            timelines = stale,
            hijriConverter = { d, _ -> com.example.utils.HijriCalendarHelper.HijriDate(1, 1, "Muharram", d.year) }
        )
        val expected = PrayerTimeCalculator.calculateTimeline(date, 24.8607, 67.0011, newZone)
        assertEquals(newZone.id, contextSnapshot.timelines.timeZoneId)
        assertEquals(24.8607, contextSnapshot.timelines.latitude, 0.0)
        assertEquals(expected, contextSnapshot.timelines.current)
    }

    @Test
    fun deletedCustomHistoryRestoresRealMetadataWithoutChangingProgressInputs() {
        val deletedId = "custom_deleted"
        val current = AamalCustomActivity("custom_current", "Current act", dateAdded = "2026-10-28")
        val deleted = AamalCustomActivity(deletedId, "Actual saved title", dateAdded = "2026-10-20")
        val completions = listOf(
            AamalCompletion("2026-10-27", deletedId, true),
            AamalCompletion("2026-10-27", current.id, true)
        )
        val merged = AamalHistoryPolicy.mergeHistoricalActivities(
            listOf(current),
            completions
        ) { id -> if (id == deletedId) deleted else null }
        val historical = merged.single { it.id == deletedId }
        assertEquals("Actual saved title", historical.title)
        assertFalse(historical.isEnabled)
        assertEquals("Actual saved title", AamalHistoryPolicy.displayTitle(historical, "en"))

        val fallback = AamalHistoryPolicy.mergeHistoricalActivities(emptyList(), completions) { null }
            .single { it.id == deletedId }
        assertEquals("Deleted custom act", AamalHistoryPolicy.displayTitle(fallback, "en"))
        assertEquals("حذف شدہ کسٹم عمل", AamalHistoryPolicy.displayTitle(fallback, "ur"))

        val required = AamalProgressPolicy.requiredActivityIds(
            "2026-10-27",
            false,
            emptyList(),
            emptyList(),
            AamalCategory.values().associateWith { true },
            completions
        )
        assertTrue(deletedId in required)
        assertEquals(2, required.size)
        assertEquals(2, completions.count { it.isCompleted && it.activityId in required })

        val defaults = getDefaultAamalList()
        val streakToday = localMillis(TimeZone.getTimeZone("UTC"), 2026, 10, 28, 12, 0)
        val streakCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = streakToday
        }
        val streakRecords = defaults.map { AamalCompletion("2026-10-27", it.id, true) } +
            AamalCompletion("2026-10-27", deletedId, true)
        val beforeDeletion = AamalProgressPolicy.calculateStreak(
            streakRecords,
            listOf(deleted),
            defaults,
            today = streakCalendar
        )
        val afterDeletion = AamalProgressPolicy.calculateStreak(
            streakRecords,
            emptyList(),
            defaults,
            today = streakCalendar
        )
        assertEquals(beforeDeletion, afterDeletion)
    }

    @Test
    fun tomorrowFajrLabelUsesExistingUrduAndArabicLocalization() {
        val zone = TimeZone.getTimeZone("Asia/Karachi")
        val date = PrayerTimeCalculator.PrayerDate(2026, 10, 28)
        val today = PrayerTimeCalculator.calculateTimeline(date, 24.8607, 67.0011, zone)
        val tomorrow = PrayerTimeCalculator.calculateTimeline(date.plusDays(1), 24.8607, 67.0011, zone)
        val afterMidnight = today.instant(PrayerTimeCalculator.PrayerEvent.MIDNIGHT) + 60_000L
        assertEquals("فجر (کل)", calculateNextPrayer(today, tomorrow, afterMidnight, "ur").nextPrayerName)
        assertEquals("الفجر (غداً)", calculateNextPrayer(today, tomorrow, afterMidnight, "ar").nextPrayerName)
    }

    @Test
    fun delayedConfigurationResultCannotOverwriteNewerResult() = runBlocking {
        val preferences = context.getSharedPreferences("shiapulse_prefs", Context.MODE_PRIVATE)
        preferences.edit().putBoolean("hijri_auto_sync", true).putInt("resolved_hijri_offset", 0).commit()
        val delayed = CompletableDeferred<AppRemoteConfig>()
        var fetchCount = 0
        val remote = object : RemoteConfigurationService {
            override suspend fun fetchRemoteConfig(): AppRemoteConfig {
                fetchCount++
                return if (fetchCount == 1) {
                    delayed.await()
                } else {
                    AppRemoteConfig(hijri_config = HijriConfig(defaultOffset = 2, lastUpdated = "new"))
                }
            }

            override suspend fun saveRemoteConfig(config: AppRemoteConfig): Boolean = true
            override fun getCachedConfig(): AppRemoteConfig = AppRemoteConfig()
            override fun startRealtimeSync(onConfigUpdated: (AppRemoteConfig) -> Unit) = Unit
        }
        val country = object : CountryDetectionService {
            override fun detectCountry(context: Context?): String = "PK"
        }
        val service = DefaultHijriConfigurationService(country, remote)
        val oldResult = async(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
            service.resolveAndApplyOffset(context)
        }
        val newResult = service.resolveAndApplyOffset(context)
        assertEquals(2, newResult)
        delayed.complete(AppRemoteConfig(hijri_config = HijriConfig(defaultOffset = 9, lastUpdated = "old")))
        oldResult.await()
        assertEquals(2, preferences.getInt("resolved_hijri_offset", -1))
    }

    @Test
    fun delayedRealtimeStyleResultCannotOverwriteNewerResult() {
        val preferences = context.getSharedPreferences("shiapulse_prefs", Context.MODE_PRIVATE)
        preferences.edit().putBoolean("hijri_auto_sync", true).commit()
        val firstDetectionStarted = CountDownLatch(1)
        val releaseFirstDetection = CountDownLatch(1)
        var detectionCount = 0
        val country = object : CountryDetectionService {
            override fun detectCountry(context: Context?): String {
                detectionCount++
                if (detectionCount == 1) {
                    firstDetectionStarted.countDown()
                    releaseFirstDetection.await(5, TimeUnit.SECONDS)
                }
                return "PK"
            }
        }
        val remote = object : RemoteConfigurationService {
            override suspend fun fetchRemoteConfig(): AppRemoteConfig = AppRemoteConfig()
            override suspend fun saveRemoteConfig(config: AppRemoteConfig): Boolean = true
            override fun getCachedConfig(): AppRemoteConfig = AppRemoteConfig()
            override fun startRealtimeSync(onConfigUpdated: (AppRemoteConfig) -> Unit) = Unit
        }
        val service = DefaultHijriConfigurationService(country, remote)
        val oldConfig = AppRemoteConfig(hijri_config = HijriConfig(defaultOffset = 9, lastUpdated = "old"))
        val newConfig = AppRemoteConfig(hijri_config = HijriConfig(defaultOffset = 3, lastUpdated = "new"))
        val oldThread = Thread { service.applyRemoteConfigDirectly(context, oldConfig) }
        oldThread.start()
        try {
            assertTrue(firstDetectionStarted.await(5, TimeUnit.SECONDS))
            assertEquals(3, service.applyRemoteConfigDirectly(context, newConfig))
        } finally {
            releaseFirstDetection.countDown()
        }
        oldThread.join(5000)
        assertFalse(oldThread.isAlive)
        assertEquals(3, preferences.getInt("resolved_hijri_offset", -1))
    }

    private fun localMillis(
        zone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long = Calendar.getInstance(zone).apply {
        clear()
        set(year, month - 1, day, hour, minute, 0)
    }.timeInMillis
}
