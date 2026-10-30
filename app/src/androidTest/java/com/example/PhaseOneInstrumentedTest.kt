package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.font.FontFamily
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.QuranAssetLoader
import com.example.data.QuranProgressStore
import com.example.data.QuranReadingPosition
import com.example.ui.DashboardScreen
import com.example.ui.HomeQuranContinueReading
import com.example.ui.PrayerViewModel
import com.example.ui.QuranScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhaseOneInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun configureEnglishHome() {
        app.getSharedPreferences("shia_tracker_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("has_seen_onboarding", true)
            .putBoolean("show_guidance_on_startup", false)
            .putString("selected_language", "en")
            .commit()
    }

    @Test
    fun homeKnowledgeQuranRouteSupportsBookmarkAndIndexContinuation() {
        val viewModel = PrayerViewModel(app)
        composeRule.setContent {
            DashboardScreen(viewModel = viewModel, azimuth = 0f)
        }
        composeRule.waitForIdle()

        scrollToAndClick("Knowledge")
        waitForText("Complete 114 Surahs, offline")
        scrollToAndClick("Quran")
        waitForText("The complete Quran, offline")
        scrollToAndClick("Al-Fatihah")

        waitForText("1:1")
        composeRule.onNodeWithText("1:1").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            QuranProgressStore.state.value.lastRead?.let {
                it.surahNumber == 1 && it.ayahNumber == 1
            } == true
        }

        composeRule.onNodeWithContentDescription("Bookmark ayah").performClick()
        composeRule.onNodeWithContentDescription("Remove bookmark").assertExists()
        clickLastContentDescription("Back")

        waitForText("Continue reading")
        composeRule.onNodeWithText("Continue reading").assertIsDisplayed()
        scrollToAndClick("Continue reading")
        waitForText("1:1")
        composeRule.onNodeWithText("1:1").assertIsDisplayed()
    }

    @Test
    fun homeContinueReadingReturnsExactSavedPosition() {
        QuranProgressStore.recordReading(app, 2, 255)
        val surahs = QuranAssetLoader.loadIndex(app)
        var openedPosition: QuranReadingPosition? = null

        composeRule.setContent {
            HomeQuranContinueReading(
                surahs = surahs,
                currentLang = "en",
                fontFamily = FontFamily.Default,
                onClick = { openedPosition = it }
            )
        }

        composeRule.onNodeWithText("Continue Quran reading").performClick()
        composeRule.waitForIdle()
        assertEquals(2, openedPosition?.surahNumber)
        assertEquals(255, openedPosition?.ayahNumber)
    }

    @Test
    fun directAyahRouteDisplaysRequestedVerseAndReaderSettings() {
        composeRule.setContent {
            QuranScreen(
                currentLang = "en",
                onBack = {},
                initialSurahNumber = 2,
                initialAyahNumber = 255
            )
        }

        waitForText("2:255")
        composeRule.onNodeWithText("2:255").assertIsDisplayed()
        clickLastContentDescription("Quran tools")
        composeRule.onNodeWithText("Reader settings").performClick()
        waitForText("Keep screen awake while reading")
        composeRule.onNodeWithText("Keep screen awake while reading").assertIsDisplayed()
        composeRule.onNodeWithText("Hifz / memorisation mode").assertIsDisplayed()
    }

    private fun scrollToAndClick(text: String) {
        waitForText(text)
        composeRule.onAllNodesWithText(text, ignoreCase = true)
            .onLast()
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
    }

    private fun clickLastContentDescription(description: String) {
        composeRule.onAllNodesWithContentDescription(description, ignoreCase = true)
            .onLast()
            .performClick()
        composeRule.waitForIdle()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(text, ignoreCase = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
