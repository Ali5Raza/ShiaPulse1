package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.DashboardScreen
import com.example.ui.PrayerViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.app.Application
import androidx.test.core.app.ApplicationProvider

@RunWith(AndroidJUnit4::class)
class StartupComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashBoardDoesNotCrash() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PrayerViewModel(app)
        composeTestRule.setContent {
            DashboardScreen(viewModel = viewModel, azimuth = 0f)
        }
        
        composeTestRule.waitForIdle()
    }
}
