package com.example.ui.academy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun AcademyQuizScreen(
    onBack: () -> Unit,
    currentLang: String,
    content: AcademyContent,
    repository: AcademyProgressRepository
) {
    var quizStarted by rememberSaveable { mutableStateOf(false) }
    var currentQuestionIndex by rememberSaveable { mutableStateOf(0) }
    var score by rememberSaveable { mutableStateOf(0) }
    var quizFinished by rememberSaveable { mutableStateOf(false) }
    var roundSeed by rememberSaveable { mutableStateOf(Random.nextInt()) }
    var pointsAwarded by rememberSaveable { mutableStateOf(0) }
    var showExitConfirm by rememberSaveable { mutableStateOf(false) }
    val progress by repository.progress.collectAsState()
    val questions = remember(roundSeed, content) { content.quiz.shuffled(Random(roundSeed)).take(10) }

    fun startQuiz() {
        currentQuestionIndex = 0
        score = 0
        pointsAwarded = 0
        quizFinished = false
        roundSeed = Random.nextInt()
        quizStarted = true
    }

    fun handleBack() {
        when {
            showExitConfirm -> showExitConfirm = false
            quizStarted && !quizFinished -> showExitConfirm = true
            else -> onBack()
        }
    }

    BackHandler { handleBack() }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text(tr(currentLang, "Leave this round?", "یہ راؤنڈ چھوڑیں؟", "مغادرة هذه الجولة؟")) },
            text = { Text(tr(currentLang, "This unfinished round will not be saved. Your previous best is safe.", "نامکمل راؤنڈ محفوظ نہیں ہوگا۔ پچھلا بہترین اسکور محفوظ رہے گا۔", "لن تُحفظ هذه الجولة، وستبقى أفضل نتيجة سابقة محفوظة.")) },
            confirmButton = {
                TextButton(onClick = { showExitConfirm = false; quizStarted = false }) {
                    Text(tr(currentLang, "Leave round", "راؤنڈ چھوڑیں", "مغادرة"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text(tr(currentLang, "Keep playing", "جاری رکھیں", "متابعة")) }
            }
        )
    }

    Scaffold(
        topBar = {
            AcademyTopBar(
                title = tr(currentLang, "Quiz Challenge", "کوئز چیلنج", "تحدي الأسئلة"),
                onBack = ::handleBack,
                accent = Color(0xFF00838F),
                currentLang = currentLang
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            when {
                quizFinished -> QuizResultScreen(
                    score = score,
                    total = questions.size,
                    bestScore = progress.bestQuizScore,
                    bestTotal = progress.bestQuizTotal,
                    pointsAwarded = pointsAwarded,
                    currentLang = currentLang,
                    onRetry = ::startQuiz,
                    onFinish = onBack
                )
                quizStarted && questions.isNotEmpty() -> QuizActiveScreen(
                    question = questions[currentQuestionIndex],
                    questionNumber = currentQuestionIndex + 1,
                    totalQuestions = questions.size,
                    currentLang = currentLang,
                    onAnswerSelected = { isCorrect ->
                        val newScore = score + if (isCorrect) 1 else 0
                        score = newScore
                        if (currentQuestionIndex < questions.lastIndex) {
                            currentQuestionIndex++
                        } else {
                            pointsAwarded = repository.recordQuiz(newScore, questions.size)
                            quizFinished = true
                        }
                    }
                )
                else -> QuizStartScreen(
                    bankSize = content.quiz.size,
                    roundSize = 10,
                    bestScore = progress.bestQuizScore,
                    bestTotal = progress.bestQuizTotal,
                    attempts = progress.quizAttempts,
                    currentLang = currentLang,
                    onStart = ::startQuiz
                )
            }
        }
    }
}

@Composable
fun QuizStartScreen(
    bankSize: Int,
    roundSize: Int,
    bestScore: Int,
    bestTotal: Int,
    attempts: Int,
    currentLang: String,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Quiz, contentDescription = null, tint = Color(0xFF00BCD4), modifier = Modifier.size(110.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            tr(currentLang, "Ready for a knowledge adventure?", "علم کی مہم کے لیے تیار؟", "هل أنت مستعد لمغامرة المعرفة؟"),
            fontSize = 29.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF00838F),
            textAlign = TextAlign.Center,
            lineHeight = 39.sp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "$roundSize " + tr(currentLang, "questions from a bank of ", "سوالات، کل بینک ", "أسئلة من بنك من ") + "$bankSize",
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        if (bestTotal > 0) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))) {
                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        tr(currentLang, "Best score", "بہترین اسکور", "أفضل نتيجة") + ": $bestScore / $bestTotal",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006064)
                    )
                    Text(
                        tr(currentLang, "Completed rounds", "مکمل راؤنڈز", "الجولات المكتملة") + ": $attempts",
                        color = Color(0xFF006064),
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        Text(
            tr(currentLang, "New points are earned when you improve your best score.", "بہترین اسکور بہتر بنانے پر نئے پوائنٹس ملیں گے۔", "تكسب نقاطاً جديدة عند تحسين أفضل نتيجة."),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(26.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1)),
            shape = RoundedCornerShape(29.dp)
        ) {
            Text(tr(currentLang, "Start Quiz", "کوئز شروع کریں", "ابدأ الاختبار"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuizActiveScreen(
    question: QuizQuestion,
    questionNumber: Int,
    totalQuestions: Int,
    currentLang: String,
    onAnswerSelected: (Boolean) -> Unit
) {
    var selectedOption by rememberSaveable(question.id) { mutableStateOf(-1) }
    var answered by rememberSaveable(question.id) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(
            tr(currentLang, "Question", "سوال", "سؤال") + " $questionNumber / $totalQuestions",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00838F)
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = questionNumber.toFloat() / totalQuestions,
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = Color(0xFF00BCD4),
            trackColor = Color(0xFFB2EBF2)
        )
        Spacer(Modifier.height(22.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                question.question.get(currentLang),
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 33.sp
            )
        }
        Spacer(Modifier.height(18.dp))
        question.options.forEachIndexed { index, option ->
            val isSelected = selectedOption == index
            val isCorrectOption = index == question.correctIndex
            val backgroundColor = when {
                !answered -> MaterialTheme.colorScheme.surface
                isCorrectOption -> Color(0xFFC8E6C9)
                isSelected -> Color(0xFFFFCDD2)
                else -> MaterialTheme.colorScheme.surface
            }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(enabled = !answered) {
                    selectedOption = index
                    answered = true
                },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor)
            ) {
                Row(modifier = Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        option.get(currentLang),
                        modifier = Modifier.weight(1f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )
                    if (answered && isCorrectOption) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = tr(currentLang, "Correct", "درست", "صحيح"), tint = Color(0xFF2E7D32))
                    } else if (answered && isSelected) {
                        Icon(Icons.Rounded.Cancel, contentDescription = tr(currentLang, "Try again next time", "اگلی بار کوشش کریں", "حاول لاحقاً"), tint = Color(0xFFC62828))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (answered) {
            Text(
                if (selectedOption == question.correctIndex) {
                    tr(currentLang, "MashaAllah—correct!", "ما شاء اللہ — درست!", "ما شاء الله، إجابة صحيحة!")
                } else {
                    tr(currentLang, "The correct answer is highlighted.", "درست جواب نمایاں ہے۔", "الإجابة الصحيحة مظللة.")
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = if (selectedOption == question.correctIndex) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onAnswerSelected(selectedOption == question.correctIndex) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1)),
                shape = RoundedCornerShape(27.dp)
            ) {
                Text(
                    if (questionNumber == totalQuestions) tr(currentLang, "Finish Quiz", "کوئز مکمل کریں", "إنهاء الاختبار") else tr(currentLang, "Next Question", "اگلا سوال", "السؤال التالي"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuizResultScreen(
    score: Int,
    total: Int,
    bestScore: Int,
    bestTotal: Int,
    pointsAwarded: Int,
    currentLang: String,
    onRetry: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(130.dp))
        Spacer(Modifier.height(18.dp))
        Text(tr(currentLang, "Quiz Complete!", "کوئز مکمل!", "اكتمل الاختبار!"), fontSize = 31.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00838F))
        Spacer(Modifier.height(12.dp))
        Text("$score / $total", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00ACC1))
        if (bestTotal > 0) {
            Text(tr(currentLang, "Your saved best", "آپ کا محفوظ بہترین اسکور", "أفضل نتيجة محفوظة") + ": $bestScore / $bestTotal", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(18.dp))
        Surface(color = if (pointsAwarded > 0) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
            Text(
                if (pointsAwarded > 0) {
                    "+$pointsAwarded " + tr(currentLang, "new best-score points", "بہترین اسکور کے نئے پوائنٹس", "نقاط تحسين النتيجة")
                } else {
                    tr(currentLang, "No new points this time—improve your best to earn more.", "اس بار نئے پوائنٹس نہیں — بہترین اسکور بڑھائیں۔", "لا نقاط جديدة؛ حسن أفضل نتيجة لتكسب المزيد.")
                },
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (pointsAwarded > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1))
        ) { Text(tr(currentLang, "Try another round", "نیا راؤنڈ کھیلیں", "جولة أخرى")) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
            Text(tr(currentLang, "Return to Academy", "اکیڈمی پر واپس", "العودة إلى الأكاديمية"))
        }
    }
}
