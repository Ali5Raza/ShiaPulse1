package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.QuranAssetLoader
import com.example.data.QuranReferenceParser
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuranAssetTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun completeBundleContainsAllSurahsAndEveryCanonicalVerse() {
        val index = QuranAssetLoader.loadIndex(context)
        assertEquals(QuranAssetLoader.TOTAL_SURAHS, index.size)
        assertEquals(QuranAssetLoader.TOTAL_VERSES, index.sumOf { it.verseCount })
        assertEquals((1..114).toList(), index.map { it.number })

        index.forEach { info ->
            val surah = QuranAssetLoader.loadSurah(context, info)
            assertEquals(info.number, surah.number)
            assertEquals("Verse count mismatch in Surah ${info.number}", info.verseCount, surah.verses.size)
            surah.verses.forEachIndexed { index, ayah ->
                assertEquals("Non-sequential ayah in Surah ${info.number}", index + 1, ayah.number)
                assertTrue("Missing Arabic at ${info.number}:${ayah.number}", ayah.arabic.isNotBlank())
                assertTrue("Missing Qarai English at ${info.number}:${ayah.number}", ayah.english.isNotBlank())
                assertTrue("Missing Jawadi Urdu at ${info.number}:${ayah.number}", ayah.urdu.isNotBlank())
            }
        }
    }

    @Test
    fun canonicalFirstAndLastVersesRemainVerbatim() {
        val index = QuranAssetLoader.loadIndex(context)
        val fatiha = QuranAssetLoader.loadSurah(context, index.first()).verses.first()
        assertEquals("بِسْمِ اللَّهِ الرَّحْمَـٰنِ الرَّحِيمِ", fatiha.arabic)
        assertEquals("In the Name of Allah, the All-beneficent, the All-merciful.", fatiha.english)
        assertEquals("عظیم اور دائمی رحمتوں والے خدا کے نام سے", fatiha.urdu)

        val nas = QuranAssetLoader.loadSurah(context, index.last()).verses.last()
        assertEquals("مِنَ الْجِنَّةِ وَالنَّاسِ", nas.arabic)
        assertEquals("from among the jinn and humans.’", nas.english)
        assertEquals("وہ جنات میں سے ہو یا انسانوں میں سے", nas.urdu)
    }

    @Test
    fun everyBundledDailyAyatCitationOpensAValidOfflineReference() {
        val index = QuranAssetLoader.loadIndex(context)
        val ayats = JSONArray(
            context.assets.open("ayats.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        )
        assertTrue(ayats.length() > 0)

        for (position in 0 until ayats.length()) {
            val source = ayats.getJSONObject(position).getString("source")
            val reference = QuranReferenceParser.parse(source, index)
                ?: error("Unresolved Daily Ayat reference: $source")
            val surah = index.first { it.number == reference.surahNumber }
            val ayahNumber = reference.ayahNumber ?: error("Missing ayah number: $source")
            assertTrue(ayahNumber in 1..surah.verseCount)
        }

        val numeric = QuranReferenceParser.parse("Quran 2:255", index)
        assertEquals(2, numeric?.surahNumber)
        assertEquals(255, numeric?.ayahNumber)
        assertNull(QuranReferenceParser.parse("Surah Al-Fatihah, Verse 8", index))
    }

    @Test
    fun attributionAndSourceManifestAreBundled() {
        val attribution = context.assets.open("Quran/LICENSE-TANZIL.txt")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        assertTrue(attribution.contains("Tanzil Quran Text"))
        assertTrue(attribution.contains("https://tanzil.net/trans/en.qarai"))
        assertTrue(attribution.contains("https://tanzil.net/trans/ur.jawadi"))

        val manifest = context.assets.open("Quran/source_manifest.json")
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        assertTrue(manifest.contains("\"surahCount\": 114"))
        assertTrue(manifest.contains("\"verseCount\": 6236"))
        assertTrue(manifest.contains("\"crossSourceMismatches\": 0"))
    }
}
