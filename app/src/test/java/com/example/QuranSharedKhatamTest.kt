package com.example

import com.example.data.QuranSharedJuz
import com.example.data.QuranSharedKhatamSnapshot
import com.example.data.normalizeSharedKhatamCode
import com.example.data.sharedKhatamDailyJuzGoal
import com.example.data.sharedKhatamDaysRemaining
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranSharedKhatamTest {
    @Test
    fun inviteCodesAreNormalizedForSharing() {
        assertEquals("ABC234", normalizeSharedKhatamCode(" ab-c2 34 "))
        assertEquals("ABC234XY", normalizeSharedKhatamCode("abc234xyz9"))
    }

    @Test
    fun sharedSnapshotCountsOnlyAssignedAndCompletedJuz() {
        val juz = (1..30).map { number ->
            when (number) {
                1 -> QuranSharedJuz(number, userId = "user-a", displayName = "A", completed = true)
                2 -> QuranSharedJuz(number, userId = "user-b", displayName = "B")
                else -> QuranSharedJuz(number)
            }
        }
        val snapshot = QuranSharedKhatamSnapshot(
            code = "ABC234",
            title = "Family Khatam",
            ownerId = "user-a",
            createdAt = 1L,
            targetDays = 30,
            targetStartedAt = 1L,
            members = emptyMap(),
            juz = juz
        )

        assertEquals(1, snapshot.completedCount)
        assertEquals(2, snapshot.assignedCount)
        assertTrue(snapshot.juz[0].isAssigned)
        assertFalse(snapshot.juz[2].isAssigned)
        assertEquals(30, snapshot.juz.size)
    }

    @Test
    fun targetCalculatesRemainingDaysAndSuggestedPace() {
        val day = 24L * 60L * 60L * 1000L
        assertEquals(20, sharedKhatamDaysRemaining(0L, 30, 10L * day))
        assertEquals(0, sharedKhatamDaysRemaining(0L, 30, 31L * day))
        assertEquals(2, sharedKhatamDailyJuzGoal(completedCount = 0, daysRemaining = 20))
        assertEquals(1, sharedKhatamDailyJuzGoal(completedCount = 29, daysRemaining = 0))
        assertEquals(0, sharedKhatamDailyJuzGoal(completedCount = 30, daysRemaining = 0))
    }
}
