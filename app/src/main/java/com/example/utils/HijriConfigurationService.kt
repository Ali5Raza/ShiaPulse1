package com.example.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

interface HijriConfigurationService {
    suspend fun resolveAndApplyOffset(context: Context): Int
    fun applyRemoteConfigDirectly(context: Context, remoteConfig: AppRemoteConfig): Int
}

class DefaultHijriConfigurationService(
    private val countryDetectionService: CountryDetectionService,
    private val remoteConfigurationService: RemoteConfigurationService
) : HijriConfigurationService {

    override suspend fun resolveAndApplyOffset(context: Context): Int {
        val generation = HijriConfigurationGeneration.begin()
        val prefs = context.getSharedPreferences("shiapulse_prefs", Context.MODE_PRIVATE)
        val autoSync = prefs.getBoolean("hijri_auto_sync", true)
        val userManualOffset = prefs.getInt("user_manual_offset", prefs.getInt("hijri_offset_days", 0))

        // Step 1: User Custom / Manual Override (Auto-Sync is OFF)
        if (!autoSync) {
            Log.d("HijriConfig", "Manual override active: User selected offset = $userManualOffset")
            val applied = HijriConfigurationGeneration.runIfCurrent(generation) {
                prefs.edit()
                    .putInt("resolved_hijri_offset", userManualOffset)
                    .putString("hijri_offset_source", "manual")
                    .putString("hijri_offset_source_desc", "Manual Custom Offset (${if (userManualOffset > 0) "+$userManualOffset" else "$userManualOffset"}d)")
                    .apply()
                HijriCalendarHelper.globalOffsetDays = userManualOffset
            }
            if (!applied) return currentOffset(prefs)
            triggerWidgetUpdate(context)
            return userManualOffset
        }

        // Step 2: Auto-Sync is ON -> Fetch Content Management (Firebase Remote Config)
        val remoteConfig = remoteConfigurationService.fetchRemoteConfig()
        return applyRemoteConfigDirectly(context, remoteConfig, generation)
    }

    override fun applyRemoteConfigDirectly(context: Context, remoteConfig: AppRemoteConfig): Int =
        applyRemoteConfigDirectly(context, remoteConfig, HijriConfigurationGeneration.begin())

    private fun applyRemoteConfigDirectly(
        context: Context,
        remoteConfig: AppRemoteConfig,
        generation: Long
    ): Int {
        val prefs = context.getSharedPreferences("shiapulse_prefs", Context.MODE_PRIVATE)
        if (!HijriConfigurationGeneration.isCurrent(generation)) return currentOffset(prefs)
        val autoSync = prefs.getBoolean("hijri_auto_sync", true)
        val userManualOffset = prefs.getInt("user_manual_offset", prefs.getInt("hijri_offset_days", 0))

        // Priority 0: If user manually disabled auto-sync, respect manual offset
        if (!autoSync) {
            Log.d("HijriConfig", "Manual override active. Preserving user manual offset: $userManualOffset")
            val applied = HijriConfigurationGeneration.runIfCurrent(generation) {
                HijriCalendarHelper.globalOffsetDays = userManualOffset
                prefs.edit()
                    .putInt("resolved_hijri_offset", userManualOffset)
                    .putString("hijri_offset_source", "manual")
                    .putString("hijri_offset_source_desc", "Manual Custom Offset (${if (userManualOffset > 0) "+$userManualOffset" else "$userManualOffset"}d)")
                    .apply()
            }
            if (!applied) return currentOffset(prefs)
            triggerWidgetUpdate(context)
            return userManualOffset
        }

        val hijriConfig = remoteConfig.hijri_config
        val detectedCountry = countryDetectionService.detectCountry(context).uppercase().trim()
        if (!HijriConfigurationGeneration.isCurrent(generation)) return currentOffset(prefs)
        Log.d("HijriConfig", "Country detected for Hijri calculation: '$detectedCountry'")

        val finalOffset: Int
        val sourceKey: String
        val sourceDesc: String

        // Priority 1: Content Management (Firebase Remote Config) Country-Wise Offset
        if (detectedCountry.isNotEmpty() && hijriConfig.countries.containsKey(detectedCountry)) {
            val countryOffset = hijriConfig.countries[detectedCountry] ?: 0
            finalOffset = countryOffset
            sourceKey = "country_cms"
            sourceDesc = "Content Management ($detectedCountry: ${if (countryOffset > 0) "+$countryOffset" else "$countryOffset"}d)"
            Log.d("HijriConfig", "Priority 1 Applied: Country Specific ($detectedCountry) -> Offset = $finalOffset")
        } else if (hijriConfig.lastUpdated.isNotBlank()) {
            // Priority 2: A published CMS default applies whenever no country
            // override exists, matching the admin panel's documented behavior.
            finalOffset = hijriConfig.defaultOffset
            sourceKey = "default_cms"
            sourceDesc = "Content Management (Default: ${if (finalOffset > 0) "+$finalOffset" else "$finalOffset"}d)"
            Log.d("HijriConfig", "Priority 2 Applied: Published CMS default -> Offset = $finalOffset")
        } else {
            // Priority 3: Automatic Library / API Calculation is only a fallback
            // when no published remote configuration is available.
            val apiCalculatedOffset = prefs.getInt("api_calculated_offset", prefs.getInt("api_synced_offset", hijriConfig.defaultOffset))
            finalOffset = apiCalculatedOffset
            sourceKey = "api_library"
            sourceDesc = "Auto Library / API Calculation (${if (finalOffset > 0) "+$finalOffset" else "$finalOffset"}d)"
            Log.d("HijriConfig", "Priority 3 Applied: No published CMS default, using Auto API/Library offset = $finalOffset")
        }

        val applied = HijriConfigurationGeneration.runIfCurrent(generation) {
            prefs.edit()
                .putInt("resolved_hijri_offset", finalOffset)
                .putString("hijri_offset_source", sourceKey)
                .putString("hijri_offset_source_desc", sourceDesc)
                .putString("hijri_detected_country", detectedCountry)
                .apply()
            HijriCalendarHelper.globalOffsetDays = finalOffset
        }
        if (!applied) return currentOffset(prefs)
        triggerWidgetUpdate(context)
        return finalOffset
    }

    private fun currentOffset(prefs: android.content.SharedPreferences): Int =
        prefs.getInt("resolved_hijri_offset", HijriCalendarHelper.globalOffsetDays)

    private fun triggerWidgetUpdate(context: Context) {
        try {
            val widgetIntent = Intent("com.example.action.UPDATE_HIJRI_WIDGET").apply {
                component = ComponentName(context, com.example.receiver.HijriWidgetProvider::class.java)
            }
            context.sendBroadcast(widgetIntent)
        } catch (e: Exception) {
            Log.e("HijriConfig", "Error triggering widget update", e)
        }
    }
}
