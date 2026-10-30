package com.example.ui.academy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier

@Composable
fun LearnSalahScreen(
    onBack: () -> Unit,
    currentLang: String,
    content: AcademyContent,
    repository: AcademyProgressRepository,
    speech: AcademySpeechController
) {
    var currentStepIndex by rememberSaveable { mutableStateOf(0) }
    var mode by rememberSaveable { mutableStateOf("lesson") }
    var pointsAwarded by rememberSaveable { mutableStateOf(0) }
    val steps = content.salah
    val palette = academyPalette(1)
    val reviewQuestions = remember(content) { content.quiz.filter { it.category == "salah" } }
    val alreadyComplete = "salah" in repository.progress.value.completedLessons
    val reviewQuestion = reviewQuestions.getOrElse(if (alreadyComplete && reviewQuestions.size > 1) 1 else 0) {
        content.quiz.first()
    }

    fun handleBack() {
        speech.stop()
        when {
            mode == "reward" -> onBack()
            mode == "review" -> mode = "lesson"
            currentStepIndex > 0 -> currentStepIndex--
            else -> onBack()
        }
    }

    BackHandler { handleBack() }
    LaunchedEffect(currentStepIndex) {
        repository.recordLessonStep("salah", currentStepIndex + 1)
    }
    LaunchedEffect(currentStepIndex, mode) { speech.stop() }

    Scaffold(
        topBar = {
            AcademyTopBar(
                title = tr(currentLang, "Learn Salah", "نماز سیکھیں", "تعلم الصلاة"),
                onBack = ::handleBack,
                accent = palette.accent,
                currentLang = currentLang
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            when (mode) {
                "reward" -> AcademyRewardContent(
                    title = tr(currentLang, "Prayer Hero!", "نماز کا ہیرو!", "بطل الصلاة!"),
                    message = tr(
                        currentLang,
                        "You completed the introductory Salah overview and review. This is a learning milestone, not a replacement for practising with a parent or scholar.",
                        "آپ نے نماز کا ابتدائی سبق اور جائزہ مکمل کیا۔ یہ سیکھنے کا مرحلہ ہے؛ والدین یا عالم کے ساتھ مشق کا متبادل نہیں۔",
                        "أكملت مقدمة الصلاة ومراجعتها. هذا إنجاز تعليمي ولا يغني عن التدريب مع الوالدين أو العالم."
                    ),
                    pointsAwarded = pointsAwarded,
                    currentLang = currentLang,
                    accent = palette.accent,
                    buttonText = tr(currentLang, "Collect and return", "انعام لے کر واپس", "استلم وعُد"),
                    onComplete = onBack
                )
                "review" -> AcademyReviewQuestion(
                    question = reviewQuestion.question.get(currentLang),
                    options = reviewQuestion.options.map { it.get(currentLang) },
                    correctIndex = reviewQuestion.correctIndex,
                    currentLang = currentLang,
                    palette = palette,
                    onCorrect = {
                        pointsAwarded = repository.completeLesson("salah", steps.size, AcademyProgressRepository.SALAH_REWARD)
                        mode = "reward"
                    }
                )
                else -> AcademyStepPager(
                    step = steps[currentStepIndex],
                    currentIndex = currentStepIndex,
                    totalSteps = steps.size,
                    onPrevious = { if (currentStepIndex > 0) currentStepIndex-- },
                    onNext = {
                        if (currentStepIndex < steps.lastIndex) currentStepIndex++ else mode = "review"
                    },
                    currentLang = currentLang,
                    speech = speech,
                    palette = palette,
                    icon = Icons.Rounded.Mosque,
                    audioId = "salah"
                )
            }
        }
    }
}
