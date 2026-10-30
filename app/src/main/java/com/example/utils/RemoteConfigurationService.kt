package com.example.utils

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class HijriConfig(
    val defaultOffset: Int = 0,
    val countries: Map<String, Int> = emptyMap(),
    val lastUpdated: String = ""
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class AppRemoteConfig(
    val hijri_config: HijriConfig = HijriConfig(),
    val prayer_calc_method: String = "MWL",
    val maintenance_mode: Boolean = false,
    val announcements: List<String> = emptyList(),
    val feature_flags: Map<String, Boolean> = emptyMap()
)

interface RemoteConfigurationService {
    suspend fun fetchRemoteConfig(): AppRemoteConfig
    suspend fun saveRemoteConfig(config: AppRemoteConfig): Boolean
    fun getCachedConfig(): AppRemoteConfig
    fun startRealtimeSync(onConfigUpdated: (AppRemoteConfig) -> Unit)
}

class DefaultRemoteConfigurationService(private val context: Context) : RemoteConfigurationService {
    private val cacheFile = File(context.filesDir, "remote_config_cache.json")
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(AppRemoteConfig::class.java)
    
    private val dbUrl = "https://shiapulse-pk-default-rtdb.asia-southeast1.firebasedatabase.app"
    private val dbNode = "remote_config"

    companion object {
        private val cacheWriteLock = Any()
    }

    private fun parseSnapshot(snapshot: com.google.firebase.database.DataSnapshot): AppRemoteConfig? {
        val rawValue = snapshot.value ?: return null
        return try {
            when (rawValue) {
                is String -> {
                    if (rawValue.isNotEmpty()) adapter.fromJson(rawValue) else null
                }
                is Map<*, *> -> {
                    val jsonStr = org.json.JSONObject(rawValue).toString()
                    adapter.fromJson(jsonStr)
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e("RemoteConfig", "Error parsing snapshot: ${e.message}", e)
            null
        }
    }

    override fun startRealtimeSync(onConfigUpdated: (AppRemoteConfig) -> Unit) {
        try {
            val database = FirebaseDatabase.getInstance(dbUrl)
            database.reference.child(dbNode).addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val config = parseSnapshot(snapshot)
                    if (config != null) {
                        val generation = HijriConfigurationGeneration.invalidate()
                        try {
                            val jsonStr = adapter.toJson(config)
                            synchronized(cacheWriteLock) {
                                HijriConfigurationGeneration.runIfCurrent(generation) {
                                    cacheFile.writeText(jsonStr)
                                }
                            }
                            HijriConfigurationGeneration.runIfCurrent(generation) {
                                Log.d("RemoteConfig", "Realtime update received from Firebase: defaultOffset=${config.hijri_config.defaultOffset}, countries=${config.hijri_config.countries}")
                                onConfigUpdated(config)
                            }
                        } catch (e: Exception) {
                            Log.e("RemoteConfig", "Error saving realtime config to cache", e)
                        }
                    }
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Log.w("RemoteConfig", "Realtime listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("RemoteConfig", "Failed to register realtime listener", e)
        }
    }

    override suspend fun fetchRemoteConfig(): AppRemoteConfig {
        val generation = HijriConfigurationGeneration.current()
        return try {
            val database = FirebaseDatabase.getInstance(dbUrl)
            val snapshot = database.reference.child(dbNode).get().await()
            val config = parseSnapshot(snapshot)
            if (config != null) {
                val jsonStr = adapter.toJson(config)
                synchronized(cacheWriteLock) {
                    HijriConfigurationGeneration.runIfCurrent(generation) {
                        cacheFile.writeText(jsonStr)
                    }
                }
                HijriConfigurationGeneration.runIfCurrent(generation) {
                    Log.d("RemoteConfig", "Firebase configuration downloaded and cached successfully.")
                }
                return config
            }
            Log.w("RemoteConfig", "Remote config is empty or invalid, fallback to local cache.")
            getCachedConfig()
        } catch (e: Exception) {
            Log.w("RemoteConfig", "Could not download Firebase configuration, falling back to cache. (Reason: ${e.message})")
            getCachedConfig()
        }
    }

    override suspend fun saveRemoteConfig(config: AppRemoteConfig): Boolean {
        return try {
            // Update last updated field in Hijri Config
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateStr = sdf.format(Date())
            val updatedConfig = config.copy(
                hijri_config = config.hijri_config.copy(lastUpdated = dateStr)
            )
            
            val jsonStr = adapter.toJson(updatedConfig)
            val database = FirebaseDatabase.getInstance(dbUrl)
            database.reference.child(dbNode).setValue(jsonStr).await()

            // Only report success after Firebase confirms the write. A local cache
            // failure must not turn a confirmed remote publish into an error.
            val generation = HijriConfigurationGeneration.invalidate()
            try {
                synchronized(cacheWriteLock) {
                    HijriConfigurationGeneration.runIfCurrent(generation) {
                        cacheFile.writeText(jsonStr)
                    }
                }
            } catch (e: Exception) {
                Log.w("RemoteConfig", "Remote save succeeded but local cache failed", e)
            }
            Log.d("RemoteConfig", "Configuration updated successfully and published to Firebase.")
            true
        } catch (e: Exception) {
            Log.e("RemoteConfig", "Failed to save configuration to Firebase: ${e.message}")
            false
        }
    }

    override fun getCachedConfig(): AppRemoteConfig {
        return if (cacheFile.exists()) {
            try {
                val jsonStr = synchronized(cacheWriteLock) { cacheFile.readText() }
                val config = adapter.fromJson(jsonStr)
                if (config != null) {
                    Log.d("RemoteConfig", "Offline configuration loaded from local cache.")
                    config
                } else {
                    Log.w("RemoteConfig", "Failed to parse cached configuration, using default.")
                    AppRemoteConfig()
                }
            } catch (e: Exception) {
                Log.e("RemoteConfig", "Error reading cached configuration: ${e.message}")
                AppRemoteConfig()
            }
        } else {
            Log.d("RemoteConfig", "No cached configuration found, returning default.")
            AppRemoteConfig()
        }
    }
}
