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
fun AhlulBaytStoriesScreen(
    onBack: () -> Unit,
    currentLang: String,
    content: AcademyContent,
    repository: AcademyProgressRepository,
    speech: AcademySpeechController
) {
    var selectedStoryId by rememberSaveable { mutableStateOf(-1) }
    var currentPage by rememberSaveable { mutableStateOf(0) }
    var mode by rememberSaveable { mutableStateOf("pages") }
    var pointsAwarded by rememberSaveable { mutableStateOf(0) }
    val progress by repository.progress.collectAsState()
    val story = content.stories.firstOrNull { it.id == selectedStoryId }
    val palette = academyPalette(selectedStoryId.coerceAtLeast(0))

    fun handleBack() {
        speech.stop()
        when {
            story == null -> onBack()
            mode == "reward" -> selectedStoryId = -1
            mode == "review" -> mode = "pages"
            currentPage > 0 -> currentPage--
            else -> selectedStoryId = -1
        }
    }

    BackHandler { handleBack() }
    LaunchedEffect(selectedStoryId) {
        currentPage = 0
        mode = "pages"
        pointsAwarded = 0
        speech.stop()
    }
    LaunchedEffect(selectedStoryId, currentPage, mode) {
        if (story != null) {
            repository.recordLessonStep("story:${story.id}", currentPage + 1)
            speech.stop()
        }
    }

    Scaffold(
        topBar = {
            AcademyTopBar(
                title = story?.title?.get(currentLang) ?: tr(currentLang, "Ahlul Bayt Stories", "اہل بیت کی کہانیاں", "قصص أهل البيت"),
                onBack = ::handleBack,
                accent = if (story == null) Color(0xFF2E7D32) else palette.accent,
                currentLang = currentLang
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            if (story == null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        val completed = progress.completedLessons.count { it.startsWith("story:") }
                        Text(
                            tr(currentLang, "Meet all fourteen Infallibles", "تمام چودہ معصومین سے متعارف ہوں", "تعرف على المعصومين الأربعة عشر"),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2E7D32),
                            lineHeight = 31.sp
                        )
                        Text(
                            "$completed / ${content.stories.size} " + tr(currentLang, "stories completed", "کہانیاں مکمل", "قصص مكتملة"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    items(content.stories, key = { it.id }) { item ->
                        val itemPalette = academyPalette(item.id)
                        val lessonKey = "story:${item.id}"
                        val completed = lessonKey in progress.completedLessons
                        val percent = if (completed) 100 else ((progress.lessonSteps[lessonKey] ?: 0) * 100 / (item.pages.size + 1)).coerceIn(0, 99)
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedStoryId = item.id },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = itemPalette.container)
                        ) {
                            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(storyIcon(item.id), contentDescription = null, tint = itemPalette.accent, modifier = Modifier.size(30.dp))
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
                        title = story.badge.get(currentLang),
                        message = if (pointsAwarded > 0) {
                            tr(currentLang, "A new story badge is saved in your real learning progress.", "نیا اسٹوری بیج آپ کی حقیقی پیش رفت میں محفوظ ہو گیا۔", "تم حفظ شارة القصة في تقدمك الحقيقي.")
                        } else {
                            tr(currentLang, "Story revised. Your earned badge remains safely saved.", "کہانی دہرائی گئی۔ آپ کا حاصل شدہ بیج محفوظ ہے۔", "راجعت القصة، وشارتك المكتسبة محفوظة.")
                        },
                        pointsAwarded = pointsAwarded,
                        currentLang = currentLang,
                        accent = palette.accent,
                        buttonText = tr(currentLang, "More stories", "مزید کہانیاں", "المزيد من القصص"),
                        onComplete = { selectedStoryId = -1 }
                    )
                    "review" -> AcademyReviewQuestion(
                        question = story.question.get(currentLang),
                        options = story.options.map { it.get(currentLang) },
                        correctIndex = story.correctIndex,
                        currentLang = currentLang,
                        palette = palette,
                        onCorrect = {
                            pointsAwarded = repository.completeLesson(
                                "story:${story.id}", story.pages.size + 1, AcademyProgressRepository.STORY_REWARD
                            )
                            mode = "reward"
                        }
                    )
                    else -> AcademyPagedLesson(
                        title = story.title.get(currentLang),
                        pages = story.pages.map { it.get(currentLang) },
                        currentPage = currentPage,
                        onPageChange = { currentPage = it },
                        onPagesComplete = { mode = "review" },
                        source = story.source,
                        currentLang = currentLang,
                        speech = speech,
                        palette = palette,
                        icon = storyIcon(story.id),
                        audioId = "story-${story.id}"
                    )
                }
            }
        }
    }
}

private fun storyIcon(id: Int) = when (id) {
    1 -> Icons.Rounded.AutoAwesome
    2 -> Icons.Rounded.Shield
    3 -> Icons.Rounded.LocalFlorist
    4 -> Icons.Rounded.FavoriteBorder
    5 -> Icons.Rounded.Favorite
    6 -> Icons.Rounded.MenuBook
    7 -> Icons.Rounded.School
    8 -> Icons.Rounded.Lightbulb
    9 -> Icons.Rounded.Groups
    10 -> Icons.Rounded.Star
    11 -> Icons.Rounded.CardGiftcard
    12 -> Icons.Rounded.Explore
    13 -> Icons.Rounded.Flag
    else -> Icons.Rounded.WbSunny
}
