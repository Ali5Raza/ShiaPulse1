package com.example.ui.academy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun LearnWuduScreen(
    onBack: () -> Unit,
    currentLang: String,
    content: AcademyContent,
    repository: AcademyProgressRepository,
    speech: AcademySpeechController
) {
    var currentStepIndex by rememberSaveable { mutableStateOf(0) }
    var mode by rememberSaveable { mutableStateOf("lesson") }
    var pointsAwarded by rememberSaveable { mutableStateOf(0) }
    val steps = content.wudu
    val palette = academyPalette(0)
    val reviewQuestions = remember(content) { content.quiz.filter { it.category == "wudu" } }
    val alreadyComplete = "wudu" in repository.progress.value.completedLessons
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
        repository.recordLessonStep("wudu", currentStepIndex + 1)
    }
    LaunchedEffect(currentStepIndex, mode) { speech.stop() }

    Scaffold(
        topBar = {
            AcademyTopBar(
                title = tr(currentLang, "Learn Wudu", "وضو سیکھیں", "تعلم الوضوء"),
                onBack = ::handleBack,
                accent = palette.accent,
                currentLang = currentLang
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            when (mode) {
                "reward" -> AcademyRewardContent(
                    title = tr(currentLang, "Wudu Master!", "وضو میں ماہر!", "بطل الوضوء!"),
                    message = tr(
                        currentLang,
                        "You completed the introductory Wudu lesson and its review. Keep practising with a parent.",
                        "آپ نے وضو کا ابتدائی سبق اور جائزہ مکمل کیا۔ والدین کے ساتھ مشق جاری رکھیں۔",
                        "أكملت درس الوضوء التمهيدي ومراجعته. واصل التدريب مع والديك."
                    ),
                    pointsAwarded = pointsAwarded,
                    currentLang = currentLang,
                    accent = palette.accent,
                    buttonText = tr(currentLang, "Return to Academy", "اکیڈمی پر واپس", "العودة إلى الأكاديمية"),
                    onComplete = onBack
                )
                "review" -> AcademyReviewQuestion(
                    question = reviewQuestion.question.get(currentLang),
                    options = reviewQuestion.options.map { it.get(currentLang) },
                    correctIndex = reviewQuestion.correctIndex,
                    currentLang = currentLang,
                    palette = palette,
                    onCorrect = {
                        pointsAwarded = repository.completeLesson("wudu", steps.size, AcademyProgressRepository.WUDU_REWARD)
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
                    icon = Icons.Rounded.WaterDrop,
                    audioId = "wudu"
                )
            }
        }
    }
}
