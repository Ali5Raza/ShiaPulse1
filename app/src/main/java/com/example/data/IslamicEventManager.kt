package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.database.ValueEventListener
import org.json.JSONArray
import org.json.JSONObject

enum class RegionConvention(val code: String, val label: String, val labelUrdu: String, val flag: String) {
    SOUTH_ASIA("SOUTH_ASIA", "Pakistan & South Asia (17 Safar)", "پاکستان و جنوبی ایشیا (۱۷ صفر)", "🇵🇰"),
    IRAN_HAWZA("IRAN_HAWZA", "Iran & Hawza Qom/Najaf (30 Safar)", "ایران و حوزہ علمیہ (۳۰ صفر)", "🇮🇷"),
    ALL("ALL", "All Narrations", "تمام روایتیں", "🌐")
}

object IslamicEventManager {
    private const val PREF_NAME = "app_preferences"
    private const val KEY_CUSTOM_EVENTS = "custom_islamic_events_json"
    private const val KEY_REGION_PREFERENCE = "regional_narration_preference"
    private const val DATABASE_URL = "https://shiapulse-pk-default-rtdb.asia-southeast1.firebasedatabase.app"

    private var cachedEvents: List<MasoomEvent>? = null
    private val _eventsRevision = MutableStateFlow(0L)
    val eventsRevision: StateFlow<Long> = _eventsRevision.asStateFlow()

    private fun notifyEventsUpdated() {
        _eventsRevision.value += 1L
    }

    fun getRegionPreference(context: Context): RegionConvention {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        // Check if explicitly set
        if (prefs.contains(KEY_REGION_PREFERENCE)) {
            val code = prefs.getString(KEY_REGION_PREFERENCE, RegionConvention.ALL.code) ?: RegionConvention.ALL.code
            return RegionConvention.values().find { it.code == code } ?: RegionConvention.ALL
        }

        // Auto-detect only from validated location metadata or unambiguous country text.
        val prayerPrefs = context.getSharedPreferences("shia_tracker_prefs", Context.MODE_PRIVATE)
        val city = prayerPrefs.getString("selected_location_city", "") ?: ""
        val countryCode = com.example.utils.CountryLocationResolver.resolve(
            prayerPrefs.getString("selected_location_country_code", null),
            city
        )

        return when (countryCode) {
            "PK", "IN", "BD", "LK", "AF" -> RegionConvention.SOUTH_ASIA
            "IR", "IQ" -> RegionConvention.IRAN_HAWZA
            else -> RegionConvention.ALL
        }
    }

    /** Re-reads automatic region filtering after a location change without replacing user data. */
    fun onLocationContextChanged() {
        notifyEventsUpdated()
    }

    fun setRegionPreference(context: Context, region: RegionConvention) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REGION_PREFERENCE, region.code)
            .apply()
        cachedEvents = null
        notifyEventsUpdated()
    }

    fun getFilteredEvents(context: Context): List<MasoomEvent> {
        val rawEvents = getEvents(context)
        val pref = getRegionPreference(context)
        if (pref == RegionConvention.ALL) return rawEvents

        return rawEvents.filter { event ->
            event.region.isBlank() ||
            event.region.equals("ALL", ignoreCase = true) || 
            event.region.equals(pref.code, ignoreCase = true)
        }
    }

    fun getEvents(context: Context): List<MasoomEvent> {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val customJson = prefs.getString(KEY_CUSTOM_EVENTS, null)

            val jsonString = customJson?.takeIf { it.isNotBlank() }
                ?: context.assets.open("islamic_events.json").bufferedReader().use { it.readText() }

            val list = parseJsonToEvents(jsonString)
            val uniqueList = list.distinctBy { "${it.dateStringHijri.lowercase().trim()}_${it.title.lowercase().trim()}" }
            cachedEvents = uniqueList
            return uniqueList
        } catch (e: Exception) {
            Log.e("IslamicEventManager", "Failed to load events from JSON", e)
            cachedEvents?.let { return it }

            // A malformed custom/Firebase payload must not erase the calendar. Fall back
            // to the same bundled JSON, never to a second hardcoded Kotlin event list.
            return try {
                readBundledEvents(context).also { cachedEvents = it }
            } catch (assetError: Exception) {
                Log.e("IslamicEventManager", "Failed to load bundled Islamic events", assetError)
                emptyList()
            }
        }
    }

    private fun readBundledEvents(context: Context): List<MasoomEvent> {
        val json = context.assets.open("islamic_events.json").bufferedReader().use { it.readText() }
        return parseJsonToEvents(json)
            .distinctBy { "${it.dateStringHijri.lowercase().trim()}_${it.title.lowercase().trim()}" }
    }

    fun parseJsonToEvents(jsonString: String): List<MasoomEvent> {
        val list = mutableListOf<MasoomEvent>()
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val eventTypeCode = obj.optString("eventType", "WILADAT").uppercase()
            val eventType = EventType.values().firstOrNull { it.name == eventTypeCode }
                ?: throw IllegalArgumentException("Unknown eventType '$eventTypeCode' at index $i.")
            val event = MasoomEvent(
                id = obj.optString("id", "event_$i"),
                title = obj.optString("title", ""),
                titleUrdu = obj.optString("titleUrdu", ""),
                titleArabic = obj.optString("titleArabic", ""),
                dateStringHijri = obj.optString("dateStringHijri", ""),
                hijriMonth = obj.optInt("hijriMonth", 0),
                hijriDay = obj.optInt("hijriDay", 0),
                eventType = eventType,
                masoomIndex = obj.optInt("masoomIndex", 0),
                description = obj.optString("description", ""),
                region = obj.optString("region", "ALL").uppercase(),
                descriptionUrdu = obj.optString("descriptionUrdu", ""),
                descriptionArabic = obj.optString("descriptionArabic", ""),
                sourceUrl = obj.optString("sourceUrl", "")
            )
            list.add(event)
        }
        return list
    }

    fun eventsToJsonString(events: List<MasoomEvent>): String {
        val array = JSONArray()
        events.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id.ifEmpty { "event_${System.currentTimeMillis()}_${(1000..9999).random()}" })
            obj.put("title", e.title)
            obj.put("titleUrdu", e.titleUrdu)
            obj.put("titleArabic", e.titleArabic)
            obj.put("dateStringHijri", e.dateStringHijri)
            obj.put("hijriMonth", e.hijriMonth)
            obj.put("hijriDay", e.hijriDay)
            obj.put("eventType", e.eventType.name)
            obj.put("masoomIndex", e.masoomIndex)
            obj.put("description", e.description)
            obj.put("descriptionUrdu", e.descriptionUrdu)
            obj.put("descriptionArabic", e.descriptionArabic)
            obj.put("region", e.region)
            obj.put("sourceUrl", e.sourceUrl)
            array.put(obj)
        }
        return array.toString(2)
    }

    fun saveEvents(context: Context, events: List<MasoomEvent>) {
        val jsonString = eventsToJsonString(events)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_EVENTS, jsonString)
            .apply()
        cachedEvents = events
        notifyEventsUpdated()
    }

    fun saveEventsJson(context: Context, jsonString: String) {
        val events = parseJsonToEvents(jsonString)
        validateEventCollection(events)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_EVENTS, jsonString)
            .apply()
        cachedEvents = events
        notifyEventsUpdated()
    }

    fun resetToDefault(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CUSTOM_EVENTS)
            .apply()
        cachedEvents = null
        notifyEventsUpdated()
    }

    fun getRawJson(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val customJson = prefs.getString(KEY_CUSTOM_EVENTS, null)
        customJson?.takeIf { it.isNotBlank() }?.let { return it }
        return try {
            context.assets.open("islamic_events.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            eventsToJsonString(cachedEvents ?: emptyList())
        }
    }

    private fun validateEventCollection(events: List<MasoomEvent>) {
        require(events.isNotEmpty()) { "Islamic event collection cannot be empty." }
        events.forEachIndexed { index, event ->
            require(event.title.isNotBlank()) { "Event $index is missing its English title." }
            require(event.dateStringHijri.isNotBlank()) { "Event $index is missing its Hijri date." }
            require(event.hijriMonth in 0..12) { "Event $index has an invalid Hijri month." }
            require(event.hijriDay in 0..30) { "Event $index has an invalid Hijri day." }
            require(event.masoomIndex in 0..14) { "Event $index has an invalid Masoom linkage." }
            require(event.sourceUrl.isBlank() || event.sourceUrl.startsWith("https://") || event.sourceUrl.startsWith("http://")) {
                "Event $index has an invalid reference URL."
            }
            require(event.region.isBlank() || RegionConvention.values().any { it.code == event.region }) {
                "Event $index has an unknown regional narration tag."
            }
        }
    }

    suspend fun publishEventsToFirebase(events: List<MasoomEvent>) {
        validateEventCollection(events)
        val database = FirebaseDatabase.getInstance(DATABASE_URL)
        val timestamp = System.currentTimeMillis()
        val updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
        val updates = mapOf(
            "events_json_string" to eventsToJsonString(events),
            "events_version" to ServerValue.TIMESTAMP,
            "events_version_last_updated" to updatedAt
        )

        // JSON and its version must become visible together. Awaiting also surfaces
        // permission/network failures to the admin instead of reporting a false success.
        database.reference.updateChildren(updates).await()
        Log.d("IslamicEventManager", "Atomically published ${events.size} events to Firebase.")
    }

    fun syncEventsFromFirebase(context: Context, onSyncComplete: (List<MasoomEvent>) -> Unit) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val database = FirebaseDatabase.getInstance(DATABASE_URL)
        val versionRef = database.reference.child("events_version")

        versionRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(versionSnapshot: DataSnapshot) {
                val rawVersion = versionSnapshot.value
                val remoteVersion = when (rawVersion) {
                    is Number -> rawVersion.toLong()
                    is String -> rawVersion.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val localVersion = prefs.getLong("events_version_local", 0L)
                val hasLocalEvents = prefs.contains(KEY_CUSTOM_EVENTS)
                val shouldSync = if (remoteVersion > 0L) {
                    // Remote content is authoritative. Using != also repairs updates made
                    // by older builds whose admin device clock produced a lower version.
                    remoteVersion != localVersion || !hasLocalEvents
                } else {
                    // Legacy databases without a version still sync once.
                    localVersion == 0L
                }
                if (!shouldSync) return

                database.reference.child("events_json_string").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val jsonStringValue = dataSnapshot.getValue(String::class.java)
                        if (jsonStringValue.isNullOrBlank()) {
                            Log.w("IslamicEventManager", "Remote events JSON is empty or unavailable.")
                            return
                        }

                        try {
                            val events = parseJsonToEvents(jsonStringValue)
                            saveEventsJson(appContext, jsonStringValue)
                            val newVersionToSave = if (remoteVersion > 0L) remoteVersion else 1L
                            prefs.edit().putLong("events_version_local", newVersionToSave).apply()
                            Log.d("IslamicEventManager", "Synced ${events.size} events. Version: $newVersionToSave")
                            onSyncComplete(events)
                        } catch (e: Exception) {
                            // Do not advance the local version when content could not be saved.
                            Log.e("IslamicEventManager", "Error applying updated events", e)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("IslamicEventManager", "Events content sync cancelled: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("IslamicEventManager", "Events version listener cancelled: ${error.message}")
            }
        })
    }

}
