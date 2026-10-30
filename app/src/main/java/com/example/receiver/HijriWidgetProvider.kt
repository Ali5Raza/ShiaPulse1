package com.example.receiver

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R
import com.example.data.MasoomeenData
import com.example.utils.HijriCalendarHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HijriWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == Intent.ACTION_DATE_CHANGED || 
            action == Intent.ACTION_TIME_CHANGED || 
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == "com.example.action.UPDATE_HIJRI_WIDGET") {
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, HijriWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }

            // Also keep mobile notification bar calendar updated on date/time change
            if (CalendarNotificationManager.isCalendarNotificationEnabled(context)) {
                CalendarNotificationManager.showOrUpdateCalendarNotification(context)
            }
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            HijriCalendarHelper.syncOffset(context)
            val views = RemoteViews(context.packageName, R.layout.hijri_widget_layout)

            // 1. Gregorian Date
            val cal = Calendar.getInstance()
            val gregFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
            views.setTextViewText(R.id.widget_gregorian_date, gregFormat.format(cal.time))

            // Check if Maghrib passed
            val isPastMaghrib = HijriCalendarHelper.isPastMaghribToday(context)
            val currentExtraOffset = if (isPastMaghrib) 1 else 0

            // 2. Hijri Date
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val hijriDate = HijriCalendarHelper.convertGregorianToHijri(year, month, day, HijriCalendarHelper.globalOffsetDays + currentExtraOffset)
            views.setTextViewText(R.id.widget_hijri_date, "${hijriDate.day} ${hijriDate.monthName} ${hijriDate.year} AH")

            // 3. Find closest upcoming Shia/Islamic Event
            val eventInfo = HijriCalendarHelper.getUpcomingShiaEvent(context, currentExtraOffset)
            views.setTextViewText(R.id.widget_event_text, eventInfo.displayText)

            // Click action retains the existing Home destination.
            val pendingIntent = AppPendingIntents.hijriWidgetContent(context)
            views.setOnClickPendingIntent(R.id.widget_hijri_date, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_event_text, pendingIntent)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
