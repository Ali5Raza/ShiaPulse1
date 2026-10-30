package com.example.receiver

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.example.data.PrayerDatabase
import com.example.utils.ResolvedLocationTimeZone
import com.example.utils.SelectedLocationTimeZone
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Identifies one enabled epoch and one scheduled alarm within that epoch. */
internal data class AamalDeliveryToken(
    val enabledGeneration: Long,
    val scheduleGeneration: Long
)

internal data class AamalEnableResult(
    val persisted: Boolean,
    val token: AamalDeliveryToken? = null
)

object AamalEventScheduler {
    const val ACTION_AAMAL_REMINDER = "com.example.action.AAMAL_DAILY_REMINDER"
    internal const val EXTRA_ENABLED_GENERATION =
        "com.example.action.AAMAL_DAILY_REMINDER.ENABLED_GENERATION"
    internal const val EXTRA_SCHEDULE_GENERATION =
        "com.example.action.AAMAL_DAILY_REMINDER.SCHEDULE_GENERATION"
    internal const val NOTIFICATION_ID = 3003

    private const val TAG = "AamalEventScheduler"
    private const val PREFS_NAME = "shia_tracker_prefs"
    private const val ENABLED_KEY = "aamal_notif_enabled"
    private const val AUTO_KEY = "aamal_notif_auto"
    private const val HOUR_KEY = "aamal_notif_hour"
    private const val MINUTE_KEY = "aamal_notif_minute"
    private const val ENABLED_GENERATION_KEY = "aamal_notif_enabled_generation"
    private const val SCHEDULE_GENERATION_KEY = "aamal_notif_schedule_generation"
    private val initialLegacyToken = AamalDeliveryToken(0L, 0L)

    /*
     * AlarmManager and NotificationManager do not provide an atomic check-and-set operation with
     * SharedPreferences. All in-process Aamal state transitions and side effects therefore share
     * this lock. Persisted generations also reject stale intents after scheduler/receiver recreation.
     */
    private val stateLock = Any()

    /* Internal dispatchers make the real coroutine deterministic in tests; production is unchanged. */
    @Volatile
    internal var schedulingDispatcher: CoroutineDispatcher = Dispatchers.IO

    @Volatile
    internal var installationDispatcher: CoroutineDispatcher = Dispatchers.Main

    private data class ScheduleRequest(
        val token: AamalDeliveryToken,
        val isAuto: Boolean,
        val locationSnapshot: ResolvedLocationTimeZone
    )

    private data class PreferenceSnapshot(
        val enabled: Boolean,
        val enabledGeneration: Long,
        val scheduleGeneration: Long
    )

    /**
     * Persists an enable/disable transition before starting or cancelling reminder work.
     *
     * The synchronous commit is deliberate. Moving the transition to an uncoordinated background
     * job would reopen ordering and process-durability races. If persistence fails, the previous
     * in-memory snapshot is restored, no alarm/notification action is taken, and failure is returned.
     */
    internal fun setEnabled(context: Context, enabled: Boolean): AamalEnableResult {
        val appContext = context.applicationContext
        val result = synchronized(stateLock) {
            val prefs = preferences(appContext)
            val previous = PreferenceSnapshot(
                enabled = prefs.getBoolean(ENABLED_KEY, true),
                enabledGeneration = prefs.getLong(ENABLED_GENERATION_KEY, 0L),
                scheduleGeneration = prefs.getLong(SCHEDULE_GENERATION_KEY, 0L)
            )
            val enabledGeneration = if (previous.enabled == enabled) {
                previous.enabledGeneration
            } else {
                nextGeneration(previous.enabledGeneration)
            }
            val token = AamalDeliveryToken(
                enabledGeneration = enabledGeneration,
                scheduleGeneration = nextGeneration(previous.scheduleGeneration)
            )
            val committed = prefs.edit()
                .putBoolean(ENABLED_KEY, enabled)
                .putLong(ENABLED_GENERATION_KEY, token.enabledGeneration)
                .putLong(SCHEDULE_GENERATION_KEY, token.scheduleGeneration)
                .commit()

            if (!committed) {
                val restored = restoreLocked(prefs, previous)
                if (!restored) {
                    Log.w(TAG, "Preference commit and rollback both failed; no reminder action taken")
                }
                AamalEnableResult(persisted = false)
            } else {
                if (!enabled) {
                    // A scheduler holding the lock finishes first and is then cancelled; a later
                    // scheduler sees the persisted disabled state and cannot install an alarm.
                    cancelAlarmAndNotificationLocked(appContext)
                }
                AamalEnableResult(persisted = true, token = token)
            }
        }

        // An older enable must not schedule after a newer state or schedule transition.
        if (enabled && result.persisted) {
            scheduleNextIfCurrent(appContext, checkNotNull(result.token))
        }
        return result
    }

    internal fun currentEnabled(context: Context): Boolean {
        val appContext = context.applicationContext
        return synchronized(stateLock) {
            preferences(appContext).getBoolean(ENABLED_KEY, true)
        }
    }

    fun scheduleNext(context: Context) {
        scheduleNextInternal(context.applicationContext, expectedToken = null)
    }

    /** Starts the next schedule only if the supplied transition/delivery is still current. */
    internal fun scheduleNextIfCurrent(context: Context, expectedToken: AamalDeliveryToken) {
        scheduleNextInternal(context.applicationContext, expectedToken)
    }

    private fun scheduleNextInternal(context: Context, expectedToken: AamalDeliveryToken?) {
        val request = synchronized(stateLock) {
            val prefs = preferences(context)
            val currentToken = currentTokenLocked(prefs)
            when {
                !prefs.getBoolean(ENABLED_KEY, true) -> {
                    if (!invalidateScheduleGenerationLocked(prefs)) {
                        Log.w(TAG, "Could not persist schedule invalidation while disabled")
                    }
                    cancelAlarmAndNotificationLocked(context)
                    null
                }
                expectedToken != null && expectedToken != currentToken -> null
                else -> {
                    val requestToken = currentToken.copy(
                        scheduleGeneration = nextGeneration(currentToken.scheduleGeneration)
                    )
                    val committed = prefs.edit()
                        .putLong(SCHEDULE_GENERATION_KEY, requestToken.scheduleGeneration)
                        .commit()
                    if (!committed) {
                        Log.w(TAG, "Could not persist a new schedule generation; alarm not installed")
                        null
                    } else {
                        ScheduleRequest(
                            token = requestToken,
                            isAuto = prefs.getBoolean(AUTO_KEY, true),
                            // One selected-location snapshot is shared by learning and trigger math.
                            locationSnapshot = SelectedLocationTimeZone.fromPreferences(context)
                        )
                    }
                }
            }
        } ?: return

        GlobalScope.launch(schedulingDispatcher) {
            val targetHour: Int
            val targetMinute: Int
            if (request.isAuto) {
                val dao = PrayerDatabase.getDatabase(context).prayerDao()
                val allActs = dao.getAllAamalCompletionsSync().filter { it.isCompleted }
                targetHour = AamalReminderPolicy.learnedReminderHour(
                    allActs.map { it.timestamp },
                    request.locationSnapshot.timeZone
                )
                targetMinute = 0
            } else {
                val prefs = preferences(context)
                targetHour = prefs.getInt(HOUR_KEY, AamalReminderPolicy.DEFAULT_HOUR)
                targetMinute = prefs.getInt(MINUTE_KEY, AamalReminderPolicy.DEFAULT_MINUTE)
            }

            withContext(installationDispatcher) {
                scheduleExact(
                    context = context,
                    request = request,
                    hour = targetHour,
                    minute = targetMinute,
                    nowMillis = System.currentTimeMillis()
                )
            }
        }
    }

    private fun scheduleExact(
        context: Context,
        request: ScheduleRequest,
        hour: Int,
        minute: Int,
        nowMillis: Long
    ) {
        val timeToSchedule = AamalReminderPolicy.nextTriggerMillis(
            nowMillis,
            hour,
            minute,
            request.locationSnapshot.timeZone
        )

        runIfScheduleCurrent(
            context = context,
            token = request.token,
            locationSnapshot = request.locationSnapshot
        ) {
            cancelLegacy(context)
            val pendingIntent = reminderPendingIntent(
                context,
                PendingIntent.FLAG_UPDATE_CURRENT,
                request.token
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !alarmManager.canScheduleExactAlarms()
                ) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeToSchedule,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeToSchedule,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // Existing fallback for devices where exact-alarm permission is unavailable.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeToSchedule,
                    pendingIntent
                )
            }
        }
    }

    /** The final alarm-install boundary, serialized with every enabled/schedule transition. */
    internal fun runIfScheduleCurrent(
        context: Context,
        token: AamalDeliveryToken,
        locationSnapshot: ResolvedLocationTimeZone,
        action: () -> Unit
    ): Boolean {
        val appContext = context.applicationContext
        return synchronized(stateLock) {
            val prefs = preferences(appContext)
            if (!isCurrentLocked(prefs, token, locationSnapshot, appContext)) {
                false
            } else {
                action()
                true
            }
        }
    }

    /**
     * Validates both generations at receiver entry. Tokenless alarms are accepted only in the
     * initial all-zero legacy state. A stale intent is rejected without cancelling the current PI.
     */
    internal fun beginDelivery(
        context: Context,
        expectedToken: AamalDeliveryToken?
    ): AamalDeliveryToken? {
        val appContext = context.applicationContext
        return synchronized(stateLock) {
            val prefs = preferences(appContext)
            if (!prefs.getBoolean(ENABLED_KEY, true)) {
                cancelAlarmAndNotificationLocked(appContext)
                null
            } else {
                val currentToken = currentTokenLocked(prefs)
                when {
                    expectedToken == null && currentToken == initialLegacyToken -> currentToken
                    expectedToken == currentToken -> currentToken
                    else -> null
                }
            }
        }
    }

    /** The final notification boundary; both generations are required to remain current. */
    internal fun runIfDeliveryCurrent(
        context: Context,
        token: AamalDeliveryToken,
        locationSnapshot: ResolvedLocationTimeZone,
        notificationFactory: () -> Notification
    ): Boolean {
        val appContext = context.applicationContext
        return synchronized(stateLock) {
            val prefs = preferences(appContext)
            if (!isCurrentLocked(prefs, token, locationSnapshot, appContext)) {
                false
            } else {
                val notificationManager =
                    appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notificationFactory())
                true
            }
        }
    }

    private fun isCurrentLocked(
        prefs: SharedPreferences,
        token: AamalDeliveryToken,
        locationSnapshot: ResolvedLocationTimeZone,
        appContext: Context
    ): Boolean {
        if (!prefs.getBoolean(ENABLED_KEY, true)) return false
        if (currentTokenLocked(prefs) != token) return false
        val latestSnapshot = SelectedLocationTimeZone.fromPreferences(appContext)
        return latestSnapshot.timeZone.id == locationSnapshot.timeZone.id &&
            latestSnapshot.source == locationSnapshot.source
    }

    private fun currentTokenLocked(prefs: SharedPreferences): AamalDeliveryToken =
        AamalDeliveryToken(
            enabledGeneration = prefs.getLong(ENABLED_GENERATION_KEY, 0L),
            scheduleGeneration = prefs.getLong(SCHEDULE_GENERATION_KEY, 0L)
        )

    private fun reminderPendingIntent(
        context: Context,
        mutationFlag: Int,
        token: AamalDeliveryToken? = null
    ): PendingIntent {
        val intent = Intent(context, AamalEventReceiver::class.java).apply {
            action = ACTION_AAMAL_REMINDER
            token?.let {
                putExtra(EXTRA_ENABLED_GENERATION, it.enabledGeneration)
                putExtra(EXTRA_SCHEDULE_GENERATION, it.scheduleGeneration)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            intent,
            mutationFlag or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelLegacy(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val legacy = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            Intent(context, AamalEventReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        legacy?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun cancelAlarmAndNotificationLocked(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            Intent(context, AamalEventReceiver::class.java).apply {
                action = ACTION_AAMAL_REMINDER
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
        cancelLegacy(context)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        synchronized(stateLock) {
            if (!invalidateScheduleGenerationLocked(preferences(appContext))) {
                Log.w(TAG, "Could not persist schedule invalidation during cancellation")
            }
            cancelAlarmAndNotificationLocked(appContext)
        }
    }

    private fun invalidateScheduleGenerationLocked(prefs: SharedPreferences): Boolean {
        val next = nextGeneration(prefs.getLong(SCHEDULE_GENERATION_KEY, 0L))
        return prefs.edit().putLong(SCHEDULE_GENERATION_KEY, next).commit()
    }

    private fun restoreLocked(prefs: SharedPreferences, snapshot: PreferenceSnapshot): Boolean =
        prefs.edit()
            .putBoolean(ENABLED_KEY, snapshot.enabled)
            .putLong(ENABLED_GENERATION_KEY, snapshot.enabledGeneration)
            .putLong(SCHEDULE_GENERATION_KEY, snapshot.scheduleGeneration)
            .commit()

    private fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun nextGeneration(current: Long): Long =
        if (current == Long.MAX_VALUE) 1L else current + 1L
}
