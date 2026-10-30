package com.example.ui.academy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AqaidScreen(
    onBack: () -> Unit,
    currentLang: String,
    content: AcademyContent,
    repository: AcademyProgressRepository,
    speech: AcademySpeechController
) {
    var selectedLessonId by rememberSaveable { mutableStateOf(-1) }
    var currentPage by rememberSaveable { mutableStateOf(0) }
    var mode by rememberSaveable { mutableStateOf("pages") }
    var pointsAwarded by rememberSaveable { mutableStateOf(0) }
    val progress by repository.progress.collectAsState()
    val lesson = content.aqaid.firstOrNull { it.id == selectedLessonId }
    val palette = academyPalette(selectedLessonId.coerceAtLeast(0) + 1)

    fun handleBack() {
        speech.stop()
        when {
            lesson == null -> onBack()
            mode == "reward" -> selectedLessonId = -1
            mode == "review" -> mode = "pages"
            currentPage > 0 -> currentPage--
            else -> selectedLessonId = -1
        }
    }

    BackHandler { handleBack() }
    LaunchedEffect(selectedLessonId) {
        currentPage = 0
        mode = "pages"
        pointsAwarded = 0
        speech.stop()
    }
    LaunchedEffect(selectedLessonId, currentPage, mode) {
        if (lesson != null) {
            repository.recordLessonStep("aqaid:${lesson.id}", currentPage + 1)
            speech.stop()
        }
    }

    Scaffold(
        topBar = {
            AcademyTopBar(
                title = lesson?.title?.get(currentLang) ?: tr(currentLang, "Aqaid: Roots of Religion", "عقائد: دین کی اصول", "العقائد: أصول الدين"),
                onBack = ::handleBack,
                accent = Color(0xFF5E35B1),
                currentLang = currentLang
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            if (lesson == null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            tr(currentLang, "Five foundations, explained with care", "پانچ بنیادی عقائد کی واضح تعلیم", "خمسة أصول بشرح واضح"),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF5E35B1),
                            lineHeight = 31.sp
                        )
                    }
                    items(content.aqaid, key = { it.id }) { item ->
                        val itemPalette = academyPalette(item.id + 1)
                        val lessonKey = "aqaid:${item.id}"
                        val completed = lessonKey in progress.completedLessons
                        val percent = if (completed) 100 else ((progress.lessonSteps[lessonKey] ?: 0) * 100 / (item.pages.size + 1)).coerceIn(0, 99)
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedLessonId = item.id },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = itemPalette.container)
                        ) {
                            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(aqaidIcon(item.id), contentDescription = null, tint = itemPalette.accent, modifier = Modifier.size(30.dp))
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title.get(currentLang), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = itemPalette.accent, lineHeight = 25.sp)
                                    Text(item.description.get(currentLang), fontSize = 13.sp, color = itemPalette.accent.copy(alpha = 0.85f), lineHeight = 19.sp)
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = percent / 100f,
                                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                                        color = itemPalette.accent,
                                        trackColor = MaterialTheme.colorScheme.surface
                                    )
                                }
                                if (completed) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                }
                            }
                        }
                    }
                }
            } else {
                when (mode) {
                    "reward" -> AcademyRewardContent(
                        title = lesson.badge.get(currentLang),
                        message = tr(currentLang, "You finished this belief lesson and answered its review correctly.", "آپ نے یہ عقیدہ سبق اور اس کا جائزہ مکمل کر لیا۔", "أكملت درس العقيدة وأجبت عن مراجعته بشكل صحيح."),
                        pointsAwarded = pointsAwarded,
                        currentLang = currentLang,
                        accent = palette.accent,
                        buttonText = tr(currentLang, "Choose another lesson", "اگلا سبق منتخب کریں", "اختر درساً آخر"),
                        onComplete = { selectedLessonId = -1 }
                    )
                    "review" -> AcademyReviewQuestion(
                        question = lesson.question.get(currentLang),
                        options = lesson.options.map { it.get(currentLang) },
                        correctIndex = lesson.correctIndex,
                        currentLang = currentLang,
                        palette = palette,
                        onCorrect = {
                            pointsAwarded = repository.completeLesson(
                                "aqaid:${lesson.id}", lesson.pages.size + 1, AcademyProgressRepository.AQAID_REWARD
                            )
                            mode = "reward"
                        }
                    )
                    else -> AcademyPagedLesson(
                        title = lesson.title.get(currentLang),
                        pages = lesson.pages.map { it.get(currentLang) },
                        currentPage = currentPage,
                        onPageChange = { currentPage = it },
                        onPagesComplete = { mode = "review" },
                        source = lesson.source,
                        currentLang = currentLang,
                        speech = speech,
                        palette = palette,
                        icon = aqaidIcon(lesson.id),
                        audioId = "aqaid-${lesson.id}"
                    )
                }
            }
        }
    }
}

private fun aqaidIcon(id: Int) = when (id) {
    1 -> Icons.Rounded.WbSunny
    2 -> Icons.Rounded.Balance
    3 -> Icons.Rounded.MenuBook
    4 -> Icons.Rounded.Groups
    else -> Icons.Rounded.EmojiEvents
}
