package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.DashboardScreen
import com.example.ui.PrayerViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardClickTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testClickAllTiles() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PrayerViewModel(app)
        composeTestRule.setContent {
            DashboardScreen(viewModel = viewModel, azimuth = 0f)
        }
        composeTestRule.waitForIdle()

        val categories = listOf(
            CategoryDestination(
                menuTitle = "Worship",
                tiles = listOf("Dua", "Qibla", "Tasbeeh", "Tracker", "Ramadan Tracker", "AI Chat")
            ),
            CategoryDestination(
                menuTitle = "Calculations",
                tiles = listOf("Zakat")
            ),
            CategoryDestination(
                menuTitle = "Knowledge",
                tiles = listOf(
                    "Quran",
                    "Calendar",
                    "Masoomeen",
                    "99 Names of Allah",
                    "Names of Muhammad",
                    "Karbala Journey Map"
                )
            )
        )

        for (category in categories) {
            println("[TEST] Opening category ${category.menuTitle}")
            scrollTo(category.menuTitle)
            composeTestRule.onAllNodesWithText(category.menuTitle, ignoreCase = true)
                .onLast()
                .performClick()
            composeTestRule.waitForIdle()

            for (tile in category.tiles) {
                println("[TEST] Opening $tile from ${category.menuTitle}")
                scrollTo(tile)
                composeTestRule.onAllNodesWithText(tile, ignoreCase = true)
                    .onLast()
                    .assertIsDisplayed()
                    .performClick()
                composeTestRule.waitForIdle()

                closeOverlay(tile)
                composeTestRule.waitForIdle()
            }

            composeTestRule.onAllNodesWithContentDescription("Return", ignoreCase = true)
                .onLast()
                .performClick()
            composeTestRule.mainClock.advanceTimeBy(1_000)
            composeTestRule.waitForIdle()
        }
    }

    private fun scrollTo(text: String) {
        val textMatcher = hasText(text, ignoreCase = true)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(text, ignoreCase = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onAllNodes(hasScrollAction())
            .filter(hasAnyDescendant(textMatcher))
            .onLast()
            .performScrollToNode(textMatcher)
        composeTestRule.waitForIdle()
    }

    private fun closeOverlay(tile: String) {
        val closeButtons = composeTestRule.onAllNodesWithContentDescription("Close", ignoreCase = true)
        if (closeButtons.fetchSemanticsNodes().isNotEmpty()) {
            closeButtons.onLast().performClick()
            return
        }

        val backButtons = composeTestRule.onAllNodesWithContentDescription("Back", ignoreCase = true)
        check(backButtons.fetchSemanticsNodes().isNotEmpty()) {
            "$tile opened without a Close or Back action"
        }
        backButtons.onLast().performClick()
    }

    private data class CategoryDestination(
        val menuTitle: String,
        val tiles: List<String>
    )
}
