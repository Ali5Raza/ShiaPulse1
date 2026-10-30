package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.EventType
import com.example.data.IslamicEventManager
import com.example.data.MasoomEvent
import com.example.data.MasoomeenData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class IslamicEventManagerTest {

    @Test
    fun serializationRoundTripPreservesEveryLocalizedField() {
        val event = MasoomEvent(
            id = "event_test",
            title = "Test Event",
            titleUrdu = "آزمائشی مناسبت",
            titleArabic = "مناسبة تجريبية",
            dateStringHijri = "13 Rajab",
            hijriMonth = 7,
            hijriDay = 13,
            eventType = EventType.WILADAT,
            masoomIndex = 2,
            description = "English description",
            descriptionUrdu = "اردو تفصیل",
            descriptionArabic = "الوصف بالعربية",
            region = "SOUTH_ASIA",
            sourceUrl = "https://example.com/source"
        )

        val restored = IslamicEventManager.parseJsonToEvents(
            IslamicEventManager.eventsToJsonString(listOf(event))
        )

        assertEquals(listOf(event), restored)
    }

    @Test
    fun bundledEventsContainCompleteLocalizedContent() {
        val asset = listOf(
            File("src/main/assets/islamic_events.json"),
            File("app/src/main/assets/islamic_events.json")
        ).firstOrNull { it.isFile } ?: error("Bundled Islamic events asset was not found")

        val events = IslamicEventManager.parseJsonToEvents(asset.readText())
        assertEquals(65, events.size)
        assertEquals(30, events.count { it.eventType == EventType.WILADAT })
        assertEquals(35, events.count { it.eventType == EventType.SHAHADAT })
        assertEquals(events.size, events.map { it.id }.distinct().size)
        assertEquals(
            events.size,
            events.map { "${it.dateStringHijri.lowercase()}_${it.title.lowercase()}" }.distinct().size
        )
        assertEquals((1..14).toSet(), events.map { it.masoomIndex }.filter { it > 0 }.toSet())

        val requiredEventIds = setOf(
            "event_islamic_new_year",
            "event_karbala_arrival",
            "event_karbala_water_cutoff",
            "event_eid_zahra",
            "event_masuma_qum_birth",
            "event_masuma_qum_shahadat",
            "event_bibi_fatima_shahadat_13",
            "event_eid_bithah",
            "event_bibi_khadija_shahadat",
            "event_imam_ali_struck",
            "event_laylatul_qadr_19",
            "event_laylatul_qadr_21",
            "event_laylatul_qadr_23",
            "event_baqi_destruction",
            "event_ali_fatima_marriage",
            "event_day_arafah",
            "event_muslim_hani_shahadat",
            "event_eid_fitr",
            "event_eid_adha",
            "event_bibi_zainab_shahadat",
            "event_abu_talib_shahadat",
            "event_hamza_uhud_shahadat",
            "event_umm_ul_banin_shahadat",
            "event_sayyid_muhammad_shahadat",
            "event_sukayna_shahadat",
            "event_abdul_muttalib_shahadat",
            "event_mukhtar_shahadat",
            "event_maytham_shahadat"
        )
        assertTrue(events.map { it.id }.containsAll(requiredEventIds))

        events.forEach { event ->
            assertTrue(event.id.isNotBlank())
            assertTrue(event.title.isNotBlank())
            assertTrue(event.titleUrdu.isNotBlank())
            assertTrue(event.titleArabic.isNotBlank())
            assertTrue(event.description.isNotBlank())
            assertTrue(event.descriptionUrdu.isNotBlank())
            assertTrue(event.descriptionArabic.isNotBlank())
            assertTrue(event.sourceUrl.startsWith("https://") || event.sourceUrl.startsWith("http://"))
            assertTrue(event.hijriMonth in 1..12)
            assertTrue(event.hijriDay in 1..30)
            assertTrue(event.masoomIndex in 0..14)
            assertTrue(event.region in setOf("ALL", "SOUTH_ASIA", "IRAN_HAWZA"))
            assertEquals(event.titleUrdu, event.localizedTitle("ur"))
            assertEquals(event.titleArabic, event.localizedTitle("ar"))
            assertEquals(event.descriptionUrdu, event.localizedDescription("ur"))
            assertEquals(event.descriptionArabic, event.localizedDescription("ar"))
        }
    }

    @Test
    fun regionalNarrationsAndRuqayyaSukaynaRemainDistinct() {
        val asset = listOf(
            File("src/main/assets/islamic_events.json"),
            File("app/src/main/assets/islamic_events.json")
        ).firstOrNull { it.isFile } ?: error("Bundled Islamic events asset was not found")
        val events = IslamicEventManager.parseJsonToEvents(asset.readText())
        val byId = events.associateBy { it.id }

        assertTrue(byId.containsKey("event_imam_hasan_shahadat_7"))
        assertTrue(byId.containsKey("event_imam_hasan_shahadat_28"))
        assertTrue(byId.containsKey("event_imam_reza_shahadat_17"))
        assertTrue(byId.containsKey("event_imam_reza_shahadat_30"))
        assertTrue(byId.containsKey("event_bibi_fatima_shahadat"))
        assertTrue(byId.containsKey("event_bibi_fatima_shahadat_13"))
        assertTrue(byId.containsKey("event_sukayna_shahadat"))
        assertTrue(byId.getValue("event_bibi_sakina_5_safar").title.contains("Ruqayya"))
        assertTrue(!byId.getValue("event_bibi_sakina_5_safar").title.contains("Sakina"))
        assertTrue(!byId.getValue("event_bibi_sakina_13_safar").title.contains("Sakina"))
        assertEquals(3, events.count { it.id.startsWith("event_laylatul_qadr_") })
    }

    @Test
    fun exactDateMatchingUsesCanonicalNumbersAndSupportsLegacyJson() {
        val event = MasoomEvent(
            title = "Test Event",
            dateStringHijri = "1 Muharram",
            eventType = EventType.WILADAT,
            description = "Description",
            hijriMonth = 7,
            hijriDay = 13
        )

        assertTrue(event.occursOn(13, 7, "Rajab"))
        assertTrue(!event.occursOn(1, 1, "Muharram"))

        val legacyEvent = event.copy(hijriMonth = 0, hijriDay = 0)
        assertTrue(legacyEvent.occursOn(1, 1, "Muharram"))
        assertTrue(!legacyEvent.occursOn(13, 7, "Rajab"))
    }

    @Test
    fun gregorianEventsAreAlsoCompletelyLocalized() {
        assertEquals(15, MasoomeenData.gregorianEvents.size)
        MasoomeenData.gregorianEvents.forEach { event ->
            assertTrue(event.titleUrdu.isNotBlank())
            assertTrue(event.titleArabic.isNotBlank())
            assertTrue(event.descriptionUrdu.isNotBlank())
            assertTrue(event.descriptionArabic.isNotBlank())
            assertEquals(event.titleUrdu, event.localizedTitle("ur"))
            assertEquals(event.titleArabic, event.localizedTitle("ar"))
            assertEquals(event.descriptionUrdu, event.localizedDescription("ur"))
            assertEquals(event.descriptionArabic, event.localizedDescription("ar"))
        }
    }

    @Test
    fun localizedContentUsesSelectedLanguageAndSafeFallback() {
        val event = MasoomEvent(
            title = "English title",
            titleUrdu = "اردو عنوان",
            titleArabic = "عنوان عربي",
            dateStringHijri = "1 Muharram",
            eventType = EventType.WILADAT,
            description = "English description",
            descriptionUrdu = "اردو تفصیل",
            descriptionArabic = "وصف عربي"
        )

        assertEquals("اردو عنوان", event.localizedTitle("ur"))
        assertEquals("عنوان عربي", event.localizedTitle("ar"))
        assertEquals("English title", event.localizedTitle("en"))
        assertEquals("اردو تفصیل", event.localizedDescription("ur"))
        assertEquals("وصف عربي", event.localizedDescription("ar"))
        assertEquals("English title", event.copy(titleArabic = "").localizedTitle("ar"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyRemoteCollectionCannotEraseTheCalendar() {
        IslamicEventManager.saveEventsJson(
            ApplicationProvider.getApplicationContext(),
            "[]"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun remoteEventWithoutTitleIsRejected() {
        IslamicEventManager.saveEventsJson(
            ApplicationProvider.getApplicationContext(),
            """[{"title":"","dateStringHijri":"1 Muharram","eventType":"WILADAT"}]"""
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownEventTypeIsRejectedInsteadOfBecomingABirthCelebration() {
        IslamicEventManager.parseJsonToEvents(
            """[{"title":"Invalid","dateStringHijri":"1 Muharram","eventType":"UNKNOWN"}]"""
        )
    }
}
