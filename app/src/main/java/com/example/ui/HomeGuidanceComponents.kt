package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookOnline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranProgressStore
import com.example.data.QuranReadingPosition
import com.example.data.QuranSurahInfo
import kotlin.math.roundToInt

@Composable
fun HomeGuidanceSection(
    plan: HomeGuidancePlan,
    currentLang: String,
    hijriDateLabel: String,
    fontFamily: FontFamily,
    onRecommendationClick: (HomeRecommendation) -> Unit,
    isLoading: Boolean = true,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hasOccasion = plan.occasionEvents.isNotEmpty()
    val title = when {
        hasOccasion -> localizedText(currentLang, "Guidance for today’s occasion", "آج کی مناسبت کی رہنمائی", "إرشادات مناسبة اليوم", "راهنمای مناسبت امروز", "आज के अवसर की मार्गदर्शिका")
        plan.isJumuah -> localizedText(currentLang, "Your Jumu’ah guidance", "آپ کی جمعہ رہنمائی", "إرشادات يوم الجمعة", "راهنمای جمعه شما", "आपका जुमा मार्गदर्शन")
        plan.isRamadan -> localizedText(currentLang, "Your Ramadan guidance", "آپ کی رمضان رہنمائی", "إرشاداتك في رمضان", "راهنمای رمضان شما", "आपका रमज़ान मार्गदर्शन")
        else -> localizedText(currentLang, "Made for your day", "آج کے دن کے لیے منتخب", "مختارات ليومك", "منتخب‌های امروز شما", "आपके दिन के लिए चुने गए")
    }
    val occasionNames = plan.occasionEvents.joinToString(
        separator = if (currentLang == "ur" || currentLang == "ar" || currentLang == "fa") "، " else ", "
    ) { it.localizedTitle(currentLang) }
    val contextSubtitle = if (occasionNames.isNotBlank()) {
        "$occasionNames • $hijriDateLabel"
    } else {
        "$hijriDateLabel • " + localizedText(
            currentLang,
            "weekday and time-aware picks",
            "دن اور وقت کے مطابق انتخاب",
            "اختيارات حسب اليوم والوقت",
            "انتخاب بر اساس روز و زمان",
            "दिन और समय के अनुसार चयन"
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = fontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = contextSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = fontFamily,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (plan.recommendations.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            ) {
                if (isLoading) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = localizedText(currentLang, "Preparing today’s verified guidance…", "آج کی رہنمائی تیار کی جا رہی ہے…", "جاري تجهيز إرشادات اليوم…", "راهنمای امروز آماده می‌شود…", "आज का मार्गदर्शन तैयार हो रहा है…"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = fontFamily
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = localizedText(currentLang, "No verified guidance is available right now.", "اس وقت تصدیق شدہ رہنمائی دستیاب نہیں۔", "لا تتوفر إرشادات موثقة حالياً.", "در حال حاضر راهنمای تأییدشده‌ای موجود نیست.", "अभी कोई सत्यापित मार्गदर्शन उपलब्ध नहीं है।"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = fontFamily
                        )
                        TextButton(onClick = onRetry) {
                            Text(localizedText(currentLang, "Retry", "دوبارہ کوشش کریں", "إعادة المحاولة", "تلاش دوباره", "पुनः प्रयास करें"))
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                plan.recommendations.forEach { recommendation ->
                    HomeRecommendationCard(
                        recommendation = recommendation,
                        currentLang = currentLang,
                        fontFamily = fontFamily,
                        onClick = { onRecommendationClick(recommendation) }
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
fun HomeQuranContinueReading(
    surahs: List<QuranSurahInfo>,
    currentLang: String,
    fontFamily: FontFamily,
    onClick: (QuranReadingPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progress by QuranProgressStore.state.collectAsState()
    LaunchedEffect(context) {
        QuranProgressStore.initialize(context)
    }

    val position = progress.lastRead
    val surah = position?.let { saved ->
        surahs.firstOrNull {
            it.number == saved.surahNumber && saved.ayahNumber in 1..it.verseCount
        }
    }
    if (position != null && surah != null) {
        HomeQuranContinueReadingCard(
            position = position,
            surah = surah,
            currentLang = currentLang,
            fontFamily = fontFamily,
            onClick = { onClick(position) },
            modifier = modifier
        )
    }
}

@Composable
private fun HomeQuranContinueReadingCard(
    position: QuranReadingPosition,
    surah: QuranSurahInfo,
    currentLang: String,
    fontFamily: FontFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedText(
                        currentLang,
                        "Continue Quran reading",
                        "قرآن کی تلاوت جاری رکھیں",
                        "متابعة قراءة القرآن",
                        "ادامه قرائت قرآن",
                        "क़ुरान पढ़ना जारी रखें"
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    fontFamily = fontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = (if (currentLang == "en") surah.name else surah.nameAr) +
                        " • ${position.surahNumber}:${position.ayahNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = fontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun HomeRecommendationCard(
    recommendation: HomeRecommendation,
    currentLang: String,
    fontFamily: FontFamily,
    onClick: () -> Unit
) {
    val accent = if (recommendation.kind == HomeRecommendationKind.QURAN) {
        MaterialTheme.colorScheme.secondary
    } else when (recommendation.reason) {
        GuidanceReason.EXACT_EVENT -> MaterialTheme.colorScheme.primary
        GuidanceReason.HIJRI_DATE -> MaterialTheme.colorScheme.secondary
        GuidanceReason.HOLY_MONTH -> MaterialTheme.colorScheme.tertiary
        GuidanceReason.WEEKLY -> MaterialTheme.colorScheme.primary
        GuidanceReason.TIME_OF_DAY -> MaterialTheme.colorScheme.secondary
        GuidanceReason.DAILY -> MaterialTheme.colorScheme.primary
    }
    val icon = when {
        recommendation.kind == HomeRecommendationKind.QURAN -> Icons.Default.MenuBook
        recommendation.kind == HomeRecommendationKind.AAMAL -> Icons.Default.BookOnline
        recommendation.kind == HomeRecommendationKind.WISDOM -> Icons.Default.MenuBook
        recommendation.kind == HomeRecommendationKind.ZIYARAT -> Icons.Default.Star
        recommendation.reason == GuidanceReason.EXACT_EVENT -> Icons.Default.Star
        recommendation.reason == GuidanceReason.HIJRI_DATE -> Icons.Default.CalendarMonth
        recommendation.reason == GuidanceReason.HOLY_MONTH -> Icons.Default.CalendarMonth
        recommendation.reason == GuidanceReason.WEEKLY -> Icons.Default.Event
        recommendation.reason == GuidanceReason.TIME_OF_DAY -> Icons.Default.AccessTime
        else -> Icons.Default.MenuBook
    }

    Card(
        modifier = Modifier
            .width(272.dp)
            .height(216.dp)
            .clickable(onClick = onClick)
            .testTag("home_guidance_${recommendation.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.16f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.09f), MaterialTheme.colorScheme.surface)
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(accent.copy(alpha = 0.13f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        color = accent.copy(alpha = 0.10f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = recommendation.reason.localizedLabel(currentLang),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = recommendation.localizedTitle(currentLang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = fontFamily,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = recommendation.localizedDescription(currentLang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = fontFamily,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = recommendation.localizedRelevance(currentLang),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontFamily = fontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = localizedText(currentLang, "Open", "کھولیں", "فتح", "باز کنید", "खोलें"),
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeAamalProgressCard(
    total: Int,
    completed: Int,
    streak: Int,
    nextAamalTitle: String?,
    currentLang: String,
    fontFamily: FontFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetProgress = if (total > 0) (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val progress by animateFloatAsState(targetValue = targetProgress, label = "home_aamal_progress")
    val percentage = (progress * 100).roundToInt()
    val remaining = (total - completed).coerceAtLeast(0)
    val isComplete = total > 0 && completed >= total
    val title = localizedText(currentLang, "Aamal-e-Rozana", "اعمالِ روزانہ", "الأعمال اليومية", "اعمال روزانه", "दैनिक आमाल")
    val subtitle = localizedText(currentLang, "Your daily rhythm", "آپ کا روزانہ معمول", "إيقاعك اليومي", "روتین روزانه شما", "आपकी दैनिक दिनचर्या")
    val progressMessage = when {
        total == 0 -> localizedText(currentLang, "Open the tracker to begin your routine.", "اپنا معمول شروع کرنے کے لیے ٹریکر کھولیں۔", "افتح المتتبع لبدء روتينك.", "برای شروع، ردیاب را باز کنید.", "अपनी दिनचर्या शुरू करने के लिए ट्रैकर खोलें।")
        isComplete -> localizedText(currentLang, "Beautifully completed. May your worship be accepted.", "آج کے اعمال مکمل ہیں۔ اللہ قبول فرمائے۔", "اكتملت أعمال اليوم. تقبل الله عبادتك.", "اعمال امروز کامل شد. خدا قبول کند.", "आज के आमाल पूर्ण हैं। ईबादत क़बूल हो।")
        completed == 0 -> localizedText(currentLang, "Begin with one small act of worship.", "ایک چھوٹے سے عمل سے آغاز کریں۔", "ابدأ بعمل عبادي صغير.", "با یک عمل کوچک شروع کنید.", "एक छोटी सी ईबादत से शुरुआत करें।")
        else -> localizedText(currentLang, "$remaining acts remaining — keep your momentum.", "$remaining اعمال باقی ہیں — اپنے تسلسل کو برقرار رکھیں۔", "تبقى $remaining أعمال — حافظ على استمرارك.", "$remaining اعمال باقی است — ادامه دهید.", "$remaining आमाल शेष हैं — अपनी लय बनाए रखें।")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("aamal_home_tracker_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.035f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = fontFamily
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = fontFamily
                        )
                    }
                    if (streak > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = streak.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            strokeWidth = 7.dp
                        )
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = localizedText(currentLang, "$completed of $total completed", "$total میں سے $completed مکمل", "$completed من $total مكتمل", "$completed از $total کامل", "$total में से $completed पूर्ण"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = fontFamily
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = progressMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = fontFamily,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = if (isComplete) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isComplete) {
                                    localizedText(currentLang, "Routine complete", "آج کا معمول مکمل", "اكتمل روتين اليوم", "روتین امروز کامل است", "आज की दिनचर्या पूर्ण")
                                } else {
                                    localizedText(currentLang, "Up next", "اگلا عمل", "العمل التالي", "عمل بعدی", "अगला आमल")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamily
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = nextAamalTitle ?: localizedText(currentLang, "Review your daily Aamal", "اپنے روزانہ اعمال دیکھیں", "راجع أعمالك اليومية", "اعمال روزانه را مرور کنید", "अपने दैनिक आमाल देखें"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = fontFamily,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = localizedText(currentLang, "Open daily tracker", "روزانہ ٹریکر کھولیں", "افتح المتتبع اليومي", "ردیاب روزانه را باز کنید", "दैनिक ट्रैकर खोलें"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeDailyReflectionHeader(
    currentLang: String,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.11f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = localizedText(currentLang, "Daily reflection", "روزانہ غور و فکر", "تأمل يومي", "تأمل روزانه", "दैनिक चिंतन"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = fontFamily
            )
            Text(
                text = localizedText(currentLang, "Quran and Hadith chosen for today", "آج کے لیے قرآن و حدیث کا انتخاب", "القرآن والحديث المختاران لليوم", "قرآن و حدیث منتخب امروز", "आज के लिए चुना गया क़ुरान और हदीस"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = fontFamily
            )
        }
    }
}

private fun GuidanceReason.localizedLabel(languageCode: String): String = when (this) {
    GuidanceReason.EXACT_EVENT -> localizedText(languageCode, "Today’s occasion", "آج کی مناسبت", "مناسبة اليوم", "مناسبت امروز", "आज का अवसर")
    GuidanceReason.HIJRI_DATE -> localizedText(languageCode, "Special Hijri day", "خصوصی قمری تاریخ", "يوم هجري خاص", "روز خاص هجری", "विशेष हिजरी दिन")
    GuidanceReason.HOLY_MONTH -> localizedText(languageCode, "Sacred month", "مبارک مہینہ", "الشهر المبارك", "ماه مبارک", "पवित्र महीना")
    GuidanceReason.WEEKLY -> localizedText(languageCode, "Weekly rhythm", "ہفتہ وار معمول", "إيقاع أسبوعي", "روتین هفتگی", "साप्ताहिक क्रम")
    GuidanceReason.TIME_OF_DAY -> localizedText(languageCode, "Right now", "اس وقت کے لیے", "لهذا الوقت", "برای این زمان", "इस समय के लिए")
    GuidanceReason.DAILY -> localizedText(languageCode, "Daily pick", "روزانہ انتخاب", "اختيار يومي", "انتخاب روزانه", "दैनिक चयन")
}

private fun localizedText(
    languageCode: String,
    english: String,
    urdu: String,
    arabic: String,
    farsi: String,
    hindi: String
): String = when (languageCode) {
    "ur" -> urdu
    "ar" -> arabic
    "fa" -> farsi
    "hi" -> hindi
    else -> english
}
