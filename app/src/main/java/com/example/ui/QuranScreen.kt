package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranAssetLoader
import com.example.data.QuranAyah
import com.example.data.QuranLearningRepository
import com.example.data.QuranHifzStore
import com.example.data.QuranReadingAnalyticsStore
import com.example.data.QuranLearningSurah
import com.example.data.QuranMemorisationStatus
import com.example.data.QuranWord
import com.example.data.QuranProgressStore
import com.example.data.QuranReadingPosition
import com.example.data.QuranSurah
import com.example.data.QuranSurahInfo
import com.example.data.QuranStudyStore
import com.example.data.quranPhraseSegments
import com.example.ui.theme.AmiriFont
import com.example.ui.theme.NoorehudaFont
import com.example.ui.theme.NotoNastaliqUrduFont
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class QuranDestination {
    INDEX,
    NAVIGATION,
    HIFZ,
    ANALYTICS,
    SEARCH,
    TAFSIR_SEARCH,
    BOOKMARKS,
    KHATAM,
    SHARED_KHATAM,
    STUDY_LIBRARY,
    SETTINGS
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    currentLang: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialSurahNumber: Int? = null,
    initialAyahNumber: Int? = null,
    initialOpenSharedKhatam: Boolean = false
) {
    val context = LocalContext.current
    var surahs by remember { mutableStateOf<List<QuranSurahInfo>>(emptyList()) }
    var isIndexLoading by remember { mutableStateOf(true) }
    var indexLoadFailed by remember { mutableStateOf(false) }
    var indexReloadTrigger by remember { mutableIntStateOf(0) }
    var selectedSurahNumber by rememberSaveable(initialSurahNumber) {
        mutableStateOf(initialSurahNumber?.takeIf { it in 1..QuranAssetLoader.TOTAL_SURAHS })
    }
    var targetAyahNumber by rememberSaveable(initialSurahNumber, initialAyahNumber) {
        mutableStateOf(initialAyahNumber?.takeIf { it > 0 })
    }
    var showSources by remember { mutableStateOf(false) }
    var showEnglish by rememberSaveable(currentLang) { mutableStateOf(currentLang == "en") }
    var showUrdu by rememberSaveable(currentLang) { mutableStateOf(currentLang == "ur") }
    var destination by rememberSaveable(initialOpenSharedKhatam) {
        mutableStateOf(if (initialOpenSharedKhatam) QuranDestination.SHARED_KHATAM else QuranDestination.INDEX)
    }
    var showToolsMenu by remember { mutableStateOf(false) }
    var showAyahJump by remember { mutableStateOf(false) }
    var ayahJumpRequest by remember { mutableIntStateOf(0) }
    var currentAyahNumber by rememberSaveable(selectedSurahNumber) {
        mutableIntStateOf(targetAyahNumber ?: 1)
    }
    val progress by QuranProgressStore.state.collectAsState()
    val studyState by QuranStudyStore.state.collectAsState()
    val readingPrefs = remember {
        context.getSharedPreferences("shia_tracker_prefs", Context.MODE_PRIVATE)
    }
    var readingTheme by remember {
        mutableStateOf(readingPrefs.getString("reading_theme", "Cream") ?: "Cream")
    }
    var arabicFontType by remember {
        mutableStateOf(readingPrefs.getString("arabic_font_type", "Indo-Pak") ?: "Indo-Pak")
    }
    var viewingMode by rememberSaveable { mutableIntStateOf(0) }
    var showReaderSettings by remember { mutableStateOf(false) }
    var isFocusMode by remember { mutableStateOf(false) }
    var isAutoScrollActive by remember { mutableStateOf(false) }
    var readerLayout by rememberSaveable { mutableIntStateOf(0) }
    val readingPalette = quranReadingPalette(readingTheme)
    val updateShowEnglish: (Boolean) -> Unit = { enabled ->
        if (enabled || showUrdu || viewingMode != 2) showEnglish = enabled
    }
    val updateShowUrdu: (Boolean) -> Unit = { enabled ->
        if (enabled || showEnglish || viewingMode != 2) showUrdu = enabled
    }
    val updateViewingMode: (Int) -> Unit = { mode ->
        viewingMode = mode
        if (mode == 2 && !showEnglish && !showUrdu) showEnglish = true
    }
    val updateReadingTheme: (String) -> Unit = { theme ->
        readingTheme = theme
        readingPrefs.edit().putString("reading_theme", theme).apply()
    }
    val updateArabicFontType: (String) -> Unit = { fontType ->
        arabicFontType = fontType
        readingPrefs.edit().putString("arabic_font_type", fontType).apply()
    }

    LaunchedEffect(context) {
        QuranProgressStore.initialize(context)
        QuranStudyStore.initialize(context)
        QuranHifzStore.initialize(context)
        QuranReadingAnalyticsStore.initialize(context)
    }

    LaunchedEffect(progress.isLoaded, progress.memorisation) {
        if (progress.isLoaded) {
            QuranHifzStore.migrateLegacyStatuses(context, progress.memorisation)
        }
    }

    LaunchedEffect(indexReloadTrigger) {
        isIndexLoading = true
        indexLoadFailed = false
        try {
            surahs = withContext(Dispatchers.IO) { QuranAssetLoader.loadIndex(context) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            indexLoadFailed = true
        } finally {
            isIndexLoading = false
        }
    }

    BackHandler(
        enabled = isFocusMode || showReaderSettings || selectedSurahNumber != null ||
            destination != QuranDestination.INDEX
    ) {
        when {
            isFocusMode -> isFocusMode = false
            showReaderSettings -> showReaderSettings = false
            destination != QuranDestination.INDEX -> destination = QuranDestination.INDEX
            selectedSurahNumber != null -> {
                selectedSurahNumber = null
                targetAyahNumber = null
            }
        }
    }

    LaunchedEffect(destination, selectedSurahNumber) {
        if (destination != QuranDestination.INDEX || selectedSurahNumber == null) {
            showReaderSettings = false
            isFocusMode = false
            isAutoScrollActive = false
        }
    }

    LaunchedEffect(targetAyahNumber) {
        if (targetAyahNumber != null) currentAyahNumber = targetAyahNumber ?: 1
    }

    val selectedInfo = surahs.firstOrNull { it.number == selectedSurahNumber }
    val currentReference = selectedSurahNumber?.let {
        QuranProgressStore.quranReferenceKey(it, currentAyahNumber)
    }
    val isCurrentBookmarked = currentReference != null && currentReference in progress.bookmarks
    val isReaderVisible = destination == QuranDestination.INDEX && selectedInfo != null
    QuranReadingAnalyticsTracker(enabled = isReaderVisible)

    LaunchedEffect(isReaderVisible, currentReference) {
        if (isReaderVisible && selectedSurahNumber != null) {
            QuranReadingAnalyticsStore.recordAyah(
                context,
                selectedSurahNumber ?: return@LaunchedEffect,
                currentAyahNumber
            )
        }
    }

    val useReadingPalette = isReaderVisible || destination == QuranDestination.SETTINGS
    Scaffold(
        modifier = modifier,
        containerColor = if (useReadingPalette) readingPalette.background else MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (isReaderVisible && isFocusMode) {
                FloatingActionButton(
                    onClick = { isFocusMode = false },
                    containerColor = readingPalette.accent,
                    contentColor = readingPalette.onAccent
                ) {
                    Icon(
                        Icons.Default.FullscreenExit,
                        contentDescription = quranReadingText("focus_mode", currentLang)
                    )
                }
            }
        },
        topBar = {
            if (!isReaderVisible || !isFocusMode) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (destination != QuranDestination.INDEX) {
                                quranToolText(
                                    when (destination) {
                                        QuranDestination.NAVIGATION -> "advanced_navigation"
                                        QuranDestination.HIFZ -> "hifz_revision"
                                        QuranDestination.ANALYTICS -> "quran_journey"
                                        QuranDestination.SEARCH -> "search"
                                        QuranDestination.TAFSIR_SEARCH -> "tafsir_search"
                                        QuranDestination.BOOKMARKS -> "bookmarks"
                                        QuranDestination.KHATAM -> "personal_khatam"
                                        QuranDestination.SHARED_KHATAM -> "shared_khatam"
                                        QuranDestination.STUDY_LIBRARY -> "my_study"
                                        else -> "reader_settings"
                                    },
                                    currentLang
                                )
                            } else {
                                selectedInfo?.let { quranSurahName(it, currentLang) }
                                    ?: quranUiText("title", currentLang)
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (destination == QuranDestination.SHARED_KHATAM) {
                                quranSharedText("live_shared", currentLang)
                            } else if (destination == QuranDestination.NAVIGATION) {
                                quranNavigationText("source_short", currentLang)
                            } else if (destination != QuranDestination.INDEX) {
                                quranToolText("private_offline", currentLang)
                            } else {
                                selectedInfo?.let {
                                    "${quranUiText("surah", currentLang)} ${it.number} · " +
                                        "${quranToolText("ayah", currentLang)} " +
                                        "${currentAyahNumber.coerceIn(1, it.verseCount)}/${it.verseCount}"
                                } ?: quranUiText("subtitle", currentLang)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (useReadingPalette) readingPalette.secondaryText else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when {
                                destination != QuranDestination.INDEX -> {
                                    destination = QuranDestination.INDEX
                                }
                                selectedSurahNumber != null -> {
                                    selectedSurahNumber = null
                                    targetAyahNumber = null
                                }
                                else -> onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = quranUiText("back", currentLang)
                        )
                    }
                },
                actions = {
                    if (selectedSurahNumber != null && destination == QuranDestination.INDEX) {
                        IconButton(
                            onClick = {
                                val info = selectedInfo ?: return@IconButton
                                if (currentAyahNumber !in 1..info.verseCount) return@IconButton
                                QuranProgressStore.toggleBookmark(
                                    context,
                                    info.number,
                                    currentAyahNumber
                                )
                            }
                        ) {
                            Icon(
                                imageVector = if (isCurrentBookmarked) {
                                    Icons.Default.Bookmark
                                } else {
                                    Icons.Default.BookmarkBorder
                                },
                                contentDescription = quranToolText(
                                    if (isCurrentBookmarked) "remove_bookmark" else "bookmark",
                                    currentLang
                                ),
                                tint = if (isCurrentBookmarked) {
                                    if (useReadingPalette) readingPalette.accent else MaterialTheme.colorScheme.primary
                                } else {
                                    LocalContentColor.current
                                }
                            )
                        }
                    } else if (destination == QuranDestination.INDEX) {
                        IconButton(onClick = { destination = QuranDestination.SEARCH }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = quranToolText("search", currentLang)
                            )
                        }
                    }
                    if (isReaderVisible) {
                        IconButton(onClick = { showReaderSettings = !showReaderSettings }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = quranToolText("reader_settings", currentLang)
                            )
                        }
                    }
                    IconButton(onClick = { showSources = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = quranUiText("sources", currentLang)
                        )
                    }
                    Box {
                        IconButton(onClick = { showToolsMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = quranToolText("tools", currentLang)
                            )
                        }
                        DropdownMenu(
                            expanded = showToolsMenu,
                            onDismissRequest = { showToolsMenu = false }
                        ) {
                            if (selectedSurahNumber != null && destination == QuranDestination.INDEX) {
                                DropdownMenuItem(
                                    text = { Text(quranReadingText("focus_mode", currentLang)) },
                                    onClick = {
                                        showToolsMenu = false
                                        showReaderSettings = false
                                        isFocusMode = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(quranToolText("jump_ayah", currentLang)) },
                                    onClick = {
                                        showToolsMenu = false
                                        showAyahJump = true
                                    }
                                )
                            }
                            listOf(
                                QuranDestination.NAVIGATION,
                                QuranDestination.HIFZ,
                                QuranDestination.ANALYTICS,
                                QuranDestination.SEARCH,
                                QuranDestination.TAFSIR_SEARCH,
                                QuranDestination.BOOKMARKS,
                                QuranDestination.KHATAM,
                                QuranDestination.SHARED_KHATAM,
                                QuranDestination.STUDY_LIBRARY,
                                QuranDestination.SETTINGS
                            ).forEach { tool ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            quranToolText(
                                                when (tool) {
                                                    QuranDestination.NAVIGATION -> "advanced_navigation"
                                                    QuranDestination.HIFZ -> "hifz_revision"
                                                    QuranDestination.ANALYTICS -> "quran_journey"
                                                    QuranDestination.SEARCH -> "search"
                                                    QuranDestination.TAFSIR_SEARCH -> "tafsir_search"
                                                    QuranDestination.BOOKMARKS -> "bookmarks"
                                                    QuranDestination.KHATAM -> "personal_khatam"
                                                    QuranDestination.SHARED_KHATAM -> "shared_khatam"
                                                    QuranDestination.STUDY_LIBRARY -> "my_study"
                                                    else -> "reader_settings"
                                                },
                                                currentLang
                                            )
                                        )
                                    },
                                    onClick = {
                                        showToolsMenu = false
                                        if (selectedSurahNumber != null && destination == QuranDestination.INDEX) {
                                            targetAyahNumber = currentAyahNumber
                                        }
                                        destination = tool
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (useReadingPalette) readingPalette.surface else MaterialTheme.colorScheme.surface,
                    titleContentColor = if (useReadingPalette) readingPalette.arabic else MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = if (useReadingPalette) readingPalette.text else LocalContentColor.current,
                    actionIconContentColor = if (useReadingPalette) readingPalette.text else LocalContentColor.current
                )
            )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isIndexLoading -> QuranLoadingState(currentLang)
                indexLoadFailed -> QuranErrorState(
                    languageCode = currentLang,
                    onRetry = { indexReloadTrigger++ }
                )
                destination == QuranDestination.INDEX && selectedInfo != null -> Column(
                    modifier = Modifier.fillMaxSize().background(readingPalette.background)
                ) {
                    AnimatedVisibility(visible = showReaderSettings && !isFocusMode) {
                        QuranReaderSettingsPanel(
                            progress = progress,
                            languageCode = currentLang,
                            viewingMode = viewingMode,
                            onViewingModeChange = updateViewingMode,
                            showEnglish = showEnglish,
                            onShowEnglishChange = updateShowEnglish,
                            showUrdu = showUrdu,
                            onShowUrduChange = updateShowUrdu,
                            readingTheme = readingTheme,
                            onReadingThemeChange = updateReadingTheme,
                            arabicFontType = arabicFontType,
                            onArabicFontTypeChange = updateArabicFontType,
                            onArabicScale = { QuranProgressStore.setArabicFontScale(context, it) },
                            onTranslationScale = { QuranProgressStore.setTranslationFontScale(context, it) },
                            onKeepAwake = { QuranProgressStore.setKeepScreenAwake(context, it) },
                            onHifzMode = { QuranProgressStore.setHifzMode(context, it) },
                            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                            autoScrollActive = isAutoScrollActive,
                            onAutoScrollChange = { isAutoScrollActive = it }
                        )
                    }
                    if (!isFocusMode) {
                        QuranReaderLayoutSelector(
                            readerLayout = readerLayout,
                            languageCode = currentLang,
                            onLayoutChange = {
                                readerLayout = it
                                targetAyahNumber = currentAyahNumber
                                ayahJumpRequest++
                            }
                        )
                    }
                    if (readerLayout == 0) {
                        QuranReader(
                            info = selectedInfo,
                            languageCode = currentLang,
                            showEnglish = showEnglish,
                            showUrdu = showUrdu,
                            targetAyahNumber = targetAyahNumber,
                            ayahJumpRequest = ayahJumpRequest,
                            palette = readingPalette,
                            viewingMode = viewingMode,
                            arabicFontType = arabicFontType,
                            isFocusMode = isFocusMode,
                            isAutoScrollActive = isAutoScrollActive,
                            onShowEnglishChange = updateShowEnglish,
                            onShowUrduChange = updateShowUrdu,
                            onVisibleAyahChanged = { currentAyahNumber = it },
                            onSelectSurah = {
                                targetAyahNumber = null
                                currentAyahNumber = 1
                                selectedSurahNumber = it
                            },
                            onStudyAyahNavigate = {
                                currentAyahNumber = it
                                targetAyahNumber = it
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        QuranMushafView(
                            surahs = surahs,
                            languageCode = currentLang,
                            initialSurahNumber = selectedInfo.number,
                            initialAyahNumber = currentAyahNumber.coerceIn(1, selectedInfo.verseCount),
                            palette = readingPalette,
                            arabicFontType = arabicFontType,
                            isFocusMode = isFocusMode,
                            onVisibleReference = { surahNumber, ayahNumber ->
                                selectedSurahNumber = surahNumber
                                currentAyahNumber = ayahNumber
                                targetAyahNumber = ayahNumber
                            },
                            onPageVisible = { references ->
                                QuranReadingAnalyticsStore.recordAyahs(
                                    context,
                                    references.map { it.reference }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                destination == QuranDestination.NAVIGATION -> QuranNavigationPanel(
                    surahs = surahs,
                    languageCode = currentLang,
                    currentSurahNumber = selectedSurahNumber,
                    currentAyahNumber = currentAyahNumber,
                    lastRead = progress.lastRead,
                    onOpenReference = { surahNumber, ayahNumber ->
                        currentAyahNumber = ayahNumber
                        targetAyahNumber = ayahNumber
                        selectedSurahNumber = surahNumber
                        destination = QuranDestination.INDEX
                    }
                )
                destination == QuranDestination.ANALYTICS -> QuranReadingAnalyticsPanel(
                    surahs = surahs,
                    languageCode = currentLang
                )
                destination == QuranDestination.HIFZ -> QuranHifzPanel(
                    surahs = surahs,
                    languageCode = currentLang,
                    onOpenReference = { surahNumber, ayahNumber ->
                        currentAyahNumber = ayahNumber
                        targetAyahNumber = ayahNumber
                        selectedSurahNumber = surahNumber
                        destination = QuranDestination.INDEX
                    }
                )
                destination == QuranDestination.SEARCH -> QuranSearchPanel(
                    surahs = surahs,
                    languageCode = currentLang,
                    onOpenReference = { surahNumber, ayahNumber ->
                        currentAyahNumber = ayahNumber
                        targetAyahNumber = ayahNumber
                        selectedSurahNumber = surahNumber
                        destination = QuranDestination.INDEX
                    }
                )
                destination == QuranDestination.TAFSIR_SEARCH -> QuranTafsirSearchPanel(
                    surahs = surahs,
                    languageCode = currentLang,
                    onOpenReference = { surahNumber, ayahNumber ->
                        currentAyahNumber = ayahNumber
                        targetAyahNumber = ayahNumber
                        selectedSurahNumber = surahNumber
                        destination = QuranDestination.INDEX
                    }
                )
                destination == QuranDestination.BOOKMARKS -> QuranBookmarksPanel(
                    surahs = surahs,
                    bookmarks = progress.bookmarks,
                    languageCode = currentLang,
                    onOpenReference = { surahNumber, ayahNumber ->
                        currentAyahNumber = ayahNumber
                        targetAyahNumber = ayahNumber
                        selectedSurahNumber = surahNumber
                        destination = QuranDestination.INDEX
                    },
                    onRemove = { QuranProgressStore.removeBookmark(context, it) }
                )
                destination == QuranDestination.KHATAM -> QuranKhatamPanel(
                    surahs = surahs,
                    progress = progress,
                    languageCode = currentLang,
                    onToggleSurah = { QuranProgressStore.toggleSurahCompleted(context, it) },
                    onStartPlan = { QuranProgressStore.startKhatamPlan(context, it) },
                    onStopPlan = { QuranProgressStore.stopKhatamPlan(context) },
                    onClearProgress = { QuranProgressStore.clearKhatamProgress(context) },
                    onOpenSurah = {
                        targetAyahNumber = null
                        currentAyahNumber = 1
                        selectedSurahNumber = it
                        destination = QuranDestination.INDEX
                    }
                )
                destination == QuranDestination.SHARED_KHATAM -> QuranSharedKhatamPanel(
                    languageCode = currentLang
                )
                destination == QuranDestination.STUDY_LIBRARY -> QuranStudyLibraryPanel(
                    surahs = surahs,
                    studyState = studyState,
                    languageCode = currentLang,
                    onOpenReference = { surahNumber, ayahNumber ->
                        currentAyahNumber = ayahNumber
                        targetAyahNumber = ayahNumber
                        selectedSurahNumber = surahNumber
                        destination = QuranDestination.INDEX
                    },
                    onCreateCollection = { name ->
                        QuranStudyStore.createCollection(context, name)
                    },
                    onDeleteCollection = { QuranStudyStore.deleteCollection(context, it) },
                    onDeleteNote = { QuranStudyStore.deleteNote(context, it) },
                    onRemoveReference = { collectionId, reference ->
                        QuranStudyStore.removeReference(context, collectionId, reference)
                    }
                )
                destination == QuranDestination.SETTINGS -> QuranReaderSettingsPanel(
                    progress = progress,
                    languageCode = currentLang,
                    viewingMode = viewingMode,
                    onViewingModeChange = updateViewingMode,
                    showEnglish = showEnglish,
                    onShowEnglishChange = updateShowEnglish,
                    showUrdu = showUrdu,
                    onShowUrduChange = updateShowUrdu,
                    readingTheme = readingTheme,
                    onReadingThemeChange = updateReadingTheme,
                    arabicFontType = arabicFontType,
                    onArabicFontTypeChange = updateArabicFontType,
                    onArabicScale = { QuranProgressStore.setArabicFontScale(context, it) },
                    onTranslationScale = { QuranProgressStore.setTranslationFontScale(context, it) },
                    onKeepAwake = { QuranProgressStore.setKeepScreenAwake(context, it) },
                    onHifzMode = { QuranProgressStore.setHifzMode(context, it) },
                    modifier = Modifier.fillMaxSize()
                )
                else -> QuranSurahIndex(
                    surahs = surahs,
                    languageCode = currentLang,
                    lastRead = progress.lastRead,
                    onSurahClick = {
                        targetAyahNumber = null
                        currentAyahNumber = 1
                        selectedSurahNumber = it
                    },
                    onContinueReading = { position ->
                        currentAyahNumber = position.ayahNumber
                        targetAyahNumber = position.ayahNumber
                        selectedSurahNumber = position.surahNumber
                    },
                    onShowSources = { showSources = true }
                )
            }
        }
    }

    if (showSources) {
        QuranSourcesDialog(
            languageCode = currentLang,
            onDismiss = { showSources = false },
            onOpenUrl = { openQuranSource(context, it) }
        )
    }

    val jumpSurah = selectedInfo
    if (showAyahJump && jumpSurah != null) {
        QuranAyahJumpDialog(
            info = jumpSurah,
            currentAyahNumber = currentAyahNumber,
            languageCode = currentLang,
            onDismiss = { showAyahJump = false },
            onJump = { ayahNumber ->
                currentAyahNumber = ayahNumber
                targetAyahNumber = ayahNumber
                ayahJumpRequest++
                showAyahJump = false
            }
        )
    }
}

@Composable
private fun QuranSurahIndex(
    surahs: List<QuranSurahInfo>,
    languageCode: String,
    lastRead: QuranReadingPosition?,
    onSurahClick: (Int) -> Unit,
    onContinueReading: (QuranReadingPosition) -> Unit,
    onShowSources: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredSurahs = remember(surahs, query) {
        val normalized = query.trim().lowercase(Locale.ROOT)
        if (normalized.isEmpty()) {
            surahs
        } else {
            surahs.filter {
                it.name.lowercase(Locale.ROOT).contains(normalized) ||
                    it.nameAr.contains(query.trim()) ||
                    it.number.toString() == normalized
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.88f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(27.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = quranUiText("offline", languageCode),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "114 ${quranUiText("surahs", languageCode)} · 6,236 ${quranUiText("verses", languageCode)}",
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Tanzil · Qarai · Jawadi",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        val continuePosition = lastRead
        val continueSurah = continuePosition?.let { position ->
            surahs.firstOrNull {
                it.number == position.surahNumber && position.ayahNumber in 1..it.verseCount
            }
        }
        if (continuePosition != null && continueSurah != null) {
            Surface(
                onClick = { onContinueReading(continuePosition) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            quranToolText("continue_reading", languageCode),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${quranSurahName(continueSurah, languageCode)} · ${continuePosition.surahNumber}:${continuePosition.ayahNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text(quranUiText("search", languageCode)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = quranUiText("clear", languageCode))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (filteredSurahs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = quranUiText("no_results", languageCode),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(filteredSurahs, key = { it.number }) { info ->
                    QuranSurahListItem(
                        info = info,
                        languageCode = languageCode,
                        onClick = { onSurahClick(info.number) }
                    )
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ) {
                        TextButton(
                            onClick = onShowSources,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = quranUiText("attribution", languageCode),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuranSurahListItem(
    info: QuranSurahInfo,
    languageCode: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        ),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = info.number.toString(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quranSurahName(info, languageCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${quranRevelationType(info.revelationType, languageCode)} · ${info.verseCount} ${quranUiText("verses", languageCode)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (languageCode == "en") info.nameAr else info.name,
                fontFamily = if (languageCode == "en") AmiriFont else null,
                style = if (languageCode == "en") {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.labelMedium
                },
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuranReaderLayoutSelector(
    readerLayout: Int,
    languageCode: String,
    onLayoutChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = readerLayout == 0,
            onClick = { onLayoutChange(0) },
            label = { Text(quranToolText("reader_view", languageCode)) }
        )
        FilterChip(
            selected = readerLayout == 1,
            onClick = { onLayoutChange(1) },
            label = { Text(quranToolText("mushaf_view", languageCode)) }
        )
    }
}

@Composable
private fun QuranReader(
    info: QuranSurahInfo,
    languageCode: String,
    showEnglish: Boolean,
    showUrdu: Boolean,
    targetAyahNumber: Int?,
    ayahJumpRequest: Int,
    palette: QuranReadingPalette,
    viewingMode: Int,
    arabicFontType: String,
    isFocusMode: Boolean,
    isAutoScrollActive: Boolean,
    onShowEnglishChange: (Boolean) -> Unit,
    onShowUrduChange: (Boolean) -> Unit,
    onVisibleAyahChanged: (Int) -> Unit,
    onSelectSurah: (Int) -> Unit,
    onStudyAyahNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progress by QuranProgressStore.state.collectAsState()
    val studyState by QuranStudyStore.state.collectAsState()
    var surah by remember(info.number) { mutableStateOf<QuranSurah?>(null) }
    var isLoading by remember(info.number) { mutableStateOf(true) }
    var loadFailed by remember(info.number) { mutableStateOf(false) }
    var reloadTrigger by remember(info.number) { mutableIntStateOf(0) }
    var learningMode by rememberSaveable { mutableIntStateOf(0) }
    var learningSurah by remember(info.number) { mutableStateOf<QuranLearningSurah?>(null) }
    var learningLoading by remember(info.number) { mutableStateOf(false) }
    var learningLoadFailed by remember(info.number) { mutableStateOf(false) }
    var learningReloadTrigger by remember(info.number) { mutableIntStateOf(0) }
    var audioUrl by remember(info.number) { mutableStateOf("") }
    var audioAyahNumber by remember(info.number) { mutableStateOf<Int?>(null) }
    var studyAyah by remember(info.number) { mutableStateOf<QuranAyah?>(null) }
    val listState = rememberLazyListState()
    var revealedAyahs by remember(info.number, progress.hifzMode) {
        mutableStateOf(setOf<Int>())
    }
    val activity = context as? Activity

    DisposableEffect(activity, progress.keepScreenAwake) {
        val window = activity?.window
        updateQuranKeepScreenAwake(window, progress.keepScreenAwake)
        onDispose {
            updateQuranKeepScreenAwake(window, enabled = false)
        }
    }

    LaunchedEffect(info.number, reloadTrigger) {
        isLoading = true
        loadFailed = false
        surah = null
        try {
            val loadedSurah = withContext(Dispatchers.IO) { QuranAssetLoader.loadSurah(context, info) }
            surah = loadedSurah
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            loadFailed = true
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(info.number, learningMode, learningReloadTrigger, studyAyah != null) {
        if ((learningMode != 1 && studyAyah == null) || learningSurah != null) return@LaunchedEffect
        learningLoading = true
        learningLoadFailed = false
        try {
            learningSurah = withContext(Dispatchers.IO) {
                QuranLearningRepository.loadSurah(context, info)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            learningLoadFailed = true
        } finally {
            learningLoading = false
        }
    }

    LaunchedEffect(surah, targetAyahNumber, ayahJumpRequest) {
        val loadedSurah = surah ?: return@LaunchedEffect
        val targetIndex = targetAyahNumber
            ?.takeIf { it in 1..loadedSurah.verses.size }
            ?.let { it - 1 }
            ?: 0
        listState.scrollToItem(targetIndex)
    }

    LaunchedEffect(isAutoScrollActive, surah?.number) {
        if (isAutoScrollActive && surah != null) {
            while (true) {
                listState.animateScrollBy(2.0f)
                delay(32)
            }
        }
    }

    LaunchedEffect(listState, info.number, surah?.number) {
        if (surah == null) return@LaunchedEffect
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key is Int }
                ?.key as? Int
        }
            .distinctUntilChanged()
            .collectLatest { ayahNumber ->
                if (ayahNumber != null) {
                    onVisibleAyahChanged(ayahNumber)
                    delay(600)
                    QuranProgressStore.recordReading(context, info.number, ayahNumber)
                }
            }
    }

    val playAyah: (Int) -> Unit = { number ->
        val validNumber = number.coerceIn(1, info.verseCount)
        audioAyahNumber = validNumber
        audioUrl = QuranLearningRepository.ayahAudioUrl(info.number, validNumber)
    }
    val playWord: (String) -> Unit = { url ->
        audioAyahNumber = null
        audioUrl = url
    }

    Column(modifier = modifier.fillMaxSize().background(palette.background)) {
        if (!isFocusMode) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(18.dp),
            color = palette.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = info.nameAr,
                    fontFamily = if (arabicFontType == "Indo-Pak") NoorehudaFont else AmiriFont,
                    fontSize = 28.sp,
                    color = palette.arabic,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${info.name} · ${quranRevelationType(info.revelationType, languageCode)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { playAyah(targetAyahNumber ?: 1) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(quranUiText("listen", languageCode))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = quranUiText("translations", languageCode),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.text
            )
            FilterChip(
                selected = showEnglish,
                onClick = { onShowEnglishChange(!showEnglish) },
                label = { Text("English") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = palette.surface,
                    labelColor = palette.text,
                    selectedContainerColor = palette.accent,
                    selectedLabelColor = palette.onAccent
                )
            )
            FilterChip(
                selected = showUrdu,
                onClick = { onShowUrduChange(!showUrdu) },
                label = { Text(quranUiText("urdu", languageCode)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = palette.surface,
                    labelColor = palette.text,
                    selectedContainerColor = palette.accent,
                    selectedLabelColor = palette.onAccent
                )
            )
            FilterChip(
                selected = learningMode == 1,
                onClick = { learningMode = if (learningMode == 1) 0 else 1 },
                label = { Text(quranUiText("word_by_word", languageCode)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = palette.surface,
                    labelColor = palette.text,
                    selectedContainerColor = palette.accent,
                    selectedLabelColor = palette.onAccent
                )
            )
            FilterChip(
                selected = learningMode == 2,
                onClick = { learningMode = if (learningMode == 2) 0 else 2 },
                label = { Text(quranUiText("phrases", languageCode)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = palette.surface,
                    labelColor = palette.text,
                    selectedContainerColor = palette.accent,
                    selectedLabelColor = palette.onAccent
                )
            )
        }
        }

        if (learningMode == 1 && (learningLoading || learningLoadFailed)) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = palette.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (learningLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(quranUiText("word_loading", languageCode), color = palette.secondaryText)
                    } else {
                        Text(
                            quranUiText("word_load_error", languageCode),
                            modifier = Modifier.weight(1f),
                            color = palette.secondaryText,
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = { learningReloadTrigger++ }) {
                            Text(quranUiText("retry", languageCode))
                        }
                    }
                }
            }
        }

        val loadedSurah = surah
        when {
            isLoading -> QuranLoadingState(languageCode, Modifier.weight(1f))
            loadFailed -> QuranErrorState(
                languageCode = languageCode,
                onRetry = { reloadTrigger++ },
                modifier = Modifier.weight(1f)
            )
            loadedSurah != null -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(loadedSurah.verses, key = { it.number }) { ayah ->
                    val reference = QuranProgressStore.quranReferenceKey(info.number, ayah.number)
                    val memorisationStatus = progress.memorisation[reference]
                    QuranAyahCard(
                        surahNumber = info.number,
                        ayah = ayah,
                        languageCode = languageCode,
                        showEnglish = showEnglish,
                        showUrdu = showUrdu,
                        learningMode = learningMode,
                        words = learningSurah?.wordsByAyah?.get(ayah.number),
                        onPlayAyah = { playAyah(ayah.number) },
                        onStudyAyah = { studyAyah = ayah },
                        onPlayWord = playWord,
                        isHighlighted = ayah.number == targetAyahNumber,
                        palette = palette,
                        viewingMode = viewingMode,
                        arabicFontType = arabicFontType,
                        arabicFontScale = progress.arabicFontScale,
                        translationFontScale = progress.translationFontScale,
                        hifzMode = progress.hifzMode,
                        isRevealed = ayah.number in revealedAyahs,
                        memorisationStatus = memorisationStatus,
                        onReveal = {
                            revealedAyahs = if (ayah.number in revealedAyahs) {
                                revealedAyahs - ayah.number
                            } else {
                                revealedAyahs + ayah.number
                            }
                        },
                        onMemorisationStatusChange = { status ->
                            val updatedStatus = if (status == memorisationStatus) null else status
                            QuranProgressStore.setMemorisationStatus(
                                context,
                                info.number,
                                ayah.number,
                                updatedStatus
                            )
                            QuranHifzStore.setMasteryFromLegacy(
                                context,
                                info.number,
                                ayah.number,
                                updatedStatus
                            )
                        }
                    )
                }
            }
        }

        if (audioUrl.isNotBlank() && !isFocusMode) {
            Column(modifier = Modifier.fillMaxWidth().background(palette.background)) {
                Text(
                    text = quranUiText("audio_source", languageCode)
                        .replace("%s", QuranLearningRepository.RECITER_NAME),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondaryText
                )
                com.example.ui.components.AudioPlayerComponent(
                    audioUrl = audioUrl,
                    onPlaybackEnded = {
                        val currentAyah = audioAyahNumber
                        if (currentAyah != null && currentAyah < info.verseCount) {
                            playAyah(currentAyah + 1)
                        } else {
                            audioAyahNumber = null
                            audioUrl = ""
                        }
                    },
                    onProgressUpdate = { _, _ -> },
                    autoPlay = true,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        if (!isFocusMode) {
        Surface(
            color = palette.surface,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onSelectSurah(info.number - 1) },
                    enabled = info.number > 1
                ) {
                    Text(quranUiText("previous", languageCode), maxLines = 1)
                }
                Text(
                    text = "${info.number} / ${QuranAssetLoader.TOTAL_SURAHS}",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.secondaryText
                )
                Button(
                    onClick = { onSelectSurah(info.number + 1) },
                    enabled = info.number < QuranAssetLoader.TOTAL_SURAHS
                ) {
                    Text(quranUiText("next", languageCode), maxLines = 1)
                }
            }
        }
        }
    }

    studyAyah?.let { selectedAyah ->
        val reference = QuranProgressStore.quranReferenceKey(info.number, selectedAyah.number)
        QuranStudyAyahDialog(
            info = info,
            ayah = selectedAyah,
            languageCode = languageCode,
            palette = palette,
            arabicFontType = arabicFontType,
            arabicFontScale = progress.arabicFontScale,
            translationFontScale = progress.translationFontScale,
            words = learningSurah?.wordsByAyah?.get(selectedAyah.number),
            wordsLoading = learningLoading,
            wordsFailed = learningLoadFailed,
            note = studyState.notes[reference].orEmpty(),
            collections = studyState.collections,
            isBookmarked = reference in progress.bookmarks,
            onDismiss = { studyAyah = null },
            onBookmark = {
                QuranProgressStore.toggleBookmark(context, info.number, selectedAyah.number)
            },
            onPlayAyah = { playAyah(selectedAyah.number) },
            onPlayWord = playWord,
            onRetryWords = { learningReloadTrigger++ },
            onSaveNote = { QuranStudyStore.saveNote(context, reference, it) },
            onToggleCollection = { QuranStudyStore.toggleReference(context, it, reference) },
            onNavigateAyah = { target ->
                surah?.verses?.firstOrNull { it.number == target }?.let { targetAyah ->
                    studyAyah = targetAyah
                    onStudyAyahNavigate(target)
                }
            }
        )
    }
}

@Composable
private fun QuranAyahCard(
    surahNumber: Int,
    ayah: QuranAyah,
    languageCode: String,
    showEnglish: Boolean,
    showUrdu: Boolean,
    learningMode: Int,
    words: List<QuranWord>?,
    onPlayAyah: () -> Unit,
    onStudyAyah: () -> Unit,
    onPlayWord: (String) -> Unit,
    isHighlighted: Boolean,
    palette: QuranReadingPalette,
    viewingMode: Int,
    arabicFontType: String,
    arabicFontScale: Float,
    translationFontScale: Float,
    hifzMode: Boolean,
    isRevealed: Boolean,
    memorisationStatus: QuranMemorisationStatus?,
    onReveal: () -> Unit,
    onMemorisationStatusChange: (QuranMemorisationStatus) -> Unit
) {
    val showArabic = viewingMode != 2 || hifzMode
    val showTranslations = viewingMode != 1 && learningMode != 2
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isHighlighted) palette.accent.copy(alpha = 0.16f) else palette.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isHighlighted) palette.accent.copy(alpha = 0.75f) else palette.border
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = palette.accent
                ) {
                    Text(
                        text = "$surahNumber:${ayah.number}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onStudyAyah) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = quranStudyText("study_ayah", languageCode),
                        tint = palette.accent
                    )
                }
                IconButton(onClick = onPlayAyah) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = quranUiText("play_ayah", languageCode),
                        tint = palette.accent
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (hifzMode && !isRevealed) {
                OutlinedButton(
                    onClick = onReveal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(quranToolText("reveal_ayah", languageCode))
                }
            } else {
            SelectionContainer {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (showArabic) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = ayah.arabic,
                                modifier = Modifier.fillMaxWidth(),
                                fontFamily = if (arabicFontType == "Indo-Pak") NoorehudaFont else AmiriFont,
                                fontSize = (26 * arabicFontScale).sp,
                                lineHeight = (43 * arabicFontScale).sp,
                                textAlign = TextAlign.Right,
                                color = palette.arabic
                            )
                        }
                    }
                    if (showArabic && showTranslations && (showEnglish || showUrdu)) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = palette.border
                        )
                    }
                    if (showTranslations && showEnglish) {
                        Text(
                            text = ayah.english,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = (16 * translationFontScale).sp,
                            lineHeight = (25 * translationFontScale).sp,
                            color = palette.secondaryText
                        )
                    }
                    if (showTranslations && showEnglish && showUrdu) {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    if (showTranslations && showUrdu) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = ayah.urdu,
                                modifier = Modifier.fillMaxWidth(),
                                fontFamily = NotoNastaliqUrduFont,
                                fontSize = (20 * translationFontScale).sp,
                                lineHeight = (35 * translationFontScale).sp,
                                textAlign = TextAlign.Right,
                                color = palette.secondaryText
                            )
                        }
                    }
                }
            }
            if (learningMode == 1 && !words.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                QuranWordByWordSection(
                    words = words.orEmpty(),
                    languageCode = languageCode,
                    palette = palette,
                    onPlayWord = onPlayWord
                )
            } else if (learningMode == 2) {
                Spacer(modifier = Modifier.height(14.dp))
                QuranPhraseSection(
                    ayah = ayah,
                    languageCode = languageCode,
                    showEnglish = showEnglish,
                    showUrdu = showUrdu,
                    palette = palette,
                    translationFontScale = translationFontScale
                )
            }
            }
            if (hifzMode) {
                if (isRevealed) {
                    TextButton(onClick = onReveal, modifier = Modifier.align(Alignment.End)) {
                        Text(quranToolText("hide_ayah", languageCode))
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = palette.border
                )
                Text(
                    quranToolText("revision_status", languageCode),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.accent
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuranMemorisationStatus.entries.forEach { status ->
                        FilterChip(
                            selected = memorisationStatus == status,
                            onClick = { onMemorisationStatusChange(status) },
                            label = {
                                Text(
                                    quranToolText(
                                        when (status) {
                                            QuranMemorisationStatus.NEEDS_PRACTICE -> "needs_practice"
                                            QuranMemorisationStatus.ALMOST_MEMORISED -> "almost_memorised"
                                            QuranMemorisationStatus.MEMORISED -> "memorised"
                                        },
                                        languageCode
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = palette.surface,
                                labelColor = palette.text,
                                selectedContainerColor = palette.accent,
                                selectedLabelColor = palette.onAccent
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun QuranWordByWordSection(
    words: List<QuranWord>,
    languageCode: String,
    palette: QuranReadingPalette,
    onPlayWord: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            quranUiText("word_by_word", languageCode),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.accent
        )
        Text(
            quranUiText("word_source", languageCode),
            style = MaterialTheme.typography.labelSmall,
            color = palette.secondaryText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            words.forEach { word ->
                Surface(
                    modifier = Modifier
                        .width(138.dp)
                        .clickable(enabled = !word.audioUrl.isNullOrBlank()) {
                            word.audioUrl?.let(onPlayWord)
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = palette.background,
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = word.arabic,
                            fontFamily = NoorehudaFont,
                            fontSize = 23.sp,
                            lineHeight = 32.sp,
                            textAlign = TextAlign.Center,
                            color = palette.arabic,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (word.transliteration.isNotBlank()) {
                            Text(
                                text = word.transliteration,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryText,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (word.translation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = word.translation,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.text,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!word.audioUrl.isNullOrBlank()) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = quranUiText("play_word", languageCode),
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuranPhraseSection(
    ayah: QuranAyah,
    languageCode: String,
    showEnglish: Boolean,
    showUrdu: Boolean,
    palette: QuranReadingPalette,
    translationFontScale: Float
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            quranUiText("phrases", languageCode),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.accent
        )
        Text(
            quranUiText("phrase_note", languageCode),
            style = MaterialTheme.typography.labelSmall,
            color = palette.secondaryText
        )
        if (showEnglish || !showUrdu) {
            QuranPhraseList(
                title = "Ali Quli Qarai · English",
                phrases = quranPhraseSegments(ayah.english),
                palette = palette,
                fontScale = translationFontScale,
                isUrdu = false
            )
        }
        if (showUrdu) {
            QuranPhraseList(
                title = "Syed Zeeshan Haider Jawadi · اردو",
                phrases = quranPhraseSegments(ayah.urdu),
                palette = palette,
                fontScale = translationFontScale,
                isUrdu = true
            )
        }
    }
}

@Composable
private fun QuranPhraseList(
    title: String,
    phrases: List<String>,
    palette: QuranReadingPalette,
    fontScale: Float,
    isUrdu: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = palette.secondaryText,
            fontWeight = FontWeight.SemiBold
        )
        phrases.forEachIndexed { index, phrase ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.background,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        "${index + 1}.",
                        color = palette.accent,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    if (isUrdu) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                phrase,
                                modifier = Modifier.weight(1f),
                                fontFamily = NotoNastaliqUrduFont,
                                fontSize = (18 * fontScale).sp,
                                lineHeight = (32 * fontScale).sp,
                                textAlign = TextAlign.Right,
                                color = palette.text
                            )
                        }
                    } else {
                        Text(
                            phrase,
                            modifier = Modifier.weight(1f),
                            fontSize = (15 * fontScale).sp,
                            lineHeight = (24 * fontScale).sp,
                            color = palette.text
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuranAyahJumpDialog(
    info: QuranSurahInfo,
    currentAyahNumber: Int,
    languageCode: String,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit
) {
    var input by rememberSaveable(info.number, currentAyahNumber) {
        mutableStateOf(currentAyahNumber.toString())
    }
    var isInvalid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(quranToolText("jump_ayah", languageCode)) },
        text = {
            Column {
                Text(
                    "${quranSurahName(info, languageCode)} · 1–${info.verseCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it.filter(Char::isDigit).take(4)
                        isInvalid = false
                    },
                    label = { Text(quranToolText("ayah_number", languageCode)) },
                    singleLine = true,
                    isError = isInvalid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (isInvalid) {
                    Text(
                        quranToolText("invalid_ayah", languageCode),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val ayahNumber = input.toIntOrNull()
                if (ayahNumber == null || ayahNumber !in 1..info.verseCount) {
                    isInvalid = true
                } else {
                    onJump(ayahNumber)
                }
            }) {
                Text(quranToolText("go", languageCode))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(quranToolText("cancel", languageCode))
            }
        }
    )
}

@Composable
private fun QuranLoadingState(languageCode: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = quranUiText("loading", languageCode),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuranErrorState(
    languageCode: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = quranUiText("load_error", languageCode),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(onClick = onRetry) {
                Text(quranUiText("retry", languageCode))
            }
        }
    }
}

@Composable
private fun QuranSourcesDialog(
    languageCode: String,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = quranUiText("sources", languageCode),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = quranUiText("source_note", languageCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                QuranSourceLink(
                    title = "Tanzil Quran Text",
                    subtitle = "Arabic · Simple v1.1",
                    onClick = { onOpenUrl("https://tanzil.net/download/") }
                )
                QuranSourceLink(
                    title = "Ali Quli Qarai",
                    subtitle = "English translation · Tanzil",
                    onClick = { onOpenUrl("https://tanzil.net/trans/en.qarai") }
                )
                QuranSourceLink(
                    title = "Syed Zeeshan Haider Jawadi",
                    subtitle = "Urdu translation · Tanzil",
                    onClick = { onOpenUrl("https://tanzil.net/trans/ur.jawadi") }
                )
                QuranSourceLink(
                    title = QuranLearningRepository.SOURCE_NAME,
                    subtitle = "Word meanings, ayah metadata & transliteration · Runtime learning aid",
                    onClick = { onOpenUrl(QuranLearningRepository.SOURCE_URL) }
                )
                QuranSourceLink(
                    title = QuranLearningRepository.RECITER_NAME,
                    subtitle = "Ayah & word recitation · Quran Foundation",
                    onClick = { onOpenUrl(QuranLearningRepository.RECITER_URL) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(quranUiText("close", languageCode))
            }
        }
    )
}

@Composable
private fun QuranSourceLink(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun openQuranSource(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

internal fun updateQuranKeepScreenAwake(window: Window?, enabled: Boolean) {
    if (enabled) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

private fun quranSurahName(info: QuranSurahInfo, languageCode: String): String =
    if (languageCode == "en") info.name else info.nameAr

private fun quranRevelationType(type: String, languageCode: String): String =
    quranUiText(if (type.equals("Meccan", ignoreCase = true)) "meccan" else "medinan", languageCode)

private fun quranUiText(key: String, languageCode: String): String = when (languageCode) {
    "ur" -> when (key) {
        "title" -> "قرآن مجید"
        "subtitle" -> "عربی متن، انگریزی اور اردو ترجمہ"
        "surah" -> "سورۃ"
        "surahs" -> "سورہیں"
        "verses" -> "آیات"
        "offline" -> "مکمل قرآن، آف لائن"
        "search" -> "سورہ کا نام یا نمبر تلاش کریں"
        "clear" -> "تلاش صاف کریں"
        "no_results" -> "آپ کی تلاش سے کوئی سورہ نہیں ملی۔"
        "meccan" -> "مکی"
        "medinan" -> "مدنی"
        "translations" -> "ترجمے"
        "urdu" -> "اردو"
        "previous" -> "پچھلی سورۃ"
        "next" -> "اگلی سورۃ"
        "loading" -> "قرآن مجید لوڈ ہو رہا ہے…"
        "load_error" -> "قرآن کا ڈیٹا لوڈ نہیں ہو سکا۔ دوبارہ کوشش کریں۔"
        "retry" -> "دوبارہ کوشش"
        "sources" -> "ماخذ اور حوالہ"
        "source_note" -> "عربی متن Tanzil Simple v1.1 ہے۔ مترجمین کے نام اور Tanzil کے حوالے محفوظ ہیں۔ کلمات کا ڈیٹا اور آواز Quran Foundation فراہم کرتا ہے۔ ترجمہ عربی قرآن کا بدل نہیں ہے۔"
        "listen" -> "تلاوت سنیں"
        "play_ayah" -> "آیت چلائیں"
        "play_word" -> "لفظ کا تلفظ سنیں"
        "word_by_word" -> "لفظ بہ لفظ"
        "phrases" -> "عبارات"
        "word_loading" -> "اس سورت کے کلمات کا ڈیٹا لوڈ ہو رہا ہے…"
        "word_load_error" -> "پہلی بار کلمات کے ڈیٹا کے لیے انٹرنیٹ درکار ہے۔ پہلے لوڈ شدہ سورتیں آف لائن دستیاب رہتی ہیں۔"
        "word_source" -> "Quran data provided by Quran Foundation. یہ لغوی معاونت ہے؛ اوپر آیت کا ترجمہ Qarai/Jawadi ہی ہے۔"
        "audio_source" -> "قاری: %s · Quran Foundation · پہلے URL سے، پھر کیش سے"
        "phrase_note" -> "یہ مطالعے کے حصے موجودہ ترجمہ اور رموز اوقاف برقرار رکھتے ہیں؛ یہ نیا ترجمہ یا علمی عربی وابستگی نہیں۔"
        "attribution" -> "عربی متن و تراجم: Tanzil Project — ماخذ دیکھیں"
        "back" -> "واپس"
        "close" -> "بند کریں"
        else -> key
    }
    "ar" -> when (key) {
        "title" -> "القرآن الكريم"
        "subtitle" -> "النص العربي مع الترجمتين الإنجليزية والأردية"
        "surah" -> "سورة"
        "surahs" -> "سور"
        "verses" -> "آيات"
        "offline" -> "القرآن الكامل دون اتصال"
        "search" -> "ابحث باسم السورة أو رقمها"
        "clear" -> "مسح البحث"
        "no_results" -> "لا توجد سورة تطابق بحثك."
        "meccan" -> "مكية"
        "medinan" -> "مدنية"
        "translations" -> "الترجمات"
        "urdu" -> "الأردية"
        "previous" -> "السورة السابقة"
        "next" -> "السورة التالية"
        "loading" -> "جارٍ تحميل القرآن…"
        "load_error" -> "تعذر تحميل بيانات القرآن. حاول مرة أخرى."
        "retry" -> "إعادة المحاولة"
        "sources" -> "المصادر والنسب"
        "source_note" -> "النص العربي من Tanzil Simple v1.1 مع أسماء المترجمين وروابط Tanzil. بيانات الكلمات والصوت من Quran Foundation. الترجمة ليست بديلاً عن النص العربي للقرآن."
        "listen" -> "استمع"
        "play_ayah" -> "تشغيل الآية"
        "play_word" -> "تشغيل نطق الكلمة"
        "word_by_word" -> "كلمة بكلمة"
        "phrases" -> "العبارات"
        "word_loading" -> "جارٍ تحميل بيانات كلمات هذه السورة…"
        "word_load_error" -> "يلزم الاتصال بالإنترنت أول مرة لبيانات الكلمات. تبقى السور المحملة سابقاً متاحة دون اتصال."
        "word_source" -> "Quran data provided by Quran Foundation. هذا عون معجمي فقط؛ وتبقى ترجمة الآية أعلاه من Qarai/Jawadi."
        "audio_source" -> "القارئ: %s · Quran Foundation · من الرابط أولاً ثم من ذاكرة التخزين"
        "phrase_note" -> "تحافظ مقاطع القراءة على الترجمة وعلامات الترقيم الحالية؛ وهي ليست ترجمة جديدة أو محاذاة عربية علمية."
        "attribution" -> "النص العربي والترجمات: Tanzil Project — عرض المصادر"
        "back" -> "رجوع"
        "close" -> "إغلاق"
        else -> key
    }
    else -> when (key) {
        "title" -> "Holy Quran"
        "subtitle" -> "Arabic text with English & Urdu translations"
        "surah" -> "Surah"
        "surahs" -> "Surahs"
        "verses" -> "verses"
        "offline" -> "The complete Quran, offline"
        "search" -> "Search Surah name or number"
        "clear" -> "Clear search"
        "no_results" -> "No Surah matches your search."
        "meccan" -> "Meccan"
        "medinan" -> "Medinan"
        "translations" -> "Translations"
        "urdu" -> "Urdu"
        "previous" -> "Previous"
        "next" -> "Next"
        "loading" -> "Loading the Quran…"
        "load_error" -> "Quran data could not be loaded. Please retry."
        "retry" -> "Retry"
        "sources" -> "Sources & attribution"
        "source_note" -> "Arabic text: Tanzil Simple v1.1. Translator names and Tanzil references are retained. Word data and audio are provided by Quran Foundation. A translation is not a substitute for the Arabic Quran."
        "listen" -> "Listen"
        "play_ayah" -> "Play Ayah"
        "play_word" -> "Play word pronunciation"
        "word_by_word" -> "Word by word"
        "phrases" -> "Phrases"
        "word_loading" -> "Loading word data for this Surah…"
        "word_load_error" -> "Word data needs internet the first time. Previously loaded Surahs remain available offline."
        "word_source" -> "Quran data provided by Quran Foundation. Lexical aid only; the ayah translation above remains Qarai/Jawadi."
        "audio_source" -> "Recitation: %s · Quran Foundation · URL-first, cached after playback"
        "phrase_note" -> "Reading segments preserve the existing translation and punctuation; they are not a new translation or scholarly Arabic alignment."
        "attribution" -> "Arabic text & translations: Tanzil Project — View sources"
        "back" -> "Back"
        "close" -> "Close"
        else -> key
    }
}
