package com.example

import com.example.ui.ziyaratLocations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZiyaratMapDataTest {

    @Test
    fun everyInfallibleIsLinkedToAZiyaratLocation() {
        val linkedIndices = ziyaratLocations
            .flatMap { it.masoomeenIndices }
            .toSet()

        assertEquals((1..14).toSet(), linkedIndices)
    }

    @Test
    fun locationsHaveUniqueIdsValidCoordinatesAndCompleteTranslations() {
        assertEquals(ziyaratLocations.size, ziyaratLocations.map { it.id }.distinct().size)

        ziyaratLocations.forEach { location ->
            assertTrue("Invalid latitude for ${location.id}", location.lat in -90.0..90.0)
            assertTrue("Invalid longitude for ${location.id}", location.lon in -180.0..180.0)

            listOf(
                location.nameEn, location.nameUr, location.nameAr,
                location.cityEn, location.cityUr, location.cityAr,
                location.descEn, location.descUr, location.descAr,
                location.tipsEn, location.tipsUr, location.tipsAr
            ).forEachIndexed { index, value ->
                assertTrue("Blank localized field $index for ${location.id}", value.isNotBlank())
            }
        }
    }

    @Test
    fun correctedShrineCoordinatesDoNotRegress() {
        val najaf = ziyaratLocations.single { it.id == "najaf" }
        assertEquals(31.99611, najaf.lat, 0.000001)
        assertEquals(44.31417, najaf.lon, 0.000001)

        val kadhimiya = ziyaratLocations.single { it.id == "baghdad_kadhim" }
        assertEquals(33.38000, kadhimiya.lat, 0.000001)
        assertEquals(44.33806, kadhimiya.lon, 0.000001)

        val tiflan = ziyaratLocations.single { it.id == "musayyib_tiflan" }
        assertEquals(32.785271, tiflan.lat, 0.000001)
        assertEquals(44.312849, tiflan.lon, 0.000001)
    }

    @Test
    fun fatimaMapPointIsExplicitlyNotAConfirmedGrave() {
        val fatima = ziyaratLocations.single { it.id == "medina_zahra" }

        assertEquals(listOf(3), fatima.masoomeenIndices)
        assertTrue(fatima.descEn.contains("does not identify a confirmed grave"))
        assertTrue(fatima.descUr.contains("تصدیق شدہ قبر کی نشاندہی نہیں کرتا"))
        assertTrue(fatima.descAr.contains("قبراً مؤكداً"))
    }
}
