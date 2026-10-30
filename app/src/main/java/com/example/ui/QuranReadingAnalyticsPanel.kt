package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.QuranAssetLoader
import com.example.data.QuranDailyReading
import com.example.data.QuranJuzIndex
import com.example.data.QuranProgressStore
import com.example.data.QuranReadingAnalyticsStore
import com.example.data.QuranReadingSession
import com.example.data.QuranSurahInfo
import com.example.data.quranReadingStreaks
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlinx.coroutines.delay

@Composable
fun QuranReadingAnalyticsTracker(enabled: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, enabled) {
        if (!enabled) {
            resumed = false
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        resumed = true
                        QuranReadingAnalyticsStore.startSession(context)
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        resumed = false
                        QuranReadingAnalyticsStore.endSession(context)
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                resumed = true
                QuranReadingAnalyticsStore.startSession(context)
            }
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                resumed = false
                QuranReadingAnalyticsStore.endSession(context)
            }
        }
    }

    LaunchedEffect(enabled, resumed) {
        if (enabled && resumed) {
            while (true) {
                delay(5_000)
                QuranReadingAnalyticsStore.recordActiveTime(context, 5)
            }
        }
    }
}

@Composable
fun QuranReadingAnalyticsPanel(
    surahs: List<QuranSurahInfo>,
    languageCode: String
) {
    val state by QuranReadingAnalyticsStore.state.collectAsState()
    val today = QuranProgressStore.currentLocalEpochDay()
    val allReferences = remember(state.daily, surahs) {
        state.daily.values.flatMap { it.ayahReferences }.filter { reference ->
            val surah = reference.substringBefore(':').toIntOrNull()
            val ayah = reference.substringAfter(':').toIntOrNull()
            val info = surahs.firstOrNull { it.number == surah }
            info != null && ayah != null && ayah in 1..info.verseCount
        }.toSet()
    }
    val readingDays = remember(state.daily) {
        state.daily.values.filter { it.ayahReferences.isNotEmpty() }.map { it.day }.toSet()
    }
    val (currentStreak, longestStreak) = remember(readingDays, today) {
        quranReadingStreaks(readingDays, today)
    }
    val completedSurahs = remember(allReferences, surahs) {
        surahs.count { info ->
            (1..info.verseCount).all { ayah -> "${info.number}:$ayah" in allReferences }
        }
    }
    val completedJuz = remember(allReferences, surahs) {
        QuranJuzIndex.completedCount(allReferences, surahs)
    }
    val last30Days = remember(today) { (29L downTo 0L).map { today - it } }
    val last30Ayahs = last30Days.sumOf { state.daily[it]?.ayahReferences?.size ?: 0 }
    val last30Seconds = last30Days.sumOf { state.daily[it]?.activeSeconds ?: 0L }
    val averageAyahs = last30Ayahs / 30.0
    val averageSeconds = last30Seconds / 30L
    val todayReading = state.daily[today] ?: QuranDailyReading(today)
    val remaining = (QuranAssetLoader.TOTAL_VERSES - allReferences.size).coerceAtLeast(0)
    val etaDays = if (remaining > 0 && averageAyahs > 0.0) {
        ceil(remaining / averageAyahs).toLong().coerceAtLeast(1L)
    } else {
        null
    }
    var chartRange by rememberSaveable { mutableStateOf("7") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                quranAnalyticsText("title", languageCode),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                quranAnalyticsText("subtitle", languageCode),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${allReferences.size} / ${QuranAssetLoader.TOTAL_VERSES} " +
                            quranAnalyticsText("unique_ayahs", languageCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = {
                            allReferences.size.toFloat() / QuranAssetLoader.TOTAL_VERSES.toFloat()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "$completedJuz/30 ${quranAnalyticsText("juz_completed", languageCode)} · " +
                            "$completedSurahs/114 ${quranAnalyticsText("surahs_completed", languageCode)}"
                    )
                    Text(
                        "${quranFormatDuration(state.totalActiveSeconds)} " +
                            quranAnalyticsText("total_reading", languageCode)
                    )
                }
            }
        }
        item {
            AnalyticsMetricRow(
                firstTitle = quranAnalyticsText("today", languageCode),
                firstValue = "${todayReading.ayahReferences.size} " +
                    quranAnalyticsText("ayahs", languageCode),
                firstDetail = quranFormatDuration(todayReading.activeSeconds),
                secondTitle = quranAnalyticsText("reading_streak", languageCode),
                secondValue = "$currentStreak " + quranAnalyticsText("days", languageCode),
                secondDetail = "$longestStreak " + quranAnalyticsText("longest_streak", languageCode)
            )
        }
        item {
            AnalyticsMetricRow(
                firstTitle = quranAnalyticsText("average_daily", languageCode),
                firstValue = "${averageAyahs.toInt()} " + quranAnalyticsText("ayahs_per_day", languageCode),
                firstDetail = quranFormatDuration(averageSeconds) + "/" +
                    quranAnalyticsText("day", languageCode),
                secondTitle = quranAnalyticsText("sessions", languageCode),
                secondValue = state.totalSessions.toString(),
                secondDetail = "${state.sessions.size} " + quranAnalyticsText("recent_saved", languageCode)
            )
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            quranAnalyticsText("reading_graph", languageCode),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilterChip(
                            selected = chartRange == "7",
                            onClick = { chartRange = "7" },
                            label = { Text(quranAnalyticsText("weekly", languageCode)) }
                        )
                        Spacer(Modifier.width(6.dp))
                        FilterChip(
                            selected = chartRange == "30",
                            onClick = { chartRange = "30" },
                            label = { Text(quranAnalyticsText("monthly", languageCode)) }
                        )
                    }
                    QuranReadingBarChart(
                        days = if (chartRange == "7") last30Days.takeLast(7) else last30Days,
                        daily = state.daily,
                        showValues = chartRange == "7"
                    )
                    Text(
                        quranAnalyticsText("graph_caption", languageCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        quranAnalyticsText("khatam_eta", languageCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    when {
                        remaining == 0 -> Text(quranAnalyticsText("khatam_complete", languageCode))
                        etaDays != null -> {
                            Text(
                                "$remaining " + quranAnalyticsText("ayahs_remaining", languageCode)
                            )
                            Text(
                                "$etaDays " + quranAnalyticsText("days_estimate", languageCode) +
                                    " · ${quranEstimateDate(etaDays)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(quranAnalyticsText("eta_note", languageCode))
                        }
                        else -> Text(quranAnalyticsText("eta_unavailable", languageCode))
                    }
                }
            }
        }
        item {
            Text(
                quranAnalyticsText("recent_sessions", languageCode),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        if (state.sessions.isEmpty()) {
            item {
                Card {
                    Text(
                        quranAnalyticsText("no_sessions", languageCode),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.sessions.take(8).size) { index ->
                QuranSessionRow(state.sessions[index], languageCode)
            }
        }
        item {
            HorizontalDivider()
            Text(
                quranAnalyticsText("method_note", languageCode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalyticsMetricRow(
    firstTitle: String,
    firstValue: String,
    firstDetail: String,
    secondTitle: String,
    secondValue: String,
    secondDetail: String
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuranMetricCard(firstTitle, firstValue, firstDetail, Modifier.weight(1f))
        QuranMetricCard(secondTitle, secondValue, secondDetail, Modifier.weight(1f))
    }
}

@Composable
private fun QuranMetricCard(
    title: String,
    value: String,
    detail: String,
    modifier: Modifier
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuranReadingBarChart(
    days: List<Long>,
    daily: Map<Long, QuranDailyReading>,
    showValues: Boolean
) {
    val values = days.map { daily[it]?.ayahReferences?.size ?: 0 }
    val maximum = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier.fillMaxWidth().height(142.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEachIndexed { index, day ->
            val value = values[index]
            val barHeight = if (value == 0) 4 else (100 * value / maximum).coerceAtLeast(8)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (showValues) {
                    Text(value.toString(), style = MaterialTheme.typography.labelSmall)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (value == 0) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primary
                        )
                )
                if (showValues || index % 5 == 0 || index == days.lastIndex) {
                    Text(
                        quranDayOfMonth(day).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun QuranSessionRow(session: QuranReadingSession, languageCode: String) {
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(quranSessionDate(session.startedAt), fontWeight = FontWeight.Bold)
                Text(
                    "${session.ayahCount} " + quranAnalyticsText("unique_ayahs", languageCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(quranFormatDuration(session.activeSeconds))
        }
    }
}

private fun quranDayOfMonth(day: Long): Int = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
    timeInMillis = day * 86_400_000L
}.get(Calendar.DAY_OF_MONTH)

private fun quranSessionDate(timestamp: Long): String =
    SimpleDateFormat("d MMM yyyy · h:mm a", Locale.getDefault()).format(Date(timestamp))

private fun quranEstimateDate(days: Long): String = SimpleDateFormat(
    "d MMM yyyy",
    Locale.getDefault()
).format(Date(System.currentTimeMillis() + days * 86_400_000L))

private fun quranFormatDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${safe}s"
    }
}

internal fun quranAnalyticsText(key: String, languageCode: String): String = when (languageCode) {
    "ur" -> when (key) {
        "title" -> "قرآن کا سفر"
        "subtitle" -> "پرائیویٹ reading history؛ کوئی leaderboard یا مقابلہ نہیں۔"
        "unique_ayahs" -> "منفرد آیات"
        "juz_completed" -> "جز مکمل"
        "surahs_completed" -> "سورہیں مکمل"
        "total_reading" -> "کل reading time"
        "today" -> "آج کی تلاوت"
        "ayahs" -> "آیات"
        "reading_streak" -> "Reading streak"
        "days" -> "دن"
        "longest_streak" -> "دن longest streak"
        "average_daily" -> "30 دن کی اوسط"
        "ayahs_per_day" -> "آیات/دن"
        "day" -> "دن"
        "sessions" -> "Reading sessions"
        "recent_saved" -> "حال کے محفوظ"
        "reading_graph" -> "Daily ayahs graph"
        "weekly" -> "7 دن"
        "monthly" -> "30 دن"
        "graph_caption" -> "ہر دن کھلی/پڑھی گئی منفرد آیات؛ تاریخ کے نیچے دن کا نمبر ہے۔"
        "khatam_eta" -> "Khatam ETA"
        "khatam_complete" -> "Recorded unique ayah coverage مکمل ہے۔"
        "ayahs_remaining" -> "آیات باقی"
        "days_estimate" -> "دن کا تخمینہ"
        "eta_note" -> "یہ حالیہ 30 دن کی اوسط پر مبنی تخمینہ ہے، مقررہ deadline نہیں۔"
        "eta_unavailable" -> "ETA کے لیے reading history شروع کریں؛ average ابھی دستیاب نہیں۔"
        "recent_sessions" -> "حال کے reading sessions"
        "no_sessions" -> "ابھی کوئی recorded session نہیں۔ Reader کھولنے سے history شروع ہوگی۔"
        "method_note" -> "Completion actual unique ayah coverage سے ہے؛ وقت صرف reader active ہونے پر گنا جاتا ہے۔ Juz boundaries: Quran Foundation. ماضی کی dates یا time invent نہیں کیے گئے۔"
        else -> key
    }
    "ar" -> when (key) {
        "title" -> "رحلة القرآن"
        "subtitle" -> "سجل قراءة خاص؛ لا لوحات ترتيب ولا منافسة."
        "unique_ayahs" -> "آيات فريدة"
        "juz_completed" -> "أجزاء مكتملة"
        "surahs_completed" -> "سور مكتملة"
        "total_reading" -> "إجمالي وقت القراءة"
        "today" -> "قراءة اليوم"
        "ayahs" -> "آيات"
        "reading_streak" -> "تتابع القراءة"
        "days" -> "أيام"
        "longest_streak" -> "أيام لأطول تتابع"
        "average_daily" -> "متوسط 30 يوماً"
        "ayahs_per_day" -> "آيات/يوم"
        "day" -> "يوم"
        "sessions" -> "جلسات القراءة"
        "recent_saved" -> "محفوظة حديثاً"
        "reading_graph" -> "رسم الآيات اليومية"
        "weekly" -> "7 أيام"
        "monthly" -> "30 يوماً"
        "graph_caption" -> "الآيات الفريدة المقروءة كل يوم؛ ويظهر رقم اليوم أسفل التاريخ."
        "khatam_eta" -> "الوقت المتوقع للختم"
        "khatam_complete" -> "اكتملت تغطية الآيات الفريدة المسجلة."
        "ayahs_remaining" -> "آيات متبقية"
        "days_estimate" -> "أيام تقديرية"
        "eta_note" -> "تقدير يعتمد على متوسط آخر 30 يوماً، وليس موعداً نهائياً."
        "eta_unavailable" -> "ابدأ سجل القراءة لحساب التقدير؛ المتوسط غير متاح بعد."
        "recent_sessions" -> "جلسات القراءة الأخيرة"
        "no_sessions" -> "لا توجد جلسة مسجلة بعد. يبدأ السجل عند فتح القارئ."
        "method_note" -> "يعتمد الإكمال على تغطية الآيات الفريدة الفعلية، ويحسب الوقت فقط أثناء نشاط القارئ. حدود الأجزاء: Quran Foundation. لم يتم اختلاق تواريخ أو أوقات سابقة."
        else -> key
    }
    else -> when (key) {
        "title" -> "Quran Journey"
        "subtitle" -> "Private reading history; no leaderboard or competition."
        "unique_ayahs" -> "unique ayahs"
        "juz_completed" -> "Juz completed"
        "surahs_completed" -> "Surahs completed"
        "total_reading" -> "total reading time"
        "today" -> "Today's reading"
        "ayahs" -> "ayahs"
        "reading_streak" -> "Reading streak"
        "days" -> "days"
        "longest_streak" -> "days longest streak"
        "average_daily" -> "30-day average"
        "ayahs_per_day" -> "ayahs/day"
        "day" -> "day"
        "sessions" -> "Reading sessions"
        "recent_saved" -> "recent saved"
        "reading_graph" -> "Daily ayahs graph"
        "weekly" -> "7 days"
        "monthly" -> "30 days"
        "graph_caption" -> "Unique ayahs viewed each day; the date's day number appears below."
        "khatam_eta" -> "Khatam ETA"
        "khatam_complete" -> "Recorded unique-ayah coverage is complete."
        "ayahs_remaining" -> "ayahs remaining"
        "days_estimate" -> "estimated days"
        "eta_note" -> "An estimate from your recent 30-day average, not a fixed deadline."
        "eta_unavailable" -> "Start a reading history for an ETA; an average is not available yet."
        "recent_sessions" -> "Recent reading sessions"
        "no_sessions" -> "No recorded sessions yet. History starts when the reader opens."
        "method_note" -> "Completion uses actual unique-ayah coverage; time counts only while the reader is active. Juz boundaries: Quran Foundation. Past dates and time are never invented."
        else -> key
    }
}
