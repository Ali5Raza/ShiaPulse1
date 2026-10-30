package com.example.utils

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import java.util.Locale

interface CountryDetectionService {
    fun detectCountry(context: Context? = null): String
}

/** Conservative, text-only fallback used when explicit country metadata is unavailable. */
object CountryLocationResolver {
    private val validCountryCodes: Set<String> by lazy {
        Locale.getISOCountries().toHashSet()
    }

    private val countryNames: Map<String, String> by lazy {
        validCountryCodes.mapNotNull { code ->
            val name = normalize(Locale("", code).getDisplayCountry(Locale.US))
            if (name.isBlank()) null else name to code
        }.toMap()
    }

    private val aliases: Map<String, String> = mapOf(
        "usa" to "US",
        "united states of america" to "US",
        "uk" to "GB",
        "great britain" to "GB",
        "britain" to "GB",
        "england" to "GB",
        "uae" to "AE",
        "saudi" to "SA",
        "south korea" to "KR",
        "north korea" to "KP",
        "viet nam" to "VN",
        "russia" to "RU",
        "iran" to "IR",
        "syria" to "SY",
        "turkey" to "TR",
        "turkiye" to "TR"
    )

    fun resolve(explicitCountryCode: String?, locationText: String?): String {
        validatedCode(explicitCountryCode)?.let { return it }

        val rawText = locationText?.trim().orEmpty()
        if (rawText.isBlank()) return ""

        val allNames = countryNames + aliases
        val trailingText = rawText.substringAfterLast(',', rawText).trim()

        // A separate trailing ISO code or country name is explicit metadata,
        // not a substring found inside an ordinary place name.
        validatedCode(trailingText)?.let { return it }
        val normalizedTrailing = normalize(trailingText)
        allNames[normalizedTrailing]?.let { return it }

        val normalizedText = normalize(rawText)
        val matches = allNames.entries.mapNotNull { (name, code) ->
            code.takeIf { containsWholePhrase(normalizedText, name) }
        }.distinct()
        // More than one possible country is ambiguous and must not be guessed.
        return matches.singleOrNull().orEmpty()
    }

    fun validatedCode(candidate: String?): String? {
        val code = candidate?.trim()?.uppercase(Locale.US).orEmpty()
        return code.takeIf { it.length == 2 && it in validCountryCodes }
    }

    private fun containsWholePhrase(text: String, phrase: String): Boolean {
        if (phrase.isBlank()) return false
        val pattern = "(^| )" + Regex.escape(phrase) + "(?= |$)"
        return Regex(pattern).containsMatchIn(text)
    }

    private fun normalize(value: String): String = value
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}

class DefaultCountryDetectionService(private val appContext: Context) : CountryDetectionService {
    override fun detectCountry(context: Context?): String {
        val targetContext = context ?: appContext

        try {
            val prayerPrefs = targetContext.getSharedPreferences("shia_tracker_prefs", Context.MODE_PRIVATE)
            val pulsePrefs = targetContext.getSharedPreferences("shiapulse_prefs", Context.MODE_PRIVATE)
            val city = (prayerPrefs.getString("selected_location_city", null)
                ?: pulsePrefs.getString("selected_location_city", null)
                ?: pulsePrefs.getString("loc_city", "")).orEmpty().trim()
            val explicitCode = prayerPrefs.getString("selected_location_country_code", null)
                ?: pulsePrefs.getString("selected_location_country_code", null)
                ?: pulsePrefs.getString("loc_country_code", null)
            val hasSelectedLocation = city.isNotBlank() || !explicitCode.isNullOrBlank()
            val detected = CountryLocationResolver.resolve(explicitCode, city)

            if (detected.isNotEmpty()) {
                Log.d("CountryDetection", "Resolved selected-location country -> $detected")
                return detected
            }
            if (hasSelectedLocation) {
                // Do not silently apply the SIM's country to an ambiguous remote city.
                Log.w("CountryDetection", "Selected location has no unambiguous country metadata; using CMS default")
                return ""
            }
        } catch (e: Exception) {
            Log.e("CountryDetection", "Error reading selected-location metadata", e)
        }

        // Existing device-based fallback remains for users who have not selected a location.
        try {
            val telephony = targetContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            CountryLocationResolver.validatedCode(telephony?.simCountryIso)?.let {
                Log.d("CountryDetection", "Detected country from SIM: $it")
                return it
            }
            CountryLocationResolver.validatedCode(telephony?.networkCountryIso)?.let {
                Log.d("CountryDetection", "Detected country from network: $it")
                return it
            }
        } catch (e: Exception) {
            Log.e("CountryDetection", "Error detecting country from telephony", e)
        }

        val localeCountry = CountryLocationResolver.validatedCode(Locale.getDefault().country).orEmpty()
        if (localeCountry.isNotEmpty()) {
            Log.d("CountryDetection", "Detected country from locale: $localeCountry")
        }
        return localeCountry
    }
}
