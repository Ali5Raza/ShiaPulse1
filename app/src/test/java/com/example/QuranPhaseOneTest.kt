package com.example

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.example.data.QuranAssetLoader
import com.example.data.QuranMemorisationStatus
import com.example.data.QuranProgressStore
import com.example.data.QuranSearchEngine
import com.example.data.QuranSearchMatch
import com.example.ui.updateQuranKeepScreenAwake
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuranPhaseOneTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun searchNormalizesArabicAndFindsExactAyahInAllThreeLanguages() = runTest {
        val index = QuranAssetLoader.loadIndex(context)

        val arabic = QuranSearchEngine.search(context, index, "الحمد", maxResults = 20)
        assertTrue(arabic.any {
            it.surahNumber == 1 && it.ayahNumber == 2 && it.matchedIn == QuranSearchMatch.ARABIC
        })

        val english = QuranSearchEngine.search(context, index, "ALL-BENEFICENT", maxResults = 20)
        assertTrue(english.any {
            it.surahNumber == 1 && it.ayahNumber == 1 && it.matchedIn == QuranSearchMatch.ENGLISH
        })

        val urdu = QuranSearchEngine.search(context, index, "عظیم اور دائمی", maxResults = 20)
        assertTrue(urdu.any {
            it.surahNumber == 1 && it.ayahNumber == 1 && it.matchedIn == QuranSearchMatch.URDU
        })
    }

    @Test
    fun searchHonorsCapAndRejectsTooShortQueries() = runTest {
        val index = QuranAssetLoader.loadIndex(context)

        val results = QuranSearchEngine.search(context, index, "the", maxResults = 5)
        assertEquals(5, results.size)
        assertEquals(5, results.map { it.surahNumber to it.ayahNumber }.toSet().size)
        results.forEach { result ->
            val info = index.first { it.number == result.surahNumber }
            assertTrue(result.ayahNumber in 1..info.verseCount)
            assertTrue(QuranSearchEngine.normalize(result.text).contains("the"))
        }

        assertTrue(QuranSearchEngine.search(context, index, "a").isEmpty())
        assertTrue(QuranSearchEngine.search(context, index, "  ").isEmpty())
        assertTrue(QuranSearchEngine.search(context, index, "the", maxResults = 0).isEmpty())
    }

    @Test
    fun personalProgressStoreKeepsAllPhaseOneStateOfflineAndReactive() {
        QuranProgressStore.initialize(context)
        QuranProgressStore.recordReading(context, 2, 255)
        assertEquals(2, QuranProgressStore.state.value.lastRead?.surahNumber)
        assertEquals(255, QuranProgressStore.state.value.lastRead?.ayahNumber)

        QuranProgressStore.toggleBookmark(context, 2, 255)
        assertTrue("2:255" in QuranProgressStore.state.value.bookmarks)
        QuranProgressStore.toggleBookmark(context, 2, 255)
        assertFalse("2:255" in QuranProgressStore.state.value.bookmarks)
        QuranProgressStore.toggleBookmark(context, 2, 255)
        QuranProgressStore.removeBookmark(context, "2:255")
        assertFalse("2:255" in QuranProgressStore.state.value.bookmarks)

        QuranProgressStore.toggleSurahCompleted(context, 1)
        QuranProgressStore.toggleSurahCompleted(context, 2)
        QuranProgressStore.startKhatamPlan(context, 40)
        assertEquals(setOf(1, 2), QuranProgressStore.state.value.completedSurahs)
        assertEquals(40, QuranProgressStore.state.value.khatamTargetDays)
        assertNotNull(QuranProgressStore.state.value.khatamStartDay)

        QuranProgressStore.setMemorisationStatus(
            context,
            2,
            255,
            QuranMemorisationStatus.NEEDS_PRACTICE
        )
        assertEquals(
            QuranMemorisationStatus.NEEDS_PRACTICE,
            QuranProgressStore.state.value.memorisation["2:255"]
        )
        QuranProgressStore.setHifzMode(context, true)
        QuranProgressStore.setKeepScreenAwake(context, true)
        QuranProgressStore.setArabicFontScale(context, 1.25f)
        QuranProgressStore.setTranslationFontScale(context, 10f)
        assertTrue(QuranProgressStore.state.value.hifzMode)
        assertTrue(QuranProgressStore.state.value.keepScreenAwake)
        assertEquals(1.25f, QuranProgressStore.state.value.arabicFontScale, 0.0001f)
        assertEquals(1.5f, QuranProgressStore.state.value.translationFontScale, 0.0001f)

        QuranProgressStore.clearKhatamProgress(context)
        assertTrue(QuranProgressStore.state.value.completedSurahs.isEmpty())
        assertNull(QuranProgressStore.state.value.khatamStartDay)
        assertEquals(
            QuranMemorisationStatus.NEEDS_PRACTICE,
            QuranProgressStore.state.value.memorisation["2:255"]
        )
        QuranProgressStore.setMemorisationStatus(context, 2, 255, null)
        assertNull(QuranProgressStore.state.value.memorisation["2:255"])
    }

    @Test
    fun readerKeepAwakeFlagCanBeCleared() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val window = controller.get().window
            updateQuranKeepScreenAwake(window, enabled = true)
            assertTrue(
                window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
            )

            updateQuranKeepScreenAwake(window, enabled = false)
            assertFalse(
                window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
            )
        } finally {
            controller.destroy()
        }
    }

    @Test
    fun localPlanDayUsesWallClockDateOnBothSidesOfUtc() {
        val timestamp = Instant.parse("2026-01-01T00:30:00Z").toEpochMilli()
        val plusFiveHours = 5 * 60 * 60 * 1000
        val minusFiveHours = -plusFiveHours

        assertEquals(
            LocalDate.of(2026, 1, 1).toEpochDay(),
            QuranProgressStore.localEpochDay(timestamp, 0)
        )
        assertEquals(
            LocalDate.of(2026, 1, 1).toEpochDay(),
            QuranProgressStore.localEpochDay(timestamp, plusFiveHours)
        )
        assertEquals(
            LocalDate.of(2025, 12, 31).toEpochDay(),
            QuranProgressStore.localEpochDay(timestamp, minusFiveHours)
        )
    }
}
