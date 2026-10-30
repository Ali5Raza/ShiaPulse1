package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.AamalCompletion
import com.example.data.AamalCustomActivity
import java.text.SimpleDateFormat
import java.util.*

// Formats
private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AamalTrackerScreen(
    viewModel: PrayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {}
) {
    val context = LocalContext.current
    val currentLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme() || (viewModel.themeMode.collectAsStateWithLifecycle().value == "dark")
    
    // DB States
    val customAamal by viewModel.allCustomAamal.collectAsStateWithLifecycle()
    val todayCompletedList by viewModel.todayAamalCompletions.collectAsStateWithLifecycle()
    val allCompletions by viewModel.allAamalCompletions.collectAsStateWithLifecycle()
    val homeDayContext by viewModel.homeDayContext.collectAsStateWithLifecycle()
    val activeDateStr = homeDayContext.selectedDate.key
    val actualTodayStr = homeDayContext.timelines.current.date.key
    val isViewingHistory = activeDateStr != actualTodayStr
    
    // UI Dialog States
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var selectedDetailItem by remember { mutableStateOf<AamalDef?>(null) }
    var selectedDetailCustom by remember { mutableStateOf<AamalCustomActivity?>(null) }
    
    // Selected Categories Visibility Filter (stored in SharedPreferences)
    val sharedPrefs = remember { context.getSharedPreferences("aamal_filters", Context.MODE_PRIVATE) }
    var visibleCategories by remember {
        mutableStateOf(
            AamalCategory.values().associateWith { cat ->
                sharedPrefs.getBoolean("cat_${cat.name}", true)
            }
        )
    }
    
    // Sync category toggle persistency
    fun toggleCategoryVisibility(category: AamalCategory) {
        val currentVal = visibleCategories[category] ?: true
        val newVal = !currentVal
        visibleCategories = visibleCategories.toMutableMap().apply { put(category, newVal) }
        sharedPrefs.edit().putBoolean("cat_${category.name}", newVal).apply()
    }

    // Default checklist objects defined statically below
    val defaultList = remember { getDefaultAamalList() }
    
    // Calculate statistics for the selected date, not necessarily actual today.
    val requiredIdsForSelectedDate = remember(
        activeDateStr,
        actualTodayStr,
        defaultList,
        customAamal,
        visibleCategories,
        allCompletions
    ) {
        AamalProgressPolicy.requiredActivityIds(
            date = activeDateStr,
            isToday = activeDateStr == actualTodayStr,
            defaultList = defaultList,
            customAamal = customAamal,
            visibleCategories = visibleCategories,
            allCompletions = allCompletions
        )
    }
    val todayRequiredDefaultList = defaultList.filter { it.id in requiredIdsForSelectedDate }
    val currentCustomIds = remember(customAamal) { customAamal.mapTo(hashSetOf()) { it.id } }
    val displayCustomAamal = remember(customAamal, allCompletions) {
        AamalHistoryPolicy.mergeHistoricalActivities(
            currentActivities = customAamal,
            allCompletions = allCompletions,
            metadata = { id -> AamalHistoryMetadataStore.read(context, id) }
        )
    }
    val todayRequiredCustomList = displayCustomAamal.filter { it.id in requiredIdsForSelectedDate }

    val totalRequiredCount = requiredIdsForSelectedDate.size
    val totalCompletedCount = remember(todayCompletedList, requiredIdsForSelectedDate) {
        todayCompletedList.count { it.isCompleted && it.activityId in requiredIdsForSelectedDate }
    }
    
    val completionPercentage = if (totalRequiredCount > 0) {
        (totalCompletedCount.toFloat() / totalRequiredCount.toFloat())
    } else 0f

    val currentStreak = remember(
        allCompletions,
        customAamal,
        defaultList,
        visibleCategories,
        actualTodayStr,
        homeDayContext.timeZoneId
    ) {
        val today = homeDayContext.timelines.current.date.atStartOfDay(
            java.util.TimeZone.getTimeZone(homeDayContext.timeZoneId)
        )
        AamalProgressPolicy.calculateStreak(
            allCompletions = allCompletions,
            customAamal = customAamal,
            defaultList = defaultList,
            visibleCategories = visibleCategories,
            today = today
        )
    }

    val displayDateStr by viewModel.displayDateStr.collectAsStateWithLifecycle()

    var currentTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTick = System.currentTimeMillis()
        }
    }

    val allPrayerLogs by viewModel.allPrayerLogs.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = if (currentLang == "ur") listOf("روزانہ اعمال", "اعداد و شمار")
            else if (currentLang == "ar") listOf("الأعمال اليومية", "إحصائيات")
            else if (currentLang == "fa") listOf("اعمال روزانه", "آمار")
            else listOf("Daily Acts", "Statistics")

    // Main layout
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().testTag("aamal_tracker_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            PremiumScreenTopBar(
                title = when (currentLang) {
                    "ur" -> "اعمالِ روزانہ"
                    "ar" -> "الأعمال اليومية"
                    "fa" -> "اعمال روزانه"
                    "hi" -> "आमले रोज़ाना"
                    else -> "Aamal-e-Rozana"
                },
                onBack = onBack,
                backModifier = Modifier.testTag("aamal_back_btn"),
                actions = {
                    IconButton(
                        onClick = { showAddCustomDialog = true },
                        modifier = Modifier.testTag("aamal_add_custom_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Custom Act",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            if (selectedTab == 0) {
                // Dynamic Scrollable Body
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Date Navigation Card
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.changeDate(-1) }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Previous Day",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (currentLang == "ur") "تاریخ" else if (currentLang == "ar") "التاريخ" else "DATE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.2.sp
                                    )
                                    Text(
                                        text = displayDateStr,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    if (isViewingHistory) {
                                        Text(
                                            text = when (currentLang) {
                                                "ur" -> if (activeDateStr > actualTodayStr) "مستقبل کی تاریخ" else "گزشتہ تاریخ"
                                                "ar" -> if (activeDateStr > actualTodayStr) "تاريخ مستقبلي" else "تاريخ سابق"
                                                "fa" -> if (activeDateStr > actualTodayStr) "تاریخ آینده" else "تاریخ گذشته"
                                                "hi" -> if (activeDateStr > actualTodayStr) "भविष्य की तिथि" else "पिछली तिथि"
                                                else -> if (activeDateStr > actualTodayStr) "Future date" else "Historical date"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                IconButton(onClick = { viewModel.changeDate(1) }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Next Day",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }


                // Daily Dua Goal Tracker
                item {
                    val duaPrefs = context.getSharedPreferences("dua_goals", Context.MODE_PRIVATE)
                    val todayGoalStr = remember(currentTick) {
                        java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
                    }
                    var goalMinutes by remember { mutableIntStateOf(duaPrefs.getInt("daily_goal_minutes", 15)) }
                    var readSeconds by remember(currentTick) { mutableIntStateOf(duaPrefs.getInt("duration_seconds_$todayGoalStr", 0)) }
                    var showGoalDialog by remember { mutableStateOf(false) }

                    val progressFraction = if (goalMinutes > 0) {
                        (readSeconds.toFloat() / (goalMinutes * 60)).coerceIn(0f, 1f)
                    } else 0f

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { showGoalDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(50.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { 1f },
                                    color = Color.LightGray.copy(alpha = 0.5f),
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                                CircularProgressIndicator(
                                    progress = { progressFraction },
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Dua Goal",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentLang == "ur") "روزانہ دعا کا ہدف" else "Daily Dua Goal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                val readMins = readSeconds / 60
                                Text(
                                    text = "$readMins / $goalMinutes ${if (currentLang == "ur") "منٹ" else "min"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                            if (progressFraction >= 1f) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Goal Met",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    if (showGoalDialog) {
                        var tempGoal by remember { mutableStateOf(goalMinutes.toString()) }
                        AlertDialog(
                            onDismissRequest = { showGoalDialog = false },
                            title = { Text(if (currentLang == "ur") "ہدف مقرر کریں" else "Set Daily Goal") },
                            text = {
                                OutlinedTextField(
                                    value = tempGoal,
                                    onValueChange = { tempGoal = it },
                                    label = { Text(if (currentLang == "ur") "منٹ (Minutes)" else "Minutes") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val newGoal = tempGoal.toIntOrNull() ?: 15
                                    goalMinutes = newGoal
                                    duaPrefs.edit().putInt("daily_goal_minutes", newGoal).apply()
                                    showGoalDialog = false
                                }) {
                                    Text(if (currentLang == "ur") "محفوظ کریں" else "Save")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showGoalDialog = false }) {
                                    Text(if (currentLang == "ur") "منسوخ" else "Cancel")
                                }
                            }
                        )
                    }
                }

                    // Top Visual Summary Panel Card
                    item {
                    DailyProgressCard(
                        percentage = completionPercentage,
                        completed = totalCompletedCount,
                        total = totalRequiredCount,
                        streak = currentStreak,
                        languageCode = currentLang
                    )
                }

                // Category Quick Filter Box
                item {
                    CategoryFilterPanel(
                        visibleCategories = visibleCategories,
                        onToggle = { toggleCategoryVisibility(it) },
                        languageCode = currentLang
                    )
                }

                // Checklists partitioned by Categories
                AamalCategory.values().forEach { category ->
                    val isVisible = visibleCategories[category] ?: true
                    if (isVisible) {
                        val isCustom = (category == AamalCategory.CUSTOM)
                        val categoryItems = defaultList.filter { it.category == category }
                        
                        if (isCustom && todayRequiredCustomList.isNotEmpty()) {
                            item {
                                CategorySectionHeader(category, currentLang)
                            }
                            items(todayRequiredCustomList, key = { it.id }) { customAct ->
                                val isChecked = todayCompletedList.any { it.activityId == customAct.id && it.isCompleted }
                                val isHistorical = customAct.id !in currentCustomIds
                                AamalCardItem(
                                    titleStr = if (isHistorical) {
                                        AamalHistoryPolicy.displayTitle(customAct, currentLang)
                                    } else {
                                        customAct.title
                                    },
                                    subtitleStr = if (isHistorical) {
                                        when (currentLang) {
                                            "ur" -> "حذف شدہ عمل؛ تاریخی تکمیل محفوظ ہے"
                                            "ar" -> "عمل محذوف؛ سجل الإكمال محفوظ"
                                            "fa" -> "عمل حذف‌شده؛ سابقه تکمیل حفظ شده است"
                                            "hi" -> "हटाया गया कार्य; पूरा होने का इतिहास सुरक्षित है"
                                            else -> "Deleted act; completion history preserved"
                                        }
                                    } else when (currentLang) {
                                        "ur" -> "صارف کا کسٹم عمل"
                                        "ar" -> "عمل مخصص مضاف"
                                        "fa" -> "عمل سفارشی کاربر"
                                        "hi" -> "कस्टम कार्य"
                                        else -> "Custom user-generated act"
                                    },
                                    enabled = !isHistorical,
                                    isChecked = isChecked,
                                    onCheckChange = { checked ->
                                        viewModel.toggleAamalCompletion(
                                            activityId = customAct.id,
                                            isCompleted = checked,
                                            dateStr = activeDateStr
                                        )
                                    },
                                    onInfoClick = {
                                        selectedDetailCustom = customAct
                                    }
                                )
                            }
                        } else if (!isCustom && categoryItems.isNotEmpty()) {
                            item {
                                CategorySectionHeader(category, currentLang)
                            }
                            items(categoryItems) { item ->
                                val isChecked = todayCompletedList.any { it.activityId == item.id && it.isCompleted }
                                AamalCardItem(
                                    titleStr = item.getTitle(currentLang),
                                    subtitleStr = item.getSubtitle(currentLang),
                                    isChecked = isChecked,
                                    onCheckChange = { checked ->
                                        viewModel.toggleAamalCompletion(
                                            activityId = item.id,
                                            isCompleted = checked,
                                            dateStr = activeDateStr
                                        )
                                    },
                                    onInfoClick = {
                                        selectedDetailItem = item
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Extra Spacing bottom
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
            } else {
                AamalStatisticsView(
                    allCompletions = allCompletions,
                    allPrayerLogs = allPrayerLogs,
                    languageCode = currentLang,
                    currentAamalStreak = currentStreak
                )
            }
        }
    }

    // Add Custom Act dialog
    if (showAddCustomDialog) {
        AddCustomActDialog(
            languageCode = currentLang,
            onDismiss = { showAddCustomDialog = false },
            onSave = { title ->
                viewModel.addCustomAamal(title, "Custom", activeDateStr)
                showAddCustomDialog = false
                Toast.makeText(context, "Added custom routine!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Details/Rulings Dialogue for Default Acts
    selectedDetailItem?.let { item ->
        AamalDetailDialog(
            item = item,
            languageCode = currentLang,
            onDismiss = { selectedDetailItem = null },
            onEditCard = onEditCard
        )
    }

    // Details Dialogue for Custom Acts
    selectedDetailCustom?.let { act ->
        CustomAamalDetailDialog(
            act = act,
            languageCode = currentLang,
            onDismiss = { selectedDetailCustom = null },
            canDelete = act.id in currentCustomIds,
            onDelete = {
                viewModel.deleteCustomAamal(act.id)
                selectedDetailCustom = null
                Toast.makeText(context, "Routine deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// Visual layout components
@Composable
fun DailyProgressCard(
    percentage: Float,
    completed: Int,
    total: Int,
    streak: Int,
    languageCode: String
) {
    val percentString = "${(percentage * 100).toInt()}%"
    
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("aamal_progress_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular widget with drawBehind
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .drawBehind {
                        // Background circle track
                        drawArc(
                            color = Color.White.copy(alpha = 0.28f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Progress arc
                        drawArc(
                            color = Color(0xFFFFD54F), // Premium gold
                            startAngle = -90f,
                            sweepAngle = percentage * 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
            ) {
                Text(
                    text = percentString,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when (languageCode) {
                        "ur" -> "آج کی روحانی پیشرفت"
                        "ar" -> "التقدم الروحي اليوم"
                        "fa" -> "پیشرفت معنوی امروز"
                        "hi" -> "आज की प्रगति"
                        else -> "Today's Spiritual Progress"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (total == 0) {
                        when (languageCode) {
                            "ur" -> "کوئی اعمال ابھی منتخب نہیں"
                            else -> "No tasks listed for today"
                        }
                    } else {
                        when (languageCode) {
                            "ur" -> "$completed میں سے $total اعمال مکمل"
                            "ar" -> "تم إكمال $completed من أصل $total"
                            "fa" -> "$completed از $total کار انجام شد"
                            "hi" -> "$completed میں سے $total پورے"
                            else -> "Completed $completed of $total acts"
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                )

                // Streak Badge
                if (streak > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Streak",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (languageCode) {
                                    "ur" -> "مسلسل روحانی تسلسل: $streak دن 🔥"
                                    "ar" -> "سلسلة الأيام الروحية: $streak أيام 🔥"
                                    "fa" -> "تداوم معنوی: $streak روز 🔥"
                                    "hi" -> "लगातार आध्यात्मिक दिन: $streak 🔥"
                                    else -> "Spiritual Streak: $streak days 🔥"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryFilterPanel(
    visibleCategories: Map<AamalCategory, Boolean>,
    onToggle: (AamalCategory) -> Unit,
    languageCode: String
) {
    Column {
        Text(
            text = when (languageCode) {
                "ur" -> "دستور العمل کے حصے فلٹر کریں:"
                "ar" -> "تصفية الأقسام اليومية:"
                "fa" -> "فیلتر کردن بخش اعمال:"
                "hi" -> "श्रेणी चुने:"
                else -> "Toggle Active Sections:"
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AamalCategory.values().forEach { category ->
                val isActive = visibleCategories[category] ?: true
                FilterChip(
                    selected = isActive,
                    onClick = { onToggle(category) },
                    label = {
                        Text(
                            text = category.getLabel(languageCode),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("aamal_filter_chip_${category.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
fun CategorySectionHeader(category: AamalCategory, languageCode: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = category.getLabel(languageCode),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Divider(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AamalCardItem(
    titleStr: String,
    subtitleStr: String,
    isChecked: Boolean,
    onCheckChange: (Boolean) -> Unit,
    onInfoClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.38f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckChange(!isChecked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ripple checkbox
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckChange,
                enabled = enabled,
                modifier = Modifier
                    .scale(1.1f)
                    .testTag("aamal_checkbox_${titleStr.replace(" ", "_").lowercase()}")
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = titleStr,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (subtitleStr.isNotEmpty()) {
                    Text(
                        text = subtitleStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }

            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.testTag("aamal_info_btn_${titleStr.replace(" ", "_").lowercase()}")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Details",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// Dialog for adding custom act
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomActDialog(
    languageCode: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (languageCode) {
                        "ur" -> "ایک نیا کار شامل کریں"
                        "ar" -> "إضافة عمل مخصص جديد"
                        "fa" -> "افزودن کار جدید"
                        "hi" -> "नया कार्य जोड़ें"
                        else -> "Add Custom Routine"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(text = "e.g., Read 1 page of Nahjul Balagha")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_custom_input_field"),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when (languageCode) {
                                "ur" -> "منسوخ"
                                else -> "Cancel"
                            }
                        )
                    }
                    Button(
                        onClick = {
                            if (textInput.trim().isNotEmpty()) {
                                onSave(textInput.trim())
                            }
                        },
                        enabled = textInput.trim().isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_custom_activity_btn")
                    ) {
                        Text(
                            text = when (languageCode) {
                                "ur" -> "محفوظ کریں"
                                else -> "Save"
                            }
                        )
                    }
                }
            }
        }
    }
}

// Dialog sharing button
fun shareAamalRoutineText(context: Context, label: String, ar: String, enDesc: String) {
    val shareBody = """
        🕌 Aamal-e-Rozana Routine Details 🕌
        ✨ Act: $label
        
        📖 Arabic: $ar
        📝 Description & Reference:
        $enDesc
        
        Shared via Shia Pulse App.
    """.trimIndent()
    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, shareBody)
        type = "text/plain"
    }
    context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Aamal Routine Details"))
}

// Detail Dialog for default core items
@Composable
fun AamalDetailDialog(
    item: AamalDef,
    languageCode: String,
    onDismiss: () -> Unit,
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {}
) {
    val context = LocalContext.current
    var personalNotes by remember {
        mutableStateOf(
            context.getSharedPreferences("aamal_item_notes", Context.MODE_PRIVATE)
                .getString("notes_${item.id}", "") ?: ""
        )
    }
    
    var fullArabicText by remember { mutableStateOf(item.arabic) }
    var fullTranslatedText by remember { mutableStateOf("") }
    
    LaunchedEffect(item.id, languageCode) {
        try {
            val inputStream = context.assets.open("aamal_content.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, Charsets.UTF_8)
            val jsonObject = org.json.JSONObject(jsonString)
            
            var lookupId = item.id
            if (lookupId == "daily_dua_weekday" || lookupId == "dua_ziyarat_weekday") {
                val calendar = java.util.Calendar.getInstance()
                val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                val dayString = when (dayOfWeek) {
                    java.util.Calendar.SUNDAY -> "sunday"
                    java.util.Calendar.MONDAY -> "monday"
                    java.util.Calendar.TUESDAY -> "tuesday"
                    java.util.Calendar.WEDNESDAY -> "wednesday"
                    java.util.Calendar.THURSDAY -> "thursday"
                    java.util.Calendar.FRIDAY -> "friday"
                    java.util.Calendar.SATURDAY -> "saturday"
                    else -> "monday"
                }
                if (lookupId == "daily_dua_weekday") {
                    lookupId = "daily_dua_$dayString"
                }
            }
            
            if (jsonObject.has(lookupId)) {
                val itemObj = jsonObject.getJSONObject(lookupId)
                if (itemObj.has("arabic")) {
                    fullArabicText = itemObj.getString("arabic")
                }
                
                val langKey = if (languageCode == "ur") "ur" else "en"
                if (itemObj.has(langKey)) {
                    fullTranslatedText = itemObj.getString(langKey)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(vertical = 16.dp, horizontal = 4.dp)
                .testTag("aamal_detail_dialog_${item.id}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Title and category badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.getTitle(languageCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        IconButton(
                            onClick = {
                                onEditCard(
                                    com.example.api.ShiaQuote(
                                        arabic = fullArabicText,
                                        english = if (languageCode != "ur") fullTranslatedText else "",
                                        urdu = if (languageCode == "ur") fullTranslatedText.ifEmpty { item.getFarsiDescription() } else "",
                                        source = item.getTitle(languageCode)
                                    )
                                )
                                onDismiss()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = "Edit in Cards Maker",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                shareAamalRoutineText(
                                    context,
                                    item.getTitle(languageCode),
                                    fullArabicText,
                                    fullTranslatedText.ifEmpty { item.getFarsiDescription() }
                                )
                            },
                            modifier = Modifier.testTag("share_aamal_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share details",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.category.getLabel(languageCode),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Shia Ja'fari Fiqh timings and descriptions
                Text(
                    text = when (languageCode) {
                        "ur" -> "وضاحت اور احکام / حوالہ:"
                        "ar" -> "الشرح والأحكام / المرجع:"
                        else -> "Explanation, Rulings & Authentic Reference:"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.getDetailedNotes(languageCode),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (fullArabicText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha=0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "عربي (Arabic):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = fullArabicText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Right,
                        lineHeight = 34.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().clickable {
                            onEditCard(
                                com.example.api.ShiaQuote(
                                    arabic = fullArabicText,
                                    english = "",
                                    urdu = "",
                                    source = item.getTitle(languageCode)
                                )
                            )
                            onDismiss()
                        }
                    )
                }
                
                if (fullTranslatedText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (languageCode == "ur") "ترجمہ (Translation):" else "Translation:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = fullTranslatedText,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = if (languageCode == "ur") TextAlign.Right else TextAlign.Left,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().clickable {
                            onEditCard(
                                com.example.api.ShiaQuote(
                                    arabic = "",
                                    english = if (languageCode != "ur") fullTranslatedText else "",
                                    urdu = if (languageCode == "ur") fullTranslatedText else "",
                                    source = item.getTitle(languageCode)
                                )
                            )
                            onDismiss()
                        }
                    )
                }

                // Reference Citation label
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha=0.3f))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = when (languageCode) {
                        "ur" -> "مستند کتاب:"
                        "ar" -> "الكتاب المعتمد:"
                        else -> "Authentic Ja'fari Fiqh Reference:"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = item.reference,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Personal Reflections Notes Field
                Spacer(modifier = Modifier.height(20.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when (languageCode) {
                        "ur" -> "ذاتی عکاسی اور نوٹس:"
                        else -> "Personal Reflections & Notes:"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = personalNotes,
                    onValueChange = { notes ->
                        personalNotes = notes
                        context.getSharedPreferences("aamal_item_notes", Context.MODE_PRIVATE)
                            .edit()
                            .putString("notes_${item.id}", notes)
                            .apply()
                    },
                    placeholder = {
                        Text(text = "Capture spiritually enlightening thoughts or reflections here...")
                    },
                    modifier = Modifier.fillMaxWidth().testTag("personal_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (languageCode) {
                            "ur" -> "بند کریں"
                            "ar" -> "إغلاق"
                            else -> "Dismiss"
                        }
                    )
                }
            }
        }
    }
}

// Custom Aamal Detail Dialog
@Composable
fun CustomAamalDetailDialog(
    act: AamalCustomActivity,
    languageCode: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean = true
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (canDelete) act.title else AamalHistoryPolicy.displayTitle(act, languageCode),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = when (languageCode) {
                        "ur" -> "ایک صارف مضاف عمل"
                        else -> "Custom user routine created locally."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Created date: ${act.dateAdded}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Close")
                    }
                    Button(
                        onClick = onDelete,
                        enabled = canDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.weight(1f).testTag("delete_custom_activity_btn")
                    ) {
                        Text(
                            text = when (languageCode) {
                                "ur" -> "حذف کریں"
                                else -> "Delete"
                            }
                        )
                    }
                }
            }
        }
    }
}

// Category Struct
enum class AamalCategory {
    ADHKAR,
    DUAS_ZIYARAT,
    MUSTAHAB,
    CUSTOM;

    fun getLabel(languageCode: String): String {
        return when (this) {
            ADHKAR -> when (languageCode) {
                "ur" -> "روزانہ کے اذکار"
                "ar" -> "الأذكار والتسبيح"
                "fa" -> "اذکار روزانه"
                "hi" -> "दैनिक अज़कार"
                else -> "Daily Adhkar"
            }
            DUAS_ZIYARAT -> when (languageCode) {
                "ur" -> "دعائیں اور زیارات"
                "ar" -> "الأدعية والزيارات"
                "fa" -> "دعاها و زیارات"
                "hi" -> "दुआ और ज़ियारत"
                else -> "Daily Duas & Ziyarats"
            }
            MUSTAHAB -> when (languageCode) {
                "ur" -> "دیگر مستحب اعمال"
                "ar" -> "الأعمال المستحبة"
                "fa" -> "اعمال مستحبی"
                "hi" -> "अन्य मुस्तहब कार्य"
                else -> "Daily Mustahab Acts"
            }
            CUSTOM -> when (languageCode) {
                "ur" -> "ذاتی اعمال"
                "ar" -> "أعمالي الخاصة"
                "fa" -> "اعمال شخصی"
                "hi" -> "निजी कार्य"
                else -> "Custom Routine"
            }
        }
    }
}

// Class representing an Aamal routine checklist item details
data class AamalDef(
    val id: String,
    val category: AamalCategory,
    val titleEn: String,
    val titleUr: String,
    val titleAr: String,
    val subEn: String,
    val subUr: String,
    val arabic: String = "",
    val reference: String = "Mafatih al-Jinan",
    // Elaborated explanations based on authentic Shia (Ja'fari) Fiqh rulings
    val descEn: String,
    val descUr: String
) {
    fun getTitle(languageCode: String): String {
        return when (languageCode) {
            "ur" -> titleUr
            "ar" -> titleAr
            "fa" -> titleUr // Persian fallback to ur or fallback
            "hi" -> titleEn
            else -> titleEn
        }
    }

    fun getSubtitle(languageCode: String): String {
        return when (languageCode) {
            "ur" -> subUr
            else -> subEn
        }
    }

    fun getDetailedNotes(languageCode: String): String {
        return when (languageCode) {
            "ur" -> descUr
            else -> descEn
        }
    }

    fun getFarsiDescription(): String {
        return descEn
    }
}

// Compatibility wrapper for existing callers; the shared policy prevents double-counting.
fun calculateCurrentStreak(
    allCompletions: List<AamalCompletion>,
    customAamal: List<AamalCustomActivity>,
    defaultAamalCount: Int,
    today: Calendar = Calendar.getInstance()
): Int = AamalProgressPolicy.calculateStreak(
    allCompletions = allCompletions,
    customAamal = customAamal,
    defaultList = getDefaultAamalList().take(defaultAamalCount.coerceAtLeast(0)),
    today = today
)

// Static definition of Shia Daily Routine checklists
fun getDefaultAamalList(): List<AamalDef> {
    return listOf(
        // Adhkar
        AamalDef(
            id = "adhkar_fatima",
            category = AamalCategory.ADHKAR,
            titleEn = "Tasbeeh of Lady Fatima (sa)",
            titleUr = "تسبیح حضرتِ فاطمہ زہراؑ",
            titleAr = "تسبيح السيدة فاطمة الزهراء عليها السلام",
            subEn = "34x Allahu Akbar, 33x Alhamd.., 33x Subhan..",
            subUr = "34 مرتبہ تکبیر، 33 مرتبہ حمد، 33 مرتبہ تسبیح",
            arabic = "الله أكبر (٣٤) ، الحمد لله (٣٣) ، سبحان الله (٣٣)",
            reference = "Al-Kafi",
            descEn = "The legendary spiritual gift of the Holy Prophet (saw) to his beloved daughter Hazrat Fatima-al-Zahra (sa). To be recited after each obligatory prayer to earn tremendous rewards.",
            descUr = "رسول اللہؐ کا حضرت بی بی فاطمہ زہراؑ کو دیا گیا عظیم تحفہ۔ ہر واجب نماز کے بعد اسے پڑھنا گناہوں کی بخشش کا باعث ہے۔"
        ),
        AamalDef(
            id = "adhkar_salawat",
            category = AamalCategory.ADHKAR,
            titleEn = "Durood / Salawat",
            titleUr = "درود شریف (صلوات)",
            titleAr = "الصلوات على محمد وآل محمد",
            subEn = "At least 100 times daily",
            subUr = "روزانہ کم از کم 100 مرتبہ",
            arabic = "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَآلِ مُحَمَّدٍ",
            reference = "Mafatih al-Jinan",
            descEn = "Invoking divine blessings upon Prophet Muhammad (saw) and his pure Ahlul Bayt (as). Extremely powerful and removes all difficulties.",
            descUr = "حضرت محمد مصطفیٰؐ اور ان کی آلِ پاکؑ پر کثرت سے درود شریف بھیجنا دلی حاجات کی تکمیل کرتا ہے۔"
        ),
        AamalDef(
            id = "adhkar_ayat_kursi",
            category = AamalCategory.ADHKAR,
            titleEn = "Ayat-ul-Kursi",
            titleUr = "آیتہ الکرسی",
            titleAr = "آية الكرسي",
            subEn = "Recitation after prayers",
            subUr = "ہر نماز کے بعد تلاوت",
            arabic = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ...",
            reference = "Holy Quran (2:255-257)",
            descEn = "Recitation of the greatest verse of the Holy Quran, securing angelic protection and massive rewards after prayers.",
            descUr = "قرآن مجید کی انتہائی بابرکت آیت۔ ہر نماز کے بعد اس کی تلاوت شیطان کے اثرات و بلاؤں سے بچاتی ہے۔"
        ),

        // Duas & Ziyarat
        AamalDef(
            id = "dua_parents",
            category = AamalCategory.DUAS_ZIYARAT,
            titleEn = "Dua for Parents",
            titleUr = "والدین کے لیے دعا",
            titleAr = "دعاء للوالدين",
            subEn = "From Sahifa Sajjadiya",
            subUr = "صحیفہ سجادیہ سے دعا نمبر 24",
            arabic = "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ عَبْدِكَ وَرَسُولِكَ...",
            reference = "Sahifa al-Sajjadiya (Dua 24)",
            descEn = "The magnificent supplication of Imam Zainul Abideen (as) for showing gratitude and seeking absolute forgiveness and mercy for one's parents.",
            descUr = "امام علی سجاد علیہ السلام کی اپنے والدین کی خیر و عافیت اور درجات کی بلندی کے لیے مانگی گئی خوبصورت دعا۔"
        ),
        AamalDef(
            id = "dua_ziyarat_weekday",
            category = AamalCategory.DUAS_ZIYARAT,
            titleEn = "Weekday Ziyarat of Masoomeen",
            titleUr = "زیارتِ ایامِ ہفتہ",
            titleAr = "زيارة المعصومين لأيام الأسبوع",
            subEn = "Special daily Ziyarat based on day",
            subUr = "ہفتے کے ہر دن کے لیے مخصوص معصوم کی زیارت",
            arabic = "",
            reference = "Mafatih al-Jinan",
            descEn = "Each day of the week is dedicated to the spiritual visits of specific Masoomeen (as). E.g., Saturday is for Holy Prophet (saw), Sunday for Imam Ali (as) & Lady Fatima (sa), Friday for Imam Mahdi (as).",
            descUr = "ہفتے کے ہر دن مخصوص معصومینِ پاکؑ کی منسوب زیارت پڑھنا قربتِ الہیٰ اور شفاعت کا وسیلہ ہے۔"
        ),
        AamalDef(
            id = "daily_dua_weekday",
            category = AamalCategory.DUAS_ZIYARAT,
            titleEn = "Daily Dua of the Day",
            titleUr = "آج کے دن کی دعا",
            titleAr = "دعاء اليوم",
            subEn = "Specific dua for today",
            subUr = "آج کے دن کی مخصوص دعا",
            arabic = "",
            reference = "Sahifa al-Sajjadiya",
            descEn = "Supplication related to the current day of the week from the Holy Imams (as).",
            descUr = "آج کے دن یعنی ہفتے کے ایام سے منسوب ائمہ اطہارؑ کی خاص دعا۔"
        ),

        // Mustahab
        AamalDef(
            id = "mustahab_sadaqah",
            category = AamalCategory.MUSTAHAB,
            titleEn = "Give Charity (Sadaqah)",
            titleUr = "صدقہ دینا",
            titleAr = "إعطاء الصدقة",
            subEn = "Spiritual cure & protection",
            subUr = "آفتوں کی دوری اور خیرات",
            arabic = "",
            reference = "Nahjul Balagha",
            descEn = "Giving even a very small amount of charity (Sadaqah) daily to help the poor and ward off evil calamities.",
            descUr = "روزانہ کی بنیاد پر غریبوں کی خوشحالی کے لیے بلا ضرورت صدقہ و خیرات نکالنا جس سے ستر بلائیں ٹلتی ہیں۔"
        ),
        AamalDef(
            id = "mustahab_quran",
            category = AamalCategory.MUSTAHAB,
            titleEn = "Recite Holy Quran",
            titleUr = "قرآن مجید کی تلاوت",
            titleAr = "تلاوة القرآن الكريم",
            subEn = "Minimum 50 verses daily",
            subUr = "روزانہ کم از کم 50 آیات",
            arabic = "",
            reference = "Al-Kafi",
            descEn = "Reciting at least 50 verses of the Holy Quran daily as recommended by Imam Jafar al-Sadiq (as).",
            descUr = "امام جعفر صادقؑ کی تاکید کے مطابق روزانہ تلاوتِ قرآنِ مجید کے کم از کم 50 مقامات کی تلاوت۔"
        )
    )
}

@Composable
fun AamalStatisticsView(
    allCompletions: List<AamalCompletion>,
    allPrayerLogs: List<com.example.data.PrayerLog>,
    languageCode: String,
    currentAamalStreak: Int
) {
    var timeRange by remember { mutableStateOf(7) }
    
    val wajibSet = remember { setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha") }
    
    val (aamalStats, prayerStats, mergedKeys) = remember(allCompletions, allPrayerLogs, timeRange) {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -timeRange)
        val cutoffDateStr = sdf.format(calendar.time)
        
        val filteredAamal = allCompletions.filter { it.isCompleted && it.dateString >= cutoffDateStr }
        val filteredPrayers = allPrayerLogs.filter { (it.status == "Prayed" || it.status == "Jamaat") && it.prayerName in wajibSet && it.dateString >= cutoffDateStr }
        
        val aamalGrouped = filteredAamal.groupBy { it.dateString }
        val prayerGrouped = filteredPrayers.groupBy { it.dateString }
        
        val keys = (aamalGrouped.keys + prayerGrouped.keys).sorted().takeLast(if (timeRange == 7) 7 else if (timeRange == 30) 30 else 30) // simplified to 30 max points for display
        Triple(aamalGrouped, prayerGrouped, keys)
    }

    val totalAamal = allCompletions.count { it.isCompleted } // We show total acts recorded instead of just in range
    val totalPrayers = allPrayerLogs.count { (it.status == "Prayed" || it.status == "Jamaat") && it.prayerName in wajibSet }
    val totalNawafil = allPrayerLogs.count { (it.status == "Prayed" || it.status == "Jamaat") && it.prayerName !in wajibSet }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Time Range Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val ranges = listOf(7 to if(languageCode=="ur") "1 ہفتہ" else "1 Week", 
                               30 to if(languageCode=="ur") "1 مہینہ" else "1 Month", 
                               365 to if(languageCode=="ur") "1 سال" else "1 Year")
            ranges.forEach { (days, label) ->
                val isSelected = timeRange == days
                Button(
                    onClick = { timeRange = days },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Stats Overview Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = if (languageCode == "ur") "کل عبادات" else "Aamal Acts",
                value = totalAamal.toString(),
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = if (languageCode == "ur") "واجب نمازیں" else "Wajib Prayers",
                value = totalPrayers.toString(),
                icon = Icons.Default.Star,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = if (languageCode == "ur") "نوافل/شب" else "Nawafil & Shab",
                value = totalNawafil.toString(),
                icon = Icons.Default.NightsStay,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Recharts-style Combined Bar Chart
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (languageCode == "ur") "سرگرمی کا چارٹ ($timeRange دن)" else "Activity Chart ($timeRange Days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Legend
                Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aamal", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (languageCode == "ur") "نماز" else "Prayers", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (mergedKeys.isEmpty()) {
                    Text(if (languageCode == "ur") "اس دوران کوئی سرگرمی نہیں" else "No activities in this range.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val maxAamal = aamalStats.maxOfOrNull { it.value.size } ?: 1
                        val maxPrayers = prayerStats.maxOfOrNull { it.value.size } ?: 1
                        val maxGlobalCount = maxOf(maxAamal, maxPrayers).coerceAtLeast(1)
                        
                        mergedKeys.forEach { dateKey ->
                            val aCount = aamalStats[dateKey]?.size ?: 0
                            val pCount = prayerStats[dateKey]?.size ?: 0
                            
                            val aRatio = aCount.toFloat() / maxGlobalCount.toFloat()
                            val pRatio = pCount.toFloat() / maxGlobalCount.toFloat()
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Aamal Bar
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(aRatio.coerceAtLeast(0.02f))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    // Prayer Bar
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(pRatio.coerceAtLeast(0.02f))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(MaterialTheme.colorScheme.secondary)
                                    )
                                }
                                
                                val printDay = dateKey.takeLast(2)
                                Text(
                                    text = printDay,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        BadgesSection(allCompletions, allPrayerLogs, languageCode, currentAamalStreak)
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun BadgesSection(
    aamalCompletions: List<AamalCompletion>,
    prayerLogs: List<com.example.data.PrayerLog>,
    languageCode: String,
    currentAamalStreak: Int
) {
    val wajibSet = remember { setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha") }
    val totalAamal = aamalCompletions.count { it.isCompleted }
    val totalPrayers = prayerLogs.count { (it.status == "Prayed" || it.status == "Jamaat") && it.prayerName in wajibSet }
    val totalNawafil = prayerLogs.count { (it.status == "Prayed" || it.status == "Jamaat") && it.prayerName !in wajibSet }
    
    // Aamal streak is supplied by the shared perfect-day policy.
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    
    val prayerDates = prayerLogs.filter { (it.status == "Prayed" || it.status == "Jamaat") && it.prayerName in wajibSet }.map { it.dateString }.distinct().sortedDescending()
    var currentPrayerStreak = 0
    val pTodayDate = java.util.Calendar.getInstance()
    var pCheckDate = sdf.format(pTodayDate.time)
    
    if (prayerDates.contains(pCheckDate)) {
        currentPrayerStreak++
    } else {
        pTodayDate.add(java.util.Calendar.DAY_OF_YEAR, -1)
        pCheckDate = sdf.format(pTodayDate.time)
        if (prayerDates.contains(pCheckDate)) {
            currentPrayerStreak++
        }
    }
    
    if (currentPrayerStreak > 0) {
        var streakActive = true
        while(streakActive) {
            pTodayDate.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val prevDate = sdf.format(pTodayDate.time)
            if (prayerDates.contains(prevDate)) {
                currentPrayerStreak++
            } else {
                streakActive = false
            }
        }
    }

    val badges = mutableListOf<Pair<String, ImageVector>>()
    
    // Evaluators
    if (currentAamalStreak >= 3) badges.add("Aamal 3-Day" to Icons.Default.Star)
    if (currentAamalStreak >= 7) badges.add("Aamal 7-Day" to Icons.Default.CheckCircle)
    if (currentPrayerStreak >= 7) badges.add("Prayer 7-Day" to Icons.Default.CheckCircle)
    if (currentAamalStreak >= 30) badges.add("Aamal Monthly" to Icons.Default.Star)
    if (currentPrayerStreak >= 30) badges.add("Prayer Monthly" to Icons.Default.Star)
    
    // Milestones
    if (totalAamal >= 50) badges.add("50 Aamal Acts" to Icons.Filled.CheckCircle)
    if (totalPrayers >= 50) badges.add("50 Prayers" to Icons.Filled.CheckCircle)
    if (totalAamal >= 100) badges.add("Centurion Aamal" to Icons.Filled.Star)
    
    if (badges.isEmpty()) {
        badges.add((if (languageCode == "ur") "سفر کا آغاز" else "Journey Begins") to Icons.Default.Star)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = if (languageCode == "ur") "بیجز اور کامیابیاں" else "Badges & Milestones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(badges) { badge ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(80.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = badge.second,
                                contentDescription = badge.first,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = badge.first,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text = title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
    }
}
