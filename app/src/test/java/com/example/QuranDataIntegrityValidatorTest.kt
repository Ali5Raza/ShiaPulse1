package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.QuranAssetLoader
import com.example.data.QuranDataIntegrityValidator
import com.example.data.QuranJuzIndex
import com.example.data.quranReadingStreaks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuranDataIntegrityValidatorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun bundledQuranAndRuntimeCacheRelationshipsPassIntegrityValidation() {
        val bundled = QuranDataIntegrityValidator.validateBundled(context)
        assertTrue(bundled.issues.joinToString("\n"), bundled.isValid)
        assertEquals(QuranAssetLoader.TOTAL_VERSES, bundled.checkedAyahs)

        val runtime = QuranDataIntegrityValidator.validateRuntimeCaches(context)
        assertTrue(runtime.issues.joinToString("\n"), runtime.isValid)
    }

    @Test
    fun juzCompletionRequiresEveryAyahInItsVerifiedRange() {
        val index = QuranAssetLoader.loadIndex(context)
        val juzOne = buildSet {
            (1..7).forEach { add("1:$it") }
            (1..141).forEach { add("2:$it") }
        }
        assertEquals(148, juzOne.size)
        assertEquals(1, QuranJuzIndex.completedCount(juzOne, index))
        assertEquals(0, QuranJuzIndex.completedCount(juzOne - "2:141", index))

        val complete = index.flatMap { info ->
            (1..info.verseCount).map { "${info.number}:$it" }
        }.toSet()
        assertEquals(30, QuranJuzIndex.completedCount(complete, index))
    }

    @Test
    fun readingStreaksUseLocalReadingDaysWithoutInventingActivity() {
        val today = 20_000L
        val days = setOf(today, today - 1, today - 2, today - 5, today - 6, today - 7, today - 8)
        val (current, longest) = quranReadingStreaks(days, today)
        assertEquals(3, current)
        assertEquals(4, longest)
        assertEquals(2, quranReadingStreaks(days - today, today).first)
        assertEquals(0, quranReadingStreaks(setOf(today - 3), today).first)
    }
}
