package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.accounts.AccountManager
import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.api.ShiaQuote
import com.example.utils.HijriCalendarHelper
import com.example.utils.PrayerTimeCalculator
import com.example.utils.LocalizationUtility
import java.text.SimpleDateFormat
import java.util.*
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import android.content.Context
import com.example.R
import com.example.data.MasoomeenData
import com.example.data.MasoomDetails
import com.example.data.IslamicEventManager
import com.example.data.QuranAssetLoader
import com.example.data.QuranReferenceParser
import com.example.data.QuranSurahInfo

import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PrayerViewModel,
    azimuth: Float,
    modifier: Modifier = Modifier,
    eventType: com.example.data.EventType? = null
) {
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val homeDayContext by viewModel.homeDayContext.collectAsState()
    val homeClock by viewModel.homeClockMillis.collectAsState()
    val dailyQuote by viewModel.dailyQuote.collectAsState()
    val isGeneratingQuote by viewModel.isGeneratingQuote.collectAsState()
    val isGeneratingAyat by viewModel.isGeneratingAyat.collectAsState()
    val locationError by viewModel.locationError.collectAsState()
    val favoriteQuotes by viewModel.favoriteQuotes.collectAsState()
    val arabicUrduFontScale by viewModel.arabicUrduFontScale.collectAsState()
    
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentLang by viewModel.appLanguage.collectAsState()
    var showLocationSelector by remember { mutableStateOf(false) }
    var showDetailedTimings by remember { mutableStateOf(false) }
    var showFavoritesDialog by remember { mutableStateOf(false) }

    val showRateUsPrompt by viewModel.showRateUsPrompt.collectAsState()
    val globalAppFont by viewModel.appFont.collectAsState()
    val baseAppFontFamily = com.example.ui.theme.getAppFontFamily(globalAppFont, currentLang)

    val showGuidanceOnStartup by viewModel.showGuidanceOnStartup.collectAsState()
    val use24HourFormat by viewModel.use24HourFormat.collectAsState()
    val showCalendarInNotificationBar by viewModel.showCalendarInNotificationBar.collectAsState()
    val showCalendarInToolbar by viewModel.showCalendarInToolbar.collectAsState()
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()
    var showedStartupPopup by rememberSaveable { mutableStateOf(false) }
    var showStartupDialog by remember { mutableStateOf(false) }
    var showOnboardingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(hasSeenOnboarding) {
        if (!hasSeenOnboarding) {
            showOnboardingDialog = true
        }
    }

    LaunchedEffect(Unit) {
        val fineGranted = androidx.core.app.ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarseGranted = androidx.core.app.ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            viewModel.requestExactLocation(context)
        }
    }

    LaunchedEffect(locationError) {
        val message = locationError
        if (!message.isNullOrBlank()) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearLocationError()
        }
    }

    LaunchedEffect(showedStartupPopup, showGuidanceOnStartup, showOnboardingDialog) {
        if (!showOnboardingDialog && !showedStartupPopup && showGuidanceOnStartup) {
            showStartupDialog = true
        }
    }
    var pendingNotificationTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val target = pendingNotificationTarget?.let {
            runCatching { NotificationPermissionTarget.valueOf(it) }.getOrNull()
        }
        pendingNotificationTarget = null
        if (target != null) {
            NotificationPermissionPolicy.handleResult(
                target = target,
                isGranted = isGranted,
                enableAdhan = { viewModel.setAdhanEnabled(true, context) },
                enableAamal = { viewModel.setAamalNotifEnabled(true, context) },
                enableCalendar = { viewModel.setShowCalendarInNotificationBar(true) }
            )
            if (!isGranted && target == NotificationPermissionTarget.CALENDAR) {
                val message = when (currentLang) {
                    "ur" -> "کیلنڈر نوٹیفیکیشن کی اجازت نہیں ملی۔ اینڈرائیڈ سیٹنگز سے اجازت دیں، پھر یہ آپشن دوبارہ کھولیں۔"
                    "ar" -> "لم يُسمح بإشعار التقويم. فعّل الإذن من إعدادات أندرويد ثم أعد تشغيل هذا الخيار."
                    else -> "Calendar notification permission was not granted. Enable it in Android Settings, then turn this option on again."
                }
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    val onNotificationPermissionRequested: (NotificationPermissionTarget) -> Unit = { target ->
        pendingNotificationTarget = target.name
        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    val onboardLocationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            viewModel.requestExactLocation(context)
        }
    }
    
    val scope = rememberCoroutineScope()
    
    var customCity by remember { mutableStateOf("") }
    var customLat by remember { mutableStateOf("") }
    var customLon by remember { mutableStateOf("") }
    var customTz by remember { mutableStateOf("") }
    var customZoneId by remember { mutableStateOf("") }
    var customLocError by remember { mutableStateOf("") }

    // Navigation Overlay system for the 9 beautiful Platform tiles
    var activeOverlay by remember { mutableStateOf<String?>(null) }
    var activeOverlayParam by remember { mutableStateOf<String?>(null) }
    var quranReturnOverlay by remember { mutableStateOf<String?>(null) }
    var quranReturnOverlayParam by remember { mutableStateOf<String?>(null) }
    var activeHomeCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var cardsMakerInitialQuote by remember { mutableStateOf<com.example.api.ShiaQuote?>(null) }
    var selectedTickerItem by remember { mutableStateOf<TickerItem?>(null) }

    fun openQuranRoute(route: String? = null, returnToCurrentOverlay: Boolean = false) {
        quranReturnOverlay = activeOverlay.takeIf { returnToCurrentOverlay && it != "quran" }
        quranReturnOverlayParam = activeOverlayParam.takeIf { quranReturnOverlay != null }
        activeOverlayParam = route
        activeOverlay = "quran"
    }

    fun closeQuranRoute() {
        val returnOverlay = quranReturnOverlay
        val returnParam = quranReturnOverlayParam
        quranReturnOverlay = null
        quranReturnOverlayParam = null
        activeOverlay = returnOverlay
        activeOverlayParam = returnParam.takeIf { returnOverlay != null }
    }

    val openQuranReference: (Int, Int) -> Unit = { surahNumber, ayahNumber ->
        openQuranRoute("$surahNumber:$ayahNumber", returnToCurrentOverlay = true)
    }

    // Handle notification routes on cold start and when onNewIntent delivers a warm-start intent.
    val activity = context as? androidx.activity.ComponentActivity
    var externalIntentTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, activity) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) externalIntentTick += 1
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(activity?.intent, externalIntentTick) {
        val targetScreen = activity?.intent?.getStringExtra("open_screen")
        when (targetScreen) {
            "calendar" -> {
                activeOverlayParam = null
                activeOverlay = "calendar"
            }
            "aamal_tracker" -> {
                activeOverlayParam = null
                activeOverlay = "aamal_tracker"
            }
            "tracker" -> {
                activeOverlayParam = null
                activeOverlay = "tracker"
            }
            "quran_shared_khatam" -> {
                val code = activity?.intent?.getStringExtra("quran_khatam_code")
                if (!code.isNullOrBlank()) {
                    context.getSharedPreferences("quran_shared_khatam", Context.MODE_PRIVATE)
                        .edit().putString("active_code", code.trim().uppercase()).apply()
                }
                quranReturnOverlay = null
                quranReturnOverlayParam = null
                activeOverlayParam = "shared_khatam"
                activeOverlay = "quran"
            }
        }
        if (targetScreen != null) {
            activity?.intent?.removeExtra("open_screen")
            activity?.intent?.removeExtra("quran_khatam_code")
        }
    }

    // Live Media Playback states for Hussainiyah
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var selectedAudioTrack by remember { mutableStateOf("Karbala Shia Adhan") }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    


    // Release MediaPlayer safely
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    val hijriDate = homeDayContext.hijriDate
    val times = homeDayContext.timelines.selected.times

    LaunchedEffect(Unit) {
        viewModel.refreshHomeClock()
        while (true) {
            kotlinx.coroutines.delay(1000L * 30L)
            viewModel.refreshHomeClock()
            HijriCalendarHelper.syncOffset(context)
        }
    }

    // Countdown and active artwork use absolute instants in the selected location.
    val countdownInfo = remember(
        homeDayContext.timelines.current,
        homeDayContext.timelines.tomorrow,
        homeClock,
        currentLang
    ) {
        calculateNextPrayer(
            homeDayContext.timelines.current,
            homeDayContext.timelines.tomorrow,
            homeClock,
            currentLang
        )
    }

    val activeKey = remember(homeDayContext.timelines.current, homeClock) {
        val markers = listOf(
            "imsak" to PrayerTimeCalculator.PrayerEvent.IMSAK,
            "fajr" to PrayerTimeCalculator.PrayerEvent.FAJR,
            "sunrise" to PrayerTimeCalculator.PrayerEvent.SUNRISE,
            "dhuhr" to PrayerTimeCalculator.PrayerEvent.DHUHR,
            "asr" to PrayerTimeCalculator.PrayerEvent.ASR,
            "sunset" to PrayerTimeCalculator.PrayerEvent.SUNSET,
            "maghrib" to PrayerTimeCalculator.PrayerEvent.MAGHRIB,
            "isha" to PrayerTimeCalculator.PrayerEvent.ISHA,
            "midnight" to PrayerTimeCalculator.PrayerEvent.MIDNIGHT
        )
        markers
            .filter { (_, event) -> homeDayContext.timelines.current.instant(event) <= homeClock }
            .maxByOrNull { (_, event) -> homeDayContext.timelines.current.instant(event) }
            ?.first
            ?: "imsak"
    }

    var showTaqeebatDialog by remember { mutableStateOf(false) }

    var backPressTime by remember { mutableLongStateOf(0L) }
    val exitToastMessage = if (currentLang == "ur") "باہر نکلنے کے لیے دوبارہ بیک دبائیں" else "Press back again to exit"

    // Manage standard android physical back press
    BackHandler {
        if (activeOverlay != null) {
            activeOverlay = null
        } else if (activeHomeCategory != null) {
            activeHomeCategory = null
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressTime < 2000) {
                val activity = (context as? android.app.Activity)
                activity?.finish()
            } else {
                backPressTime = currentTime
                android.widget.Toast.makeText(context, exitToastMessage, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Home shows category destinations; their existing features live on dedicated pages.
    val worshipCategoryTitle = if (currentLang == "ur") "عبادات اور روحانیت" else if (currentLang == "ar") "العبادات والروحانيات" else "Worship & Spiritual"
    val worshipCategoryDescription = if (currentLang == "ur") "دعائیں، زیارات اور روحانی کتب تک رسائی حاصل کریں۔" else if (currentLang == "ar") "الوصول إلى الأدعية، الزيارات والأعمال الروحية." else "Access Duas, Ziyarats and spiritual items."
    val calculationsCategoryTitle = if (currentLang == "ur") "حسابات اور قواعد" else if (currentLang == "ar") "الحسابات والأحكام" else "Calculations & Rules"
    val calculationsCategoryDescription = if (currentLang == "ur") "روزمرہ کی زندگی کے لئے اسلامی کیلکولیٹر اور قواعد۔" else if (currentLang == "ar") "الحاسبات الإسلامية والقواعد للحياة اليومية." else "Islamic calculators and rules for daily life."
    val knowledgeCategoryTitle = if (currentLang == "ur") "علم اور حوالہ" else if (currentLang == "ar") "العلم والمراجع" else "Knowledge & Reference"
    val knowledgeCategoryDescription = if (currentLang == "ur") "اسلامی معلومات، کیلنڈر اور ناموں کا مطالعہ کریں۔" else if (currentLang == "ar") "اكتشاف المعلومات الإسلامية، التقويم والأسماء." else "Discover facts, calendars and Islamic knowledge."

    // Localized string for buttons
    val duaBtnTitle = LocalizationUtility.get("tile_dua", currentLang)
    val calculatorBtnTitle = LocalizationUtility.get("tile_calculator", currentLang)
    val qiblaBtnTitle = LocalizationUtility.get("tile_qibla", currentLang)
    val calendarBtnTitle = LocalizationUtility.get("tile_calendar", currentLang)
    val tasbeehBtnTitle = LocalizationUtility.get("tile_tasbeeh", currentLang)
    val trackerBtnTitle = LocalizationUtility.get("tile_tracker", currentLang)
    val aiChatBtnTitle = LocalizationUtility.get("tile_aichat", currentLang)
    val masoomeenBtnTitle = LocalizationUtility.get("tile_masoomeen", currentLang)
    val namesOfAllahBtnTitle = LocalizationUtility.get("tile_names_allah", currentLang)
    val namesOfMuhammadBtnTitle = LocalizationUtility.get("tile_names_muhammad", currentLang)
    val ramadanTrackerBtnTitle = LocalizationUtility.get("tile_ramadan", currentLang)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Fully disable Scaffold's default insets
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Main Noor Screen View
            val isTickerActive by viewModel.isTickerActive.collectAsState()
            val navBarsInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val bottomNavigationSpace = 96.dp + navBarsInset
            val mainBottomPadding = bottomNavigationSpace + if (isTickerActive) 56.dp else 0.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = mainBottomPadding) // Clear the bottom navigation, ticker and system insets
                    .verticalScroll(scrollState)
            ) {
                PremiumHomeHeader(
                    viewModel = viewModel,
                    times = times,
                    countdownInfo = countdownInfo,
                    activeKey = activeKey,
                    currentTick = homeClock,
                    currentLang = currentLang,
                    arabicUrduFontScale = arabicUrduFontScale,
                    baseAppFontFamily = baseAppFontFamily,
                    showDetailedTimings = showDetailedTimings,
                    onDetailedTimingsChange = { showDetailedTimings = it },
                    onLocationClick = { showLocationSelector = true },
                    onProfileClick = { activeOverlay = "profile_settings" },
                    onTaqeebatClick = { showTaqeebatDialog = true },
                    eventType = eventType
                )


                // Home daily content and category destinations share one clean canvas.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(top = 8.dp, bottom = 32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                            // HOME DAILY CONTENT & FEATURE NAVIGATION
                            HijriGregorianCalendarHeader(viewModel = viewModel)

                            Spacer(modifier = Modifier.height(8.dp))

                            // Celestial Lunar Phase & Shia Dates component
                            MoonPhaseTracker(
                                hijriDay = hijriDate.day,
                                hijriMonth = hijriDate.monthName,
                                hijriYear = hijriDate.year,
                                fontScale = arabicUrduFontScale,
                                languageCode = currentLang,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                // Event-aware daily guidance: exact dates outrank general routines.
                var loadedDuasForGuidance by remember { mutableStateOf<List<ShiaDua>>(emptyList()) }
                var loadedAamalForGuidance by remember { mutableStateOf<List<AamalItem>>(emptyList()) }
                var loadedWisdomForGuidance by remember {
                    mutableStateOf<List<com.example.data.MasoomWisdom>>(emptyList())
                }
                var loadedQuranSurahsForGuidance by remember {
                    mutableStateOf<List<QuranSurahInfo>>(emptyList())
                }
                var guidanceDataLoading by remember { mutableStateOf(true) }
                var guidanceReloadToken by remember { mutableStateOf(0) }
                val guidanceZiyarats by viewModel.allZiyarats.collectAsState()
                val guidanceEventsRevision by IslamicEventManager.eventsRevision.collectAsState()
                LaunchedEffect(guidanceReloadToken) {
                    guidanceDataLoading = true
                    try {
                        loadedDuasForGuidance = withContext(Dispatchers.IO) {
                            loadDuas(context)
                        }
                        loadedAamalForGuidance = withContext(Dispatchers.IO) {
                            com.example.ui.admin.AamalSyncManager.loadAamal(context)
                        }
                        loadedWisdomForGuidance = withContext(Dispatchers.IO) {
                            com.example.data.MasoomeenAssetLoader.loadMasoomeenWisdom(context)
                        }
                        loadedQuranSurahsForGuidance = withContext(Dispatchers.IO) {
                            try {
                                QuranAssetLoader.loadIndex(context)
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }
                    } finally {
                        guidanceDataLoading = false
                    }
                }
                val guidanceCalendar = remember(homeClock, homeDayContext.effectiveGregorianDate) {
                    homeDayContext.guidanceCalendar(homeClock)
                }
                val guidanceWeeklyCalendar = remember(homeClock, homeDayContext.selectedDate, homeDayContext.effectiveGregorianDate) {
                    homeDayContext.weeklyGuidanceCalendar(homeClock)
                }
                val guidanceHijriDate = homeDayContext.hijriDate
                val guidanceEvents = remember(
                    guidanceEventsRevision,
                    homeDayContext.locationKey,
                    homeDayContext.countryCode
                ) {
                    IslamicEventManager.getFilteredEvents(context)
                }
                val guidancePlan = remember(
                    guidanceCalendar,
                    guidanceWeeklyCalendar,
                    guidanceHijriDate,
                    guidanceEvents,
                    loadedDuasForGuidance,
                    loadedAamalForGuidance,
                    guidanceZiyarats,
                    loadedWisdomForGuidance,
                    loadedQuranSurahsForGuidance
                ) {
                    HomeGuidanceEngine.build(
                        now = guidanceCalendar,
                        hijriDay = guidanceHijriDate.day,
                        hijriMonth = guidanceHijriDate.month,
                        hijriMonthName = guidanceHijriDate.monthName,
                        events = guidanceEvents,
                        duas = loadedDuasForGuidance,
                        aamal = loadedAamalForGuidance,
                        ziyarats = guidanceZiyarats,
                        wisdom = loadedWisdomForGuidance,
                        quranSurahs = loadedQuranSurahsForGuidance,
                        weeklyNow = guidanceWeeklyCalendar
                    )
                }
                val guidanceContentDayStamp = homeDayContext.dailyContentDayStamp
                LaunchedEffect(
                    guidancePlan.contentTopic,
                    guidancePlan.primaryMasoomIndex,
                    guidanceContentDayStamp
                ) {
                    viewModel.updateDailyContentTopic(
                        guidancePlan.contentTopic,
                        guidancePlan.primaryMasoomIndex,
                        guidanceContentDayStamp
                    )
                }
                HomeGuidanceSection(
                    plan = guidancePlan,
                    currentLang = currentLang,
                    hijriDateLabel = LocalizationUtility.formatHijriDate(guidanceHijriDate, currentLang),
                    fontFamily = baseAppFontFamily,
                    isLoading = guidanceDataLoading,
                    onRetry = { guidanceReloadToken++ },
                    onRecommendationClick = { recommendation ->
                        if (recommendation.route == "quran") {
                            openQuranRoute(recommendation.id)
                        } else {
                            activeOverlayParam = recommendation.id
                            activeOverlay = recommendation.route
                        }
                    },
                    modifier = Modifier.padding(top = 24.dp)
                )
                HomeQuranContinueReading(
                    surahs = loadedQuranSurahsForGuidance,
                    currentLang = currentLang,
                    fontFamily = baseAppFontFamily,
                    onClick = { position ->
                        openQuranRoute("${position.surahNumber}:${position.ayahNumber}")
                    },
                    modifier = Modifier.padding(top = 12.dp)
                )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Aamal-e-Rozana Premium Home Card
                            val aamalCustomAamal by viewModel.allCustomAamal.collectAsState()
                            val aamalTodayCompletedList by viewModel.todayAamalCompletions.collectAsState()
                            val aamalAllCompletions by viewModel.allAamalCompletions.collectAsState()
                            val aamalSharedPrefs = remember { context.getSharedPreferences("aamal_filters", android.content.Context.MODE_PRIVATE) }
                            val visibleCategories = remember(activeOverlay) {
                                AamalCategory.values().associateWith { cat ->
                                    aamalSharedPrefs.getBoolean("cat_${cat.name}", true)
                                }
                            }
                            
                            val aamalDefaultList = remember { getDefaultAamalList() }
                            val homeRequiredIds = remember(
                                homeDayContext.selectedDate,
                                homeDayContext.timelines.current.date,
                                aamalDefaultList,
                                aamalCustomAamal,
                                visibleCategories,
                                aamalAllCompletions
                            ) {
                                AamalProgressPolicy.requiredActivityIds(
                                    date = homeDayContext.selectedDate.key,
                                    isToday = homeDayContext.selectedDate.key == homeDayContext.timelines.current.date.key,
                                    defaultList = aamalDefaultList,
                                    customAamal = aamalCustomAamal,
                                    visibleCategories = visibleCategories,
                                    allCompletions = aamalAllCompletions
                                )
                            }
                            val todayRequiredDefaultList = remember(homeRequiredIds, aamalDefaultList) {
                                aamalDefaultList.filter { it.id in homeRequiredIds }
                            }
                            val todayRequiredCustomList = remember(homeRequiredIds, aamalCustomAamal) {
                                aamalCustomAamal.filter { it.id in homeRequiredIds }
                            }
                            
                            val aamalTotalRequired = homeRequiredIds.size
                            val aamalTotalCompleted = remember(aamalTodayCompletedList, homeRequiredIds) {
                                aamalTodayCompletedList.count { it.isCompleted && it.activityId in homeRequiredIds }
                            }
                            val aamalStreak = remember(
                                aamalAllCompletions,
                                aamalCustomAamal,
                                aamalDefaultList,
                                visibleCategories,
                                homeDayContext.timelines.current.date,
                                homeDayContext.timeZoneId
                            ) {
                                val today = homeDayContext.timelines.current.date.atStartOfDay(
                                    java.util.TimeZone.getTimeZone(homeDayContext.timeZoneId)
                                )
                                AamalProgressPolicy.calculateStreak(
                                    allCompletions = aamalAllCompletions,
                                    customAamal = aamalCustomAamal,
                                    defaultList = aamalDefaultList,
                                    visibleCategories = visibleCategories,
                                    today = today
                                )
                            }

                            val aamalCompletedIds = remember(aamalTodayCompletedList) {
                                aamalTodayCompletedList.filter { it.isCompleted }.map { it.activityId }.toSet()
                            }
                            val nextDefaultAamal = todayRequiredDefaultList.firstOrNull { it.id !in aamalCompletedIds }
                            val nextCustomAamal = todayRequiredCustomList.firstOrNull { it.id !in aamalCompletedIds }
                            val nextAamalTitle = when {
                                nextDefaultAamal != null -> nextDefaultAamal.getTitle(currentLang)
                                nextCustomAamal != null -> nextCustomAamal.title
                                else -> null
                            }
                            HomeAamalProgressCard(
                                total = aamalTotalRequired,
                                completed = aamalTotalCompleted,
                                streak = aamalStreak,
                                nextAamalTitle = nextAamalTitle,
                                currentLang = currentLang,
                                fontFamily = baseAppFontFamily,
                                onClick = { activeOverlay = "aamal_tracker" }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            HomeDailyReflectionHeader(
                                currentLang = currentLang,
                                fontFamily = baseAppFontFamily
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Ayat of the Day Component
                            AyatOfTheDayCard(
                                viewModel = viewModel,
                                fontScale = arabicUrduFontScale,
                                languageCode = currentLang,
                                modifier = Modifier.fillMaxWidth(),
                                onEditCard = { quote ->
                                    cardsMakerInitialQuote = quote
                                    activeOverlay = "cards_maker"
                                },
                                onOpenQuran = { source ->
                                    val reference = QuranReferenceParser.parse(
                                        source,
                                        loadedQuranSurahsForGuidance
                                    )
                                    openQuranRoute(
                                        reference?.let {
                                            it.surahNumber.toString() + (it.ayahNumber?.let { ayah -> ":$ayah" } ?: "")
                                        }
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Hadith of the Day Component
                            HadithOfTheDayCard(
                                viewModel = viewModel,
                                fontScale = arabicUrduFontScale,
                                languageCode = currentLang,
                                modifier = Modifier.fillMaxWidth(),
                                onEditCard = { quote ->
                                    cardsMakerInitialQuote = quote
                                    activeOverlay = "cards_maker"
                                }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Upcoming 14 Masoomeen Events Countdown and Section Component
                            UpcomingMasoomeenEventsSection(
                                viewModel = viewModel,
                                languageCode = currentLang,
                                fontScale = arabicUrduFontScale,
                                modifier = Modifier.fillMaxWidth()
                            )

                    }
                }
            }

            // Bottom News/Events Ticker Bar (sticky above system navigation)
            if (isTickerActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomNavigationSpace)
                ) {
                    NewsTickerBar(
                        viewModel = viewModel,
                        onItemClick = { item ->
                            if (item.id == "CUSTOMIZE_TICKER") {
                                activeOverlay = "ticker_settings"
                            } else {
                                selectedTickerItem = item
                            }
                        }
                    )
                }
            }

            // Ticker Item Detail View Modal
            selectedTickerItem?.let { item ->
                TickerDetailDialog(
                    item = item,
                    languageCode = currentLang,
                    onDismiss = { selectedTickerItem = null },
                    onMuteCategory = {
                        viewModel.toggleTickerMuteCategory(item.category)
                        selectedTickerItem = null
                    },
                    onDelete = {
                        viewModel.deleteTickerItem(item.id)
                        selectedTickerItem = null
                    }
                )
            }

            if (showTaqeebatDialog) {
                TaqeebatDialog(
                    prayerKey = activeKey,
                    currentLang = currentLang,
                    baseAppFontFamily = baseAppFontFamily,
                    arabicUrduFontScale = arabicUrduFontScale,
                    onDismiss = { showTaqeebatDialog = false },
                    onCompleted = {
                        showTaqeebatDialog = false
                    }
                )
            }

            // Occasion celebration or mourning falling animation overlays
            OccasionAnimationOverlay(
                eventType = eventType,
                modifier = Modifier.fillMaxSize()
            )

            // Dedicated category pages: Home stays clean while every feature remains available.
            AnimatedVisibility(
                visible = activeHomeCategory != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
                ) {
                    when (activeHomeCategory) {
                        "worship" -> PremiumHomeCategoryPage(
                            title = worshipCategoryTitle,
                            description = worshipCategoryDescription,
                            icon = Icons.Default.Favorite,
                            accent = MaterialTheme.colorScheme.primary,
                            currentLang = currentLang,
                            baseFontFamily = baseAppFontFamily,
                            onBack = { activeHomeCategory = null },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Worship & Spiritual
                            ResponsiveTileGrid { itemModifier ->
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "اعمال" else if (currentLang == "ar") "الأعمال" else "Aamal",
                                    subtitle = "الأعمال",
                                    description = "Special deeds",
                                    icon = Icons.Default.BookOnline,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "aamal" },
                                    baseFontFamily = baseAppFontFamily,
                                )
                                GridPlatformTile(
                                    title = duaBtnTitle,
                                    subtitle = "الأدعية والزيارات",
                                    description = "Supplications & Ziyarats",
                                    icon = Icons.Default.List,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "duas" },
                                    baseFontFamily = baseAppFontFamily,
                                )
                                GridPlatformTile(
                                    title = qiblaBtnTitle,
                                    subtitle = "القبلة",
                                    description = "Find direction",
                                    icon = Icons.Default.LocationOn,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "compass" },
                                    baseFontFamily = baseAppFontFamily,
                                )
                                GridPlatformTile(
                                    title = trackerBtnTitle,
                                    subtitle = "الأعمال",
                                    description = "Daily tracking",
                                    icon = Icons.Default.Check,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "tracker" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = ramadanTrackerBtnTitle,
                                    subtitle = "رمضان",
                                    description = "Fasting tracker",
                                    icon = Icons.Default.Star,
                                    modifier = itemModifier.testTag("ramadan_tracker_tile"),
                                    onClick = { activeOverlay = "ramadan_tracker" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                if (FirebaseAuth.getInstance().currentUser?.email == "syedmohammadaliraza572@gmail.com") {
                                    GridPlatformTile(
                                        title = if (currentLang == "ur") "شیعہ اکیڈمی" else if (currentLang == "ar") "أكاديمية الشيعة" else "Shia Academy",
                                        subtitle = if (currentLang == "ur") "بچوں کے لیے" else if (currentLang == "ar") "للأطفال" else "For children",
                                        description = if (currentLang == "ur") "تعلیمی سرگرمیاں" else if (currentLang == "ar") "أنشطة تعليمية" else "Kids Learning",
                                        icon = Icons.Default.Face,
                                        modifier = itemModifier.testTag("academy_tile"),
                                        onClick = { activeOverlay = "academy" },
                                        baseFontFamily = baseAppFontFamily
                                    )
                                }
                                GridPlatformTile(
                                    title = tasbeehBtnTitle,
                                    subtitle = "التسبيح",
                                    description = "Digital counter",
                                    icon = Icons.Default.Refresh,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "tasbeeh" },
                                    baseFontFamily = baseAppFontFamily,
                                )
                                GridPlatformTile(
                                    title = aiChatBtnTitle,
                                    subtitle = "الذكاء الاصطناعي",
                                    description = "Ask questions",
                                    icon = Icons.Default.Star,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "aichat" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "محفوظ شدہ" else "Saved Items",
                                    subtitle = "المحفوظات",
                                    description = "Bookmarks",
                                    icon = Icons.Default.FavoriteBorder,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "saved_bookmarks" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "میرا سفر" else if (currentLang == "ar") "رحلتي" else "My Journey",
                                    subtitle = "رحلتي",
                                    description = "Stats & Highlights",
                                    icon = Icons.Default.Timeline,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "journey" },
                                    baseFontFamily = baseAppFontFamily
                                )
                            }
                        }
                        "calculations" -> PremiumHomeCategoryPage(
                            title = calculationsCategoryTitle,
                            description = calculationsCategoryDescription,
                            icon = Icons.Default.Calculate,
                            accent = MaterialTheme.colorScheme.tertiary,
                            currentLang = currentLang,
                            baseFontFamily = baseAppFontFamily,
                            onBack = { activeHomeCategory = null },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Calculations & Rules
                            ResponsiveTileGrid { itemModifier ->
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "زکوٰۃ" else if (currentLang == "ar") "زكاة" else "Zakat",
                                    subtitle = "الزكاة",
                                    description = "Wealth calculation",
                                    icon = Icons.Default.Payments,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "calc_zakat" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "خمس" else if (currentLang == "ar") "خمس" else "Khums",
                                    subtitle = "الخمس",
                                    description = "Islamic tax",
                                    icon = Icons.Default.AccountBalanceWallet,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "calc_khums" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "فطرانہ" else if (currentLang == "ar") "فطرة" else "Fitrana",
                                    subtitle = "الفطرة",
                                    description = "Eid charity",
                                    icon = Icons.Default.Calculate,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "calc_fitrana" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "مسافر" else if (currentLang == "ar") "مسافر" else "Musafir",
                                    subtitle = "المسافر",
                                    description = "Traveler rules",
                                    icon = Icons.Default.AirplanemodeActive,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "calc_musafir" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "میراث" else if (currentLang == "ar") "ميراث" else "Inheritance",
                                    subtitle = "الميراث",
                                    description = "Estate distribution",
                                    icon = Icons.Default.FamilyRestroom,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "calc_inheritance" },
                                    baseFontFamily = baseAppFontFamily
                                )

                                GridPlatformTile(
                                    title = if (currentLang == "ur") "تاریخ کنورٹر" else if (currentLang == "ar") "محول التاريخ" else "Date Converter",
                                    subtitle = "محول التاريخ",
                                    description = "Hijri / Greg",
                                    icon = Icons.Default.DateRange,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "calc_date" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "عشر" else "Ushar",
                                    subtitle = "العشر",
                                    description = "Agricultural tax",
                                    icon = Icons.Default.Grass,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "calc_ushar" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "مہرِ فاطمیؑ" else "Mehr-e-Fatimi",
                                    subtitle = "مهر فاطمي",
                                    description = "Dowry calc",
                                    icon = Icons.Default.BrightnessHigh,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "calc_mehr" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "صدقہ ٹریکر" else "Sadaqah Tracker",
                                    subtitle = "الصدقة",
                                    description = "Track daily Sadaqah",
                                    icon = Icons.Default.VolunteerActivism,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "sadaqah_tracker" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "فنانس کی تاریخ" else "Finance History",
                                    subtitle = "التاريخ المالي",
                                    description = "Log your finances",
                                    icon = Icons.Default.HistoryEdu,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "finance_history" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "فنانس کی ترتیبات" else "Finance Settings",
                                    subtitle = "إعدادات",
                                    description = "Currency & Marja",
                                    icon = Icons.Default.Settings,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "finance_settings" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "قربانی کیلکولیٹر" else "Qurbani Calc",
                                    subtitle = "الأضحية",
                                    description = "Calculate shares",
                                    icon = Icons.Default.Pets,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "qurbani_calculator" },
                                    baseFontFamily = baseAppFontFamily
                                )
                            }
                        }
                        "knowledge" -> PremiumHomeCategoryPage(
                            title = knowledgeCategoryTitle,
                            description = knowledgeCategoryDescription,
                            icon = Icons.Default.Book,
                            accent = MaterialTheme.colorScheme.secondary,
                            currentLang = currentLang,
                            baseFontFamily = baseAppFontFamily,
                            onBack = { activeHomeCategory = null },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Knowledge & Reference
                            ResponsiveTileGrid { itemModifier ->
                                GridPlatformTile(
                                    title = LocalizationUtility.get("quran", currentLang),
                                    subtitle = "القرآن الكريم",
                                    description = when (currentLang) {
                                        "ur" -> "مکمل 114 سورہیں، آف لائن"
                                        "ar" -> "114 سورة كاملة دون اتصال"
                                        else -> "Complete 114 Surahs, offline"
                                    },
                                    icon = Icons.Default.MenuBook,
                                    modifier = itemModifier,
                                    onClick = { openQuranRoute() },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = masoomeenBtnTitle,
                                    subtitle = "المعصومين",
                                    description = "Holy infallibles",
                                    icon = Icons.Default.Star,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "masoomeen" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = calendarBtnTitle,
                                    subtitle = "التقويم",
                                    description = "Hijri dates",
                                    icon = Icons.Default.DateRange,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "calendar" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = namesOfAllahBtnTitle,
                                    subtitle = "أسماء الله",
                                    description = "Allah's names",
                                    icon = Icons.Default.Favorite,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "names_of_allah" },
                                    baseFontFamily = baseAppFontFamily,
                                )
                                GridPlatformTile(
                                    title = namesOfMuhammadBtnTitle,
                                    subtitle = "أسماء محمد",
                                    description = "Prophet's names",
                                    icon = Icons.Default.AccountCircle,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "names_of_muhammad" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "کربلا کا نقشہ و تاریخی سفر" else "Karbala Journey Map",
                                    subtitle = "كربلاء",
                                    description = "Historical map",
                                    icon = Icons.Default.Map,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "karbala_map" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "حج و عمرہ" else "Hajj & Umrah",
                                    subtitle = "الحج والعمرة",
                                    description = "Pilgrimage guide",
                                    icon = Icons.Default.EventNote,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "calc_hajj" },
                                    baseFontFamily = baseAppFontFamily
                                )
                                GridPlatformTile(
                                    title = if (currentLang == "ur") "کارڈز میکر" else if (currentLang == "ar") "صانع البطاقات" else "Cards Maker",
                                    subtitle = "صانع البطاقات",
                                    description = "Create images",
                                    icon = Icons.Default.Image,
                                    modifier = itemModifier,
                                    onClick = { activeOverlay = "cards_maker" },
                                    baseFontFamily = baseAppFontFamily
                                )

                            }
                        }
                    }
                }
            }

            if (activeOverlay == null || activeOverlay == "quran") {
                PremiumBottomNavigation(
                    selectedTab = when {
                        activeOverlay == "quran" -> 2
                        activeHomeCategory == "worship" -> 1
                        activeHomeCategory == "calculations" -> 3
                        activeHomeCategory == "knowledge" -> 4
                        else -> 0
                    },
                    onTabSelected = { tab ->
                        if (tab == 2) {
                            openQuranRoute()
                        } else {
                            quranReturnOverlay = null
                            quranReturnOverlayParam = null
                            activeOverlay = null
                            activeOverlayParam = null
                            activeHomeCategory = when (tab) {
                                1 -> "worship"
                                3 -> "calculations"
                                4 -> "knowledge"
                                else -> null
                            }
                        }
                    },
                    currentLang = currentLang,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(2f)
                        .testTag("home_bottom_navigation")
                )
            }

            // High Performance Overlays drawing screens directly above main view with slide animations
            AnimatedVisibility(
                visible = activeOverlay != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
                ) {
                    when (activeOverlay) {
                        "profile_settings" -> ProfileSettingsScreen(
                            viewModel = viewModel,
                            currentLang = currentLang,
                            onBack = { activeOverlay = null },
                            onOpenAdminPanel = { activeOverlay = "admin_panel" },
                            onNotificationPermissionRequested = onNotificationPermissionRequested,
                            modifier = Modifier.fillMaxSize()
                        )
                        "aamal" -> AamalScreen(currentLang = currentLang, onBack = { activeOverlay = null })
                        "aamal_detail" -> AamalScreen(
                            currentLang = currentLang,
                            onBack = { activeOverlay = null; activeOverlayParam = null },
                            initialAamalId = activeOverlayParam
                        )
                        "aamal_tracker" -> AamalTrackerScreen(viewModel = viewModel, onBack = { activeOverlay = null }, modifier = Modifier.fillMaxSize(), onEditCard = { quote -> cardsMakerInitialQuote = quote; activeOverlay = "cards_maker" })
                        "tracker" -> TrackerScreen(viewModel = viewModel, initialTab = 0, onBack = { activeOverlay = null }, modifier = Modifier.fillMaxSize())
                        "compass" -> CompassScreen(viewModel = viewModel, azimuth = azimuth, modifier = Modifier.fillMaxSize(), onBack = { activeOverlay = null })
                        "tasbeeh" -> TasbeehScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize(),
                            onBack = { activeOverlay = null }
                        )
                        "quran" -> {
                            val quranRouteParts = activeOverlayParam?.split(":").orEmpty()
                            QuranScreen(
                                currentLang = currentLang,
                                onBack = { closeQuranRoute() },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding()
                                    .padding(bottom = 88.dp),
                                initialSurahNumber = quranRouteParts.getOrNull(0)?.toIntOrNull(),
                                initialAyahNumber = quranRouteParts.getOrNull(1)?.toIntOrNull(),
                                initialOpenSharedKhatam = activeOverlayParam == "shared_khatam"
                            )
                        }
                        "calendar" -> CalendarScreen(viewModel = viewModel, onBack = { activeOverlay = null })
                        "duas" -> DuasScreen(
                            viewModel = viewModel, 
                            onBack = { activeOverlay = null; activeOverlayParam = null },
                            onOpenZiyaratsMap = {
                                activeOverlay = "ziyarats_map"
                            },
                            onEditCard = { quote -> cardsMakerInitialQuote = quote; activeOverlay = "cards_maker" },
                            initialDuaId = activeOverlayParam
                        )
                        "calc_khums" -> KhumsScreen(viewModel = viewModel, initialTool = "khums", onBack = { activeOverlay = null }, onOpenQuran = openQuranReference)
                        "calc_zakat" -> KhumsScreen(viewModel = viewModel, initialTool = "zakat", onBack = { activeOverlay = null }, onOpenQuran = openQuranReference)
                        "calc_fitrana" -> KhumsScreen(viewModel = viewModel, initialTool = "fitrana", onBack = { activeOverlay = null }, onOpenQuran = openQuranReference)
                        "calc_ushar" -> KhumsScreen(viewModel = viewModel, initialTool = "ushar", onBack = { activeOverlay = null }, onOpenQuran = openQuranReference)
                        "calc_mehr" -> KhumsScreen(viewModel = viewModel, initialTool = "mehr", onBack = { activeOverlay = null }, onOpenQuran = openQuranReference)
                        "sadaqah_tracker" -> KhumsScreen(viewModel = viewModel, initialTool = "sadaqah", onBack = { activeOverlay = null }, onOpenQuran = openQuranReference)
                        "finance_history" -> KhumsScreen(viewModel = viewModel, initialTool = "donations", onBack = { activeOverlay = null }, onOpenQuran = openQuranReference)
                        "finance_settings" -> KhumsScreen(viewModel = viewModel, initialTool = "finance_settings", onBack = { activeOverlay = null }, onOpenQuran = openQuranReference)
                        "qurbani_calculator" -> KhumsScreen(viewModel = viewModel, initialTool = "qurbani", onBack = { activeOverlay = null }, onOpenQuran = openQuranReference)
                        "calc_hajj" -> KhumsScreen(viewModel = viewModel, initialTool = "hajj_umrah", onBack = { activeOverlay = null }, onOpenQuran = openQuranReference)
                        "calc_qadha" -> TrackerScreen(viewModel = viewModel, initialTab = 1, onBack = { activeOverlay = null }, modifier = Modifier.fillMaxSize())
                        "karbala_map" -> KarbalaMapScreen(currentLang = currentLang, onBack = { activeOverlay = null })
                        "calc_musafir" -> KhumsScreen(viewModel = viewModel, initialTool = "musafir", onBack = { activeOverlay = null }, onOpenQuran = openQuranReference)
                        "calc_inheritance" -> KhumsScreen(viewModel = viewModel, initialTool = "inheritance", onBack = { activeOverlay = null }, onOpenQuran = openQuranReference)
                        "calc_date" -> KhumsScreen(viewModel = viewModel, initialTool = "date_conv", onBack = { activeOverlay = null }, onOpenQuran = openQuranReference)
                        "journey" -> JourneyScreen(viewModel = viewModel, onBack = { activeOverlay = null }, modifier = Modifier.fillMaxSize())
                        "khums" -> KhumsScreen(viewModel = viewModel, onBack = { activeOverlay = null }, onOpenQuran = openQuranReference)
                        "ramadan_tracker" -> RamadanTrackerScreen(viewModel = viewModel, onBack = { activeOverlay = null }, modifier = Modifier.fillMaxSize(), onEditCard = { quote -> cardsMakerInitialQuote = quote; activeOverlay = "cards_maker" })
                        "cards_maker" -> CardsMakerScreen(
                            initialQuote = cardsMakerInitialQuote,
                            onBack = {
                                activeOverlay = null
                                cardsMakerInitialQuote = null
                            },
                            currentLang = currentLang
                        )
                        "books" -> BooksScreen(
                            onBack = { activeOverlay = null },
                            modifier = Modifier.fillMaxSize(),
                            onEditCard = { quote ->
                                cardsMakerInitialQuote = quote
                                activeOverlay = "cards_maker"
                            },
                            currentLang = currentLang
                        )
                        "aichat" -> AIChatScreen(
                            onBack = { activeOverlay = null },
                            modifier = Modifier.fillMaxSize(),
                            languageCode = currentLang
                        )
                        "masoomeen" -> MasoomeenScreen(
                            onBack = { activeOverlay = null; activeOverlayParam = null },
                            languageCode = currentLang,
                            initialSelectedMasoomId = activeOverlayParam,
                            modifier = Modifier.fillMaxSize(),
                            onOpenQuran = openQuranReference
                        )
                        "ziyarats" -> ZiyaratsScreen(
                            viewModel = viewModel,
                            onBack = { activeOverlay = null; activeOverlayParam = null },
                            languageCode = currentLang,
                            initialSelectedZiyaratId = activeOverlayParam,
                            modifier = Modifier.fillMaxSize(),
                            onEditCard = { quote ->
                                cardsMakerInitialQuote = quote
                                activeOverlayParam = null
                                activeOverlay = "cards_maker"
                            },
                            onOpenZiyaratsMap = {
                                activeOverlayParam = null
                                activeOverlay = "ziyarats_map"
                            }
                        )
                        "ziyarats_map" -> ZiyaratsMapScreen(
                            viewModel = viewModel,
                            onBack = { activeOverlay = null },
                            languageCode = currentLang,
                            modifier = Modifier.fillMaxSize()
                        )
                        "names_of_allah" -> NamesOfAllahScreen(
                            onBack = { activeOverlay = null },
                            languageCode = currentLang,
                            modifier = Modifier.fillMaxSize(),
                            onEditCard = { quote -> cardsMakerInitialQuote = quote; activeOverlay = "cards_maker" },
                            onOpenQuran = openQuranReference
                        )
                        "names_of_muhammad" -> NamesOfMuhammadScreen(
                            onBack = { activeOverlay = null },
                            languageCode = currentLang,
                            modifier = Modifier.fillMaxSize(),
                            onEditCard = { quote -> cardsMakerInitialQuote = quote; activeOverlay = "cards_maker" },
                            onOpenQuran = openQuranReference
                        )
                        "saved_bookmarks" -> SavedBookmarksScreen(viewModel = viewModel, onBack = { activeOverlay = null }, languageCode = currentLang, modifier = Modifier.fillMaxSize(), onEditCard = { quote -> cardsMakerInitialQuote = quote; activeOverlay = "cards_maker" })
                        "ticker_settings" -> TickerSettingsOverlay(viewModel = viewModel, languageCode = currentLang, onDismiss = { activeOverlay = null })
                        "admin_panel" -> com.example.ui.admin.AdminPanelScreen(onBack = { activeOverlay = null })
                        "academy" -> com.example.ui.academy.AcademyDashboardScreen(
                            onBack = { activeOverlay = null },
                            currentLang = currentLang,
                            appFontFamily = baseAppFontFamily
                        )

                        "hussainiyah" -> {
                            // Immersive Shia Audio stream & Adhan controller
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("SPIRITUAL MEDIA", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = Color(0xFF00BFA5))
                                        IconButton(onClick = { 
                                            try { 
                                                mediaPlayer?.stop() 
                                                mediaPlayer?.release()
                                            } catch(e: Exception) {}
                                            mediaPlayer = null
                                            isAudioPlaying = false
                                            activeOverlay = null 
                                        }) {
                                            Icon(Icons.Default.Close, "Close")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(28.dp))

                                    // Spinnable cassette disk mockup
                                    Box(
                                        modifier = Modifier
                                            .size(200.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(Color(0xFF333333), Color(0xFF111111))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawCircle(Color.Black, radius = size.minDimension / 4)
                                        }
                                        Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Text(
                                        text = selectedAudioTrack,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isAudioPlaying) "Streaming Spiritual Frequencies..." else "Playback Paused",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    LinearProgressIndicator(
                                        progress = { playbackProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = Color(0xFF00BFA5),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(28.dp))

                                    // Track player buttons
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                try { 
                                                    mediaPlayer?.stop() 
                                                    mediaPlayer?.release()
                                                } catch(e: Exception) {}
                                                mediaPlayer = null
                                                selectedAudioTrack = "Ya Hussain Latmiyah"
                                                isAudioPlaying = false
                                                playbackProgress = 0.5f
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowBack, "Prev")
                                        }

                                        Button(
                                            onClick = {
                                                if (isAudioPlaying) {
                                                    try { mediaPlayer?.pause() } catch(e: Exception) {}
                                                    isAudioPlaying = false
                                                } else {
                                                    // Stream Shia Azan calmly
                                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                                        try {
                                                            if (mediaPlayer == null) {
                                                                val mp = MediaPlayer().apply {
                                                                    setDataSource("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
                                                                    setOnPreparedListener { 
                                                                        start()
                                                                        isAudioPlaying = true
                                                                    }
                                                                    setOnCompletionListener {
                                                                        isAudioPlaying = false
                                                                        playbackProgress = 0f
                                                                    }
                                                                }
                                                                mediaPlayer = mp
                                                                mp.prepareAsync()
                                                            } else {
                                                                try { mediaPlayer?.start() } catch (e: Exception) {}
                                                                isAudioPlaying = true
                                                            }
                                                        } catch (e: Exception) {
                                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                                                // Playback mock simulation if connection fails
                                                                isAudioPlaying = true
                                                                playbackProgress = 0.35f
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            shape = CircleShape,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                                            modifier = Modifier.size(72.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isAudioPlaying) Icons.Default.Info else Icons.Default.PlayArrow,
                                                contentDescription = "Play"
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                try { 
                                                    mediaPlayer?.stop() 
                                                    mediaPlayer?.release()
                                                } catch(e: Exception) {}
                                                mediaPlayer = null
                                                selectedAudioTrack = "Dua Kumayl Stream"
                                                isAudioPlaying = false
                                                playbackProgress = 0.8f
                                            },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, "Next")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = "Please note: streaming requires an active internet connection.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Location Selector Dialog
    if (showLocationSelector) {
        Dialog(onDismissRequest = { showLocationSelector = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "SELECT LOCATION",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Preset Locations
                    items(viewModel.locations.size) { idx ->
                        val preset = viewModel.locations[idx]
                        val isSelected = preset.city == selectedLocation.city
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .clickable {
                                    viewModel.setLocation(preset, context)
                                    showLocationSelector = false
                                }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = preset.city,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Lat: ${preset.lat}, Lon: ${preset.lon}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp).fillMaxWidth().clickable {
                                onboardLocationLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                                showLocationSelector = false
                            }, horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "GPS", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Use Device Location", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Text(
                                    text = "Fine or approximate permission is supported. You can also enter coordinates manually.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Custom Location Input
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Custom Coordinates",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = customCity,
                                    onValueChange = { customCity = it },
                                    label = { Text("Display Name (City)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = customLat,
                                        onValueChange = { customLat = it },
                                        label = { Text("Lat (e.g. 32.61)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).height(52.dp),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                                    )
                                    OutlinedTextField(
                                        value = customLon,
                                        onValueChange = { customLon = it },
                                        label = { Text("Lon (e.g. 44.02)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).height(52.dp),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = customTz,
                                    onValueChange = { customTz = it },
                                    label = { Text("Fallback Offset (e.g. 3.0)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = customZoneId,
                                    onValueChange = { customZoneId = it },
                                    label = { Text("IANA Zone ID (optional, e.g. America/Detroit)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                                )

                                if (customLocError.isNotEmpty()) {
                                    Text(
                                        text = customLocError,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        if (customCity.isBlank() || customLat.isBlank() || customLon.isBlank() || customTz.isBlank()) {
                                            customLocError = "All fields are required."
                                            return@Button
                                        }
                                        val lat = customLat.toDoubleOrNull()
                                        val lon = customLon.toDoubleOrNull()
                                        val tz = customTz.toDoubleOrNull()
                                        if (lat == null || lon == null || tz == null) {
                                            customLocError = "Latitude, Longitude and Timezone must be numerical values."
                                            return@Button
                                        }
                                        val zoneId = customZoneId.trim()
                                        if (zoneId.isNotEmpty() && !com.example.utils.LocationTimeZone.isValidId(zoneId)) {
                                            customLocError = "Enter a valid IANA zone ID, or leave it blank to use the fixed offset."
                                            return@Button
                                        }
                                        viewModel.setCustomLocation(
                                            customCity,
                                            lat,
                                            lon,
                                            tz,
                                            context,
                                            zoneId.takeIf { it.isNotEmpty() }
                                        )
                                        showLocationSelector = false
                                        customLocError = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Apply Coordinates")
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showLocationSelector = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // Saved Favorites Dialog
    if (showFavoritesDialog) {
        Dialog(onDismissRequest = { showFavoritesDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "SAVED REFLECTIONS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (favoriteQuotes.isEmpty()) {
                        Text(
                            text = "Your spiritual scrapbook is currently empty. Tap the Heart icon on any quote to preserve it here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .heightIn(max = 340.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(favoriteQuotes.size) { idx ->
                                val fav = favoriteQuotes[idx]
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                if (fav.arabic.isNotEmpty()) {
                                                    Text(
                                                        text = fav.arabic,
                                                        fontFamily = baseAppFontFamily,
                                                        fontSize = 16.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        textAlign = TextAlign.Right,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                }
                                                Text(
                                                    text = "“${fav.english}”",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontStyle = FontStyle.Italic
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "— ${fav.source}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.removeFavoriteQuote(fav)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showFavoritesDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    if (showOnboardingDialog) {
        OnboardingWizard(
            viewModel = viewModel,
            onComplete = {
                viewModel.completeOnboarding()
                showOnboardingDialog = false
            },
            onRequestLocation = {
                onboardLocationLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        )
    }

    if (showStartupDialog) {
        Dialog(onDismissRequest = {
            showStartupDialog = false
            showedStartupPopup = true
        }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 4.dp, vertical = 24.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val showUrdu = currentLang == "ur"

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Spiritual Guidance Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (showUrdu) "رہنمائی اہلبیت (ع)" else if (currentLang == "ar") "إرشاد أهل البيت (ع)" else "Ahlul Bayt (as) Guidance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (showUrdu) "روزانہ آغازِ سفر کے لیے فیضِ علم" else if (currentLang == "ar") "فيض العلم لبداية يومك" else "Pure Wisdom to Begin Your Day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            dailyQuote?.let { q ->
                                if (q.arabic.isNotEmpty()) {
                                    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                                    val arabicTextColor = if (isDark) Color(0xFFF3C654) else Color(0xFF825500)
                                    Text(
                                        text = q.arabic,
                                        fontSize = (16 * arabicUrduFontScale).sp,
                                        fontFamily = baseAppFontFamily,
                                        color = arabicTextColor,
                                        textAlign = TextAlign.Center,
                                        lineHeight = (24 * arabicUrduFontScale).sp
                                    )
                                }

                                if (currentLang != "ar") {
                                    Text(
                                        text = if (showUrdu) {
                                            "“${com.example.api.UrduTranslations.getUrdu(q)}”"
                                        } else {
                                            "“${q.english}”"
                                        },
                                        fontSize = (14 * arabicUrduFontScale).sp,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        lineHeight = (20 * arabicUrduFontScale).sp
                                    )
                                }

                                Text(
                                    text = "— ${q.source}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            } ?: run {
                                if (isGeneratingQuote || isGeneratingAyat) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Text(
                                            text = "Loading verified guidance…",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Guidance is currently unavailable.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    TextButton(onClick = { viewModel.refreshDailyContent() }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                dailyQuote?.let { q ->
                                    val urQuote = com.example.api.UrduTranslations.getUrdu(q)
                                    val shareText = """
                                        ✨ Ahlul Bayt (as) Wisdom ✨
                                        
                                        📖 Arabic: ${q.arabic}
                                        
                                        🇬🇧 English: ${q.english}
                                        
                                        🇵🇰 اردو: $urQuote
                                        
                                        Source: ${q.source}
                                        — via Shia Pulse
                                    """.trimIndent()
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Ahlul Bayt Guidance")
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Guidance Quote via"))
                                }
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("popup_share_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Quote", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                showStartupDialog = false
                                showedStartupPopup = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.2f).testTag("popup_continue_btn")
                        ) {
                            Text("Continue", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showRateUsPrompt) {
        Dialog(onDismissRequest = { viewModel.dismissRateUsPrompt(false) }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .clip(RoundedCornerShape(28.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rate Stars Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Love Shia Pulse?",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Thank you for using our app! We hope your spiritual journey is going well. Could you please take a moment to leave a rating on the Play Store? Your feedback helps us keep the app 100% free, independent, and ad-free.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val uri = Uri.parse("market://details?id=" + context.packageName)
                                    val goToMarket = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                    context.startActivity(goToMarket)
                                } catch (e: Exception) {
                                    try {
                                        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=" + context.packageName)
                                        context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_VIEW, webUri), "Open Link"))
                                    } catch (ex: Exception) {}
                                }
                                viewModel.dismissRateUsPrompt(true)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("rate_now_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Rate Shia Pulse", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.resetLaunchCounter()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("remind_me_later_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Remind Me Later")
                        }

                        TextButton(
                            onClick = {
                                viewModel.dismissRateUsPrompt(true)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dont_show_again_button")
                        ) {
                            Text(
                                "Don't Show Again",
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
        
    }
}

// Custom Grid Platform Composable Tile
@Composable
fun GridPlatformTile(
    title: String,
    subtitle: String = "",
    description: String = "",
    containerColor: Color? = null,
    iconContainerColor: Color? = null,
    iconColor: Color? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    customIconResId: Int? = null,
    baseFontFamily: androidx.compose.ui.text.font.FontFamily? = null
) {
    val isDark = MaterialTheme.colorScheme.surface.let {
        (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
    }
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tileScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(140),
        label = "tile_press"
    )

    val featureAccent = LocalHomeFeatureAccent.current
    val resolvedAccent = featureAccent ?: MaterialTheme.colorScheme.primary
    val cardShape = RoundedCornerShape(20.dp)
    val cardBorder = BorderStroke(
        1.dp,
        if (isPressed) resolvedAccent.copy(alpha = 0.34f)
        else resolvedAccent.copy(alpha = if (isDark) 0.16f else 0.08f)
    )
    val badgeBg = iconContainerColor ?: featureAccent?.copy(
        alpha = if (isDark) 0.18f else 0.12f
    ) ?: MaterialTheme.colorScheme.primaryContainer.copy(
        alpha = if (isDark) 0.72f else 0.9f
    )
    val badgeIconColor = iconColor ?: resolvedAccent

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor ?: MaterialTheme.colorScheme.surface
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .height(112.dp)
            .scale(tileScale)
            .shadow(if (isDark) 0.dp else 2.dp, cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (customIconResId != null) {
                        Image(
                            painter = painterResource(id = customIconResId),
                            contentDescription = title,
                            contentScale = ContentScale.Fit,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(badgeIconColor),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = badgeIconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = resolvedAccent,
                        fontFamily = baseFontFamily ?: FontFamily.Default,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ResponsiveTileGrid(
    modifier: Modifier = Modifier,
    content: @Composable (tileModifier: Modifier) -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val columns = if (screenWidth > 600.dp) 3 else 2
    val spacing = 12.dp
    val padding = 40.dp
    val tileWidth = ((screenWidth - padding - (spacing * (columns - 1))) / columns) - 0.5.dp

    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        content(Modifier.width(tileWidth))
    }
}

data class NextPrayerInfo(
    val nextPrayerName: String,
    val nextPrayerTime: String,
    val countdownText: String
)

internal fun calculateNextPrayer(
    today: PrayerTimeCalculator.PrayerTimeline,
    tomorrow: PrayerTimeCalculator.PrayerTimeline,
    nowMillis: Long,
    languageCode: String = "en"
): NextPrayerInfo {
    val next = PrayerTimeCalculator.findNextEvent(today, tomorrow, nowMillis)
    val eventName = when (next.event) {
        PrayerTimeCalculator.PrayerEvent.FAJR -> "Fajr"
        PrayerTimeCalculator.PrayerEvent.SUNRISE -> "Sunrise"
        PrayerTimeCalculator.PrayerEvent.DHUHR -> "Dhuhr"
        PrayerTimeCalculator.PrayerEvent.ASR -> "Asr"
        PrayerTimeCalculator.PrayerEvent.SUNSET -> "Sunset"
        PrayerTimeCalculator.PrayerEvent.MAGHRIB -> "Maghrib"
        PrayerTimeCalculator.PrayerEvent.ISHA -> "Isha"
        PrayerTimeCalculator.PrayerEvent.MIDNIGHT -> "Midnight"
        PrayerTimeCalculator.PrayerEvent.IMSAK -> "Imsak"
    }
    val displayName = if (next.isTomorrow && next.event == PrayerTimeCalculator.PrayerEvent.FAJR) {
        val fajr = LocalizationUtility.get("fajr", languageCode)
        val tomorrowLabel = LocalizationUtility.get("tomorrow", languageCode)
        "$fajr ($tomorrowLabel)"
    } else {
        eventName
    }
    val diffMinutes = maxOf(0L, (next.instant - nowMillis + 59_999L) / 60_000L)
    val countdownText = String.format(
        Locale.US,
        "%02dh %02dm",
        diffMinutes / 60,
        diffMinutes % 60
    )
    return NextPrayerInfo(
        nextPrayerName = displayName,
        nextPrayerTime = next.localTime,
        countdownText = countdownText
    )
}

@Composable
fun HijriGregorianCalendarHeader(
    viewModel: PrayerViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val homeDayContext by viewModel.homeDayContext.collectAsState()
    val context = LocalContext.current
    val hijriDate = homeDayContext.hijriDate

    val languageCode by viewModel.appLanguage.collectAsState()
    
    // Format Gregorian & Hijri dates properly for current language
    val gregDateStr = remember(selectedDate, languageCode) {
        com.example.utils.LocalizationUtility.formatGregorianDate(selectedDate, languageCode)
    }
    val hijriDateStr = remember(hijriDate, languageCode) {
        com.example.utils.LocalizationUtility.formatHijriDate(hijriDate, languageCode)
    }

    // Synchronized Lunar Calendar Events
    val eventsRevision by com.example.data.IslamicEventManager.eventsRevision.collectAsState()
    val allEvents = remember(
        context,
        eventsRevision,
        homeDayContext.locationKey,
        homeDayContext.countryCode
    ) {
        com.example.data.IslamicEventManager.getFilteredEvents(context)
    }
    
    val todaysEvents = remember(hijriDate, allEvents) {
        allEvents.filter {
            it.occursOn(hijriDate.day, hijriDate.month, hijriDate.monthName)
        }.distinctBy { event ->
            val titleClean = event.title.lowercase()
                .replace(Regex("\\s*\\(.*narration.*\\)", RegexOption.IGNORE_CASE), "")
                .trim()
            "${event.dateStringHijri.lowercase().trim()}_$titleClean"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Date Navigator Container
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .testTag("hijri_gregorian_header_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Stepper Controls & Date display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Day Button
                    IconButton(
                        onClick = { viewModel.changeDate(-1) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                            .testTag("header_prev_day_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Combined Date Display with subtle visual accents
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                // Launch DatePickerDialog to choose custom Gregorian date
                                val year = selectedDate.get(Calendar.YEAR)
                                val month = selectedDate.get(Calendar.MONTH)
                                val day = selectedDate.get(Calendar.DAY_OF_MONTH)
                                android.app.DatePickerDialog(
                                    context,
                                    { _, selYear, selMonth, selDay ->
                                        val newCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, selYear)
                                            set(Calendar.MONTH, selMonth)
                                            set(Calendar.DAY_OF_MONTH, selDay)
                                        }
                                        viewModel.selectDate(newCal)
                                    },
                                    year, month, day
                                ).show()
                            }
                            .padding(horizontal = 8.dp)
                            .testTag("header_date_picker_trigger")
                    ) {
                        // Hijri Date display with beautiful golden text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = hijriDateStr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        // Gregorian Date
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = gregDateStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Next Day Button
                    IconButton(
                        onClick = { viewModel.changeDate(1) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                            .testTag("header_next_day_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next Day",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                }
            }
        }

        // Highlight Synchronized Lunar Events for today!
        AnimatedVisibility(
            visible = todaysEvents.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                todaysEvents.forEach { event ->
                    val isShahadat = event.eventType == com.example.data.EventType.SHAHADAT
                    val bgGradient = if (isShahadat) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF3E2723).copy(alpha = 0.9f), // deep brown
                                Color(0xFF212121).copy(alpha = 0.95f) // charcoal
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0D533A).copy(alpha = 0.9f), // dark emerald
                                Color(0xFF1B4D3E).copy(alpha = 0.95f) // deep forest green
                            )
                        )
                    }

                    val accentColor = if (isShahadat) Color(0xFFFF8A80) else Color(0xFF69F0AE)
                    val iconSymbol = if (isShahadat) "🕯️" else "🎉"

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(accentColor.copy(alpha = 0.4f), Color.Transparent)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("header_lunar_event_${event.title.replace(" ", "_").lowercase()}"),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(bgGradient)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = iconSymbol,
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = event.localizedTitle(languageCode),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Black,
                                            color = accentColor,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(accentColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (isShahadat) "SHAHADAT" else "WILADAT",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp,
                                                color = accentColor,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = event.localizedDescription(languageCode),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(2.dp))
                                    
                                    val formattedHijriCardStr = com.example.utils.LocalizationUtility.translateHijriDateString(event.dateStringHijri, languageCode)
                                    val lunarFooterText = if (languageCode == "ur") "قمری کیلنڈر کی تاریخ: $formattedHijriCardStr" else if (languageCode == "ar") "التاريخ في التقويم الهجري: $formattedHijriCardStr" else "Occurs on ${event.dateStringHijri} in the Lunar Calendar"
                                    Text(
                                        text = lunarFooterText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontStyle = FontStyle.Italic,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
fun CardDateRow(
    languageCode: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val cal = remember { java.util.Calendar.getInstance() }
    
    val gregDateText = remember(languageCode) {
        com.example.utils.LocalizationUtility.formatGregorianDate(cal, languageCode)
    }

    val hijriDateText = remember(languageCode, com.example.utils.HijriCalendarHelper.globalOffsetDays) {
        val hijriDate = com.example.utils.HijriCalendarHelper.convertGregorianToHijri(cal)
        com.example.utils.LocalizationUtility.formatHijriDate(hijriDate, languageCode)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isDark) Color.White.copy(alpha = 0.04f)
                else Color.Black.copy(alpha = 0.02f)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gregorian Date Pill
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isDark) Color(0xFF1E293B)
                    else Color(0xFFF1F5F9)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Event,
                contentDescription = "Gregorian Calendar",
                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = gregDateText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Hijri Date Pill
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isDark) Color(0xFF3A3018)
                    else Color(0xFFFFFBEB)
                )
                .border(
                    width = 0.8.dp,
                    color = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.3f) else Color(0xFFF59E0B).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Hijri Calendar",
                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = hijriDateText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HadithOfTheDayCard(
    viewModel: PrayerViewModel,
    fontScale: Float,
    languageCode: String,
    modifier: Modifier = Modifier,
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {}
) {
    val dailyQuote by viewModel.dailyQuote.collectAsState()
    val isGeneratingQuote by viewModel.isGeneratingQuote.collectAsState()
    val favoriteQuotes by viewModel.favoriteQuotes.collectAsState()
    val appFont by viewModel.appFont.collectAsState()
    val baseAppFontFamily = com.example.ui.theme.getAppFontFamily(appFont, languageCode)
    val context = LocalContext.current

    val showUrdu = languageCode == "ur"

    val themeMode by viewModel.themeMode.collectAsState()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    // Determine lock/favorite status
    val isFavorited = remember(favoriteQuotes, dailyQuote) {
        val dq = dailyQuote
        if (dq != null) {
            favoriteQuotes.any { it.english.trim().lowercase() == dq.english.trim().lowercase() }
        } else {
            false
        }
    }

    var showShareDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = if (isDark) 0.24f else 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .padding(vertical = 4.dp)
            .testTag("hadith_of_the_day_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title with sparkle & Gemini label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = if (showUrdu) "حدیثِ مبارکہ مبارک" else "Hadith of the Day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        fontFamily = if (showUrdu) baseAppFontFamily else FontFamily.Default
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = if (showUrdu) "اہلبیت اطہار (ع) کے مستند اور معتبر ارشادات" else "Verified Shia Islamic traditions from the Prophet (saws) and the Ahlul Bayt (as)",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Hadith Display Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isGeneratingQuote) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (showUrdu) "حدیث مبارکہ لوڈ ہو رہی ہے..." else "Retrieving Shia Hadith from sources...",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val q = dailyQuote
                    if (q != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Arabic Calligraphy
                            if (q.arabic.isNotEmpty()) {
                                Text(
                                    text = q.arabic,
                                    fontSize = (18 * fontScale).sp,
                                    fontFamily = baseAppFontFamily,
                                    color = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = (22 * fontScale).sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("hadith_arabic_text")
                                        .clickable {
                                            onEditCard(
                                                com.example.api.ShiaQuote(
                                                    arabic = q.arabic,
                                                    english = "",
                                                    urdu = "",
                                                    source = q.source
                                                )
                                            )
                                        }
                                )
                            }

                            // Translation (Urdu / English)
                            if (languageCode != "ar") {
                                val displayText = if (showUrdu) {
                                    com.example.api.UrduTranslations.getUrdu(q)
                                } else {
                                    q.english
                                }

                                Text(
                                    text = "“$displayText”",
                                    fontSize = (14 * fontScale).sp,
                                    fontStyle = FontStyle.Italic,
                                    color = if (isDark) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    lineHeight = (18 * fontScale).sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("hadith_translation_text")
                                        .clickable {
                                            onEditCard(
                                                com.example.api.ShiaQuote(
                                                    arabic = "",
                                                    english = if (!showUrdu) q.english else "",
                                                    urdu = if (showUrdu) com.example.api.UrduTranslations.getUrdu(q) else "",
                                                    source = q.source
                                                )
                                            )
                                        }
                                )
                            }

                            // Authenticated Shia source badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = q.source,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("hadith_source_label")
                                )
                            }
                        }
                    } else {
                        // Edge case fallback
                        Text(
                            text = "Please tap below to retrieve a majestic Shia Hadith",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorite Button
                OutlinedButton(
                    onClick = {
                        if (!isGeneratingQuote && dailyQuote != null) {
                            viewModel.toggleFavoriteQuote()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hadith_action_favorite_btn"),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isFavorited) Color(0xFFFF5252) else (if (isDark) Color.White else MaterialTheme.colorScheme.onSurface)
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite Hadith",
                        tint = if (isFavorited) Color(0xFFFF5252) else (if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showUrdu) {
                             if (isFavorited) "محفوظ" else "محفوظ"
                        } else {
                             if (isFavorited) "Saved" else "Save"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // Share Text Button
                OutlinedButton(
                    onClick = {
                        val q = dailyQuote ?: return@OutlinedButton
                        val urQuote = com.example.api.UrduTranslations.getUrdu(q)
                        val shareText = """
                            ✨ Ahlul Bayt (as) Wisdom ✨
                            
                            📖 Arabic: ${q.arabic}
                            
                            🇬🇧 English: ${q.english}
                            
                            🇵🇰 اردو: $urQuote
                            
                            Source: ${q.source}
                            — via Shia Pulse (Guidance Portal)
                        """.trimIndent()
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Ahlul Bayt Hadith of the Day")
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Shia Wisdom via"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hadith_action_share_btn"),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Text",
                        tint = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showUrdu) "شیئر" else "Share",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                
                // Share Card Button
                OutlinedButton(
                    onClick = { showShareDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hadith_action_share_card_btn"),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = "Share Card",
                        tint = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showUrdu) "کارڈ" else "Card",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
    
    val scope = rememberCoroutineScope()
    val quoteToShare = dailyQuote
    if (showShareDialog && quoteToShare != null) {
        AyatImageShareDialog(
            ayat = quoteToShare,
            onDismiss = { showShareDialog = false },
            onShare = { includeArabic, includeEnglish, includeUrdu ->
                com.example.utils.ShareService.shareQuoteCard(
                    context = context,
                    quote = quoteToShare,
                    includeArabic = includeArabic,
                    includeEnglish = includeEnglish,
                    includeUrdu = includeUrdu,
                    fileNamePrefix = "shiapulse_hadith",
                    chooserTitle = "Share Hadith Card",
                    scope = scope
                )
            },
            onEditCard = { includeArabic, includeEnglish, includeUrdu -> 
                onEditCard(
                    com.example.api.ShiaQuote(
                        arabic = if (includeArabic) quoteToShare.arabic else "",
                        english = if (includeEnglish) quoteToShare.english else "",
                        urdu = if (includeUrdu) quoteToShare.urdu else "",
                        source = quoteToShare.source
                    )
                ) 
            }
        )
    }
}

@Composable
fun AyatOfTheDayCard(
    viewModel: PrayerViewModel,
    fontScale: Float,
    languageCode: String,
    modifier: Modifier = Modifier,
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {},
    onOpenQuran: ((String) -> Unit)? = null
) {
    val dailyAyat by viewModel.dailyAyat.collectAsState()
    val isGeneratingAyat by viewModel.isGeneratingAyat.collectAsState()
    val favoriteQuotes by viewModel.favoriteQuotes.collectAsState()
    val appFont by viewModel.appFont.collectAsState()
    val baseAppFontFamily = com.example.ui.theme.getAppFontFamily(appFont, languageCode)
    val context = LocalContext.current

    val showUrdu = languageCode == "ur"
    val openQuranLabel = when (languageCode) {
        "ur" -> "قرآن میں آیت کھولیں"
        "ar" -> "افتح الآية في القرآن"
        "fa" -> "مشاهده آیه در قرآن"
        "hi" -> "क़ुरान में आयत खोलें"
        else -> "Read this ayah in Quran"
    }

    val themeMode by viewModel.themeMode.collectAsState()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    val isFavorited = remember(favoriteQuotes, dailyAyat) {
        val dq = dailyAyat
        if (dq != null) {
            favoriteQuotes.any { it.english.trim().lowercase() == dq.english.trim().lowercase() }
        } else {
            false
        }
    }

    var showShareDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.24f else 0.12f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .padding(vertical = 4.dp)
            .testTag("ayat_of_the_day_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = if (showUrdu) "آیتِ مبارکہ" else "Ayat of the Day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        fontFamily = if (showUrdu) baseAppFontFamily else FontFamily.Default
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (showUrdu) "قرآن مجید کی روشنی، روزانہ کی بنیاد پر" else "Daily illumination from the Holy Quran",
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isGeneratingAyat) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = if (isDark) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (showUrdu) "آیت مبارکہ تلاش ہو رہی ہے..." else "Retrieving Ayah from Quran...",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val q = dailyAyat
                    if (q != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (q.arabic.isNotEmpty()) {
                                Text(
                                    text = q.arabic,
                                    fontSize = (18 * fontScale).sp,
                                    fontFamily = baseAppFontFamily,
                                    color = if (isDark) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = (22 * fontScale).sp,
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable {
                                            onEditCard(
                                                com.example.api.ShiaQuote(
                                                    arabic = q.arabic,
                                                    english = "",
                                                    urdu = "",
                                                    source = q.source
                                                )
                                            )
                                        }
                                )
                            }

                            if (languageCode != "ar") {
                                val displayText = if (showUrdu) {
                                    com.example.api.UrduTranslations.getUrdu(q)
                                } else {
                                    q.english
                                }

                                Text(
                                    text = "“$displayText”",
                                    fontSize = (14 * fontScale).sp,
                                    fontStyle = FontStyle.Italic,
                                    color = if (isDark) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    lineHeight = (18 * fontScale).sp,
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable {
                                            onEditCard(
                                                com.example.api.ShiaQuote(
                                                    arabic = "",
                                                    english = if (!showUrdu) q.english else "",
                                                    urdu = if (showUrdu) com.example.api.UrduTranslations.getUrdu(q) else "",
                                                    source = q.source
                                                )
                                            )
                                        }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = q.source,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (onOpenQuran != null) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onOpenQuran.invoke(q.source) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                        .testTag("open_daily_ayat_in_quran"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = openQuranLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = baseAppFontFamily,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Please tap below to retrieve a beautiful Ayat",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (!isGeneratingAyat && dailyAyat != null) {
                            viewModel.toggleFavoriteAyat()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isFavorited) Color(0xFFFF5252) else (if (isDark) Color.White else MaterialTheme.colorScheme.onSurface)
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite Ayat",
                        tint = if (isFavorited) Color(0xFFFF5252) else (if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showUrdu) {
                             if (isFavorited) "محفوظ" else "محفوظ"
                        } else {
                             if (isFavorited) "Saved" else "Save"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = {
                        val q = dailyAyat ?: return@OutlinedButton
                        val urAyat = com.example.api.UrduTranslations.getUrdu(q)
                        val shareText = """
                            ✨ Ayat of the Day ✨
                            
                            📖 Arabic: ${q.arabic}
                            
                            🇬🇧 English: ${q.english}
                            
                            🇵🇰 اردو: $urAyat
                            
                            Source: ${q.source}
                            — via Shia Pulse (Guidance Portal)
                        """.trimIndent()
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Ayat of the Day")
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Ayat via"))
                    },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Text",
                        tint = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showUrdu) "شیئر" else "Share",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                
                OutlinedButton(
                    onClick = { showShareDialog = true },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = "Share Card",
                        tint = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showUrdu) "کارڈ" else "Card",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
    
    val scope = rememberCoroutineScope()
    val quoteToShare = dailyAyat
    if (showShareDialog && quoteToShare != null) {
        AyatImageShareDialog(
            ayat = quoteToShare,
            onDismiss = { showShareDialog = false },
            onShare = { includeArabic, includeEnglish, includeUrdu ->
                com.example.utils.ShareService.shareQuoteCard(
                    context = context,
                    quote = quoteToShare,
                    includeArabic = includeArabic,
                    includeEnglish = includeEnglish,
                    includeUrdu = includeUrdu,
                    fileNamePrefix = "shiapulse_ayat",
                    chooserTitle = "Share Ayat Card",
                    scope = scope
                )
            },
            onEditCard = { includeArabic, includeEnglish, includeUrdu -> 
                onEditCard(
                    com.example.api.ShiaQuote(
                        arabic = if (includeArabic) quoteToShare.arabic else "",
                        english = if (includeEnglish) quoteToShare.english else "",
                        urdu = if (includeUrdu) quoteToShare.urdu else "",
                        source = quoteToShare.source
                    )
                ) 
            }
        )
    }
}

@Composable
fun AyatImageShareDialog(
    ayat: com.example.api.ShiaQuote,
    onDismiss: () -> Unit,
    onShare: (includeArabic: Boolean, includeEnglish: Boolean, includeUrdu: Boolean) -> Unit,
    onEditCard: (includeArabic: Boolean, includeEnglish: Boolean, includeUrdu: Boolean) -> Unit = {_,_,_ ->}
) {
    var includeArabic by remember { mutableStateOf(true) }
    var includeEnglish by remember { mutableStateOf(true) }
    var includeUrdu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Share as Image",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                Text(
                    text = "Select what to include in the shared image:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(checked = includeArabic, onCheckedChange = { includeArabic = it })
                    Text("Arabic Text")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(checked = includeEnglish, onCheckedChange = { includeEnglish = it })
                    Text("English Translation")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(checked = includeUrdu, onCheckedChange = { includeUrdu = it })
                    Text("Urdu Translation")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = {
                        onEditCard(includeArabic, includeEnglish, includeUrdu)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ColorLens, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit in Cards Maker")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (includeArabic || includeEnglish || includeUrdu) {
                        onShare(includeArabic, includeEnglish, includeUrdu)
                        onDismiss()
                    }
                }
            ) {
                Text("Generate & Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}



@Composable
fun TasbeehDailyChartCard(viewModel: PrayerViewModel, modifier: Modifier = Modifier) {
    val records by viewModel.tasbeehRecords.collectAsState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Daily Tasbeeh History", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No history yet. Start your daily tasbeeh!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val last7 = records.takeLast(7)
                    val maxCount = last7.maxOfOrNull { it.totalCount }?.coerceAtLeast(1) ?: 1
                    
                    last7.forEach { record ->
                        val heightRatio = (record.totalCount.toFloat() / maxCount.toFloat()).coerceIn(0.05f, 1f)
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .fillMaxHeight(heightRatio)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val dayStr = record.dateString.takeLast(5)
                            Text(
                                text = dayStr,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${record.totalCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// Data structures and functions for Upcoming Masoomeen Events Countdown
data class UpcomingMasoomEvent(
    val title: String,
    val titleUrdu: String = "",
    val titleArabic: String = "",
    val dateStringHijri: String,
    val eventType: com.example.data.EventType,
    val description: String,
    val masoomName: String,
    val targetGregorian: Calendar,
    val daysRemaining: Long
) {
    fun localizedTitle(languageCode: String): String = when (languageCode) {
        "ur" -> titleUrdu.ifBlank { title }
        "ar" -> titleArabic.ifBlank { title }
        else -> title
    }
}

fun parseHijriDateString(dateStr: String): Pair<Int, Int>? {
    val clean = dateStr.substringBefore("(").trim()
    val parts = clean.split(" ")
    if (parts.isEmpty()) return null
    val day = parts[0].toIntOrNull() ?: return null
    val monthName = parts.drop(1).joinToString(" ").trim()
    
    // Clean month name further to align with HijriCalendarHelper constants
    val cleanMonthName = monthName.replace("'", "").replace("-", " ").lowercase()
    
    val monthIndex = HijriCalendarHelper.MONTH_NAMES.indexOfFirst {
        val helperName = it.replace("'", "").replace("-", " ").lowercase()
        helperName == cleanMonthName || cleanMonthName.contains(helperName) || helperName.contains(cleanMonthName)
    }
    
    if (monthIndex == -1) return null
    return Pair(day, monthIndex + 1)
}

fun getUpcomingEvents(nowMillis: Long, isPastMaghrib: Boolean = false, context: Context): List<UpcomingMasoomEvent> {
    val timeZone = TimeZone.getDefault()
    val currentCal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
    val extraOffset = if (isPastMaghrib) 1 else 0
    val currentHijri = HijriCalendarHelper.convertGregorianToHijri(currentCal, extraOffset)
    val effectiveDate = PrayerTimeCalculator.PrayerDate.fromCalendar(currentCal).plusDays(extraOffset)
    return getUpcomingEventsForEffectiveDay(
        currentHijri = currentHijri,
        effectiveGregorianDate = effectiveDate,
        timeZoneId = timeZone.id,
        hijriOffset = HijriCalendarHelper.globalOffsetDays,
        context = context
    )
}

fun getUpcomingEvents(
    dayContext: com.example.utils.HomeDayContext,
    context: Context
): List<UpcomingMasoomEvent> = getUpcomingEventsForEffectiveDay(
    currentHijri = dayContext.hijriDate,
    effectiveGregorianDate = dayContext.effectiveGregorianDate,
    timeZoneId = dayContext.timeZoneId,
    hijriOffset = dayContext.hijriOffset,
    context = context
)

private fun getUpcomingEventsForEffectiveDay(
    currentHijri: HijriCalendarHelper.HijriDate,
    effectiveGregorianDate: PrayerTimeCalculator.PrayerDate,
    timeZoneId: String,
    hijriOffset: Int,
    context: Context
): List<UpcomingMasoomEvent> {
    val results = mutableListOf<UpcomingMasoomEvent>()
    val curHYear = currentHijri.year
    val allEvents = com.example.data.IslamicEventManager.getFilteredEvents(context)

    for (event in allEvents) {
        val masoomName = if (event.masoomIndex in 1..14) {
            com.example.data.MasoomeenData.list.find { it.index == event.masoomIndex }?.name
                ?: "Ahlul Bayt (as)"
        } else {
            "Ahlul Bayt (as)"
        }
        val legacyDate = parseHijriDateString(event.dateStringHijri)
        val hDay = event.hijriDay.takeIf { it in 1..30 } ?: legacyDate?.first ?: continue
        val hMonth = event.hijriMonth.takeIf { it in 1..12 } ?: legacyDate?.second ?: continue

        var targetDate = PrayerTimeCalculator.PrayerDate.fromCalendar(
            HijriCalendarHelper.convertHijriToGregorian(
                curHYear,
                hMonth,
                hDay,
                forceOffset = hijriOffset
            )
        )
        if (targetDate.epochDay() < effectiveGregorianDate.epochDay()) {
            targetDate = PrayerTimeCalculator.PrayerDate.fromCalendar(
                HijriCalendarHelper.convertHijriToGregorian(
                    curHYear + 1,
                    hMonth,
                    hDay,
                    forceOffset = hijriOffset
                )
            )
        }
        val daysRemaining = maxOf(0L, targetDate.epochDay() - effectiveGregorianDate.epochDay())
        val targetCalendar = targetDate.atStartOfDay(TimeZone.getTimeZone(timeZoneId))
        results.add(
            UpcomingMasoomEvent(
                title = event.title,
                titleUrdu = event.titleUrdu,
                titleArabic = event.titleArabic,
                dateStringHijri = event.dateStringHijri,
                eventType = event.eventType,
                description = event.description,
                masoomName = masoomName,
                targetGregorian = targetCalendar,
                daysRemaining = daysRemaining
            )
        )
    }
    results.sortBy { it.targetGregorian.timeInMillis }
    return results
}

@Composable
fun UpcomingMasoomeenEventsSection(
    viewModel: PrayerViewModel,
    languageCode: String,
    fontScale: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val eventsRevision by com.example.data.IslamicEventManager.eventsRevision.collectAsState()
    val homeDayContext by viewModel.homeDayContext.collectAsState()
    val upcomingEvents = remember(homeDayContext, context, eventsRevision) {
        getUpcomingEvents(homeDayContext, context)
    }
    
    if (upcomingEvents.isEmpty()) return
    
    val nextEvents = upcomingEvents.filter { it.targetGregorian.timeInMillis == upcomingEvents.first().targetGregorian.timeInMillis }
    val nextEvent = nextEvents.first()
    val combinedTitle = nextEvents.joinToString(" & ") { it.localizedTitle(languageCode) }
    val combinedMasoomName = nextEvents.joinToString(" & ") { com.example.utils.LocalizationUtility.translateMasoomName(it.masoomName, languageCode) }
    
    val daysVisible = nextEvent.daysRemaining
    
    val isUrdu = languageCode == "ur"

    val themeMode by viewModel.themeMode.collectAsState()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    
    Column(modifier = modifier) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFB300))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = com.example.utils.LocalizationUtility.get("masoomeen_remembrance_calendar", languageCode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.5.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Next Big Anniversary Glowing Countdown Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = if (isDark) 0.24f else 0.14f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                // EventType Badge (Birth vs Martyrdom)
                val isBirth = nextEvent.eventType == com.example.data.EventType.WILADAT
                val badgeColor = if (isBirth) Color(0xFF00E676) else Color(0xFFFF5252)
                val badgeText = if (isBirth) {
                    com.example.utils.LocalizationUtility.get("wiladat", languageCode)
                } else {
                    com.example.utils.LocalizationUtility.get("shahadat", languageCode)
                }

                // Top line: Next event badge & type badge
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = com.example.utils.LocalizationUtility.get("next_event", languageCode),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(0.8.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Event Title & Target Masoom Name
                Text(
                    text = combinedTitle,
                    fontSize = (20 * fontScale).sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                val ahlulBaytLabel = com.example.utils.LocalizationUtility.get("ahlul_bayt", languageCode)
                Text(
                    text = "$ahlulBaytLabel: $combinedMasoomName",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Gregorian date conversion notice & Hijri date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    val lunarDateLabelText = com.example.utils.LocalizationUtility.get("lunar_date", languageCode)
                    val formattedHijriDateStr = com.example.utils.LocalizationUtility.translateHijriDateString(nextEvent.dateStringHijri, languageCode)
                    Text(
                        text = "🗓️ $lunarDateLabelText $formattedHijriDateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Format Gregorian Date
                    val gregorianDateStr = com.example.utils.LocalizationUtility.formatGregorianDate(nextEvent.targetGregorian, languageCode)
                    val gregorianLabelText = com.example.utils.LocalizationUtility.get("gregorian_label", languageCode)
                    
                    Text(
                        text = gregorianLabelText + gregorianDateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                
                // Real-time Countdown timer block (Days only)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(if (daysVisible == 0L) 0.8f else 0.5f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.05f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            )
                            .border(
                                width = 0.8.dp,
                                color = if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (daysVisible == 0L) {
                            val todayText = if (languageCode == "ur") "آج" else if (languageCode == "ar") "اليوم" else "Today"
                            Text(
                                text = todayText,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            Text(
                                text = "$daysVisible",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = com.example.utils.LocalizationUtility.get("days_remaining", languageCode),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // Upcoming list on the horizon: Mini upcoming event sliders
        Text(
            text = com.example.utils.LocalizationUtility.get("upcoming_anniversary", languageCode),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val futureSchedule = upcomingEvents.drop(nextEvents.size).take(6)
            items(futureSchedule) { ev ->
                UpcomingEventMiniCard(ev, languageCode, isDark)
            }
        }
    }
}

@Composable
fun UpcomingEventMiniCard(event: UpcomingMasoomEvent, languageCode: String, isDark: Boolean) {
    val isBirth = event.eventType == com.example.data.EventType.WILADAT
    val accentColor = if (isBirth) Color(0xFF00E676) else Color(0xFFFF5252)
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color.White.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        ),
        modifier = Modifier
            .width(170.dp)
            .height(115.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = com.example.utils.LocalizationUtility.translateHijriDateString(event.dateStringHijri, languageCode),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = event.localizedTitle(languageCode),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
            }
            
            Text(
                text = if (event.daysRemaining == 0L) {
                    com.example.utils.LocalizationUtility.get("today", languageCode)
                } else if (event.daysRemaining == 1L) {
                    com.example.utils.LocalizationUtility.get("tomorrow", languageCode)
                } else {
                    com.example.utils.LocalizationUtility.get("in_days", languageCode).replace("%d", event.daysRemaining.toString())
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = if (isDark) accentColor else (if (isBirth) Color(0xFF00796B) else Color(0xFFC62828))
            )
        }
    }
}

// Sparkly celebratory particles or rich dark solemn fading embers depending on the Lunar Hijri occasion
data class CelebrationParticle(
    var x: Float,
    var y: Float,
    var speed: Float,
    var size: Float,
    val color: Color,
    val isStar: Boolean = false,
    var angle: Float = 0f,
    var angleSpeed: Float = 0f,
    var opacity: Float = 1f
)

@Composable
fun OccasionAnimationOverlay(
    eventType: com.example.data.EventType?,
    modifier: Modifier = Modifier
) {
    if (eventType == null) return

    val isWiladat = eventType == com.example.data.EventType.WILADAT
    if (!isWiladat) return
    
    // Track lifecycle to trigger animation on resume
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var resumeCounter by remember { mutableIntStateOf(0) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                resumeCounter++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var isFinished by remember(eventType, resumeCounter) { mutableStateOf(false) }

    if (isFinished) return

    val particles = remember(eventType, resumeCounter) { mutableStateListOf<CelebrationParticle>() }
    var lastTimeNanos by remember(eventType, resumeCounter) { mutableStateOf(0L) }
    var elapsedTotalSeconds by remember(eventType, resumeCounter) { mutableStateOf(0f) }
    var frameTick by remember(eventType, resumeCounter) { mutableIntStateOf(0) } // Force canvas redraw

    LaunchedEffect(eventType, resumeCounter) {
        // Initialize particles ONCE
        particles.clear()
        val count = if (isWiladat) 95 else 16 // More confetti for Wiladat, 16 for dhabay
        for (i in 0 until count) {
            if (isWiladat) {
                // Festive Confetti / Stars
                val r = kotlin.random.Random.nextFloat()
                val color = when {
                    r < 0.35f -> Color(0xFF00E676) // Mint Emerald
                    r < 0.70f -> Color(0xFFFFD600) // Gold
                    r < 0.85f -> Color(0xFF26A69A) // Teal
                    else -> Color(0xFFFFFFFF) // Sparkle
                }
                particles.add(
                    CelebrationParticle(
                        x = kotlin.random.Random.nextFloat(),
                        y = -0.1f - kotlin.random.Random.nextFloat() * 4.5f, // Spread far above for continuous fall over 10 seconds
                        speed = 0.25f + kotlin.random.Random.nextFloat() * 0.35f, // Screen heights per sec
                        size = 18f + kotlin.random.Random.nextFloat() * 14f,
                        color = color,
                        isStar = kotlin.random.Random.nextFloat() < 0.32f,
                        angle = kotlin.random.Random.nextFloat() * 360f,
                        angleSpeed = (kotlin.random.Random.nextFloat() - 0.5f) * 200f,
                        opacity = 1f
                    )
                )
            } else {
                // Shahadat Blood Stains (Khoon k dhabay)
                val r = kotlin.random.Random.nextFloat()
                val color = when {
                    r < 0.4f -> Color(0xFF8B0000) // Dark Red
                    r < 0.7f -> Color(0xFFB22222) // Firebrick
                    else -> Color(0xFF600000) // Deep Maroon
                }
                particles.add(
                    CelebrationParticle(
                        x = 0.05f + kotlin.random.Random.nextFloat() * 0.9f, 
                        y = 0.05f + kotlin.random.Random.nextFloat() * 0.85f,
                        speed = 0.005f + kotlin.random.Random.nextFloat() * 0.02f, // Slower bleed downwards
                        size = 20f + kotlin.random.Random.nextFloat() * 35f,
                        color = color,
                        opacity = 0f // Start hidden, fade in
                    )
                )
            }
        }

        lastTimeNanos = System.nanoTime()
        val totalDuration = if (isWiladat) 10f else 8f // 10 seconds celebration, 8s for dhabay

        while (elapsedTotalSeconds < totalDuration) {
            androidx.compose.runtime.withFrameNanos { frameTimeNanos ->
                val elapsedSeconds = if (lastTimeNanos == 0L) 0f else (frameTimeNanos - lastTimeNanos) / 1_000_000_000f
                lastTimeNanos = frameTimeNanos
                elapsedTotalSeconds += elapsedSeconds

                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    if (isWiladat) {
                        p.y += p.speed * elapsedSeconds
                        p.angle += p.angleSpeed * elapsedSeconds
                        // slowly fade out at the end
                        if (elapsedTotalSeconds > 8.5f) {
                            p.opacity = maxOf(0f, p.opacity - elapsedSeconds * 0.8f)
                        }
                    } else {
                        // blood stain: fade in first 2 seconds, bleed down slightly, fade out after 6 seconds
                        if (elapsedTotalSeconds < 2f) {
                            p.opacity = minOf(0.85f, p.opacity + elapsedSeconds * 0.4f)
                        } else if (elapsedTotalSeconds > 6f) {
                            p.opacity = maxOf(0f, p.opacity - elapsedSeconds * 0.4f)
                        }
                        // extremely slow drip downwards
                        p.y += p.speed * elapsedSeconds
                    }
                }
                frameTick++ // Force recomposition
            }
        }
        isFinished = true
    }

    if (particles.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("occasion_particle_canvas")
    ) {
        val currentTick = frameTick // Read to force redraw
        
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            if (p.opacity <= 0f) return@forEach

            val px = p.x * width
            val py = p.y * height

            val roundedX = px.coerceIn(0f, width)

            if (isWiladat) {
                // Confetti drawing
                val pathAlpha = p.opacity * 0.85f
                
                if (p.isStar) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(roundedX, py - p.size)
                        lineTo(roundedX + p.size / 3.5f, py - p.size / 3.5f)
                        lineTo(roundedX + p.size, py)
                        lineTo(roundedX + p.size / 3.5f, py + p.size / 3.5f)
                        lineTo(roundedX, py + p.size)
                        lineTo(roundedX - p.size / 3.5f, py + p.size / 3.5f)
                        lineTo(roundedX - p.size, py)
                        lineTo(roundedX - p.size / 3.5f, py - p.size / 3.5f)
                        close()
                    }
                    drawPath(path, p.color.copy(alpha = pathAlpha))
                } else {
                    val halfSize = p.size / 2f
                    rotate(degrees = p.angle, pivot = androidx.compose.ui.geometry.Offset(roundedX, py)) {
                        drawRect(
                            color = p.color.copy(alpha = p.opacity * 0.72f),
                            topLeft = androidx.compose.ui.geometry.Offset(roundedX - halfSize, py - halfSize / 1.8f),
                            size = androidx.compose.ui.geometry.Size(p.size, p.size / 1.8f)
                        )
                    }
                }
            } else {
                // Shahadat Blood Stain (Dhabba)
                val dropAlpha = p.opacity
                
                // Main droplet circle
                drawCircle(
                    color = p.color.copy(alpha = dropAlpha),
                    radius = p.size,
                    center = androidx.compose.ui.geometry.Offset(roundedX, py)
                )
                // Droplet tail bleeding upwards
                val tailPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(roundedX - p.size * 0.85f, py)
                    lineTo(roundedX + p.size * 0.85f, py)
                    lineTo(roundedX, py - p.size * 2.8f)
                    close()
                }
                drawPath(tailPath, p.color.copy(alpha = dropAlpha * 0.8f))
                
                // Small adjacent blood splatter circles
                drawCircle(
                    color = p.color.copy(alpha = dropAlpha * 0.7f),
                    radius = p.size * 0.35f,
                    center = androidx.compose.ui.geometry.Offset(roundedX + p.size * 1.6f, py + p.size * 0.6f)
                )
                drawCircle(
                    color = p.color.copy(alpha = dropAlpha * 0.5f),
                    radius = p.size * 0.2f,
                    center = androidx.compose.ui.geometry.Offset(roundedX - p.size * 1.3f, py - p.size * 0.9f)
                )
            }
        }
    }
}

// ==========================================
// TAQEEBAT-E-NAMAZ QUICK ACCESS IMPLEMENTATION
// ==========================================

data class TaqeebatItem(
    val title: String,
    val titleUrdu: String,
    val titleArabic: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val urduTranslation: String
)

class TaqeebatContent {
    companion object {
        fun getTaqeebatFor(prayerKey: String): List<TaqeebatItem> {
            return when (prayerKey) {
                "fajr" -> listOf(
                    TaqeebatItem(
                        title = "Glorifying Allah",
                        titleUrdu = "توحید اور تقدیس الٰہی",
                        titleArabic = "تمجيد الله تعالى",
                        arabic = "لا إِلهَ إِلاَّ اللهُ إِلهَاً واحِداً وَنَحْنُ لَهُ مُسْلِمُونَ، لا إِلهَ إِلاَّ اللهُ لا نَعْبُدُ إِلاَّ إِيَّاهُ مُخْلِصِينَ لَهُ الدِّينَ وَلَوْ كَرِهَ الْمُشْرِكُونَ، لا إِلهَ إِلاَّ اللهُ رَبُّنَا وَرَبُّ آبائِنَا الاَوَّلِينَ",
                        transliteration = "La ilaha illallahu ilahan wahidan wa nahnu lahu muslimun, la ilaha illallahu la na'budu illa iyyahu mukhlisina lahu-d-dina walaw karihal-mushrikun, la ilaha illallahu rabbuna wa rabbu aba'ina-al-awwalin.",
                        translation = "There is no god but Allah, the One and Only God, and we submit to Him. There is no god but Allah; we worship none but Him, making our devotion sincere to Him, even though the polytheists may detest it. There is no god but Allah, our Lord and the Lord of our forefathers.",
                        urduTranslation = "اللہ کے سوا کوئی معبود نہیں، وہ اکیلا معبود ہے اور ہم اسی کے فرمانبردار ہیں۔ اللہ کے سوا کوئی معبود نہیں، ہم بجز اس کے کسی کی عبادت نہیں کرتے، خالص اس کی اطاعت کرتے ہیں اگرچہ مشرکوں کو ناگوار گزرے۔ اللہ کے سوا کوئی معبود نہیں جو ہمارا پروردگار ہے اور ہمارے اگلے باپ دادا کا پروردگار ہے۔"
                    ),
                    TaqeebatItem(
                        title = "Firm Believing and Unity",
                        titleUrdu = "توحیدِ کامل",
                        titleArabic = "التوحيد الخالص",
                        arabic = "لا إِلهَ إِلاَّ اللهُ وَحْدَهُ وَحْدَهُ وَحْدَهُ، أَنْجَزَ وَعْدَهُ، وَنَصَرَ عَبْدَهُ، وَأَعَزَّ جُنْدَهُ، وَهَزَمَ الاحْزابَ وَحْدَهُ، فَلَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، يُحْيِي وَيُمِيتُ، وَيُمِيتُ وَيُحْيِي، وَهُوَ حَيٌّ لا يَمُوتُ، بِيَدِهِ الْخَيْرُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
                        transliteration = "La ilaha illallahu wahdahu wahdahu wahdah, anjaza wa'dah, wa nasara 'abdah, wa a'azza jundahu wa hazamal-ahzaba wahdah, falahul-mulku wa lahul-hamdu yuhyi wa yumit, wa yumitu wa yuhyi, wa huwa hayyun la yamut, biyadihil-khayru wa huwa 'ala kulli shay'in qadir.",
                        translation = "There is no god but Allah, He is unique, unique, unique. He fulfilled His promise, helped His servant, honored His army, and defeated the coalitions by Himself. To Him belongs sovereignty and praise; He gives life and causes death, and He causes death and gives life, and He is Ever-Living and never dies. In His hand is all good, and He has power over all things.",
                        urduTranslation = "اللہ کے سوا کوئی معبود نہیں وہ یکتا ہے، یکتا ہے، یکتا ہے۔ اس نے اپنا وعدہ پورا کیا، اپنے بندے کی مدد فرمائی، اپنے لشکر کو عزت دی اور اکیلے ہی تمام گروہوں کو شکست دی۔ پس اسی کی سلطنت ہے اور اسی کی حمد ہے، وہی جلاتا اور مارتا ہے، مارتا اور جلاتا ہے، وہ زندہ ہے جسے موت نہیں، اسی کے ہاتھ میں تمام بھلائی ہے اور وہ ہر چیز پر قادر ہے۔"
                    )
                )
                "dhuhr" -> listOf(
                    TaqeebatItem(
                        title = "Supplication of Zuhr",
                        titleUrdu = "تعقیبِ نمازِ ظہر",
                        titleArabic = "تعقيب صلاة الظهر",
                        arabic = "لا إِلهَ إِلاَّ اللهُ الْعَظِيمُ الْحَلِيمُ، لا إِلهَ إِلاَّ اللهُ رَبُّ الْعَرْشِ الْكَرِيمِ، الْحَمْدُ للهِ رَبِّ الْعالَمِينَ، اَللّهُمَّ إِنِّي أَسْأَلُكَ مُوجِباتِ رَحْمَتِكَ، وَعَزائِمَ مَغْفِرَتِكَ، وَالْغَنِيمَةَ مِنْ كُلِّ بِرِّ، وَالسَّلامَةَ مِنْ كُلِّ إِثْمٍ",
                        transliteration = "La ilaha illallahul-adhimul-halim, la ilaha illallahu rabbu-l-'arshil-karim, al-hamdu lillahi rabbi-l-'alamin. Allahumma inni as'aluka mujibati rahmatik, wa 'aza'ima maghfiratik, wal-ghanimata min kulli birr, was-salamata min kulli ithm.",
                        translation = "There is no god but Allah, the Great, the Clement. There is no god but Allah, Lord of the noble Throne. Praise be to Allah, Lord of the worlds. O Allah, I ask You for the causes of Your mercy, and the resolves of Your forgiveness, and the benefit of every good deed, and safety from every sin.",
                        urduTranslation = "اللہ کے سوا کوئی معبود نہیں جو عظمت والا اور بردبار ہے، اللہ کے سوا کوئی معبود نہیں جو عرشِ کریم کا رب ہے، حمد و ثنا اللہ ہی کے لیے ہے جو تمام جہانوں کا پالنے والا ہے۔ اے اللہ! میں تجھ سے سوال کرتا ہوں تیری رحمت کے موجبات کا اور تیری بخشش کے پختہ ارادوں کا اور ہر نیکی سے حصہ پانے کا اور ہر گناہ سے سلامتی کا۔"
                    ),
                    TaqeebatItem(
                        title = "Request for Relief",
                        titleUrdu = "حاجات کی پیشی",
                        titleArabic = "طلب الحوائج والفرج",
                        arabic = "اَللّهُمَّ لا تَدَعْ لِي ذَنْباً إِلاَّ غَفَرْتَهُ، وَلا هَمّاً إِلاَّ فَرَّجْتَهُ، وَلا سُقْماً إِلاَّ شَفَيْتَهُ، وَلا عَيْباً إِلاَّ سَتَرْتَهُ، وَلا رِزْقاً إِلاَّ بَسَطْتَهُ، وَلا خَوْفاً إِلاَّ آمَنْتَهُ، وَلا سُوءاً إِلاَّ صَرَفْتَهُ، وَلا حاحَةً هِيَ لَكَ رِضاً وَلِيَ فِيها صَلاحٌ إِلاَّ قَضَيْتَها، يا أَرْحَمَ الرَّاحِمِينَ، آمِينَ رَبَّ الْعالَمِينَ",
                        transliteration = "Allahumma la tada' li dhanban illa ghafartah, wa la hamman illa farrajtah, wa la suqman illa shafaytah, wa la 'ayban illa satartah, wa la rizqan illa basattah, wa la khawfan illa amantah, wa la su'an illa saraftah, wa la hajatan hiya laka ridan wa liya fiha salahun illa qadaytaha, ya arhamar-rahimin, amina rabbal-'alamin.",
                        translation = "O Allah, do not leave any of my sins unforgiven, nor any worry unreleased, nor any illness uncured, nor any defect unhidden, nor any sustenance unextended, nor any fear unsecured, nor any evil undisplaced, nor any need that pleases You and is good for me unfulfilled, O Most Merciful of the merciful, Amen, O Lord of the worlds.",
                        urduTranslation = "اے اللہ! میرے لیے کوئی گناہ نہ چھوڑ مگر اسے بخش دے، اور نہ کوئی غم مگر اسے دور کر دے، اور نہ کوئی بیماری مگر اسے شفا دے، اور نہ کوئی عیب مگر اسے چھپا دے، اور نہ کوئی رزق مگر اسے کشادہ کر دے، اور نہ کوئی خوف مگر اسے امن میں بدل دے، اور نہ کوئی برائی مگر اسے پھیر دے، اور نہ کوئی ایسی حاجت جس میں تیری رضا اور میری بہتری ہو مگر اسے پورا کر دے، اے سب سے زیادہ رحم کرنے والے! آمین یا رب العالمین۔"
                    )
                )
                "asr" -> listOf(
                    TaqeebatItem(
                        title = "Seeking Forgiveness (Istighfar)",
                        titleUrdu = "استغفار اور توبہ",
                        titleArabic = "الاستغفار والتوبة",
                        arabic = "أَسْتَغْفِرُ اللَّهَ الَّذِي لَا إِلَهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ الرَّحْمَنُ الرَّحِيمُ ذُو الْجَلَالِ وَالْإِكْرَامِ، وَأَسْأَلُهُ أَنْ يَتُوبَ عَلَيَّ تَوْبَةَ عَبْدٍ ذَلِيلٍ خَضِعٍ فَقِيرٍ بَائِسٍ مِسْكِينٍ مُسْتَكِينٍ لَا يَمْلِكُ لِنَفْسِهِ نَفْعاً وَلَا ضَرّاً وَلَا مَوْتاً وَلَا حَيَاةً وَلَا نُشُوراً",
                        transliteration = "Astaghfirullaha-lladhi la ilaha illa huwal-hayyul-qayyumur-rahmanur-rahimu dhul-jalali wal-ikram, wa as'aluhu an yatuba 'alayya tawbata 'abdin dhalilin khadi'in faqirin ba'isin miskinin mustakinin la yamliku linafsihi naf'an wa la darran wa la mawtan wa la hayatan wa la nushura.",
                        translation = "I seek forgiveness of Allah, there is no god but He, the Living, the Self-Subsisting, the Beneficent, the Merciful, the Lord of Majesty and Generosity, and I ask Him to accept my repentance, the repentance of a humble, submissive, poor, destitute, miserable, helpless servant, who has no control over benefit, harm, death, life, or resurrection for himself.",
                        urduTranslation = "میں بخشش چاہتا ہوں اللہ سے جس کے سوا کوئی معبود نہیں، وہ ہمیشہ زندہ رہنے والا، خود قائم رہنے والا، بڑا مہربان، نہایت رحم کرنے والا، جلال اور بزرگی کا مالک ہے، اور میں اس سے توبہ کا سوال کرتا ہوں جیسے ایک ذلیل، عاجز, محتاج، پریشان حال بندہ کرتا ہے جو اپنے لیے نہ نفع کا مالک ہے نہ نقصان کا، نہ موت کا، نہ زندگی کا اور نہ دوبارہ جی اٹھنے کا۔"
                    ),
                    TaqeebatItem(
                        title = "Protection from Destitution",
                        titleUrdu = "شر اور فقر سے پناہ",
                        titleArabic = "الاستعاذة من الشر والفقر",
                        arabic = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ نَفْسٍ لَا تَشْبَعُ، وَمِنْ قَلْبٍ لَا يَخْشَعُ، وَمِنْ عِلْمٍ لَا يَنْفَعُ، وَمِنْ صَلَاةٍ لَا تُرْفَعُ، وَمِنْ دُعَاءٍ لَا يُسْمَعُ. اللَّهُمَّ إِنِّي أَسْأَلُكَ الْيُسْرَ بَعْدَ الْعُسْرِ، وَالْفَرَجَ بَعْدَ الْكَرْبِ، وَالرَّخَاءَ بَعْدَ الشِّدَّةِ",
                        transliteration = "Allahumma inni a'udhu bika min nafsin la tashba', wa min qalbin la yakhsha', wa min 'ilmin la yanfa', wa min salatin la turfa', wa min du'a'in la yusma'. Allahumma inni as'alukal-yusra ba'dal-'usr, wal-faraja ba'dal-karb, war-rakha'a ba'dash-shiddah.",
                        translation = "O Allah, I seek refuge in You from a soul that is never satisfied, a heart that does not humble itself, knowledge that does not benefit, a prayer that is not raised, and a supplication that is not heard. O Allah, I ask You for ease after hardship, relief after distress, and prosperity after adversity.",
                        urduTranslation = "اے اللہ! میں تیری پناہ چاہتا ہوں ایسے نفس سے جو سیر نہ ہو، اور ایسے دل سے جس میں خوفِ خدا نہ ہو، اور ایسے علم سے جو فائدہ نہ دے، اور ایسی نماز سے جو قبول نہ کی جائے، اور ایسی دعا سے جو سنی نہ جائے۔ اے اللہ! میں تجھ سے تنگی کے بعد آسانی، مصیبت کے بعد کشادگی اور سختی کے بعد خوشحالی کا سوال کرتا ہوں۔"
                    )
                )
                "maghrib" -> listOf(
                    TaqeebatItem(
                        title = "Blessings on the Holy Prophet",
                        titleUrdu = "درود شریف اور عظمتِ رسولؐ",
                        titleArabic = "الصلاة على النبي وآله",
                        arabic = "إِنَّ اللَّهَ وَمَلَائِكَتَهُ يُصَلُّونَ عَلَى النَّبِيِّ يَا أَيُّهَا الَّذِينَ آمَنُوا صَلُّوا عَلَيْهِ وَسَلِّمُوا تَسْلِيمًا. اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ النَّبِيِّ وَعَلَى ذُرِّيَّتِهِ وَعَلَى أَهْلِ بَيْتِهِ",
                        transliteration = "Innal-laha wa mala'ikatahu yusalluna 'alan-nabi, ya ayyuhalladhina amanu sallu 'alayhi wa sallimu taslima. Allahumma salli 'ala Muhammadin-nabiyyi wa 'ala dhurriyyatihi wa 'ala ahli baytih.",
                        translation = "Indeed Allah and His angels shower blessings on the Prophet; O you who believe, send blessings on him and salute him with a worthy salutation. O Allah, send blessings upon Muhammad the Prophet, and upon his progeny, and upon his Ahl al-Bayt.",
                        urduTranslation = "بیشک اللہ اور اس کے فرشتے نبیؐ پر درود بھیجتے ہیں، اے ایمان والو! تم بھی ان پر درود بھیجو اور سلام بھیجو جیسے سلام بھیجنے کا حق ہے۔ اے اللہ! درود نازل فرما محمدؐ پر جو تیرے نبی ہیں اور ان کی ذریت پر اور ان کے اہل بیت پر۔"
                    ),
                    TaqeebatItem(
                        title = "Supplication of Maghrib",
                        titleUrdu = "تعقیبِ نمازِ مغرب",
                        titleArabic = "تعقيب صلاة المغرب",
                        arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ مُوجِبَاتِ رَحْمَتِكَ، وَعَزَائِمَ مَغْفِرَتِكَ، وَالنَّجَاةَ مِنَ النَّارِ، وَمِنْ كُلِّ بَلِيَّةٍ، وَالْفَوْزَ بِالْجَنَّةِ، وَالرِّضْوَانَ فِي دَارِ السَّلَامِ، وَجِوَارَ نَبِيِّكَ مُحَمَّدٍ عَلَيْهِ وَآلِهِ السَّلَامُ",
                        transliteration = "Allahumma inni as'aluka mujibati rahmatik, wa 'aza'ima maghfiratik, wan-najata minan-nar, wa min kulli baliyyah, wal-fawza bil-jannah, war-ridwana fi daris-salam, wa jiwara nabiyyika Muhammadin 'alayhi wa alihis-salam.",
                        translation = "O Allah, I ask You for the causes of Your mercy, the determination of Your forgiveness, salvation from the Fire and from every calamity, victory in attaining Paradise, and Your pleasure in the abode of peace, and the companionship of Your Prophet Muhammad, peace be upon him and his family.",
                        urduTranslation = "اے اللہ! میں تجھ سے سوال کرتا ہوں تیری رحمت کے اسباب کا، تیری بخشش کے پختہ ارادوں کا، جہنم کی آگ اور ہر مصیبت سے نجات کا، جنت کی کامیابی کا، سلامتی کے گھر (جنت) میں تیری خوشنودی کا اور تیرے نبی حضرت محمد صلی اللہ علیہ وآلہ وسلم کی ہمسائیگی کا۔"
                    )
                )
                "isha" -> listOf(
                    TaqeebatItem(
                        title = "Supplication for Sustenance",
                        titleUrdu = "طلبِ رزق کی دعا",
                        titleArabic = "دعاء طلب الرزق بعد العشاء",
                        arabic = "اللَّهُمَّ إِنَّهُ لَيْسَ لِي عِلْمٌ بِمَوْضِعِ رِزْقِي، وَإِنَّمَا أَطْلُبُهُ بِخَطَرَاتٍ تَخْطُرُ عَلَى قَلْبِي، فَأَجُولُ فِي طَلَبِهِ الْبُلْدَانَ، فَأَنَا فِيمَا أَنَا طَالِبٌ كَالْحَيْرَانِ، لَا أَدْرِي أَفِي سَهْلٍ هو أَمْ فِي جَبَلٍ، أَمْ فِي أَرْضٍ أَمْ فِي سَمَاءٍ، أَمْ فِي بَرٍّ أَمْ فِي بَحْرٍ، وَعَلَى يَدَيْ مَنْ وَمِنْ قِبَلِ مَنْ",
                        transliteration = "Allahumma innahu laysa li 'ilmun bi-mawdi'i rizqi, wa innama atlubuhu bi-khataratin takhturu 'ala qalbi, fa-ajulu fi talabihil-buldan, fa-ana fima ana talibun kal-hayran, la adri afi sahlin huwa am fi jabalin, am fi ardin am fi sama'in, am fi barrin am fi bahrin, wa 'ala yaday man wa min qibali man...",
                        translation = "O Allah, indeed I have no knowledge of the place of my sustenance, and I only seek it through the thoughts that occur to my heart. So I travel through countries in search of it, and I am confused about what I seek, not knowing whether it is in a plain, on a mountain, on the earth, in the sky, on land, or in the sea, or at whose hands and from whom it will come...",
                        urduTranslation = "اے اللہ! بیشک مجھے معلوم نہیں کہ میرا رزق کہاں ہے، میں تو بس اپنے دل میں پیدا ہونے والے خیالات کے تحت اس کی تلاش کرتا ہوں اور شہروں میں مارا مارا پھرتا ہوں، پس میں اپنی طلب میں حیران ہوں، مجھے معلوم نہیں کہ میرا رزق میدان میں ہے یا پہاڑ پر، زمین پر ہے یا آسمان میں، خشکی پر ہے یا سمندر میں، اور کس کے ہاتھوں پر ہے اور کس کی طرف سے ملے گا۔۔۔"
                    ),
                    TaqeebatItem(
                        title = "Trust in Divine Allocation",
                        titleUrdu = "اللہ کی تقسیم پر توکل",
                        titleArabic = "التوكل على الله في الرزق",
                        arabic = "وَقَدْ عَلِمْتُ أَنَّ عِلْمَهُ عِنْدَكَ، وَأَسْبَابَهُ بِيَدِكَ، وَأَنْتَ الَّذِي تَقْسِمُهُ بِلُطْفِكَ، وَتُسَبِّبُهُ بِرَحْمَتِكَ. اللَّهُمَّ فَصَلِّ عَلَى مُحَمَّدٍ وَآلِهِ، وَاجْعَلْ يَا رَبِّ رِزْقَكَ لِي وَاسِعاً، وَمَطْلَبَهُ سَهْلًا، وَمَأْخَذَهُ قَرِيباً",
                        transliteration = "Wa qad 'alimtu anna 'ilmahu 'indaka, wa asbabahu biyadika, wa antal-ladhi taqsimuhu bilutfika, wa tusabbibuhu birahmatika. Allahumma fasalli 'ala Muhammadin wa alihi, waj'al ya rabbi rizqaka li wasi'an, wa matlabahu sahlan, wa ma'khadhahu qariba.",
                        translation = "But I know that the knowledge of it is with You, and its means are in Your hand, and You are the One Who distributes it out of Your kindness and causes it through Your mercy. O Allah, send blessings upon Muhammad and his family, and make Your sustenance for me extensive, its search easy, and its source close.",
                        urduTranslation = "اور میں جانتا ہوں کہ اس کا علم تیرے پاس ہے اور اس کے ذرائع تیرے ہاتھ میں ہیں، اور تو ہی وہ ہے جو اپنے لطف سے اسے تقسیم کرتا ہے اور اپنی رحمت سے اس کا سبب بناتا ہے۔ اے اللہ! پس محمدؐ اور ان کی آل پر درود بھیج اور اے میرے پروردگار! اپنا رزق میرے لیے کشادہ کر دے، اس کی تلاش آسان کر دے اور اس کے ملنے کا مقام قریب کر دے۔"
                    )
                )
                else -> emptyList()
            }
        }
    }
}

@Composable
fun TaqeebatDialog(
    prayerKey: String,
    currentLang: String,
    baseAppFontFamily: androidx.compose.ui.text.font.FontFamily,
    arabicUrduFontScale: Float,
    onDismiss: () -> Unit,
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val items = remember(prayerKey) { TaqeebatContent.getTaqeebatFor(prayerKey) }

    val todayStr = remember {
        val sdf = java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        sdf.format(java.util.Date())
    }

    var isCompleted by remember(prayerKey, todayStr) {
        mutableStateOf(
            context.getSharedPreferences("taqeebat_prefs", Context.MODE_PRIVATE)
                .getBoolean("taqeebat_completed_${todayStr}_${prayerKey}", false)
        )
    }

    // Interactive Tasbeeh Counter State
    var showTasbeehSection by remember { mutableStateOf(true) }
    var tasbeehPhase by remember { mutableIntStateOf(0) } // 0: Allahu Akbar, 1: Alhamdulillah, 2: Subhanallah, 3: Completed
    var tasbeehCount by remember { mutableIntStateOf(0) }

    // Real-time custom display filter preferences
    var showArabic by remember(currentLang) { mutableStateOf(true) }
    var showTransliteration by remember(currentLang) { mutableStateOf(currentLang != "ur") }
    var showEnglishTranslation by remember(currentLang) { mutableStateOf(currentLang != "ur") }
    var showUrduTranslation by remember(currentLang) { mutableStateOf(currentLang == "ur") }

    val prayerLabel = when (prayerKey) {
        "fajr" -> if (currentLang == "ur") "فجر" else if (currentLang == "ar") "الفجر" else "Fajr"
        "dhuhr" -> if (currentLang == "ur") "ظہر" else if (currentLang == "ar") "الظهر" else "Zuhr"
        "asr" -> if (currentLang == "ur") "عصر" else if (currentLang == "ar") "العصر" else "Asr"
        "maghrib" -> if (currentLang == "ur") "مغرب" else if (currentLang == "ar") "المغرب" else "Maghrib"
        "isha" -> if (currentLang == "ur") "عشاء" else if (currentLang == "ar") "العشاء" else "Isha"
        else -> ""
    }

    val titleText = when (currentLang) {
        "ur" -> "تعقیباتِ نمازِ $prayerLabel"
        "ar" -> "تعقيبات صلاة $prayerLabel"
        else -> "Taqeebat-e-$prayerLabel"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)), // Deep premium dark background
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp)
                .border(1.5.dp, Color(0xFFFFD54F).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = baseAppFontFamily
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Visual Preferences (Row of Chips)
                Text(
                    text = "Display Preferences:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontFamily = baseAppFontFamily
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                // Filter options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Arabic Chip
                    FilterChip(
                        selected = showArabic,
                        onClick = { showArabic = !showArabic },
                        label = { Text("Arabic", fontSize = 11.sp, color = if (showArabic) Color.Black else Color.White) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFD54F),
                            containerColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                    // Transliteration Chip
                    FilterChip(
                        selected = showTransliteration,
                        onClick = { showTransliteration = !showTransliteration },
                        label = { Text("Translit.", fontSize = 11.sp, color = if (showTransliteration) Color.Black else Color.White) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFD54F),
                            containerColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                    // English Chip
                    FilterChip(
                        selected = showEnglishTranslation,
                        onClick = { showEnglishTranslation = !showEnglishTranslation },
                        label = { Text("English", fontSize = 11.sp, color = if (showEnglishTranslation) Color.Black else Color.White) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFD54F),
                            containerColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                    // Urdu Chip
                    FilterChip(
                        selected = showUrduTranslation,
                        onClick = { showUrduTranslation = !showUrduTranslation },
                        label = { Text("اردو", fontSize = 11.sp, color = if (showUrduTranslation) Color.Black else Color.White) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFD54F),
                            containerColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Content List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Interactive Tasbeeh of Lady Fatima Zahra (s.a.) Item
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (tasbeehPhase == 3) Color(0xFF1B5E20).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (tasbeehPhase == 3) Color(0xFF81C784).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (currentLang == "ur") "تسبيحِ حضرتِ فاطمہ زہرا (س)" else "Tasbeeh of Fatima Zahra (s.a.)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontFamily = baseAppFontFamily
                                        )
                                    }
                                    IconButton(
                                        onClick = { showTasbeehSection = !showTasbeehSection },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showTasbeehSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Toggle Tasbeeh",
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                if (showTasbeehSection) {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    val phaseLabel = when (tasbeehPhase) {
                                        0 -> "Allahu Akbar (اللہ اکبر)"
                                        1 -> "Alhamdulillah (الحمد للہ)"
                                        2 -> "Subhanallah (سبحان اللہ)"
                                        else -> if (currentLang == "ur") "تسبيح مکمل ہو گئی!" else "Tasbeeh Completed!"
                                    }

                                    val targetMax = when (tasbeehPhase) {
                                        0 -> 34
                                        1 -> 33
                                        2 -> 33
                                        else -> 1
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Tap Counter Circle
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (tasbeehPhase == 3) Color(0xFF2E7D32)
                                                    else Color(0xFFFFD54F).copy(alpha = 0.2f)
                                                )
                                                .border(
                                                    2.dp,
                                                    if (tasbeehPhase == 3) Color(0xFF81C784) else Color(0xFFFFD54F),
                                                    CircleShape
                                                )
                                                .clickable {
                                                    if (tasbeehPhase < 3) {
                                                        tasbeehCount++
                                                        if (tasbeehPhase == 0 && tasbeehCount >= 34) {
                                                            tasbeehCount = 0
                                                            tasbeehPhase = 1
                                                        } else if (tasbeehPhase == 1 && tasbeehCount >= 33) {
                                                            tasbeehCount = 0
                                                            tasbeehPhase = 2
                                                        } else if (tasbeehPhase == 2 && tasbeehCount >= 33) {
                                                            tasbeehCount = 0
                                                            tasbeehPhase = 3
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (tasbeehPhase == 3) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Completed",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            } else {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "$tasbeehCount",
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "/$targetMax",
                                                        fontSize = 10.sp,
                                                        color = Color.White.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = phaseLabel,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (tasbeehPhase == 3) Color(0xFF81C784) else Color(0xFFFFD54F)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (tasbeehPhase < 3) {
                                                    if (currentLang == "ur") "کاؤنٹر کو بڑھانے کے لیے دائرے پر ٹیپ کریں۔" else "Tap circle to increment counter."
                                                } else {
                                                    if (currentLang == "ur") "اب نیچے دی گئی دعا پڑھیں۔" else "Now recite the specific supplication below."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontFamily = baseAppFontFamily
                                            )
                                            if (tasbeehPhase < 3) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LinearProgressIndicator(
                                                    progress = { tasbeehCount.toFloat() / targetMax.toFloat() },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = Color(0xFFFFD54F),
                                                    trackColor = Color.White.copy(alpha = 0.1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section Divider Label
                    if (items.isNotEmpty()) {
                        item {
                            Text(
                                text = if (currentLang == "ur") "مخصوص دعائیہ جملے:" else "Specific Post-Prayer Supplication:",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold,
                                fontFamily = baseAppFontFamily,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // Supplication Cards
                    items(items.size) { index ->
                        val item = items[index]
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Card Subtitle
                                Text(
                                    text = if (currentLang == "ur") item.titleUrdu else if (currentLang == "ar") item.titleArabic else item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54F).copy(alpha = 0.9f),
                                    fontFamily = baseAppFontFamily
                                )

                                if (showArabic) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = item.arabic,
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontSize = (22 * arabicUrduFontScale).sp,
                                            fontFamily = baseAppFontFamily,
                                            textAlign = TextAlign.Right,
                                            lineHeight = (32 * arabicUrduFontScale).sp
                                        ),
                                        color = Color(0xFFFFE082),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (showTransliteration) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = item.transliteration,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }

                                if (showEnglishTranslation) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = item.translation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }

                                if (showUrduTranslation) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = item.urduTranslation,
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontSize = (16 * arabicUrduFontScale).sp,
                                            fontFamily = baseAppFontFamily,
                                            textAlign = TextAlign.Right,
                                            lineHeight = (24 * arabicUrduFontScale).sp
                                        ),
                                        color = Color(0xFFA5D6A7)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Completed Button
                    Button(
                        onClick = {
                            val newStatus = !isCompleted
                            context.getSharedPreferences("taqeebat_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("taqeebat_completed_${todayStr}_${prayerKey}", newStatus)
                                .apply()
                            isCompleted = newStatus
                            onCompleted()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) Color(0xFF2E7D32) else Color(0xFFFFD54F)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (isCompleted) Color.White else Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCompleted) {
                                    if (currentLang == "ur") "مکمل شدہ" else "Completed ✓"
                                } else {
                                    if (currentLang == "ur") "مکمل کے طور پر نشان زد کریں" else "Mark as Completed"
                                },
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted) Color.White else Color.Black,
                                fontFamily = baseAppFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

