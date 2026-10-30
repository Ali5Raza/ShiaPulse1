package com.example

import com.example.data.KnowledgeReferenceType
import com.example.data.quranReference
import com.example.ui.KnowledgeNameReferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeReferenceTest {
    @Test
    fun quranReferenceMarksRelatedVersesWithAnInternalRoute() {
        val reference = quranReference(2, 245, direct = false)

        assertEquals(KnowledgeReferenceType.QURAN, reference.type)
        assertEquals("Qur’an 2:245", reference.citation)
        assertEquals(2, reference.surahNumber ?: -1)
        assertEquals(245, reference.ayahNumber ?: -1)
        assertTrue(reference.url.isEmpty())
        assertFalse(reference.direct)
    }

    @Test
    fun everyAllahNameHasAReference() {
        (1..99).forEach { id ->
            assertTrue("Missing reference for Allah name $id", KnowledgeNameReferences.forAllahName(id).isNotEmpty())
        }
    }

    @Test
    fun unresolvedProphetHonorificsRemainExplicitlyPending() {
        val pending = (1..34).filter { KnowledgeNameReferences.forProphetName(it).isEmpty() }

        assertEquals(listOf(3, 21, 22, 24), pending)
        assertTrue(KnowledgeNameReferences.forProphetName(1).isNotEmpty())
        assertTrue(KnowledgeNameReferences.forProphetName(34).isNotEmpty())
    }
}
