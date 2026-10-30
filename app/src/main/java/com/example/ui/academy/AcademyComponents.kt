package com.example.ui.academy

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AcademyPalette(val accent: Color, val container: Color)

fun academyPalette(index: Int): AcademyPalette {
    val palettes = listOf(
        AcademyPalette(Color(0xFF00796B), Color(0xFFE0F2F1)),
        AcademyPalette(Color(0xFF6A1B9A), Color(0xFFF3E5F5)),
        AcademyPalette(Color(0xFFEF6C00), Color(0xFFFFF3E0)),
        AcademyPalette(Color(0xFF1565C0), Color(0xFFE3F2FD)),
        AcademyPalette(Color(0xFFC2185B), Color(0xFFFCE4EC)),
        AcademyPalette(Color(0xFF2E7D32), Color(0xFFE8F5E9)),
        AcademyPalette(Color(0xFF00838F), Color(0xFFE0F7FA)),
        AcademyPalette(Color(0xFF5D4037), Color(0xFFEFEBE9))
    )
    return palettes[(index.coerceAtLeast(0)) % palettes.size]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademyTopBar(
    title: String,
    onBack: () -> Unit,
    accent: Color,
    currentLang: String,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 2,
                fontSize = 20.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = tr(currentLang, "Back", "واپس", "رجوع"),
                    tint = accent
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun AcademyAudioButton(
    id: String,
    text: String,
    audioLanguage: String,
    currentLang: String,
    speech: AcademySpeechController,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val speakingId by speech.speakingId.collectAsState()
    val isPlaying = speakingId == id
    FloatingActionButton(
        onClick = {
            if (isPlaying) {
                speech.stop()
            } else if (!speech.speak(id, text, audioLanguage)) {
                Toast.makeText(
                    context,
                    tr(
                        currentLang,
                        "The read-aloud voice is starting or unavailable. Try again shortly; you can still read the text.",
                        "بلند خوانی کی آواز تیار ہو رہی ہے یا دستیاب نہیں۔ کچھ دیر بعد کوشش کریں؛ آپ متن پڑھ سکتے ہیں۔",
                        "صوت القراءة قيد التجهيز أو غير متوفر. حاول بعد قليل؛ ويمكنك قراءة النص."
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        },
        containerColor = accent,
        contentColor = Color.White,
        shape = CircleShape,
        modifier = modifier.size(64.dp)
    ) {
        Icon(
            if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
            contentDescription = if (isPlaying) {
                tr(currentLang, "Stop reading aloud", "پڑھنا بند کریں", "إيقاف القراءة")
            } else {
                tr(currentLang, "Read aloud", "بلند آواز میں پڑھیں", "اقرأ بصوت مرتفع")
            },
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
fun AcademyPagedLesson(
    title: String,
    pages: List<String>,
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    onPagesComplete: () -> Unit,
    source: String,
    currentLang: String,
    speech: AcademySpeechController,
    palette: AcademyPalette,
    icon: ImageVector,
    audioId: String
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = (currentPage + 1).toFloat() / pages.size.coerceAtLeast(1),
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = palette.accent,
            trackColor = palette.container
        )
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(84.dp).clip(CircleShape).background(palette.container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    title,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.accent,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    pages.getOrElse(currentPage) { "" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 31.sp
                )
                if (currentPage == pages.lastIndex && source.isNotBlank()) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        tr(currentLang, "Source for parents: ", "والدین کے لیے ماخذ: ", "المصدر للوالدين: ") + source,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        AcademyPageControls(
            currentLang = currentLang,
            canGoBack = currentPage > 0,
            isLastPage = currentPage == pages.lastIndex,
            onPrevious = { onPageChange((currentPage - 1).coerceAtLeast(0)) },
            onNext = {
                if (currentPage < pages.lastIndex) onPageChange(currentPage + 1) else onPagesComplete()
            },
            accent = palette.accent,
            audio = {
                AcademyAudioButton(
                    id = "$audioId-$currentPage",
                    text = pages.getOrElse(currentPage) { "" },
                    audioLanguage = currentLang,
                    currentLang = currentLang,
                    speech = speech,
                    accent = palette.accent
                )
            }
        )
    }
}

@Composable
fun AcademyStepPager(
    step: AcademyStep,
    currentIndex: Int,
    totalSteps: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    currentLang: String,
    speech: AcademySpeechController,
    palette: AcademyPalette,
    icon: ImageVector,
    audioId: String
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = (currentIndex + 1).toFloat() / totalSteps.coerceAtLeast(1),
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = palette.accent,
            trackColor = palette.container
        )
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.container),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(42.dp))
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    step.title.get(currentLang),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.accent,
                    textAlign = TextAlign.Center,
                    lineHeight = 35.sp
                )
                if (step.arabic.isNotBlank()) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        step.arabic,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF263238),
                        textAlign = TextAlign.Center,
                        lineHeight = 44.sp
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    step.description.get(currentLang),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF263238),
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )
                if (currentIndex == totalSteps - 1 && step.source.isNotBlank()) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        tr(currentLang, "Source for parents: ", "والدین کے لیے ماخذ: ", "المصدر للوالدين: ") + step.source,
                        fontSize = 12.sp,
                        color = Color(0xFF5F6368),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        AcademyPageControls(
            currentLang = currentLang,
            canGoBack = currentIndex > 0,
            isLastPage = currentIndex == totalSteps - 1,
            onPrevious = onPrevious,
            onNext = onNext,
            accent = palette.accent,
            audio = {
                val speakArabic = step.arabic.isNotBlank()
                AcademyAudioButton(
                    id = "$audioId-$currentIndex",
                    text = if (speakArabic) step.arabic else step.description.get(currentLang),
                    audioLanguage = if (speakArabic) "ar" else currentLang,
                    currentLang = currentLang,
                    speech = speech,
                    accent = palette.accent
                )
            }
        )
    }
}

@Composable
private fun AcademyPageControls(
    currentLang: String,
    canGoBack: Boolean,
    isLastPage: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    accent: Color,
    audio: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = canGoBack,
            modifier = Modifier.size(56.dp).clip(CircleShape).background(
                if (canGoBack) MaterialTheme.colorScheme.surface else Color.Transparent
            )
        ) {
            if (canGoBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = tr(currentLang, "Previous", "پچھلا", "السابق"),
                    tint = accent
                )
            }
        }
        audio()
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
        ) {
            Icon(
                if (isLastPage) Icons.Rounded.Quiz else Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = if (isLastPage) {
                    tr(currentLang, "Review question", "جائزہ سوال", "سؤال المراجعة")
                } else {
                    tr(currentLang, "Next", "اگلا", "التالي")
                },
                tint = accent
            )
        }
    }
}

@Composable
fun AcademyReviewQuestion(
    question: String,
    options: List<String>,
    correctIndex: Int,
    currentLang: String,
    palette: AcademyPalette,
    onCorrect: () -> Unit
) {
    var selectedOption by rememberSaveable(question) { mutableStateOf(-1) }
    var showError by rememberSaveable(question) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Quiz, contentDescription = null, tint = palette.accent, modifier = Modifier.size(70.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            tr(currentLang, "Quick review", "مختصر جائزہ", "مراجعة سريعة"),
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = palette.accent,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            question,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )
        Spacer(Modifier.height(24.dp))
        options.forEachIndexed { index, option ->
            val isError = showError && selectedOption == index
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                    selectedOption = index
                    if (index == correctIndex) {
                        showError = false
                        onCorrect()
                    } else {
                        showError = true
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(
                            if (isError) MaterialTheme.colorScheme.error else palette.container
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            fontWeight = FontWeight.Bold,
                            color = if (isError) MaterialTheme.colorScheme.onError else palette.accent
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        option,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        if (showError) {
            Spacer(Modifier.height(12.dp))
            Text(
                tr(currentLang, "Not quite—try again. You can do it!", "کوئی بات نہیں، دوبارہ کوشش کریں!", "حاول مرة أخرى، يمكنك فعلها!"),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AcademyRewardContent(
    title: String,
    message: String,
    pointsAwarded: Int,
    currentLang: String,
    accent: Color,
    buttonText: String,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(140.dp).clip(CircleShape).background(Color(0xFFFFC107)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(86.dp))
        }
        Spacer(Modifier.height(26.dp))
        Text(title, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = accent, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(
            message,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 27.sp
        )
        Spacer(Modifier.height(20.dp))
        Surface(color = if (pointsAwarded > 0) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
            Text(
                if (pointsAwarded > 0) {
                    "+$pointsAwarded " + tr(currentLang, "Shia Points", "شیعہ پوائنٹس", "نقاط شيعة")
                } else {
                    tr(currentLang, "Reward already collected — great revision!", "انعام پہلے مل چکا ہے — اچھا دہرایا!", "تم تحصيل المكافأة — مراجعة رائعة!")
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                color = if (pointsAwarded > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(30.dp))
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(29.dp)
        ) {
            Text(buttonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AcademyContentError(currentLang: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text(
            tr(currentLang, "Learning content could not be loaded. Please reopen the Academy.", "تعلیمی مواد لوڈ نہیں ہو سکا۔ اکیڈمی دوبارہ کھولیں۔", "تعذر تحميل المحتوى التعليمي. أعد فتح الأكاديمية."),
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onBack) { Text(tr(currentLang, "Go back", "واپس جائیں", "رجوع")) }
    }
}
