package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AamalCompletion
import com.example.data.DuaReadingLog
import com.example.data.PrayerLog
import com.example.data.TasbeehDailyRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun JourneyScreen(
    viewModel: PrayerViewModel,

    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLang by viewModel.appLanguage.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
    val title = if (currentLang == "ur") "میرا سفر" else if (currentLang == "ar") "رحلتي" else "My Journey"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Text(
                            text = if (currentLang == "ur") "پروگریس" else if (currentLang == "ar") "التقدم" else "Progress",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Text(
                            text = if (currentLang == "ur") "ٹائم کیپسول" else if (currentLang == "ar") "كبسولة الزمن" else "Time Capsule",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
            
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                if (selectedTab == 0) {
                    ProgressTrackerContent(viewModel = viewModel)
                } else {
                    TimeCapsuleContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ProgressTrackerContent(viewModel: PrayerViewModel) {
    val currentLang by viewModel.appLanguage.collectAsState()
    val readingLogs by viewModel.allDuaReadingLogs.collectAsState()
    
    val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val today = Calendar.getInstance()
    
    // Stats calculation
    var dailyCount = 0
    var weeklyCount = 0
    var monthlyCount = 0
    var currentStreak = 0
    
    val logsByDate = readingLogs.groupBy { it.dateString }
    val sortedDates = logsByDate.keys.sortedDescending()
    
    // Calculate Today, This Week, This Month
    val todayStr = df.format(today.time)
    dailyCount = logsByDate[todayStr]?.size ?: 0
    
    val oneWeekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    val oneMonthAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
    
    readingLogs.forEach { log ->
        val logDate = df.parse(log.dateString)
        if (logDate != null) {
            if (logDate.after(oneWeekAgo.time) || log.dateString == todayStr) {
                weeklyCount++
            }
            if (logDate.after(oneMonthAgo.time) || log.dateString == todayStr) {
                monthlyCount++
            }
        }
    }
    
    // Calculate Streak
    var streakTemp = 0
    var checkDate = Calendar.getInstance()
    while (true) {
        val dateStr = df.format(checkDate.time)
        if (logsByDate.containsKey(dateStr)) {
            streakTemp++
            checkDate.add(Calendar.DAY_OF_YEAR, -1)
        } else if (dateStr == todayStr) {
            // If didn't read today yet, check yesterday
            checkDate.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
    }
    currentStreak = streakTemp

    // Badges calculation based on streaks and total counts
    val totalCount = readingLogs.size
    val badges = mutableListOf<Pair<String, Boolean>>()
    
    val titleFirst = if (currentLang == "ur") "پہلی دعا" else "First Dua"
    badges.add(titleFirst to (totalCount >= 1))
    
    val titleNovice = if (currentLang == "ur") "مبتدی (10 دعائیں)" else "Novice (10 Duas)"
    badges.add(titleNovice to (totalCount >= 10))
    
    val titleRegular = if (currentLang == "ur") "پابند (50 دعائیں)" else "Regular (50 Duas)"
    badges.add(titleRegular to (totalCount >= 50))
    
    val titleStreak3 = if (currentLang == "ur") "3 دن کی سٹریک" else "3-Day Streak"
    badges.add(titleStreak3 to (currentStreak >= 3))
    
    val titleStreak7 = if (currentLang == "ur") "7 دن کی سٹریک" else "7-Day Streak"
    badges.add(titleStreak7 to (currentStreak >= 7))

    val titleStreak30 = if (currentLang == "ur") "30 دن کی سٹریک" else "30-Day Streak"
    badges.add(titleStreak30 to (currentStreak >= 30))

    Box(modifier = Modifier.fillMaxSize()) { val paddingValues = PaddingValues(0.dp)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Streak Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$currentStreak",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (currentLang == "ur") "روزانہ پڑھنے کی عادت (Streak)" else "Daily Habit (Streak)",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Stats Section
            item {
                Text(
                    text = if (currentLang == "ur") "کتنی دعائیں پڑھیں؟" else "Duas Read",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = if (currentLang == "ur") "آج" else "Daily",
                        count = dailyCount,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = if (currentLang == "ur") "اس ہفتے" else "Weekly",
                        count = weeklyCount,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = if (currentLang == "ur") "اس مہینے" else "Monthly",
                        count = monthlyCount,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Weekly Consistency Chart
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (currentLang == "ur") "ہفتہ وار تسلسل" else "Weekly Consistency",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                WeeklyBarChart(logsByDate = logsByDate, currentLang = currentLang)
            }

            // Badges Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (currentLang == "ur") "انعامات اور بیجز" else "Awards / Badges",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(badges.size / 2 + 1) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (colIndex in 0..1) {
                        val index = rowIndex * 2 + colIndex
                        if (index < badges.size) {
                            val badge = badges[index]
                            BadgeCard(
                                title = badge.first,
                                isUnlocked = badge.second,
                                modifier = Modifier.weight(1f)
                            )
                        } else if (colIndex == 1 && badges.size % 2 != 0) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun StatCard(title: String, count: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = Modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BadgeCard(title: String, isUnlocked: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = Modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 4.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Badge",
                tint = if (isUnlocked) Color(0xFFFFC107) else Color.Gray.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = if (isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun WeeklyBarChart(logsByDate: Map<String, List<DuaReadingLog>>, currentLang: String) {
    val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val displayFormat = SimpleDateFormat("EE", Locale.US) // Mon, Tue...
    
    val past7Days = (6 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val dateStr = df.format(cal.time)
        val count = logsByDate[dateStr]?.size ?: 0
        val label = displayFormat.format(cal.time).take(3)
        Pair(label, count)
    }

    val maxCount = past7Days.maxOf { it.second }.coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            past7Days.forEach { (label, count) ->
                val barHeightFraction = count.toFloat() / maxCount
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (count > 0) count.toString() else "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight(barHeightFraction.coerceAtLeast(0.05f)) // minimum height
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                if (count > 0) MaterialTheme.colorScheme.primary
                                else Color.Gray.copy(alpha = 0.3f)
                            )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


data class CapsuleStats(
    val totalPrayed: Int,
    val totalNawafil: Int,
    val totalQada: Int,
    val totalTasbeehZahra: Int,
    val genericTasbeehCounts: Int,
    val duasZiyaratsRead: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeCapsuleContent(viewModel: PrayerViewModel) {
    val context = LocalContext.current
    val currentLang by viewModel.appLanguage.collectAsState()
    
    val aamalCompletions by viewModel.allAamalCompletions.collectAsState()
    val prayerLogs by viewModel.allPrayerLogs.collectAsState()
    val tasbeehRecords by viewModel.tasbeehRecords.collectAsState()

    val calendar = remember { Calendar.getInstance() }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    calendar.set(Calendar.YEAR, selectedYear)
    calendar.set(Calendar.MONTH, selectedMonth)
    val displayMonth = monthFormat.format(calendar.time)

    val monthPrefix = String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth + 1)
    
    // Data Aggregation Function wrapped in remember for optimization
    val stats = remember(monthPrefix, prayerLogs, aamalCompletions, tasbeehRecords) {
        val wajibSet = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        val logsThisMonth = prayerLogs.filter { it.dateString.startsWith(monthPrefix) }
        val aamalThisMonth = aamalCompletions.filter { it.dateString.startsWith(monthPrefix) && it.isCompleted }
        val tasbeehThisMonth = tasbeehRecords.filter { it.dateString.startsWith(monthPrefix) }

        val prayed = logsThisMonth.count { (it.status == "Prayed" || it.status == "Jamaat") && it.prayerName in wajibSet }
        val nawafil = logsThisMonth.count { (it.status == "Prayed" || it.status == "Jamaat") && it.prayerName !in wajibSet }
        val qada = logsThisMonth.count { it.status == "Qada" && it.prayerName in wajibSet }
        
        val zahra = aamalThisMonth.count { it.activityId == "adhkar_fatima" }
        val generic = tasbeehThisMonth.sumOf { it.totalCount }

        val duasZiyarats = aamalThisMonth.count { 
            it.activityId.startsWith("dua_") || it.activityId.startsWith("ziyarat_") 
        }

        CapsuleStats(
            totalPrayed = prayed,
            totalNawafil = nawafil,
            totalQada = qada,
            totalTasbeehZahra = zahra,
            genericTasbeehCounts = generic,
            duasZiyaratsRead = duasZiyarats
        )
    }

    Box(modifier = Modifier.fillMaxSize()) { val paddingVals = PaddingValues(0.dp)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                // Month Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (selectedMonth == 0) {
                                selectedMonth = 11
                                selectedYear -= 1
                            } else {
                                selectedMonth -= 1
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                    }
                    
                    Text(
                        text = displayMonth,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    IconButton(
                        onClick = {
                            val currentCal = Calendar.getInstance()
                            if (selectedYear < currentCal.get(Calendar.YEAR) || 
                                (selectedYear == currentCal.get(Calendar.YEAR) && selectedMonth < currentCal.get(Calendar.MONTH))) {
                                if (selectedMonth == 11) {
                                    selectedMonth = 0
                                    selectedYear += 1
                                } else {
                                    selectedMonth += 1
                                }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                    }
                }
            }

            item {
                // The Capsule Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (currentLang == "ur") "آپ کی ماهانه روحانی پیشرفت" else "Your Monthly Spiritual Progress",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Stats Row 1
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatItem(
                                    label = if (currentLang == "ur") "واجب نمازیں" else "Wajib Prayers",
                                    value = stats.totalPrayed.toString(),
                                    color = Color(0xFF00C853)
                                )
                                StatItem(
                                    label = if (currentLang == "ur") "نوافل و نمازِ شب" else "Nawafil & Shab",
                                    value = stats.totalNawafil.toString(),
                                    color = Color(0xFF00B0FF)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Stats Row 2
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatItem(
                                    label = if (currentLang == "ur") "تسبیح تعداد" else "Tasbeeh Counts",
                                    value = stats.genericTasbeehCounts.toString(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                StatItem(
                                    label = if (currentLang == "ur") "تسبیحِ زہراؑ پڑھی" else "Tasbeeh-e-Zahra (SA)",
                                    value = "${stats.totalTasbeehZahra} ${if(currentLang=="ur") "مرتبہ" else "times"}",
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Stats Row 3
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                StatItem(
                                    label = if (currentLang == "ur") "دعائیں/زیارات" else "Duas/Ziyarats",
                                    value = stats.duasZiyaratsRead.toString(),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))

                            // Horizontal Bar Chart
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = if (currentLang == "ur") "ماہانہ جائزہ" else "Monthly Overview",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                                    
                                    // Namaz progress
                                    val namazGoal = daysInMonth * 5
                                    val namazProgress = (stats.totalPrayed.toFloat() / namazGoal.toFloat()).coerceIn(0f, 1f)
                                    HorizontalBarItem(
                                        label = if (currentLang == "ur") "واجب نماز" else "Wajib Namaz",
                                        valueText = "${stats.totalPrayed}/$namazGoal",
                                        progress = namazProgress,
                                        color = Color(0xFF00C853)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Nawafil progress
                                    val nawafilGoal = daysInMonth * 6
                                    val nawafilProgress = (stats.totalNawafil.toFloat() / nawafilGoal.toFloat()).coerceIn(0f, 1f)
                                    HorizontalBarItem(
                                        label = if (currentLang == "ur") "نوافل و نمازِ شب" else "Nawafil & Shab",
                                        valueText = "${stats.totalNawafil}",
                                        progress = nawafilProgress,
                                        color = Color(0xFF00B0FF)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Ziyarat progress
                                    val ziyaratGoal = daysInMonth
                                    val ziyaratProgress = (stats.duasZiyaratsRead.toFloat() / ziyaratGoal.toFloat()).coerceIn(0f, 1f)
                                    HorizontalBarItem(
                                        label = if (currentLang == "ur") "زیارات و دعائیں" else "Ziyarat & Duas",
                                        valueText = "${stats.duasZiyaratsRead}/$ziyaratGoal",
                                        progress = ziyaratProgress,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Tasbeeh progress (dynamic goal)
                                    val tasbeehTotal = stats.genericTasbeehCounts + (stats.totalTasbeehZahra * 100) // approx
                                    val tasbeehGoal = maxOf(1000, ((tasbeehTotal / 1000) + 1) * 1000)
                                    val tasbeehProgress = (tasbeehTotal.toFloat() / tasbeehGoal.toFloat()).coerceIn(0f, 1f)
                                    HorizontalBarItem(
                                        label = if (currentLang == "ur") "تسبیح" else "Tasbeeh",
                                        valueText = "$tasbeehTotal",
                                        progress = tasbeehProgress,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            val motivationalText = if (currentLang == "ur") 
                                "ماشاءاللہ! اس مہینے آپ نے بہترین عبادات کیں۔ اپنی یہ نیکیاں محفوظ رکھیں یا دوسروں سے شیئر کریں۔"
                            else 
                                "Masha'Allah! You've had a great spiritual journey this month. Keep up the consistency."
                                
                            Text(
                                text = motivationalText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f),
                                textAlign = TextAlign.Center,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        shareCapsuleImage(context, stats, displayMonth, currentLang)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentLang == "ur") "تصویر کے طور پر شیئر کریں" else "Share as Image Card",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val shareText = if (currentLang == "ur") {
                            "✨ میرا روحانی ٹائم کیپسول ($displayMonth) ✨\n" +
                            "اس مہینے کی عبادات:\n" +
                            "• واجب نمازیں ادا کیں: ${stats.totalPrayed}\n" +
                            "• نوافل و نمازِ شب: ${stats.totalNawafil}\n" +
                            "• تسبیح کی تعداد: ${stats.genericTasbeehCounts}\n" +
                            "• تسبیحِ زہراؑ: ${stats.totalTasbeehZahra} مرتبہ\n" +
                            "• دعائیں و زیارات: ${stats.duasZiyaratsRead}\n\n" +
                            "Shia Pulse App کے ذریعے شیئر کیا گیا۔"
                        } else {
                            "✨ My Spiritual Time Capsule ($displayMonth) ✨\n" +
                            "This month's spiritual journey:\n" +
                            "• Wajib Prayers Offered: ${stats.totalPrayed}\n" +
                            "• Nawafil & Shab Offered: ${stats.totalNawafil}\n" +
                            "• Tasbeeh Counts: ${stats.genericTasbeehCounts}\n" +
                            "• Tasbeeh-e-Zahra (SA): ${stats.totalTasbeehZahra} times\n" +
                            "• Duas & Ziyarats: ${stats.duasZiyaratsRead}\n\n" +
                            "Shared via Shia Pulse App."
                        }
                        
                        shareCapsule(context, shareText)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentLang == "ur") "ٹیکسٹ کے طور پر شیئر کریں" else "Share as Text",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (currentLang == "ur") "اپنے تک پرائیویٹ رکھیں" else "Keep it Private",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(16.dp)
            .width(120.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun shareCapsule(context: Context, text: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Time Capsule")
    context.startActivity(shareIntent)
}

private fun shareCapsuleImage(context: Context, stats: CapsuleStats, displayMonth: String, currentLang: String) {
    val width = 1080
    val height = 1350
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    // Background gradient
    val bgGradient = android.graphics.LinearGradient(
        0f, 0f, 0f, height.toFloat(),
        android.graphics.Color.parseColor("#1A237E"), // Deep purple/blue
        android.graphics.Color.parseColor("#311B92"),
        android.graphics.Shader.TileMode.CLAMP
    )
    paint.shader = bgGradient
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    paint.shader = null
    paint.color = android.graphics.Color.WHITE
    paint.textAlign = android.graphics.Paint.Align.CENTER

    // Title
    paint.textSize = 70f
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    val title = if (currentLang == "ur") "✨ میرا روحانی ٹائم کیپسول ✨" else "✨ My Spiritual Time Capsule ✨"
    canvas.drawText(title, width / 2f, 160f, paint)

    // Month
    paint.textSize = 50f
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    paint.color = android.graphics.Color.parseColor("#B39DDB")
    canvas.drawText(displayMonth, width / 2f, 240f, paint)

    // Draw Stats Bar Chart
    val chartStartY = 380f
    val chartSpacing = 150f
    
    val daysInMonth = java.util.Calendar.getInstance().getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val namazGoal = daysInMonth * 5
    val nawafilGoal = daysInMonth * 6
    val ziyaratGoal = daysInMonth
    val tasbeehTotal = stats.genericTasbeehCounts + (stats.totalTasbeehZahra * 100)
    val tasbeehGoal = maxOf(1000, ((tasbeehTotal / 1000) + 1) * 1000)

    val items = listOf(
        Triple(if (currentLang == "ur") "واجب نمازیں" else "Wajib Prayers", "${stats.totalPrayed}/$namazGoal", (stats.totalPrayed.toFloat() / namazGoal.toFloat()).coerceIn(0f, 1f)),
        Triple(if (currentLang == "ur") "نوافل و نمازِ شب" else "Nawafil & Shab", "${stats.totalNawafil}", (stats.totalNawafil.toFloat() / nawafilGoal.toFloat()).coerceIn(0f, 1f)),
        Triple(if (currentLang == "ur") "زیارات و دعائیں" else "Ziyarat & Duas", "${stats.duasZiyaratsRead}/$ziyaratGoal", (stats.duasZiyaratsRead.toFloat() / ziyaratGoal.toFloat()).coerceIn(0f, 1f)),
        Triple(if (currentLang == "ur") "تسبیح" else "Tasbeeh Counts", "$tasbeehTotal", (tasbeehTotal.toFloat() / tasbeehGoal.toFloat()).coerceIn(0f, 1f))
    )

    var currentY = chartStartY
    for ((label, valueText, progress) in items) {
        // Label
        paint.textSize = 45f
        paint.textAlign = android.graphics.Paint.Align.LEFT
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.color = android.graphics.Color.WHITE
        canvas.drawText(label, 120f, currentY, paint)
        
        // Value
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        paint.color = android.graphics.Color.parseColor("#00E676")
        canvas.drawText(valueText, width - 120f, currentY, paint)
        
        // Bar bg
        currentY += 40f
        paint.color = android.graphics.Color.parseColor("#40FFFFFF")
        val barTop = currentY
        val barBottom = currentY + 30f
        val barLeft = 120f
        val barRight = width - 120f
        canvas.drawRoundRect(android.graphics.RectF(barLeft, barTop, barRight, barBottom), 15f, 15f, paint)
        
        // Bar fg
        paint.color = android.graphics.Color.parseColor("#00E676") // Green
        val progressRight = barLeft + (barRight - barLeft) * progress
        canvas.drawRoundRect(android.graphics.RectF(barLeft, barTop, progressRight, barBottom), 15f, 15f, paint)
        
        currentY += chartSpacing
    }

    // Footer
    paint.textAlign = android.graphics.Paint.Align.CENTER
    paint.textSize = 35f
    paint.color = android.graphics.Color.parseColor("#9E9E9E")
    paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    canvas.drawText("Shared via Shia Pulse App", width / 2f, height - 100f, paint)

    // Save and Share
    try {
        val cachePath = java.io.File(context.cacheDir, "shared_images")
        cachePath.mkdirs()
        val imageFile = java.io.File(cachePath, "capsule_stats.png")
        val stream = java.io.FileOutputStream(imageFile)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val imageUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.mojedarya.shiapulse.fileprovider",
            imageFile
        )

        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, imageUri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Image"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
private fun HorizontalBarItem(
    label: String,
    valueText: String,
    progress: Float,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
        }
    }
}
