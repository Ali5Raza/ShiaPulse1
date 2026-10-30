package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.academy.AcademyContentLoader
import com.example.ui.academy.AcademyProgressRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class AcademyProgressRepositoryTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearPreferences()
    }

    @After
    fun tearDown() = clearPreferences()

    @Test
    fun lessonAndDuaRewardsPersistWithoutDuplication() {
        val repository = AcademyProgressRepository(context)

        assertEquals(50, repository.completeLesson("wudu", 9, 50))
        assertEquals(0, repository.completeLesson("wudu", 9, 50))
        assertEquals(10, repository.markDuaRead(1))
        assertEquals(0, repository.markDuaRead(1))

        val restored = AcademyProgressRepository(context).progress.value
        assertEquals(60, restored.points)
        assertTrue("wudu" in restored.completedLessons)
        assertTrue(1 in restored.readDuas)
        assertTrue(restored.daily.dua)
        assertTrue(restored.daily.learning)
        assertEquals(1, restored.currentStreak)
    }

    @Test
    fun favoritesAndGamesArePersistentAndIdempotent() {
        val repository = AcademyProgressRepository(context)

        assertTrue(repository.toggleDuaFavorite(3))
        assertFalse(repository.toggleDuaFavorite(3))
        assertTrue(repository.toggleDuaFavorite(3))
        assertEquals(25, repository.recordGame("memory"))
        assertEquals(0, repository.recordGame("memory"))

        val restored = AcademyProgressRepository(context).progress.value
        assertTrue(3 in restored.favoriteDuas)
        assertTrue("memory" in restored.completedGames)
        assertEquals(25, restored.points)
    }

    @Test
    fun quizRewardsOnlyBestScoreImprovements() {
        val repository = AcademyProgressRepository(context)

        assertEquals(50, repository.recordQuiz(5, 10))
        assertEquals(0, repository.recordQuiz(5, 10))
        assertEquals(30, repository.recordQuiz(8, 10))
        assertEquals(0, repository.recordQuiz(3, 10))

        val result = repository.progress.value
        assertEquals(4, result.quizAttempts)
        assertEquals(8, result.bestQuizScore)
        assertEquals(10, result.bestQuizTotal)
        assertEquals(80, result.points)
        assertTrue(result.daily.quiz)
    }

    @Test
    fun kindnessCanOnlyBeRewardedOncePerDay() {
        val repository = AcademyProgressRepository(context)

        assertTrue(repository.markKindnessToday())
        assertFalse(repository.markKindnessToday())
        assertEquals(5, repository.progress.value.points)
        assertTrue(repository.progress.value.daily.kindness)
        assertEquals(1, repository.progress.value.kindnessDates.size)
    }

    @Test
    fun parentPinIsHashedVerifiedAndPreservedByProgressReset() {
        val repository = AcademyProgressRepository(context)
        assertFalse(repository.setParentPin("12"))
        assertFalse(repository.setParentPin("abcd"))
        assertTrue(repository.setParentPin("2468"))
        assertTrue(repository.verifyParentPin("2468"))
        assertFalse(repository.verifyParentPin("1357"))

        val prefs = context.getSharedPreferences("shia_academy_progress_v2", Context.MODE_PRIVATE)
        assertNotEquals("2468", prefs.getString("parent_pin_hash", null))
        repository.completeLesson("salah", 11, 100)
        repository.resetLearningProgress()

        assertEquals(0, repository.progress.value.points)
        assertTrue(repository.hasParentPin())
        assertTrue(repository.verifyParentPin("2468"))
    }

    @Test
    fun bundledContentIsCompleteAndFullyLocalized() {
        val content = AcademyContentLoader.load(context)

        assertFalse(content.loadError)
        assertTrue(content.isUsable)
        assertEquals(8, content.wudu.size)
        assertEquals(10, content.salah.size)
        assertEquals(15, content.duas.size)
        assertEquals(5, content.aqaid.size)
        assertEquals(14, content.stories.size)
        assertEquals(30, content.quiz.size)
        assertEquals(12, content.trueFalse.size)

        val localized = buildList {
            content.wudu.forEach { add(it.title); add(it.description) }
            content.salah.forEach { add(it.title); add(it.description) }
            content.duas.forEach { add(it.title); add(it.translation) }
            (content.aqaid + content.stories).forEach {
                add(it.title); add(it.description); addAll(it.pages); add(it.question); addAll(it.options); add(it.badge)
            }
            content.quiz.forEach { add(it.question); addAll(it.options) }
            content.trueFalse.forEach { add(it.statement) }
        }
        assertTrue(localized.all { text -> listOf("en", "ur", "ar").all { language -> text.values[language]?.isNotBlank() == true } })
        assertTrue(content.duas.all { it.arabic.isNotBlank() && it.source.isNotBlank() })
        assertTrue((content.aqaid + content.stories).all { it.pages.size >= 3 && it.correctIndex in it.options.indices })
        assertTrue(content.quiz.all { it.correctIndex in it.options.indices })
        assertEquals(content.quiz.size, content.quiz.map { it.id }.distinct().size)
        assertEquals(content.duas.size, content.duas.map { it.id }.distinct().size)
    }

    private fun clearPreferences() {
        context.getSharedPreferences("shia_academy_progress_v2", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
