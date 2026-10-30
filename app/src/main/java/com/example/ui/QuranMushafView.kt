package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranAssetLoader
import com.example.data.QuranAyah
import com.example.data.QuranAyahMetadataRepository
import com.example.data.QuranLearningRepository
import com.example.data.QuranLearningSurah
import com.example.data.QuranMushafReference
import com.example.data.QuranMushafRepository
import com.example.data.QuranProgressStore
import com.example.data.QuranStudyStore
import com.example.data.QuranSurah
import com.example.data.QuranSurahInfo
import com.example.ui.components.AudioPlayerComponent
import com.example.ui.theme.AmiriFont
import com.example.ui.theme.NoorehudaFont
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class QuranMushafAyah(
    val info: QuranSurahInfo,
    val ayah: QuranAyah,
    val juzNumber: Int
) {
    val reference: String get() = "${info.number}:${ayah.number}"
}

private data class QuranLoadedMushafPage(
    val references: List<QuranMushafReference>,
    val ayahs: List<QuranMushafAyah>
)

@Composable
fun QuranMushafView(
    surahs: List<QuranSurahInfo>,
    languageCode: String,
    initialSurahNumber: Int,
    initialAyahNumber: Int,
    palette: QuranReadingPalette,
    arabicFontType: String,
    isFocusMode: Boolean,
    onVisibleReference: (Int, Int) -> Unit,
    onPageVisible: (List<QuranMushafReference>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = 0) { QuranMushafRepository.TOTAL_PAGES }
    val scope = rememberCoroutineScope()
    val progress by QuranProgressStore.state.collectAsState()
    val studyState by QuranStudyStore.state.collectAsState()
    var highlightedReference by remember { mutableStateOf("$initialSurahNumber:$initialAyahNumber") }
    var referenceRequest by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var navigationFailed by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var studySelection by remember { mutableStateOf<QuranMushafAyah?>(null) }
    var studySurah by remember(studySelection?.reference) { mutableStateOf<QuranSurah?>(null) }
    var learningSurah by remember(studySelection?.info?.number) {
        mutableStateOf<QuranLearningSurah?>(null)
    }
    var learningLoading by remember(studySelection?.info?.number) { mutableStateOf(false) }
    var learningFailed by remember(studySelection?.info?.number) { mutableStateOf(false) }
    var learningRetry by remember { mutableIntStateOf(0) }
    var audioUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        referenceRequest = initialSurahNumber to initialAyahNumber
    }

    LaunchedEffect(referenceRequest) {
        val request = referenceRequest ?: return@LaunchedEffect
        navigationFailed = false
        try {
            val metadata = withContext(Dispatchers.IO) {
                QuranAyahMetadataRepository.load(context, request.first, request.second)
            }
            val page = metadata.pageNumber ?: throw IllegalStateException("Page metadata is missing")
            pagerState.scrollToPage(page - 1)
            highlightedReference = "${request.first}:${request.second}"
            onVisibleReference(request.first, request.second)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            navigationFailed = true
        } finally {
            referenceRequest = null
        }
    }

    LaunchedEffect(studySelection?.reference, learningRetry) {
        val selection = studySelection ?: return@LaunchedEffect
        studySurah = null
        learningLoading = true
        learningFailed = false
        try {
            studySurah = withContext(Dispatchers.IO) {
                QuranAssetLoader.loadSurah(context, selection.info)
            }
            if (learningSurah?.surahNumber != selection.info.number) {
                learningSurah = withContext(Dispatchers.IO) {
                    QuranLearningRepository.loadSurah(context, selection.info)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            learningFailed = true
        } finally {
            learningLoading = false
        }
    }

    Column(modifier = modifier.fillMaxSize().background(palette.background)) {
        if (!isFocusMode) {
            Surface(color = palette.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                            }
                        },
                        enabled = pagerState.currentPage > 0
                    ) { Text(quranMushafText("previous", languageCode)) }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${quranMushafText("page", languageCode)} " +
                                "${pagerState.currentPage + 1}/${QuranMushafRepository.TOTAL_PAGES}",
                            fontWeight = FontWeight.Bold,
                            color = palette.text
                        )
                        Text(
                            quranMushafText("swipe_zoom", languageCode),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryText
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    (pagerState.currentPage + 1)
                                        .coerceAtMost(QuranMushafRepository.TOTAL_PAGES - 1)
                                )
                            }
                        },
                        enabled = pagerState.currentPage < QuranMushafRepository.TOTAL_PAGES - 1
                    ) { Text(quranMushafText("next", languageCode)) }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(1f) }) {
                    Text("−")
                }
                Text(
                    "${(zoomScale * 100).toInt()}%",
                    color = palette.secondaryText,
                    style = MaterialTheme.typography.labelLarge
                )
                TextButton(onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(3f) }) {
                    Text("+")
                }
                TextButton(onClick = { zoomScale = 1f }) {
                    Text(quranMushafText("reset_zoom", languageCode))
                }
            }
            if (navigationFailed) {
                Text(
                    quranMushafText("navigation_error", languageCode),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = zoomScale <= 1.01f,
            pageSpacing = 8.dp
        ) { pageIndex ->
            QuranMushafPage(
                pageNumber = pageIndex + 1,
                surahs = surahs,
                languageCode = languageCode,
                palette = palette,
                arabicFontType = arabicFontType,
                arabicFontScale = progress.arabicFontScale,
                zoomScale = zoomScale,
                onZoom = { zoom -> zoomScale = (zoomScale * zoom).coerceIn(1f, 3f) },
                highlightedReference = highlightedReference,
                isCurrent = pagerState.currentPage == pageIndex,
                onVisibleReference = { surah, ayah ->
                    highlightedReference = "$surah:$ayah"
                    onVisibleReference(surah, ayah)
                },
                onPageVisible = onPageVisible,
                onAyahSelected = { selection ->
                    highlightedReference = selection.reference
                    onVisibleReference(selection.info.number, selection.ayah.number)
                    studySelection = selection
                }
            )
        }

        if (audioUrl.isNotBlank() && !isFocusMode) {
            Text(
                quranMushafText("audio_source", languageCode),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryText
            )
            AudioPlayerComponent(
                audioUrl = audioUrl,
                onPlaybackEnded = { audioUrl = "" },
                onProgressUpdate = { _, _ -> },
                autoPlay = true,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }

    studySelection?.let { selection ->
        val reference = selection.reference
        QuranStudyAyahDialog(
            info = selection.info,
            ayah = selection.ayah,
            languageCode = languageCode,
            palette = palette,
            arabicFontType = arabicFontType,
            arabicFontScale = progress.arabicFontScale,
            translationFontScale = progress.translationFontScale,
            words = learningSurah?.wordsByAyah?.get(selection.ayah.number),
            wordsLoading = learningLoading,
            wordsFailed = learningFailed,
            note = studyState.notes[reference].orEmpty(),
            collections = studyState.collections,
            isBookmarked = reference in progress.bookmarks,
            onDismiss = { studySelection = null },
            onBookmark = {
                QuranProgressStore.toggleBookmark(context, selection.info.number, selection.ayah.number)
            },
            onPlayAyah = {
                audioUrl = QuranLearningRepository.ayahAudioUrl(
                    selection.info.number,
                    selection.ayah.number
                )
            },
            onPlayWord = { audioUrl = it },
            onRetryWords = { learningRetry++ },
            onSaveNote = { QuranStudyStore.saveNote(context, reference, it) },
            onToggleCollection = { QuranStudyStore.toggleReference(context, it, reference) },
            onNavigateAyah = { target ->
                studySurah?.verses?.firstOrNull { it.number == target }?.let { targetAyah ->
                    studySelection = selection.copy(ayah = targetAyah)
                    referenceRequest = selection.info.number to target
                }
            }
        )
    }
}

@Composable
private fun QuranMushafPage(
    pageNumber: Int,
    surahs: List<QuranSurahInfo>,
    languageCode: String,
    palette: QuranReadingPalette,
    arabicFontType: String,
    arabicFontScale: Float,
    zoomScale: Float,
    onZoom: (Float) -> Unit,
    highlightedReference: String,
    isCurrent: Boolean,
    onVisibleReference: (Int, Int) -> Unit,
    onPageVisible: (List<QuranMushafReference>) -> Unit,
    onAyahSelected: (QuranMushafAyah) -> Unit
) {
    val context = LocalContext.current
    var loaded by remember(pageNumber) { mutableStateOf<QuranLoadedMushafPage?>(null) }
    var loading by remember(pageNumber) { mutableStateOf(true) }
    var failed by remember(pageNumber) { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }

    LaunchedEffect(pageNumber, retry) {
        loading = true
        failed = false
        try {
            loaded = withContext(Dispatchers.IO) {
                val page = QuranMushafRepository.loadPage(context, pageNumber)
                val surahCache = mutableMapOf<Int, QuranSurah>()
                val ayahs = page.ayahs.map { reference ->
                    val info = surahs.firstOrNull { it.number == reference.surahNumber }
                        ?: throw IllegalStateException("Unknown Surah")
                    val surah = surahCache.getOrPut(info.number) {
                        QuranAssetLoader.loadSurah(context, info)
                    }
                    val ayah = surah.verses.firstOrNull { it.number == reference.ayahNumber }
                        ?: throw IllegalStateException("Wrong Surah/Ayah relationship")
                    QuranMushafAyah(info, ayah, reference.juzNumber)
                }
                QuranLoadedMushafPage(page.ayahs, ayahs)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }

    LaunchedEffect(isCurrent, loaded) {
        val page = loaded ?: return@LaunchedEffect
        if (isCurrent && page.ayahs.isNotEmpty()) {
            val first = page.ayahs.first()
            onVisibleReference(first.info.number, first.ayah.number)
            onPageVisible(page.references)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        color = palette.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
    ) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.accent)
            }
            failed || loaded == null -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    quranMushafText("load_error", languageCode),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = { retry++ }) { Text(quranMushafText("retry", languageCode)) }
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pageNumber) {
                        detectTransformGestures { _, _, zoom, _ -> onZoom(zoom) }
                    }
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                    }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                val page = loaded
                if (page != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${quranMushafText("juz", languageCode)} ${page.ayahs.first().juzNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.accent
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${quranMushafText("page", languageCode)} $pageNumber",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.secondaryText
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = palette.border
                    )
                    page.ayahs.groupBy { it.info.number }.values.forEach { group ->
                        val info = group.first().info
                        Text(
                            info.nameAr,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = NoorehudaFont,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center,
                            color = palette.accent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (languageCode == "en") info.name else "${info.number} · ${info.name}",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            color = palette.secondaryText
                        )
                        Spacer(Modifier.height(8.dp))
                        QuranMushafArabicBlock(
                            ayahs = group,
                            highlightedReference = highlightedReference,
                            palette = palette,
                            arabicFontType = arabicFontType,
                            arabicFontScale = arabicFontScale,
                            onAyahSelected = onAyahSelected
                        )
                        Spacer(Modifier.height(18.dp))
                    }
                    Text(
                        quranMushafText("source_note", languageCode),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun QuranMushafArabicBlock(
    ayahs: List<QuranMushafAyah>,
    highlightedReference: String,
    palette: QuranReadingPalette,
    arabicFontType: String,
    arabicFontScale: Float,
    onAyahSelected: (QuranMushafAyah) -> Unit
) {
    val content = buildAnnotatedString {
        ayahs.forEach { item ->
            pushStringAnnotation("ayah", item.reference)
            withStyle(
                SpanStyle(
                    background = if (item.reference == highlightedReference) {
                        palette.accent.copy(alpha = 0.18f)
                    } else {
                        androidx.compose.ui.graphics.Color.Unspecified
                    }
                )
            ) {
                append(item.ayah.arabic)
                append("  ﴿${item.ayah.number}﴾  ")
            }
            pop()
        }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ClickableText(
            text = content,
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                fontFamily = if (arabicFontType == "Indo-Pak") NoorehudaFont else AmiriFont,
                fontSize = (28 * arabicFontScale).sp,
                lineHeight = (48 * arabicFontScale).sp,
                textAlign = TextAlign.Justify,
                color = palette.arabic
            ),
            onClick = { offset ->
                content.getStringAnnotations("ayah", offset, offset).firstOrNull()?.let { annotation ->
                    ayahs.firstOrNull { it.reference == annotation.item }?.let(onAyahSelected)
                }
            }
        )
    }
}

private fun quranMushafText(key: String, languageCode: String): String = when (languageCode) {
    "ur" -> when (key) {
        "previous" -> "پچھلا"
        "next" -> "اگلا"
        "page" -> "صفحہ"
        "juz" -> "جز"
        "swipe_zoom" -> "Swipe کریں · pinch zoom"
        "reset_zoom" -> "Zoom reset"
        "navigation_error" -> "موجودہ آیت کا صفحہ لوڈ نہیں ہو سکا؛ pages پھر بھی browse کر سکتے ہیں۔"
        "load_error" -> "صفحے کی metadata یا متن لوڈ نہیں ہو سکا۔"
        "retry" -> "دوبارہ کوشش"
        "audio_source" -> "قاری: Mishari Rashid al-Afasy · Quran Foundation · URL-first، پھر cache"
        "source_note" -> "Page mapping: Quran Foundation · متن: bundled Tanzil assets"
        else -> key
    }
    "ar" -> when (key) {
        "previous" -> "السابق"
        "next" -> "التالي"
        "page" -> "صفحة"
        "juz" -> "جزء"
        "swipe_zoom" -> "اسحب · كبّر باللمس"
        "reset_zoom" -> "إعادة التكبير"
        "navigation_error" -> "تعذر تحميل صفحة الآية الحالية؛ ويمكن تصفح الصفحات."
        "load_error" -> "تعذر تحميل بيانات الصفحة أو النص."
        "retry" -> "إعادة المحاولة"
        "audio_source" -> "القارئ: Mishari Rashid al-Afasy · Quran Foundation · من الرابط ثم التخزين"
        "source_note" -> "ترتيب الصفحات: Quran Foundation · النص: ملفات Tanzil المضمنة"
        else -> key
    }
    else -> when (key) {
        "previous" -> "Previous"
        "next" -> "Next"
        "page" -> "Page"
        "juz" -> "Juz"
        "swipe_zoom" -> "Swipe pages · pinch to zoom"
        "reset_zoom" -> "Reset zoom"
        "navigation_error" -> "The current ayah's page could not be loaded; pages can still be browsed."
        "load_error" -> "The page metadata or text could not be loaded."
        "retry" -> "Retry"
        "audio_source" -> "Reciter: Mishari Rashid al-Afasy · Quran Foundation · URL-first, then cache"
        "source_note" -> "Page mapping: Quran Foundation · text: bundled Tanzil assets"
        else -> key
    }
}
