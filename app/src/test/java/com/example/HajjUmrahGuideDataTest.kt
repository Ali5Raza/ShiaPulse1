package com.example

import com.example.ui.GuideReferenceType
import com.example.ui.GuideRequirement
import com.example.ui.GuideStep
import com.example.ui.LocalizedGuideText
import com.example.ui.hajjGuideSteps
import com.example.ui.hajjJourneyStops
import com.example.ui.umrahGuideSteps
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HajjUmrahGuideDataTest {

    private val allSteps = umrahGuideSteps() + hajjGuideSteps()

    @Test
    fun checklistIdsAreUniqueAndLegacyIdsKeepTheirSteps() {
        val umrah = umrahGuideSteps()
        val hajj = hajjGuideSteps()

        assertEquals(listOf(1, 2, 5, 3, 4, 6), umrah.map { it.id })
        assertEquals((1..7).toList(), hajj.map { it.id })
        assertEquals(umrah.size, umrah.map { it.id }.distinct().size)
        assertEquals(hajj.size, hajj.map { it.id }.distinct().size)

        assertEquals("Miqat & Ihram", umrah.single { it.id == 1 }.title.en)
        assertEquals("Tawaf — seven circuits", umrah.single { it.id == 2 }.title.en)
        assertEquals("Sa‘i — Safa to Marwah", umrah.single { it.id == 3 }.title.en)
        assertTrue(umrah.single { it.id == 4 }.title.en.startsWith("Taqsir"))
        assertEquals(GuideRequirement.CONDITIONAL, umrah.single { it.id == 6 }.requirement)
        assertEquals(GuideRequirement.RECOMMENDED, hajj.single { it.id == 7 }.requirement)
    }

    @Test
    fun everyStepHasScripturalOrImamContentAndVerifiableSources() {
        allSteps.forEach { step ->
            assertTrue("${step.id} has no reference", step.references.isNotEmpty())
            assertTrue(
                "${step.id} has only a fiqh reference and no dua",
                step.references.any { it.type != GuideReferenceType.FIQH } || step.duas.isNotEmpty()
            )
            step.assertLocalizedText()

            step.references.forEach { reference ->
                reference.citation.assertComplete()
                reference.summary.assertComplete()
                if (reference.type == GuideReferenceType.QURAN) {
                    assertTrue(reference.url.isEmpty())
                    assertTrue((reference.surahNumber ?: 0) in 1..114)
                    assertTrue((reference.ayahNumber ?: 0) > 0)
                } else {
                    reference.url.assertTrustedHttpsUrl()
                }
            }
            step.duas.forEach { dua ->
                dua.title.assertComplete()
                dua.body.assertComplete()
                dua.citation.assertComplete()
                dua.url.assertTrustedHttpsUrl()
                dua.arabic?.let { arabic -> assertTrue(arabic.isNotBlank()) }
            }
        }
    }

    @Test
    fun journeyDiagramHasSevenOrderedLocalizedStops() {
        val stops = hajjJourneyStops()

        assertEquals((1..7).toList(), stops.map { it.order })
        assertEquals(
            listOf("Makkah", "Mina", "Arafat", "Muzdalifah — al-Mash‘ar al-Haram", "Mina", "Makkah", "Return to Mina"),
            stops.map { it.location.en }
        )
        stops.forEach { stop ->
            stop.location.assertComplete()
            stop.date.assertComplete()
            stop.ritual.assertComplete()
        }
        assertTrue(stops[2].ritual.en.contains("Dua Arafah"))
        assertTrue(stops[4].ritual.en.contains("Jamrat al-Aqabah → Hady → Halq"))
        assertTrue(stops[6].date.en.contains("13th"))
    }

    @Test
    fun keyQuranHadithDuaAndImamReferencesDoNotRegress() {
        val umrah = umrahGuideSteps()
        val hajj = hajjGuideSteps()

        assertTrue(umrah.single { it.id == 1 }.hasQuran("2:196"))
        assertTrue(umrah.single { it.id == 2 }.hasQuran("22:29"))
        assertTrue(umrah.single { it.id == 5 }.references.any {
            it.type == GuideReferenceType.HADITH && it.citation.en.contains("Imam al-Sadiq")
        })
        assertTrue(umrah.single { it.id == 5 }.duas.any { it.title.en.contains("Zamzam") })
        assertTrue(umrah.single { it.id == 3 }.hasQuran("2:158"))
        assertTrue(umrah.single { it.id == 3 }.references.any { it.type == GuideReferenceType.IMAM })
        assertTrue(umrah.single { it.id == 4 }.hasQuran("48:27"))
        assertTrue(umrah.single { it.id == 6 }.references.any {
            it.citation.en.contains("Rule 139") && it.url.endsWith("/2086/")
        })

        assertTrue(hajj.single { it.id == 1 }.hasQuran("22:27"))
        assertTrue(hajj.single { it.id == 2 }.hasQuran("2:199"))
        assertTrue(hajj.single { it.id == 2 }.references.any { it.type == GuideReferenceType.HADITH })
        assertTrue(hajj.single { it.id == 2 }.duas.any {
            it.title.en.contains("Imam Hussain") && it.url.endsWith("dua-arafah-imam-husain.html")
        })
        assertTrue(hajj.single { it.id == 2 }.duas.any { it.title.en.contains("Supplication 47") })
        assertTrue(hajj.single { it.id == 3 }.hasQuran("2:199"))
        assertTrue(hajj.single { it.id == 4 }.hasQuran("22:36–37"))
        val rangedVerse = hajj.single { it.id == 4 }.references.single {
            it.type == GuideReferenceType.QURAN && it.citation.en == "Qur’an 22:36–37"
        }
        assertEquals(22, rangedVerse.surahNumber ?: -1)
        assertEquals(36, rangedVerse.ayahNumber ?: -1)
        assertTrue(hajj.single { it.id == 4 }.duas.any { it.title.en.contains("Qur’an 6:162") })
        assertTrue(hajj.single { it.id == 5 }.hasQuran("22:29"))
        assertTrue(hajj.single { it.id == 6 }.hasQuran("2:203"))
        assertTrue(hajj.single { it.id == 6 }.duas.any { it.title.en.contains("Takbir") })
        assertTrue(hajj.single { it.id == 7 }.references.any { it.type == GuideReferenceType.IMAM })
    }

    private fun GuideStep.assertLocalizedText() {
        title.assertComplete()
        description.assertComplete()
        note?.assertComplete()
    }

    private fun LocalizedGuideText.assertComplete() {
        assertTrue(en.isNotBlank())
        assertTrue(ur.isNotBlank())
        assertTrue(ar.isNotBlank())
    }

    private fun GuideStep.hasQuran(verse: String): Boolean = references.any {
        it.type == GuideReferenceType.QURAN && it.citation.en == "Qur’an $verse"
    }

    private fun String.assertTrustedHttpsUrl() {
        val uri = URI(this)
        assertEquals("https", uri.scheme)
        assertTrue(
            "Untrusted reference host: $this",
            uri.host in setOf("www.sistani.org", "www.duas.org")
        )
        assertTrue(uri.path.isNotBlank())
    }
}
