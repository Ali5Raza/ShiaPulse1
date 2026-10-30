package com.example.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.PrayerDatabase
import com.example.ui.AamalCategory
import com.example.ui.AamalProgressPolicy
import com.example.ui.getDefaultAamalList
import com.example.utils.SelectedLocationTimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AamalEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (AamalReminderPolicy.actionFor(intent.action)) {
            AamalReminderAction.RESCHEDULE_ONLY -> {
                AamalEventScheduler.scheduleNext(context)
                return
            }
            AamalReminderAction.IGNORE -> return
            AamalReminderAction.DELIVER -> Unit
        }

        val hasEnabledGeneration = intent.hasExtra(AamalEventScheduler.EXTRA_ENABLED_GENERATION)
        val hasScheduleGeneration = intent.hasExtra(AamalEventScheduler.EXTRA_SCHEDULE_GENERATION)
        val expectedToken = when {
            hasEnabledGeneration && hasScheduleGeneration -> AamalDeliveryToken(
                enabledGeneration = intent.getLongExtra(
                    AamalEventScheduler.EXTRA_ENABLED_GENERATION,
                    0L
                ),
                scheduleGeneration = intent.getLongExtra(
                    AamalEventScheduler.EXTRA_SCHEDULE_GENERATION,
                    0L
                )
            )
            // Partial generation payloads are malformed and must not be treated as current.
            hasEnabledGeneration || hasScheduleGeneration -> return
            // Tokenless legacy alarms are handled deliberately by beginDelivery: only the initial
            // all-zero state is eligible, and disabled state always wins.
            else -> null
        }
        val deliveryToken = AamalEventScheduler.beginDelivery(
            context,
            expectedToken
        ) ?: return
        val appContext = context.applicationContext
        val locationSnapshot = SelectedLocationTimeZone.fromPreferences(appContext)
        val nowMillis = System.currentTimeMillis()
        val todayStr = AamalReminderPolicy.dateKey(nowMillis, locationSnapshot.timeZone)
        val pendingResult = goAsync()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val dao = PrayerDatabase.getDatabase(appContext).prayerDao()
                val allCompletions = dao.getAllAamalCompletionsSync()
                val customAamal = dao.getAllCustomAamal().first()
                val filterPrefs = appContext.getSharedPreferences("aamal_filters", Context.MODE_PRIVATE)
                val visibleCategories = AamalCategory.values().associateWith { category ->
                    filterPrefs.getBoolean("cat_${category.name}", true)
                }
                val defaultList = getDefaultAamalList()
                val required = AamalProgressPolicy.requiredActivityIds(
                    date = todayStr,
                    isToday = true,
                    defaultList = defaultList,
                    customAamal = customAamal,
                    visibleCategories = visibleCategories,
                    allCompletions = allCompletions
                )
                val hasPending = required.isNotEmpty() && !AamalProgressPolicy.isPerfectDay(
                    date = todayStr,
                    isToday = true,
                    defaultList = defaultList,
                    customAamal = customAamal,
                    visibleCategories = visibleCategories,
                    allCompletions = allCompletions
                )

                val latestSnapshot = SelectedLocationTimeZone.fromPreferences(appContext)
                val locationUnchanged = latestSnapshot.timeZone.id == locationSnapshot.timeZone.id &&
                    latestSnapshot.source == locationSnapshot.source
                if (hasPending && locationUnchanged) {
                    AamalEventScheduler.runIfDeliveryCurrent(
                        context = appContext,
                        token = deliveryToken,
                        locationSnapshot = locationSnapshot
                    ) {
                        buildNotification(appContext)
                    }
                }

                // A stale delivery must not overwrite a newer enable/disable or reminder schedule.
                AamalEventScheduler.scheduleNextIfCurrent(appContext, deliveryToken)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun buildNotification(context: Context): Notification {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "aamal_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Aamal Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = AppPendingIntents.aamalContent(context)
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pending Aamal-e-Rozana")
            .setContentText("It's your usual time to complete your daily Aamals. Keep up the good work!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }
}
