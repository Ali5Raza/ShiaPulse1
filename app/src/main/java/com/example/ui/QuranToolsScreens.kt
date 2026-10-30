package com.example.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranProgressState
import com.example.data.QuranProgressStore
import com.example.data.QuranSearchEngine
import com.example.data.QuranSearchMatch
import com.example.data.QuranSearchResult
import com.example.data.QuranSurahInfo
import com.example.ui.theme.AmiriFont
import com.example.ui.theme.NotoNastaliqUrduFont
import kotlin.math.ceil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
fun QuranSearchPanel(
    surahs: List<QuranSurahInfo>,
    languageCode: String,
    onOpenReference: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<QuranSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchFailed by remember { mutableStateOf(false) }

    LaunchedEffect(query, surahs) {
        val searchQuery = query.trim()
        if (searchQuery.length < QuranSearchEngine.MIN_QUERY_LENGTH) {
            results = emptyList()
            isSearching = false
            searchFailed = false
            return@LaunchedEffect
        }
        isSearching = true
        searchFailed = false
        delay(350)
        try {
            results = QuranSearchEngine.search(context, surahs, searchQuery)
            isSearching = false
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            results = emptyList()
            searchFailed = true
            isSearching = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            placeholder = { Text(quranToolText("search_hint", languageCode)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = quranToolText("clear", languageCode))
                    }
                }
            },
            singleLine = true
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
            isSearching -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(quranToolText("searching", languageCode))
                }
            }
            query.trim().length < QuranSearchEngine.MIN_QUERY_LENGTH -> {
                QuranToolEmptyState(
                    title = quranToolText("search", languageCode),
                    message = quranToolText("search_min", languageCode)
                )
            }
            searchFailed -> QuranToolEmptyState(
                title = quranToolText("search", languageCode),
                message = quranToolText("search_error", languageCode)
            )
            results.isEmpty() -> QuranToolEmptyState(
                title = quranToolText("no_results", languageCode),
                message = quranToolText("try_another_search", languageCode)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (results.size >= QuranSearchEngine.MAX_RESULTS) {
                    item {
                        Text(
                            text = quranToolText("results_cap", languageCode),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(results, key = { "${it.surahNumber}:${it.ayahNumber}:${it.matchedIn}" }) { result ->
                    QuranSearchResultCard(result, languageCode) {
                        onOpenReference(result.surahNumber, result.ayahNumber)
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun QuranSearchResultCard(
    result: QuranSearchResult,
    languageCode: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (languageCode == "en") result.surahName else result.surahNameAr,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${result.surahNumber}:${result.ayahNumber}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val isRtl = result.matchedIn != QuranSearchMatch.ENGLISH
            CompositionLocalProvider(
                LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                Text(
                    text = result.text,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = when (result.matchedIn) {
                        QuranSearchMatch.ARABIC -> AmiriFont
                        QuranSearchMatch.URDU -> NotoNastaliqUrduFont
                        QuranSearchMatch.ENGLISH -> null
                    },
                    fontSize = if (result.matchedIn == QuranSearchMatch.ARABIC) 21.sp else 15.sp,
                    lineHeight = if (result.matchedIn == QuranSearchMatch.URDU) 28.sp else 24.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (isRtl) TextAlign.Right else TextAlign.Left
                )
            }
        }
    }
}

@Composable
fun QuranBookmarksPanel(
    surahs: List<QuranSurahInfo>,
    bookmarks: Set<String>,
    languageCode: String,
    onOpenReference: (Int, Int) -> Unit,
    onRemove: (String) -> Unit
) {
    val items = remember(surahs, bookmarks) {
        bookmarks.mapNotNull { reference ->
            val parts = reference.split(":")
            val surahNumber = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val ayahNumber = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            val surah = surahs.firstOrNull { it.number == surahNumber } ?: return@mapNotNull null
            if (ayahNumber !in 1..surah.verseCount) return@mapNotNull null
            Triple(reference, surah, ayahNumber)
        }.sortedWith(compareBy({ it.second.number }, { it.third }))
    }

    if (items.isEmpty()) {
        QuranToolEmptyState(
            title = quranToolText("bookmarks", languageCode),
            message = quranToolText("bookmarks_empty", languageCode)
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.first }) { (reference, surah, ayahNumber) ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onOpenReference(surah.number, ayahNumber)
                    }.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (languageCode == "en") surah.name else surah.nameAr,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = quranToolText("ayah", languageCode) + " $ayahNumber · ${surah.number}:$ayahNumber",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onRemove(reference) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = quranToolText("remove_bookmark", languageCode),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuranKhatamPanel(
    surahs: List<QuranSurahInfo>,
    progress: QuranProgressState,
    languageCode: String,
    onToggleSurah: (Int) -> Unit,
    onStartPlan: (Int) -> Unit,
    onStopPlan: () -> Unit,
    onClearProgress: () -> Unit,
    onOpenSurah: (Int) -> Unit
) {
    var selectedDays by rememberSaveable(progress.khatamTargetDays) {
        mutableIntStateOf(progress.khatamTargetDays)
    }
    var showReset by remember { mutableStateOf(false) }
    val completedVerses = surahs.filter { it.number in progress.completedSurahs }.sumOf { it.verseCount }
    val totalVerses = surahs.sumOf { it.verseCount }.coerceAtLeast(1)
    val completedFraction = completedVerses.toFloat() / totalVerses.toFloat()
    val nextSurah = surahs.firstOrNull { it.number !in progress.completedSurahs }
    val startDay = progress.khatamStartDay
    val daysElapsed = startDay?.let {
        (QuranProgressStore.currentLocalEpochDay() - it).coerceAtLeast(0L)
    }
    val daysRemaining = daysElapsed?.let { (progress.khatamTargetDays.toLong() - it).coerceAtLeast(0L) }
    val remainingVerses = (totalVerses - completedVerses).coerceAtLeast(0)
    val dailyGoal = daysRemaining?.let { days ->
        ceil(remainingVerses.toDouble() / days.coerceAtLeast(1L).toDouble()).toInt()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        quranToolText("personal_khatam", languageCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        quranToolText("khatam_intro", languageCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { completedFraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${progress.completedSurahs.size}/${surahs.size} ${quranToolText("surahs", languageCode)} · " +
                            "$completedVerses/$totalVerses ${quranToolText("verses", languageCode)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (nextSurah != null) {
                        TextButton(onClick = { onOpenSurah(nextSurah.number) }) {
                            Text(
                                quranToolText("next_surah", languageCode) + ": " +
                                    if (languageCode == "en") nextSurah.name else nextSurah.nameAr
                            )
                        }
                    }
                    if (startDay == null && progress.completedSurahs.isNotEmpty()) {
                        TextButton(onClick = { showReset = true }) {
                            Text(quranToolText("reset_progress", languageCode))
                        }
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (startDay == null) {
                        Text(
                            quranToolText("reading_plan", languageCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(7, 30, 40, 60).forEach { days ->
                                FilterChip(
                                    selected = selectedDays == days,
                                    onClick = { selectedDays = days },
                                    label = { Text(quranToolText("days", languageCode).replace("%d", days.toString())) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { onStartPlan(selectedDays) }) {
                            Text(quranToolText("start_plan", languageCode))
                        }
                    } else {
                        Text(
                            quranToolText("reading_plan", languageCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            if (daysRemaining != null && daysRemaining > 0L) {
                                quranToolText("days_left", languageCode)
                                    .replace("%d", daysRemaining.toString()) + " · " +
                                    quranToolText("daily_goal", languageCode)
                                        .replace("%d", dailyGoal?.toString() ?: "0")
                            } else {
                                quranToolText("target_reached", languageCode)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onStopPlan) {
                                Text(quranToolText("stop_plan", languageCode))
                            }
                            TextButton(onClick = { showReset = true }) {
                                Text(quranToolText("reset_progress", languageCode))
                            }
                        }
                    }
                }
            }
        }

        items(surahs, key = { it.number }) { surah ->
            val completed = surah.number in progress.completedSurahs
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onToggleSurah(surah.number) },
                shape = RoundedCornerShape(16.dp),
                tonalElevation = if (completed) 1.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = completed, onCheckedChange = { onToggleSurah(surah.number) })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${surah.number}. " + if (languageCode == "en") surah.name else surah.nameAr,
                            fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal,
                            color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${surah.verseCount} ${quranToolText("verses", languageCode)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (completed) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showReset) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(quranToolText("reset_progress", languageCode)) },
            text = { Text(quranToolText("reset_confirm", languageCode)) },
            confirmButton = {
                TextButton(onClick = {
                    onClearProgress()
                    showReset = false
                }) { Text(quranToolText("reset_progress", languageCode)) }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) {
                    Text(quranToolText("cancel", languageCode))
                }
            }
        )
    }
}

@Composable
fun QuranReaderSettingsPanel(
    progress: QuranProgressState,
    languageCode: String,
    viewingMode: Int,
    onViewingModeChange: (Int) -> Unit,
    showEnglish: Boolean,
    onShowEnglishChange: (Boolean) -> Unit,
    showUrdu: Boolean,
    onShowUrduChange: (Boolean) -> Unit,
    readingTheme: String,
    onReadingThemeChange: (String) -> Unit,
    arabicFontType: String,
    onArabicFontTypeChange: (String) -> Unit,
    onArabicScale: (Float) -> Unit,
    onTranslationScale: (Float) -> Unit,
    onKeepAwake: (Boolean) -> Unit,
    onHifzMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    autoScrollActive: Boolean? = null,
    onAutoScrollChange: (Boolean) -> Unit = {}
) {
    QuranReadingSettingsControls(
        languageCode = languageCode,
        palette = quranReadingPalette(readingTheme),
        viewingMode = viewingMode,
        onViewingModeChange = onViewingModeChange,
        showEnglish = showEnglish,
        onShowEnglishChange = onShowEnglishChange,
        showUrdu = showUrdu,
        onShowUrduChange = onShowUrduChange,
        readingTheme = readingTheme,
        onReadingThemeChange = onReadingThemeChange,
        arabicFontType = arabicFontType,
        onArabicFontTypeChange = onArabicFontTypeChange,
        arabicFontScale = progress.arabicFontScale,
        onArabicScale = onArabicScale,
        translationFontScale = progress.translationFontScale,
        onTranslationScale = onTranslationScale,
        keepScreenAwake = progress.keepScreenAwake,
        onKeepAwake = onKeepAwake,
        hifzMode = progress.hifzMode,
        onHifzMode = onHifzMode,
        modifier = modifier,
        autoScrollActive = autoScrollActive,
        onAutoScrollChange = onAutoScrollChange
    )
}

@Composable
private fun QuranToolEmptyState(title: String, message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

internal fun quranToolText(key: String, languageCode: String): String {
    if (languageCode == "ur") return when (key) {
        "advanced_navigation" -> "قرآن نیویگیشن"
        "hifz_revision" -> "حفظ 2.0"
        "quran_journey" -> "قرآن کا سفر"
        "reader_view" -> "ریڈر ویو"
        "mushaf_view" -> "مصحف ویو"
        "search" -> "قرآن میں تلاش"
        "tafsir_search" -> "تفسیر میں تلاش"
        "search_hint" -> "عربی، انگریزی یا اردو متن تلاش کریں"
        "search_min" -> "تلاش کے لیے کم از کم 2 حروف لکھیں۔"
        "searching" -> "پورے قرآن میں تلاش جاری ہے…"
        "search_error" -> "تلاش مکمل نہیں ہو سکی۔ دوبارہ کوشش کریں۔"
        "no_results" -> "کوئی نتیجہ نہیں ملا"
        "try_another_search" -> "دوسرے لفظ یا عبارت کے ساتھ کوشش کریں۔"
        "results_cap" -> "پہلے 200 نتائج دکھائے جا رہے ہیں۔ تلاش مزید مخصوص کریں۔"
        "bookmarks" -> "محفوظ آیات"
        "my_study" -> "میرا مطالعہ"
        "bookmarks_empty" -> "کسی آیت کے bookmark بٹن سے اسے یہاں محفوظ کریں۔"
        "remove_bookmark" -> "Bookmark حذف کریں"
        "personal_khatam" -> "ذاتی ختم قرآن"
        "shared_khatam" -> "مشترکہ آن لائن ختم قرآن"
        "khatam_intro" -> "پرائیویٹ، آف لائن tracker۔ سورہ مکمل ہونے کے بعد خود نشان لگائیں۔"
        "reading_plan" -> "Reading plan"
        "days" -> "%d دن"
        "start_plan" -> "Plan شروع کریں"
        "stop_plan" -> "Plan روکیں"
        "days_left" -> "%d دن باقی"
        "daily_goal" -> "روزانہ تقریباً %d آیات"
        "target_reached" -> "Target date آ گئی ہے—اپنی رفتار سے جاری رکھیں۔"
        "reset_progress" -> "Progress reset"
        "reset_confirm" -> "تمام completed Surahs اور reading plan reset ہو جائیں گے۔ Bookmarks محفوظ رہیں گے۔"
        "cancel" -> "منسوخ"
        "next_surah" -> "اگلی سورۃ"
        "surahs" -> "سورہیں"
        "verses" -> "آیات"
        "ayah" -> "آیت"
        "arabic_size" -> "عربی متن کا سائز"
        "translation_size" -> "ترجمے کا سائز"
        "keep_awake" -> "پڑھتے وقت Screen بیدار رکھیں"
        "hifz_mode" -> "حفظ / Memorisation mode"
        "hifz_note" -> "حفظ mode میں ہر آیت کا متن چھپ کر reveal کیا جا سکتا ہے اور اس کی practice status محفوظ ہوتی ہے۔"
        "reader_settings" -> "Reader settings"
        "private_offline" -> "Private · آف لائن"
        "tools" -> "قرآن tools"
        "bookmark" -> "آیت محفوظ کریں"
        "continue_reading" -> "تلاوت جاری رکھیں"
        "jump_ayah" -> "آیت پر جائیں"
        "ayah_number" -> "آیت نمبر"
        "invalid_ayah" -> "درست آیت نمبر درج کریں۔"
        "go" -> "جائیں"
        "reveal_ayah" -> "آیت دکھائیں"
        "hide_ayah" -> "آیت چھپائیں"
        "revision_status" -> "Revision status"
        "needs_practice" -> "مزید practice"
        "almost_memorised" -> "تقریباً یاد"
        "memorised" -> "حفظ شدہ"
        "clear" -> "صاف کریں"
        else -> key
    }
    if (languageCode == "ar") return when (key) {
        "advanced_navigation" -> "التنقل المتقدم"
        "hifz_revision" -> "الحفظ 2.0"
        "quran_journey" -> "رحلة القرآن"
        "reader_view" -> "عرض القارئ"
        "mushaf_view" -> "عرض المصحف"
        "search" -> "البحث في القرآن"
        "tafsir_search" -> "البحث في التفسير"
        "search_hint" -> "ابحث في النص العربي أو الإنجليزي أو الأردية"
        "search_min" -> "اكتب حرفين على الأقل للبحث."
        "searching" -> "جارٍ البحث في القرآن كاملاً…"
        "search_error" -> "تعذر إكمال البحث. حاول مرة أخرى."
        "no_results" -> "لا توجد نتائج"
        "try_another_search" -> "جرب كلمة أو عبارة أخرى."
        "results_cap" -> "تظهر أول 200 نتيجة. حدد بحثاً أدق."
        "bookmarks" -> "الآيات المحفوظة"
        "my_study" -> "دراستي"
        "bookmarks_empty" -> "استخدم زر الإشارة المرجعية بجانب أي آية لحفظها هنا."
        "remove_bookmark" -> "إزالة الإشارة المرجعية"
        "personal_khatam" -> "ختمة القرآن الشخصية"
        "shared_khatam" -> "ختمة القرآن المشتركة"
        "khatam_intro" -> "متتبع خاص دون اتصال. ضع علامة يدوياً عند إكمال كل سورة."
        "reading_plan" -> "خطة القراءة"
        "days" -> "%d أيام"
        "start_plan" -> "بدء الخطة"
        "stop_plan" -> "إيقاف الخطة"
        "days_left" -> "تبقى %d أيام"
        "daily_goal" -> "حوالي %d آيات يومياً"
        "target_reached" -> "حان تاريخ الهدف—تابع وفق وتيرتك."
        "reset_progress" -> "إعادة ضبط التقدم"
        "reset_confirm" -> "ستُعاد السور المكتملة وخطة القراءة. ستبقى الإشارات المرجعية محفوظة."
        "cancel" -> "إلغاء"
        "next_surah" -> "السورة التالية"
        "surahs" -> "سور"
        "verses" -> "آيات"
        "ayah" -> "آية"
        "arabic_size" -> "حجم النص العربي"
        "translation_size" -> "حجم الترجمة"
        "keep_awake" -> "إبقاء الشاشة نشطة أثناء القراءة"
        "hifz_mode" -> "وضع الحفظ"
        "hifz_note" -> "في وضع الحفظ يمكن إخفاء نص كل آية وكشفه وحفظ حالة المراجعة لكل آية."
        "reader_settings" -> "إعدادات القارئ"
        "private_offline" -> "خاص · دون اتصال"
        "tools" -> "أدوات القرآن"
        "bookmark" -> "حفظ الآية"
        "continue_reading" -> "متابعة القراءة"
        "jump_ayah" -> "الانتقال إلى آية"
        "ayah_number" -> "رقم الآية"
        "invalid_ayah" -> "أدخل رقم آية صالحاً."
        "go" -> "انتقال"
        "reveal_ayah" -> "إظهار الآية"
        "hide_ayah" -> "إخفاء الآية"
        "revision_status" -> "حالة المراجعة"
        "needs_practice" -> "يحتاج إلى مراجعة"
        "almost_memorised" -> "شارف على الحفظ"
        "memorised" -> "محفوظة"
        "clear" -> "مسح"
        else -> key
    }
    return when (key) {
        "advanced_navigation" -> "Advanced navigation"
        "hifz_revision" -> "Hifz 2.0"
        "quran_journey" -> "Quran Journey"
        "reader_view" -> "Reader View"
        "mushaf_view" -> "Mushaf View"
        "search" -> "Search the Quran"
        "tafsir_search" -> "Search Tafsir"
        "search_hint" -> "Search Arabic, English or Urdu text"
        "search_min" -> "Enter at least 2 characters to search every Surah."
        "searching" -> "Searching the complete Quran…"
        "search_error" -> "Search could not be completed. Please try again."
        "no_results" -> "No matching ayat"
        "try_another_search" -> "Try another word or phrase."
        "results_cap" -> "Showing the first 200 results. Refine your search for fewer matches."
        "bookmarks" -> "Bookmarked ayat"
        "my_study" -> "My Study"
        "bookmarks_empty" -> "Use the bookmark action on any ayah to save it here."
        "remove_bookmark" -> "Remove bookmark"
        "personal_khatam" -> "Personal Quran Khatam"
        "shared_khatam" -> "Shared Online Khatam"
        "khatam_intro" -> "Private, offline tracker. Manually mark each Surah after completing it."
        "reading_plan" -> "Reading plan"
        "days" -> "%d days"
        "start_plan" -> "Start plan"
        "stop_plan" -> "Stop plan"
        "days_left" -> "%d days left"
        "daily_goal" -> "about %d ayat/day"
        "target_reached" -> "Target date reached — continue at your own pace."
        "reset_progress" -> "Reset progress"
        "reset_confirm" -> "Completed Surahs and the reading plan will reset. Bookmarks will be kept."
        "cancel" -> "Cancel"
        "next_surah" -> "Next Surah"
        "surahs" -> "Surahs"
        "verses" -> "verses"
        "ayah" -> "Ayah"
        "arabic_size" -> "Arabic text size"
        "translation_size" -> "Translation text size"
        "keep_awake" -> "Keep screen awake while reading"
        "hifz_mode" -> "Hifz / memorisation mode"
        "hifz_note" -> "In Hifz mode, hide and reveal each ayah and save a private revision status."
        "reader_settings" -> "Reader settings"
        "private_offline" -> "Private · Offline"
        "tools" -> "Quran tools"
        "bookmark" -> "Bookmark ayah"
        "continue_reading" -> "Continue reading"
        "jump_ayah" -> "Go to ayah"
        "ayah_number" -> "Ayah number"
        "invalid_ayah" -> "Enter a valid ayah number."
        "go" -> "Go"
        "reveal_ayah" -> "Reveal ayah"
        "hide_ayah" -> "Hide ayah"
        "revision_status" -> "Revision status"
        "needs_practice" -> "Needs practice"
        "almost_memorised" -> "Almost memorised"
        "memorised" -> "Memorised"
        "clear" -> "Clear"
        else -> key
    }
}
