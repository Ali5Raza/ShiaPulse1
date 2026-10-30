package com.example.ui.academy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.academy.theme.AcademyGradients
import com.example.ui.academy.theme.AcademyTheme
import kotlinx.coroutines.delay

@Composable
fun AcademyDashboardScreen(
    onBack: () -> Unit,
    currentLang: String = "en",
    appFontFamily: FontFamily = FontFamily.Default
) {
    val context = LocalContext.current
    val content = remember { AcademyContentLoader.load(context) }
    val repository = remember { AcademyProgressRepository(context) }
    val progress by repository.progress.collectAsState()
    val speech = remember { AcademySpeechController(context) }
    var internalScreen by rememberSaveable { mutableStateOf("dashboard") }

    fun navigateBack() {
        speech.stop()
        if (internalScreen != "dashboard") internalScreen = "dashboard" else onBack()
    }

    BackHandler { navigateBack() }
    LaunchedEffect(internalScreen) {
        speech.stop()
        repository.refreshProgress()
    }
    LaunchedEffect(Unit) {
        while (true) {
            repository.refreshProgress()
            delay(60_000)
        }
    }
    DisposableEffect(Unit) { onDispose { speech.shutdown() } }

    AcademyTheme(currentLang = currentLang, appFontFamily = appFontFamily) {
        if (!content.isUsable) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AcademyContentError(currentLang = currentLang, onBack = onBack)
            }
        } else {
            when (internalScreen) {
                "wudu" -> LearnWuduScreen(::navigateBack, currentLang, content, repository, speech)
                "salah" -> LearnSalahScreen(::navigateBack, currentLang, content, repository, speech)
                "stories" -> AhlulBaytStoriesScreen(::navigateBack, currentLang, content, repository, speech)
                "tree" -> ShiaTreeScreen(::navigateBack, currentLang, progress)
                "duas" -> DailyDuasScreen(::navigateBack, currentLang, content, repository, speech)
                "aqaid" -> AqaidScreen(::navigateBack, currentLang, content, repository, speech)
                "quiz" -> AcademyQuizScreen(::navigateBack, currentLang, content, repository)
                "games" -> MiniGamesScreen(::navigateBack, currentLang, content, repository)
                "parent" -> ParentDashboardScreen(::navigateBack, currentLang, content, repository)
                else -> AcademyMainDashboard(
                    onBack = ::navigateBack,
                    navigateTo = { internalScreen = it },
                    currentLang = currentLang,
                    content = content,
                    progress = progress,
                    repository = repository
                )
            }
        }
    }
}

@Composable
fun AcademyMainDashboard(
    onBack: () -> Unit,
    navigateTo: (String) -> Unit,
    currentLang: String,
    content: AcademyContent,
    progress: AcademyProgress,
    repository: AcademyProgressRepository
) {
    Scaffold(
        topBar = {
            AcademyTopBar(
                title = tr(currentLang, "Shia Academy", "شیعہ اکیڈمی", "أكاديمية الشيعة"),
                onBack = onBack,
                accent = Color(0xFF1565C0),
                currentLang = currentLang,
                actions = {
                    Surface(
                        color = Color(0xFFFFC107),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${progress.points}",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().background(AcademyGradients.magicalSky).padding(paddingValues)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(Color.White.copy(alpha = 0.55f), radius = 60f, center = Offset(size.width * 0.2f, 150f))
                drawCircle(Color.White.copy(alpha = 0.55f), radius = 80f, center = Offset(size.width * 0.3f, 120f))
                drawCircle(Color.White.copy(alpha = 0.55f), radius = 60f, center = Offset(size.width * 0.4f, 150f))
                drawCircle(Color.White.copy(alpha = 0.45f), radius = 60f, center = Offset(size.width * 0.8f, 240f))
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item { DailyMissionsCard(progress.daily, currentLang, repository::markKindnessToday) }
                item {
                    Column {
                        Text(
                            tr(currentLang, "Let's Learn!", "آئیے سیکھیں!", "لنتعلم معاً!"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1565C0)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${content.duas.size} " + tr(currentLang, "Duas", "دعائیں", "أدعية") +
                                " • ${content.stories.size} " + tr(currentLang, "Stories", "کہانیاں", "قصص") +
                                " • ${content.quiz.size} " + tr(currentLang, "Questions", "سوالات", "أسئلة"),
                            color = Color(0xFF37576F),
                            fontSize = 13.sp
                        )
                    }
                }
                item {
                    val categories = listOf(
                        CategoryData(
                            "salah", tr(currentLang, "Learn Salah", "نماز سیکھیں", "تعلم الصلاة"),
                            tr(currentLang, "A guided ten-step introduction", "دس مراحل کی رہنمائی", "مقدمة في عشر خطوات"),
                            Icons.Rounded.Person, academyPalette(2), lessonPercentage(progress, "salah", content.salah.size)
                        ),
                        CategoryData(
                            "wudu", tr(currentLang, "Learn Wudu", "وضو سیکھیں", "تعلم الوضوء"),
                            tr(currentLang, "Correct order and practical steps", "درست ترتیب اور عملی مراحل", "الترتيب والخطوات العملية"),
                            Icons.Rounded.WaterDrop, academyPalette(0), lessonPercentage(progress, "wudu", content.wudu.size)
                        ),
                        CategoryData(
                            "duas", tr(currentLang, "Daily Duas", "روزانہ دعائیں", "الأدعية اليومية"),
                            tr(currentLang, "Morning, home, study & more", "صبح، گھر، پڑھائی اور مزید", "الصباح والمنزل والدراسة وأكثر"),
                            Icons.Rounded.Favorite, academyPalette(4), percentage(progress.readDuas.size, content.duas.size)
                        ),
                        CategoryData(
                            "aqaid", tr(currentLang, "Aqaid", "عقائد", "العقائد"),
                            tr(currentLang, "The five roots of religion", "دین کی پانچ اصول", "أصول الدين الخمسة"),
                            Icons.Rounded.MenuBook, academyPalette(1),
                            percentage(progress.completedLessons.count { it.startsWith("aqaid:") }, content.aqaid.size)
                        ),
                        CategoryData(
                            "stories", tr(currentLang, "Ahlul Bayt", "اہل بیت", "أهل البيت"),
                            tr(currentLang, "Stories of all fourteen Infallibles", "چودہ معصومین کی کہانیاں", "قصص المعصومين الأربعة عشر"),
                            Icons.Rounded.AutoAwesome, academyPalette(5),
                            percentage(progress.completedLessons.count { it.startsWith("story:") }, content.stories.size)
                        ),
                        CategoryData(
                            "games", tr(currentLang, "Mini Games", "تعلیمی کھیل", "ألعاب تعليمية"),
                            tr(currentLang, "Four complete learning games", "چار مکمل تعلیمی کھیل", "أربع ألعاب تعليمية كاملة"),
                            Icons.Rounded.VideogameAsset, academyPalette(3), percentage(progress.completedGames.size, 4)
                        ),
                        CategoryData(
                            "quiz", tr(currentLang, "Quiz Challenge", "کوئز چیلنج", "تحدي الأسئلة"),
                            tr(currentLang, "Ten new questions each round", "ہر راؤنڈ دس سوالات", "عشرة أسئلة في كل جولة"),
                            Icons.Rounded.Quiz, academyPalette(6),
                            if (progress.bestQuizTotal > 0) percentage(progress.bestQuizScore, progress.bestQuizTotal) else 0
                        ),
                        CategoryData(
                            "tree", tr(currentLang, "My Tree", "میرا درخت", "شجرتي"),
                            tr(currentLang, "See your real achievements grow", "اپنی حقیقی کامیابیاں بڑھتے دیکھیں", "شاهد إنجازاتك الحقيقية تنمو"),
                            Icons.Rounded.Nature, academyPalette(5), overallPercentage(progress, content)
                        )
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (index in categories.indices step 2) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                CategoryCard(categories[index], currentLang, Modifier.weight(1f)) {
                                    navigateTo(categories[index].route)
                                }
                                if (index + 1 < categories.size) {
                                    CategoryCard(categories[index + 1], currentLang, Modifier.weight(1f)) {
                                        navigateTo(categories[index + 1].route)
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                item { ParentDashboardButton(currentLang) { navigateTo("parent") } }
            }
        }
    }
}

private fun lessonPercentage(progress: AcademyProgress, lessonId: String, total: Int): Int =
    if (lessonId in progress.completedLessons) 100 else percentage(progress.lessonSteps[lessonId] ?: 0, total)

private fun percentage(completed: Int, total: Int): Int =
    if (total <= 0) 0 else (completed.coerceIn(0, total) * 100 / total)

private fun overallPercentage(progress: AcademyProgress, content: AcademyContent): Int {
    val total = 2 + content.duas.size + content.aqaid.size + content.stories.size + 4
    val completed = progress.completedLessons.size + progress.readDuas.size + progress.completedGames.size
    return percentage(completed, total)
}

@Composable
fun DailyMissionsCard(
    daily: AcademyDailyMissions,
    currentLang: String,
    onKindness: () -> Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        tr(currentLang, "Daily Missions", "روزانہ مشن", "مهام اليوم"),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        tr(currentLang, "Real activities • refreshed every day", "حقیقی سرگرمیاں • روز تجدید", "أنشطة حقيقية • تتجدد يومياً"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            MissionItem(tr(currentLang, "Read one Dua", "ایک دعا پڑھیں", "اقرأ دعاءً"), daily.dua, currentLang)
            MissionItem(tr(currentLang, "Complete a Quiz", "ایک کوئز مکمل کریں", "أكمل اختباراً"), daily.quiz, currentLang)
            MissionItem(tr(currentLang, "Finish a lesson or game", "سبق یا کھیل مکمل کریں", "أكمل درساً أو لعبة"), daily.learning, currentLang)
            MissionItem(
                tr(currentLang, "Help at home", "گھر میں مدد کریں", "ساعد في المنزل"),
                daily.kindness,
                currentLang,
                actionLabel = if (!daily.kindness) tr(currentLang, "I did it", "میں نے کیا", "فعلتها") else null,
                onAction = { onKindness() }
            )
        }
    }
}

@Composable
fun MissionItem(
    title: String,
    isComplete: Boolean,
    currentLang: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(
                if (isComplete) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant
            ),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = tr(currentLang, "Done", "مکمل", "مكتمل"),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = if (isComplete) FontWeight.Bold else FontWeight.Medium,
            color = if (isComplete) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

data class CategoryData(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val palette: AcademyPalette,
    val progress: Int
)

@Composable
fun CategoryCard(category: CategoryData, currentLang: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier.fillMaxWidth().heightIn(min = 190.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = category.palette.container),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(15.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(category.icon, contentDescription = null, tint = category.palette.accent, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                category.title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = category.palette.accent,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                category.subtitle,
                fontSize = 12.sp,
                color = category.palette.accent.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = category.progress / 100f,
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = category.palette.accent,
                trackColor = MaterialTheme.colorScheme.surface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (category.progress == 100) tr(currentLang, "Complete", "مکمل", "مكتمل") else "${category.progress}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = category.palette.accent
            )
        }
    }
}

@Composable
fun ParentDashboardButton(currentLang: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF283593))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tr(currentLang, "Parent Dashboard", "والدین کا ڈیش بورڈ", "لوحة الوالدين"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    tr(currentLang, "Real reports • PIN protected • on-device", "حقیقی رپورٹس • پن محفوظ • ڈیوائس پر", "تقارير حقيقية • محمية برمز • على الجهاز"),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
            }
            Icon(Icons.Rounded.Lock, contentDescription = tr(currentLang, "Parents only", "صرف والدین", "للوالدين فقط"), tint = Color.White)
        }
    }
}
