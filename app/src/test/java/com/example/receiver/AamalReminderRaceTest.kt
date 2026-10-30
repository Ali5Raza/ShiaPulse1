package com.example.receiver

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.example.utils.ResolvedLocationTimeZone
import com.example.utils.SelectedLocationTimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class AamalReminderRaceTest {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var alarmManager: AlarmManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var ioScheduler: TestCoroutineScheduler
    private lateinit var installationScheduler: TestCoroutineScheduler
    private var enabledGeneration = 0L
    private var scheduleGeneration = 0L

    @Before
    fun setUp() {
        ioScheduler = TestCoroutineScheduler()
        installationScheduler = TestCoroutineScheduler()
        AamalEventScheduler.schedulingDispatcher = StandardTestDispatcher(ioScheduler)
        AamalEventScheduler.installationDispatcher = StandardTestDispatcher(installationScheduler)

        context = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        AamalEventScheduler.cancel(context)
        enabledGeneration = prefs.getLong(ENABLED_GENERATION_KEY, 0L) + 100L
        scheduleGeneration = prefs.getLong(SCHEDULE_GENERATION_KEY, 0L) + 100L
        prefs.edit()
            .putBoolean(ENABLED_KEY, true)
            .putBoolean(AUTO_KEY, false)
            .putInt(HOUR_KEY, 20)
            .putInt(MINUTE_KEY, 0)
            .putLong(ENABLED_GENERATION_KEY, enabledGeneration)
            .putLong(SCHEDULE_GENERATION_KEY, scheduleGeneration)
            .commit()
        notificationManager.cancel(AamalEventScheduler.NOTIFICATION_ID)
    }

    @After
    fun tearDown() {
        drainSchedulers()
        AamalEventScheduler.cancel(context)
        AamalEventScheduler.schedulingDispatcher = Dispatchers.IO
        AamalEventScheduler.installationDispatcher = Dispatchers.Main
    }

    @Test
    fun actualScheduleNextCoroutineRejectsDisableBeforeFinalInstall() {
        val initialToken = currentToken()
        AamalEventScheduler.scheduleNext(context)
        assertTrue(currentToken().scheduleGeneration > initialToken.scheduleGeneration)

        // Run the real IO body only as far as its installation-dispatcher suspension.
        ioScheduler.runCurrent()
        assertEquals(0, scheduledAlarmCount())
        assertTrue(AamalEventScheduler.setEnabled(context, false).persisted)

        // The queued real installation must now reject both the old state and old schedule token.
        installationScheduler.runCurrent()
        assertFalse(AamalEventScheduler.currentEnabled(context))
        assertEquals(0, scheduledAlarmCount())
    }

    @Test
    fun notificationPostAndDisableAreSerializedAtFinalBoundary() {
        val snapshot = locationSnapshot()
        val token = requireNotNull(AamalEventScheduler.beginDelivery(context, currentToken()))
        val enteredFactory = CountDownLatch(1)
        val releaseFactory = CountDownLatch(1)
        val disableStarted = CountDownLatch(1)
        val delivered = AtomicBoolean(false)
        val disabled = AtomicBoolean(false)
        val failure = AtomicReference<Throwable>()

        val deliveryThread = Thread {
            try {
                delivered.set(
                    AamalEventScheduler.runIfDeliveryCurrent(context, token, snapshot) {
                        enteredFactory.countDown()
                        if (!releaseFactory.await(5, TimeUnit.SECONDS)) {
                            throw AssertionError("notification factory was not released")
                        }
                        testNotification()
                    }
                )
            } catch (error: Throwable) {
                failure.compareAndSet(null, error)
            }
        }
        deliveryThread.start()
        assertTrue(enteredFactory.await(5, TimeUnit.SECONDS))

        val disableThread = Thread {
            try {
                disableStarted.countDown()
                disabled.set(AamalEventScheduler.setEnabled(context, false).persisted)
            } catch (error: Throwable) {
                failure.compareAndSet(null, error)
            }
        }
        disableThread.start()
        assertTrue(disableStarted.await(5, TimeUnit.SECONDS))
        releaseFactory.countDown()
        disableThread.join(5_000)
        deliveryThread.join(5_000)

        assertFalse(disableThread.isAlive)
        assertFalse(deliveryThread.isAlive)
        assertNull(failure.get())
        assertTrue(delivered.get())
        assertTrue(disabled.get())
        assertEquals(0, notificationCount())

        var staleFactoryCalled = false
        assertFalse(
            AamalEventScheduler.runIfDeliveryCurrent(context, token, snapshot) {
                staleFactoryCalled = true
                testNotification()
            }
        )
        assertFalse(staleFactoryCalled)
        assertEquals(0, notificationCount())
    }

    @Test
    fun oldAlarmAfterScheduleOnlyAdvanceIsRejectedAndCurrentAlarmSurvives() {
        val oldToken = currentToken()
        val oldIntent = reminderIntent(oldToken)

        // Reminders stay enabled while the real scheduler advances only scheduleGeneration.
        AamalEventScheduler.scheduleNext(context)
        drainSchedulers()
        val currentToken = currentToken()
        assertTrue(AamalEventScheduler.currentEnabled(context))
        assertEquals(oldToken.enabledGeneration, currentToken.enabledGeneration)
        assertTrue(currentToken.scheduleGeneration > oldToken.scheduleGeneration)
        assertEquals(1, scheduledAlarmCount())

        var staleFactoryCalled = false
        assertFalse(
            AamalEventScheduler.runIfDeliveryCurrent(context, oldToken, locationSnapshot()) {
                staleFactoryCalled = true
                testNotification()
            }
        )
        assertFalse(staleFactoryCalled)
        AamalEventReceiver().onReceive(context, oldIntent)

        assertEquals(0, notificationCount())
        assertEquals(1, scheduledAlarmCount())
        assertEquals(currentToken, currentAlarmToken())
        assertEquals(currentToken, AamalEventScheduler.beginDelivery(context, currentToken))
    }

    @Test
    fun concurrentEnableCallsInstallOnlyOneAlarm() {
        assertTrue(AamalEventScheduler.setEnabled(context, false).persisted)
        val start = CyclicBarrier(3)
        val successes = AtomicInteger(0)
        val failure = AtomicReference<Throwable>()
        val threads = List(2) {
            Thread {
                try {
                    start.await(5, TimeUnit.SECONDS)
                    if (AamalEventScheduler.setEnabled(context, true).persisted) {
                        successes.incrementAndGet()
                    }
                } catch (error: Throwable) {
                    failure.compareAndSet(null, error)
                }
            }
        }
        threads.forEach { it.start() }
        start.await(5, TimeUnit.SECONDS)
        threads.forEach { it.join(5_000) }

        drainSchedulers()
        assertNull(failure.get())
        assertTrue(threads.none { it.isAlive })
        assertEquals(2, successes.get())
        assertTrue(AamalEventScheduler.currentEnabled(context))
        assertEquals(1, scheduledAlarmCount())
    }

    @Test
    fun rapidTogglesInvalidateAllQueuedWork() {
        repeat(24) { iteration ->
            assertTrue(AamalEventScheduler.setEnabled(context, false).persisted)
            assertTrue(AamalEventScheduler.setEnabled(context, true).persisted)
            when (iteration % 4) {
                0 -> ioScheduler.runCurrent()
                1 -> installationScheduler.runCurrent()
            }
        }
        assertTrue(AamalEventScheduler.setEnabled(context, false).persisted)
        drainSchedulers()

        assertFalse(AamalEventScheduler.currentEnabled(context))
        assertEquals(0, scheduledAlarmCount())
        assertEquals(0, notificationCount())
    }

    @Test
    fun successfulCommitReturnsSuccessAndInstallsCurrentAlarm() {
        assertTrue(AamalEventScheduler.setEnabled(context, false).persisted)
        val result = AamalEventScheduler.setEnabled(context, true)

        assertTrue(result.persisted)
        assertNotNull(result.token)
        assertTrue(AamalEventScheduler.currentEnabled(context))
        assertTrue(prefs.getBoolean(ENABLED_KEY, false))
        drainSchedulers()
        assertEquals(1, scheduledAlarmCount())
        assertEquals(requireNotNull(result.token).enabledGeneration, currentToken().enabledGeneration)
    }

    @Test
    fun failedCommitReturnsFailureRestoresStateAndDoesNotChangeAlarms() {
        val previousToken = currentToken()
        installAlarm(previousToken)
        val failingDisableContext = FailingCommitContext(context)
        val disableResult = AamalEventScheduler.setEnabled(failingDisableContext, false)

        assertFalse(disableResult.persisted)
        assertNull(disableResult.token)
        assertTrue(AamalEventScheduler.currentEnabled(failingDisableContext))
        assertEquals(previousToken, currentToken())
        assertEquals(1, scheduledAlarmCount())

        assertTrue(AamalEventScheduler.setEnabled(context, false).persisted)
        val failingEnableContext = FailingCommitContext(context)
        val enableResult = AamalEventScheduler.setEnabled(failingEnableContext, true)
        drainSchedulers()

        assertFalse(enableResult.persisted)
        assertNull(enableResult.token)
        assertFalse(AamalEventScheduler.currentEnabled(failingEnableContext))
        assertEquals(0, scheduledAlarmCount())
    }

    @Test
    fun legacyAlarmIsOnlyEligibleBeforeAnyGenerationAdvance() {
        prefs.edit()
            .putBoolean(ENABLED_KEY, true)
            .putLong(ENABLED_GENERATION_KEY, 0L)
            .putLong(SCHEDULE_GENERATION_KEY, 0L)
            .commit()
        val legacyToken = requireNotNull(AamalEventScheduler.beginDelivery(context, null))
        assertEquals(AamalDeliveryToken(0L, 0L), legacyToken)

        AamalEventScheduler.scheduleNextIfCurrent(context, legacyToken)
        drainSchedulers()
        assertEquals(1, scheduledAlarmCount())
        AamalEventReceiver().onReceive(context, legacyIntent())

        assertEquals(0, notificationCount())
        assertEquals(1, scheduledAlarmCount())
        assertNull(AamalEventScheduler.beginDelivery(context, null))
    }

    @Test
    fun persistedStateModelFreshReceiverRejectsOldAlarm() {
        // This is a persisted-state model only, not a claim of real process recreation.
        val oldToken = currentToken()
        installAlarm(oldToken)
        prefs.edit()
            .putBoolean(ENABLED_KEY, false)
            .putLong(ENABLED_GENERATION_KEY, oldToken.enabledGeneration + 1L)
            .putLong(SCHEDULE_GENERATION_KEY, oldToken.scheduleGeneration + 1L)
            .commit()

        AamalEventReceiver().onReceive(context, reminderIntent(oldToken))

        assertFalse(AamalEventScheduler.currentEnabled(context))
        assertEquals(0, scheduledAlarmCount())
        assertEquals(0, notificationCount())
        assertNull(AamalEventScheduler.beginDelivery(context, oldToken))
    }

    private fun drainSchedulers() {
        ioScheduler.advanceUntilIdle()
        installationScheduler.advanceUntilIdle()
    }

    private fun currentToken(): AamalDeliveryToken = AamalDeliveryToken(
        enabledGeneration = prefs.getLong(ENABLED_GENERATION_KEY, 0L),
        scheduleGeneration = prefs.getLong(SCHEDULE_GENERATION_KEY, 0L)
    )

    private fun currentAlarmToken(): AamalDeliveryToken {
        val pendingIntent = requireNotNull(
            shadowOf(alarmManager).scheduledAlarms.single().operation
        )
        val intent = shadowOf(pendingIntent).savedIntent
        return AamalDeliveryToken(
            enabledGeneration = intent.getLongExtra(AamalEventScheduler.EXTRA_ENABLED_GENERATION, -1L),
            scheduleGeneration = intent.getLongExtra(AamalEventScheduler.EXTRA_SCHEDULE_GENERATION, -1L)
        )
    }

    private fun locationSnapshot(): ResolvedLocationTimeZone =
        SelectedLocationTimeZone.fromPreferences(context)

    private fun reminderIntent(token: AamalDeliveryToken): Intent =
        Intent(context, AamalEventReceiver::class.java).apply {
            action = AamalEventScheduler.ACTION_AAMAL_REMINDER
            putExtra(AamalEventScheduler.EXTRA_ENABLED_GENERATION, token.enabledGeneration)
            putExtra(AamalEventScheduler.EXTRA_SCHEDULE_GENERATION, token.scheduleGeneration)
        }

    private fun legacyIntent(): Intent =
        Intent(context, AamalEventReceiver::class.java).apply {
            action = AamalEventScheduler.ACTION_AAMAL_REMINDER
        }

    private fun installAlarm(token: AamalDeliveryToken) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AamalEventScheduler.NOTIFICATION_ID,
            reminderIntent(token),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 60_000L,
            pendingIntent
        )
    }

    private fun testNotification(): Notification =
        NotificationCompat.Builder(context, "aamal_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Aamal test")
            .build()

    private fun scheduledAlarmCount(): Int = shadowOf(alarmManager).scheduledAlarms.size

    private fun notificationCount(): Int = shadowOf(notificationManager).allNotifications.size

    /** Simulates commit updating memory but reporting durable failure for exactly one commit. */
    private class FailingCommitContext(base: Context) : ContextWrapper(base) {
        private val failNextCommit = AtomicBoolean(true)

        override fun getApplicationContext(): Context = this

        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            val delegate = super.getSharedPreferences(name, mode)
            return if (name == PREFS_NAME) {
                FailingSharedPreferences(delegate, failNextCommit)
            } else {
                delegate
            }
        }
    }

    private class FailingSharedPreferences(
        private val delegate: SharedPreferences,
        private val failNextCommit: AtomicBoolean
    ) : SharedPreferences by delegate {
        override fun edit(): SharedPreferences.Editor =
            FailingEditor(delegate.edit(), failNextCommit)
    }

    private class FailingEditor(
        private val delegate: SharedPreferences.Editor,
        private val failNextCommit: AtomicBoolean
    ) : SharedPreferences.Editor by delegate {
        override fun commit(): Boolean {
            if (failNextCommit.compareAndSet(true, false)) {
                delegate.apply()
                return false
            }
            return delegate.commit()
        }
    }

    private companion object {
        const val PREFS_NAME = "shia_tracker_prefs"
        const val ENABLED_KEY = "aamal_notif_enabled"
        const val AUTO_KEY = "aamal_notif_auto"
        const val HOUR_KEY = "aamal_notif_hour"
        const val MINUTE_KEY = "aamal_notif_minute"
        const val ENABLED_GENERATION_KEY = "aamal_notif_enabled_generation"
        const val SCHEDULE_GENERATION_KEY = "aamal_notif_schedule_generation"
    }
}
