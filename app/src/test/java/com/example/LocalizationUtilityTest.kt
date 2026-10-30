package com.example

import com.example.utils.LocalizationUtility
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LocalizationUtilityTest {
    private val requiredOverallAppKeys = listOf(
        "action_back", "action_next", "action_skip", "action_cancel", "action_save",
        "onboarding_step", "onboarding_language_title", "onboarding_location_title",
        "onboarding_location_description", "onboarding_grant_location", "onboarding_or",
        "onboarding_select_city", "onboarding_selected", "onboarding_location_required",
        "onboarding_account_title", "onboarding_account_description", "onboarding_continue_google",
        "onboarding_connected", "onboarding_sign_in_error", "onboarding_ready_title",
        "onboarding_language", "onboarding_location", "onboarding_email",
        "onboarding_not_provided", "onboarding_get_started",
        "ai_companion_title", "ai_greeting", "ai_disclaimer", "ai_input_hint", "ai_send",
        "ai_cooldown", "ai_no_response", "ai_unavailable", "ai_rate_limit",
        "ai_http_error", "ai_network_error",
        "calendar_history", "calendar_grid_subtitle", "calendar_no_events",
        "calendar_shia_events", "calendar_international_events", "calendar_previous_month",
        "calendar_next_month", "calendar_joyous", "calendar_solemn", "calendar_international",
        "settings_profile_subtitle", "settings_account_cloud", "settings_personalization",
        "settings_notifications", "settings_support", "settings_faq", "settings_sign_in",
        "settings_sign_out", "settings_backup", "settings_restore", "settings_interface_language",
        "settings_theme", "settings_dark", "settings_light", "settings_system", "settings_share"
    )

    @Test
    fun overallAppKeysAreTranslatedRatherThanFallingBackToEnglish() {
        requiredOverallAppKeys.forEach { key ->
            val english = LocalizationUtility.get(key, "en")
            val urdu = LocalizationUtility.get(key, "ur")
            val arabic = LocalizationUtility.get(key, "ar")

            assertTrue("Missing English value for $key", english.isNotBlank() && english != key)
            assertTrue("Missing Urdu value for $key", urdu.isNotBlank() && urdu != key)
            assertTrue("Missing Arabic value for $key", arabic.isNotBlank() && arabic != key)
            assertNotEquals("Urdu fallback detected for $key", english, urdu)
            assertNotEquals("Arabic fallback detected for $key", english, arabic)
        }
    }

    @Test
    fun positionalFormattingPreservedInEveryLanguage() {
        listOf("en", "ur", "ar").forEach { language ->
            val step = String.format(
                Locale.ENGLISH,
                LocalizationUtility.get("onboarding_step", language),
                2,
                4
            )
            assertTrue(step.contains("2"))
            assertTrue(step.contains("4"))

            val error = String.format(
                Locale.ENGLISH,
                LocalizationUtility.get("ai_http_error", language),
                429
            )
            assertTrue(error.contains("429"))
        }
    }
}
