package com.example

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            // Enable offline persistence for Firebase Realtime Database
            val database = FirebaseDatabase.getInstance("https://shiapulse-pk-default-rtdb.asia-southeast1.firebasedatabase.app")
            runCatching { database.setPersistenceEnabled(true) }
            
            // Keep remote config and calendar events synced automatically
            runCatching { database.reference.child("remote_config").keepSynced(true) }
            runCatching { database.reference.child("events_json_string").keepSynced(true) }
            runCatching { database.reference.child("events_version").keepSynced(true) }
            runCatching { database.reference.child("events_version_last_updated").keepSynced(true) }

            // Keep calendar content live for the whole process, not only while an
            // Activity is visible. Every applied update emits an eventsRevision.
            com.example.data.IslamicEventManager.syncEventsFromFirebase(this) {}

            // Refresh mobile notification bar calendar if enabled by user
            com.example.receiver.CalendarNotificationManager.showOrUpdateCalendarNotification(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
