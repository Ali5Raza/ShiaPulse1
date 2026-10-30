package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PrayerLog
import com.example.data.QadaTally

data class MonthlyStat(
    val monthKey: String,
    val monthName: String,
    val prayedCount: Int,
    val nawafilCount: Int = 0,
    val qadaCount: Int,
    val skippedCount: Int,
    val totalCount: Int
)

@Composable
fun TrackerScreen(
    viewModel: PrayerViewModel,
    initialTab: Int = 0,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLang by viewModel.appLanguage.collectAsState()
    val displayDateStr by viewModel.displayDateStr.collectAsState()
    val todayLogs by viewModel.todayLogs.collectAsState()
    val qadaTallies by viewModel.qadaTallies.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab) }

    // Tab titles based on language
    val tabs = when (currentLang) {
        "ur" -> listOf("روزانہ ڈائری", "قضا کاؤنٹر", "حساب کتاب", "شرعی احکام")
        "ar" -> listOf("سجل الصلوات", "عداد القضاء", "حساب الفوائت", "أحكام القضاء")
        else -> listOf("Daily Register", "Qadha Tally", "Calculator", "Rules")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PremiumScreenTopBar(
            title = if (currentLang == "ur") "متابع العبادات (ٹریکر)" else if (currentLang == "ar") "متابع العبادات" else "Worship Tracker",
            onBack = onBack
        )

        // Tab Selector Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { 
                        Text(
                            text = title, 
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1
                        ) 
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> DailyRegisterTab(viewModel = viewModel, currentLang = currentLang, displayDateStr = displayDateStr, todayLogs = todayLogs)
                1 -> QadhaTallyTab(viewModel = viewModel, currentLang = currentLang, qadaTallies = qadaTallies)
                2 -> QadhaCalculatorTab(viewModel = viewModel, currentLang = currentLang)
                3 -> QadhaRulesTab(currentLang = currentLang)
            }
        }
    }
}

@Composable
fun DailyRegisterTab(
    viewModel: PrayerViewModel,
    currentLang: String,
    displayDateStr: String,
    todayLogs: List<PrayerLog>
) {
    val prayList = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha", "Nafilah Fajr", "Nafilah Dhuhr", "Nafilah Asr", "Nafilah Maghrib", "Nafilah Isha (Watira)", "Night Prayer (Tahajjud)")

    val selectedDate by viewModel.selectedDate.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 0.dp),
        contentPadding = PaddingValues(top = 0.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tracker Header Calendar
        item {
            TrackerHeaderCalendar(
                selectedDate = selectedDate,
                onDateSelected = { date -> viewModel.selectDate(date) }
            )
        }

        // Export Option Card
        item {
            val context = LocalContext.current
            val allLogs by viewModel.allPrayerLogs.collectAsState()
            val currentTallies by viewModel.qadaTallies.collectAsState()
            
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentLang == "ur") "ڈیٹا ریکارڈ ایکسپورٹ کریں" else if (currentLang == "ar") "تصدير البيانات" else "EXPORT DATA LEDGER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (currentLang == "ur") "اپنی نمازوں کی تاریخ کو فائل میں محفوظ کریں" else if (currentLang == "ar") "حفظ سجل صلواتك في ملف" else "Download prayer history and Qada tallies",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Button(
                        onClick = { shareQadaHistoryCsv(context, allLogs, currentTallies) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Ledger",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentLang == "ur") "ایکسپورٹ" else if (currentLang == "ar") "تصدير" else "Export CSV",
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Daily Prayers Checklist Box
        item {
            val wajibSet = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
            val wajibList = prayList.filter { it in wajibSet }
            val nafilahList = prayList.filter { it !in wajibSet }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = if (currentLang == "ur") "واجب نمازیں (5)" else if (currentLang == "ar") "الصلوات الواجبة (5)" else "OBLIGATORY PRAYERS (5)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )

                wajibList.forEach { name ->
                    val log = todayLogs.firstOrNull { it.prayerName == name }
                    val status = log?.status ?: "NotPrayed"

                    CompactPrayerItem(
                        prayerName = name,
                        status = status,
                        onStatusChange = { newStatus -> viewModel.updatePrayerLog(name, newStatus) },
                        currentLang = currentLang,
                        isObligatory = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (currentLang == "ur") "نوافل و نمازِ شب (6)" else if (currentLang == "ar") "النوافل وصلاة الليل (6)" else "NAWAFIL & NIGHT PRAYER (6)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )

                nafilahList.forEach { name ->
                    val log = todayLogs.firstOrNull { it.prayerName == name }
                    val status = log?.status ?: "NotPrayed"

                    CompactPrayerItem(
                        prayerName = name,
                        status = status,
                        onStatusChange = { newStatus -> viewModel.updatePrayerLog(name, newStatus) },
                        currentLang = currentLang,
                        isObligatory = false
                    )
                }
            }
        }

        // MONTH-WISE SPIRITUAL CHART
        item {
            val allLogs by viewModel.allPrayerLogs.collectAsState()
            
            val monthlyData = remember(allLogs) {
                val wajibSet = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
                allLogs.groupBy { log ->
                    if (log.dateString.length >= 7) log.dateString.substring(0, 7) else "Unknown"
                }.filter { it.key != "Unknown" }
                .map { (monthKey, logs) ->
                    val wajibLogs = logs.filter { it.prayerName in wajibSet }
                    val nawafilLogs = logs.filter { it.prayerName !in wajibSet }

                    val prayed = wajibLogs.count { it.status == "Prayed" || it.status == "Jamaat" }
                    val qada = wajibLogs.count { it.status == "Qada" }
                    val skipped = wajibLogs.count { it.status == "NotPrayed" }
                    val total = prayed + qada + skipped

                    val nawafil = nawafilLogs.count { it.status == "Prayed" || it.status == "Jamaat" }
                    
                    val displayMonth = try {
                        val parser = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                        val formatter = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                        val date = parser.parse(monthKey)
                        if (date != null) formatter.format(date) else monthKey
                    } catch (e: Exception) {
                        monthKey
                    }
                    
                    MonthlyStat(
                        monthKey = monthKey,
                        monthName = displayMonth,
                        prayedCount = prayed,
                        nawafilCount = nawafil,
                        qadaCount = qada,
                        skippedCount = skipped,
                        totalCount = total
                    )
                }.sortedByDescending { it.monthKey }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (currentLang == "ur") "ماہانہ رپورٹ چارٹ" else if (currentLang == "ar") "مخطط التقرير الشهري" else "MONTH-WISE TRACKER CHART",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = if (currentLang == "ur") "ادا و قضا نمازوں کا ماہانہ جائزہ" else if (currentLang == "ar") "نظرة عامة على الصلوات شهرياً" else "Monthly breakdown of prayed vs qada namaz",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Stats",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (monthlyData.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (currentLang == "ur") "ابھی تک کوئی ریکارڈ موجود نہیں" else if (currentLang == "ar") "لا يوجد سجل بعد" else "No history logged yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = if (currentLang == "ur") "چارٹ دیکھنے کیلئے اپنی نمازوں کا ہوم ورک شروع کریں!" else if (currentLang == "ar") "ابدأ تسجيل صلواتك لتوليد التقرير!" else "Start marking daily prayers to generate your monthly chart!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                                )
                            }
                        }
                    } else {
                        var expandedMonthDropdown by remember { mutableStateOf(false) }
                        var selectedMonthKey by remember { mutableStateOf(monthlyData.first().monthKey) }

                        val statToDisplay = monthlyData.find { it.monthKey == selectedMonthKey } ?: monthlyData.first()

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLang == "ur") "مہینہ منتخب کریں" else if (currentLang == "ar") "اختر الشهر" else "Select Month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Box {
                                OutlinedButton(
                                    onClick = { expandedMonthDropdown = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(text = statToDisplay.monthName, fontWeight = FontWeight.Bold)
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Month",
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = expandedMonthDropdown,
                                    onDismissRequest = { expandedMonthDropdown = false }
                                ) {
                                    monthlyData.forEach { stat ->
                                        DropdownMenuItem(
                                            text = { Text(text = stat.monthName, fontWeight = FontWeight.Medium) },
                                            onClick = {
                                                selectedMonthKey = stat.monthKey
                                                expandedMonthDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        val stat = statToDisplay
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stat.monthName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentLang == "ur") "کل ریکارڈ: ${stat.totalCount}" else if (currentLang == "ar") "مجموع الصلوات: ${stat.totalCount}" else "Total Logged: ${stat.totalCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val total = stat.totalCount.toFloat().coerceAtLeast(1f)
                            val prayedPct = stat.prayedCount / total
                            val qadaPct = stat.qadaCount / total
                            val skippedPct = stat.skippedCount / total

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (stat.prayedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(prayedPct.coerceAtLeast(0.01f))
                                            .background(Color(0xFF2E7D32))
                                    )
                                }
                                if (stat.qadaCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(qadaPct.coerceAtLeast(0.01f))
                                            .background(Color(0xFFEF6C00))
                                    )
                                }
                                if (stat.skippedCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(skippedPct.coerceAtLeast(0.01f))
                                            .background(Color(0xFFC62828))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LegendIndicator(
                                    label = (if (currentLang == "ur") "واجب ادا: " else if (currentLang == "ar") "واجب: " else "Wajib: ") + stat.prayedCount, 
                                    color = Color(0xFF2E7D32)
                                )
                                LegendIndicator(
                                    label = (if (currentLang == "ur") "نوافل: " else if (currentLang == "ar") "نوافل: " else "Nafilah: ") + stat.nawafilCount, 
                                    color = Color(0xFF00B0FF)
                                )
                                LegendIndicator(
                                    label = (if (currentLang == "ur") "قضا: " else if (currentLang == "ar") "قضاء: " else "Qada: ") + stat.qadaCount, 
                                    color = Color(0xFFEF6C00)
                                )
                                LegendIndicator(
                                    label = (if (currentLang == "ur") "رہ گئیں: " else if (currentLang == "ar") "فائتة: " else "Skipped: ") + stat.skippedCount, 
                                    color = Color(0xFFC62828)
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
fun QadhaTallyTab(
    viewModel: PrayerViewModel,
    currentLang: String,
    qadaTallies: List<QadaTally>
) {
    val prayList = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    var editTally by remember { mutableStateOf<QadaTally?>(null) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanations Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info, 
                        contentDescription = "Spiritual ledger", 
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (currentLang == "ur") 
                            "یہاں اپنی تمام تر واجب قضا نمازوں کا ریکارڈ مرتب رکھیں۔ کسی بھی شک کی صورت میں کم از کم یقینی تعداد یہاں لکھ لیں۔" 
                            else if (currentLang == "ar")
                            "احتفظ بسجل دقيق لجميع صلواتك الفائتة هنا. في حالة الشك، اختر الحد الأدنى الذي تتيقن منه."
                            else "Maintain your outstanding obligatory prayers and fasts below. Log completions of qadha here to keep your records beautifully organized.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Toolbar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.incrementAllQada() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add skipped day")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (currentLang == "ur") "مکمل قضا دن (+1 سب میں)" else if (currentLang == "ar") "يوم فائت كامل (+1 للكل)" else "Missed Whole Day (+1 all)", 
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { showResetConfirmation = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Ledgers", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (currentLang == "ur") "ری سیٹ کریں" else if (currentLang == "ar") "إعادة ضبط" else "Reset All", 
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Outstanding Prayers Heading
        item {
            Text(
                text = if (currentLang == "ur") "واجب قضا نمازیں" else if (currentLang == "ar") "صلوات القضاء المستحقة" else "OUTSTANDING PRAYERS TALLY",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp
            )
        }

        // List of Prayers Tallies
        prayList.forEach { name ->
            item {
                val tally = qadaTallies.firstOrNull { it.prayerName == name }
                val currentValue = tally?.count ?: 0

                Card(
                    shape = RoundedCornerShape(16.dp),
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
                        Column {
                            Text(
                                text = getLocalizedPrayerName(name, currentLang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentLang == "ur") "$currentValue نمازیں باقی ہیں" else if (currentLang == "ar") "المتبقي: $currentValue صلاة" else "$currentValue prayers remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (currentValue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.modifyQadaCount(name, -1) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(38.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("-1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = currentValue.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (currentValue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Button(
                                onClick = { viewModel.modifyQadaCount(name, 1) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(38.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.modifyQadaCount(name, 10) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier.size(height = 38.dp, width = 44.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+10", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { editTally = QadaTally(name, currentValue) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit, 
                                    contentDescription = "Edit manual",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Outstanding Fasts Heading
        item {
            Text(
                text = if (currentLang == "ur") "قضا روزے" else if (currentLang == "ar") "صيام القضاء المستحق" else "OUTSTANDING RAMADAN FASTS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Fasting Day Tally Row
        item {
            val fastsTally = qadaTallies.firstOrNull { it.prayerName == "Fasting" }
            val currentFasts = fastsTally?.count ?: 0

            Card(
                shape = RoundedCornerShape(16.dp),
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
                    Column {
                        Text(
                            text = if (currentLang == "ur") "روزوں کی قضائیں" else if (currentLang == "ar") "أيام الصيام" else "Fasting Days",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentLang == "ur") "$currentFasts روزے باقی ہیں" else if (currentLang == "ar") "$currentFasts يوماً متبقياً" else "$currentFasts days remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (currentFasts > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.modifyQadaCount("Fasting", -1) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = currentFasts.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (currentFasts > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        Button(
                            onClick = { viewModel.modifyQadaCount("Fasting", 1) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { editTally = QadaTally("Fasting", currentFasts) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit, 
                                contentDescription = "Edit manual fasts",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Tally manual custom AlertDialog
    val tallyBeingEdited = editTally
    if (tallyBeingEdited != null) {
        var newValue by remember(tallyBeingEdited) { mutableStateOf(tallyBeingEdited.count.toString()) }
        
        AlertDialog(
            onDismissRequest = { editTally = null },
            title = { Text(if (currentLang == "ur") "تعداد تبدیل کریں" else if (currentLang == "ar") "تعديل العدد" else "Update Count") },
            text = {
                OutlinedTextField(
                    value = newValue,
                    onValueChange = { newValue = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { 
                        Text(
                            if (tallyBeingEdited.prayerName == "Fasting") {
                                if (currentLang == "ur") "روزے" else if (currentLang == "ar") "الصيام" else "Fasting Days"
                            } else {
                                getLocalizedPrayerName(tallyBeingEdited.prayerName, currentLang)
                            }
                        ) 
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val count = newValue.toIntOrNull() ?: 0
                    val delta = count - tallyBeingEdited.count
                    viewModel.modifyQadaCount(tallyBeingEdited.prayerName, delta)
                    editTally = null
                }) {
                    Text(if (currentLang == "ur") "محفوظ کریں" else if (currentLang == "ar") "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editTally = null }) {
                    Text(if (currentLang == "ur") "منسوخ" else if (currentLang == "ar") "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Reset All Confirmation AlertDialog
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text(if (currentLang == "ur") "تصدیق کیجئے" else if (currentLang == "ar") "تأكيد" else "Confirm Reset") },
            text = { 
                Text(
                    if (currentLang == "ur") 
                        "کیا آپ تمام واجب قضا نمازوں اور روزوں کا ریکارڈ صفر (0) کرنا چاہتے ہیں؟ یہ عمل ناقابل واپسی ہے۔" 
                        else if (currentLang == "ar")
                        "هل تريد حقاً مسح جميع سجلات صلوات القضاء وتصفيرها بالكامل؟ هذا الإجراء لا يمكن إلغاؤه."
                        else "Are you sure you want to reset all your outstanding prayer and fasting records to zero? This action is permanent."
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllQada()
                        val fastsTally = qadaTallies.firstOrNull { it.prayerName == "Fasting" }
                        if (fastsTally != null) {
                            viewModel.modifyQadaCount("Fasting", -fastsTally.count)
                        }
                        showResetConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (currentLang == "ur") "ہاں، تصفیہ کریں" else if (currentLang == "ar") "نعم، إعادة ضبط" else "Yes, Reset All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text(if (currentLang == "ur") "منسوخ" else if (currentLang == "ar") "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun QadhaCalculatorTab(viewModel: PrayerViewModel, currentLang: String) {
    var years by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }
    
    var showDialog by remember { mutableStateOf(false) }
    var calculatedTotal by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (currentLang == "ur") "قضا نمازوں کا حساب کتاب" else if (currentLang == "ar") "حساب صلوات القضاء الفائتة" else "Estimate Missed Prayers",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = if (currentLang == "ur") 
                "اگر آپ کو فوت شدہ نمازوں کی صحیح تعداد معلوم نہیں، تو اندازہ لگائیں کہ آپ نے کتنے سال، مہینے، یا دن نمازیں ترک کیں یا ان کے قضا ہونے کا خدشہ ہے۔\n\nاس حساب سے آپ کے ٹریکر میں یکساں اضافہ کر دیا جائے گا۔" 
                else if (currentLang == "ar")
                "إذا كنت غير متأكد من عدد الصلوات الفائتة بدقة، يمكنك تقدير المدة بالسنوات، الشهور، أو الأيام.\n\nسيقوم هذا بشكل تلقائي بإضافة المجموع المقدر إلى كافة الصلوات في التراكر بالتساوي."
                else "If you are unsure of the exact count of missed prayers, estimate the missed duration below in years, months, and days.\n\nThis calculator will automatically calculate the total days and add that value to all five daily prayer records uniformly. When in doubt, estimate the lower sure amount.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = years,
                    onValueChange = { years = it },
                    label = { Text(if (currentLang == "ur") "سال" else if (currentLang == "ar") "السنوات" else "Years") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = months,
                    onValueChange = { months = it },
                    label = { Text(if (currentLang == "ur") "مہینے" else if (currentLang == "ar") "الشهور" else "Months") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it },
                    label = { Text(if (currentLang == "ur") "دن" else if (currentLang == "ar") "الأيام" else "Days") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Button(
            onClick = {
                val y = years.toIntOrNull() ?: 0
                val m = months.toIntOrNull() ?: 0
                val d = days.toIntOrNull() ?: 0
                
                calculatedTotal = (y * 365) + (m * 30) + d
                if (calculatedTotal > 0) {
                    showDialog = true
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(50.dp)
        ) {
            Text(
                text = if (currentLang == "ur") "حساب لگائیں اور شامل کریں" else if (currentLang == "ar") "احسب وأضف" else "Calculate & Add", 
                fontWeight = FontWeight.Bold
            )
        }
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (currentLang == "ur") "اضافہ کی تصدیق کریں" else if (currentLang == "ar") "تأكيد الإضافة" else "Confirm Grand Addition") },
            text = { 
                Text(
                    if (currentLang == "ur") 
                        "اس حساب کے مطابق آپ کو ہر فرض نماز کی ${calculatedTotal} قضائیں ادا کرنی ہیں۔\n\nکیا آپ انہیں اپنے ٹریکر میں شامل کرنا چاہتے ہیں؟" 
                        else if (currentLang == "ar")
                        "بناءً على هذا التقدير، فإن عدد الفوائت هو ${calculatedTotal} صلاة لكل فرض.\n\nهل تود حقاً إضافة $calculatedTotal صلاة إلى عداداتك؟"
                        else "Based on this calculation, you missed $calculatedTotal days of parameters.\n\nDo you want to add $calculatedTotal to all your prayer tallies in the tracker uniformly?"
                ) 
            },
            confirmButton = {
                Button(onClick = {
                    listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { name ->
                        viewModel.modifyQadaCount(name, calculatedTotal)
                    }
                    showDialog = false
                    years = ""
                    months = ""
                    days = ""
                }) {
                    Text(if (currentLang == "ur") "ٹریکر میں شامل کریں" else if (currentLang == "ar") "أضف إلى المتابع" else "Add to Tracker")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(if (currentLang == "ur") "منسوخ" else if (currentLang == "ar") "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun QadhaRulesTab(currentLang: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (currentLang == "ur") "قضا نماز کے شرعی احکام (فقہ جعفریہ)" else if (currentLang == "ar") "أحكام صلاة القضاء (شيعة الجعفري)" else "Rules of Qadha Prayers (Shia Fiqh)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        RuleCard(
            title = if (currentLang == "ur") "واجبات قضا" else if (currentLang == "ar") "وجوب القضاء" else "Obligation of Qadha",
            desc = if (currentLang == "ur") 
                "کوئی بھی فرض نماز جو جان بوجھ کر، بھولے سے، نیند یا غفلت کی وجہ سے یا بغیر وضو/غسل کے پڑھی گئی ہو، اس کی قضا واجب ہے۔" 
                else if (currentLang == "ar")
                "يجب قضاء الصلوات اليومية المفروضة التي فاتت عمداً أو نسياناً أو لجهل بالمسئلة أو صلاها بلا وضوء أو غسل صحيح."
                else "Any missed obligatory daily prayer must be made up, whether missed intentionally, forgetfully, due to sleep/neglect, or performed without valid Wudu/Ghusl."
        )

        RuleCard(
            title = if (currentLang == "ur") "قضا سے مستثنیٰ صورتیں" else if (currentLang == "ar") "المستثنيات من القضاء" else "Exemptions from Qadha",
            desc = if (currentLang == "ur") 
                "بچپن، جنون، کفر (جس کے بعد اسلام قبول کیا ہو)، خواتین کی مخصوص حالتوں (حیض و نفاس) اور مکمل بے ہوشی (جس میں انسان خود اپنی مرضی سے بے ہوش نہ ہوا ہو) کی حالتوں میں چھوٹی ہوئی نمازوں کی قضا واجب نہیں ہے۔" 
                else if (currentLang == "ar")
                "لا يجب القضاء على ما فات في صغر، أو جنون، أو إغماء غير متعمد لم يفق منه المكلف خلال وقت الفريضة، وكذا الصلوات الفائتة على الكافر الأصلي عند إسلامه، والحائض والنفساء."
                else "Qadha is NOT obligatory for prayers missed due to childhood, insanity, complete non-self-induced unconsciousness (for the entire prayer time), menstruation (Hayd), postpartum (Nifas), or original disbelief prior to accepting Islam."
        )

        RuleCard(
            title = if (currentLang == "ur") "سفر اور حضر کی قضا" else if (currentLang == "ar") "القضاء في السفر والحضر" else "Travel (Qasr) and Home",
            desc = if (currentLang == "ur") 
                "اگر سفر کے دوران قصر کی حالت میں قضا ہوئی ہو تو ہوم ٹاؤن میں بھی قصر (2 رکعت) ہی پڑھے گا۔ اور اگر وطن یا پوری نماز کی حالت میں قضا ہوئی ہو تو سفر میں بھی پوری (4 رکعت) ہی پڑھی جائے گی۔" 
                else if (currentLang == "ar")
                "ما فات قصراً (في السفر) يقضى قصراً (ركعتين) ولو في الحضر، وما فات تماماً (في الحضر) يقضى تماماً (أربع ركعات) ولو في السفر."
                else "A missed shortened prayer (while traveling, Qasr) must be performed shortened (2 Rak'ahs) even when making it up at home. A missed full prayer (at home, Tamam) must be performed in full (4 Rak'ahs) even when making it up while traveling."
        )

        RuleCard(
            title = if (currentLang == "ur") "شک کی صورت میں حساب" else if (currentLang == "ar") "الشك في كثرة الفائتة" else "Doubt in Calculation",
            desc = if (currentLang == "ur") 
                "اگر آپ کو معلوم نہ ہو کہ کتنی نمازیں قضا ہیں، تو اس کم از کم تعداد پر یقین کر کے اس کی قضا کر لیں جس پر یقین ہو۔ مثلاً شک ہو کہ 3 سال کی ہیں یا 4 سال کی، تو 3 سال کی پڑھنا کافی اور شرعی واجبات کو پورا کرنے کیلئے کافی ہے۔" 
                else if (currentLang == "ar")
                "من لا يعلم عدد صلواته الفائتة، يكفي أن يقضي المقدار الأقل الذي يتيقن فواته (مثال: لو دار بين سنة وسنتين، يبنى على الأقل وهو سنة)."
                else "If you are unsure of the exact number of missed prayers, you only need to perform the lesser amount that you are absolutely certain you missed (e.g., if you doubt whether you missed 3 years or 4 years of prayers, making up 3 years is legally sufficient under Shia Fiqh)."
        )

        RuleCard(
            title = if (currentLang == "ur") "قضا پڑھنے کا طریقہ" else if (currentLang == "ar") "كيفية صلاة القضاء" else "Method of Performing Qadha",
            desc = if (currentLang == "ur") 
                "قضا کا طریقہ فجر (2 رکعت)، ظہر (4 رکعت)، عصر (4 رکعت)، مغرب (3 رکعت)، اور عشاء (4 رکعت) بالکل عام نماز جیسا ہے۔ اذان و اقامت کہنا مستحب ہے۔ اگر زیادہ قضا نمازیں یکے بعد دیگرے پڑھنی ہوں تو پہلی میں اذان اور سب میں صرف اقامت کہنا کافی ہے۔" 
                else if (currentLang == "ar")
                "تؤدى صلاة القضاء تماماً مثل الأداء من حيث الموالاة والترتيب والأعداد والركعات. يستحب الأذان والإقامة، وفي قضاء الفوائت المتعددة يكتفى بأذان واحد للأولى وإقامة مستقلة لكل صلاة بعدها."
                else "The method of performing Qadha is identical to daily prayers (Fajr: 2, Dhuhr: 4, Asr: 4, Maghrib: 3, Isha: 4). Adhan and Iqamah are highly recommended. For consecutive Qadha performance, reciting Adhan once for the first prayer and Iqamah for the subsequent ones is sufficient."
        )
    }
}

@Composable
fun RuleCard(title: String, desc: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LegendIndicator(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

fun getLocalizedPrayerName(prayerName: String, lang: String): String {
    return when(lang) {
        "ur" -> when(prayerName.lowercase()) {
            "fajr" -> "فجر"
            "dhuhr" -> "ظہر"
            "asr" -> "عصر"
            "maghrib" -> "مغرب"
            "isha" -> "عشاء"
            "fasting" -> "روزہ"
            "nafilah fajr" -> "نافلہ فجر"
            "nafilah dhuhr" -> "نافلہ ظہر"
            "nafilah asr" -> "نافلہ عصر"
            "nafilah maghrib" -> "نافلہ مغرب"
            "nafilah isha (watira)" -> "نافلہ عشاء / وتیرا"
            "night prayer (tahajjud)" -> "نمازِ شب (تہجد)"
            else -> prayerName
        }
        "ar" -> when(prayerName.lowercase()) {
            "fajr" -> "الفجر"
            "dhuhr" -> "الظهر"
            "asr" -> "العصر"
            "maghrib" -> "المغرب"
            "isha" -> "العشاء"
            "fasting" -> "الصيام"
            "nafilah fajr" -> "نافلة الفجر"
            "nafilah dhuhr" -> "نافلة الظهر"
            "nafilah asr" -> "نافلة العصر"
            "nafilah maghrib" -> "نافلة المغرب"
            "nafilah isha (watira)" -> "نافلة العشاء (الوتيرة)"
            "night prayer (tahajjud)" -> "صلاة الليل"
            else -> prayerName
        }
        else -> prayerName
    }
}

fun shareQadaHistoryCsv(context: android.content.Context, logs: List<PrayerLog>, tallies: List<QadaTally>) {
    val csv = StringBuilder()
    
    csv.append("--- OUTSTANDING QADA TALLY LEDGER ---\n")
    csv.append("Prayer Name,Outstanding Count\n")
    val prayList = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha", "Fasting")
    prayList.forEach { name ->
        val count = tallies.find { it.prayerName == name }?.count ?: 0
        csv.append("$name,$count\n")
    }
    
    csv.append("\n")
    csv.append("--- DAILY PRAYER HISTORY LOGS ---\n")
    csv.append("Date,Prayer Name,Status\n")
    logs.sortedByDescending { it.dateString }.forEach { log ->
        csv.append("${log.dateString},${log.prayerName},${log.status}\n")
    }
    
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Shia Pulse Qada & Prayer History")
            putExtra(android.content.Intent.EXTRA_TEXT, csv.toString())
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Export Qada History CSV"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
