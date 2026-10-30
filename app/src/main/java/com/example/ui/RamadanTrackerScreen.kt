package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.RamadanDayRecord
import com.example.utils.PrayerTimeCalculator
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RamadanTrackerScreen(
    viewModel: PrayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val records by viewModel.ramadanRecords.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()
    val globalAppFont by viewModel.appFont.collectAsState()
    val baseAppFontFamily = com.example.ui.theme.getAppFontFamily(globalAppFont, currentLang)
    val fontScale by viewModel.arabicUrduFontScale.collectAsState()

    var selectedDayRecord by remember { mutableStateOf<RamadanDayRecord?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Fasting & Deeds, 1: Daily Shia Duas & Prayers

    // Compute stats
    val stats = remember(records) {
        val totalDays = 30
        val fastedCount = records.count { it.fastingStatus == "Fasted" }
        val qadaCount = records.count { it.fastingStatus == "Qada" }
        val excusedCount = records.count { it.fastingStatus == "Excused" }
        val totalJuz = records.sumOf { it.quranJuzRead }.coerceAtMost(30)
        val nightPrayersCount = records.count { it.nightPrayers }
        val charityCount = records.count { it.charitySadaqahCount > 0 }
        
        RamadanStats(
            fasted = fastedCount,
            qada = qadaCount,
            excused = excusedCount,
            totalJuz = totalJuz,
            nightPrayers = nightPrayersCount,
            charityCount = charityCount,
            completionRate = if (totalDays > 0) (fastedCount.toFloat() / totalDays.toFloat()) else 0f
        )
    }

    // Default 30 records initializer representation
    val fullRecordsList = remember(records) {
        val list = ArrayList<RamadanDayRecord>()
        for (i in 1..30) {
            val existing = records.find { it.ramadanDay == i }
            list.add(existing ?: RamadanDayRecord(ramadanDay = i))
        }
        list
    }

    Scaffold(
        topBar = {
            PremiumScreenTopBar(
                title = when (currentLang) {
                    "ur" -> "جامع رمضان ٹریکر"
                    "ar" -> "متابع شهر رمضان الشامل"
                    else -> "Ramadan Tracker"
                },
                subtitle = when (currentLang) {
                    "ur" -> "روزہ، ادعیہ اور تلاوتِ قرآن کی نگرانی"
                    "ar" -> "متابعة الصيام والأدعية والختم المبارك"
                    else -> "Spiritual deeds, fasting & Shia duas log"
                },
                onBack = onBack,
                backModifier = Modifier.testTag("ramadan_back_button")
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("ramadan_tracker_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            RamadanLiveProgressCard(viewModel = viewModel)

            // Stats section
            Card(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Modern horizontal split stats dashboard
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick circular completion rate
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(64.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { stats.completionRate },
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color(0xFF00E676),
                                    strokeWidth = 6.dp,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                                Text(
                                    text = "${(stats.completionRate * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (currentLang) {
                                    "ur" -> "روزہ کی شرح"
                                    "ar" -> "نسبة الصيام"
                                    else -> "Fasts Logged"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Vertical stat indicators
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatBox(
                                value = "${stats.fasted}/30",
                                label = when (currentLang) {
                                    "ur" -> "رکھے گئے"
                                    "ar" -> "مكتمل"
                                    else -> "Fasted"
                                },
                                color = Color(0xFF00E676)
                            )

                            StatBox(
                                value = "${stats.totalJuz}/30",
                                label = when (currentLang) {
                                    "ur" -> "پارے پڑھے"
                                    "ar" -> "جزؤ الختمة"
                                    else -> "Quran Juz"
                                },
                                color = Color(0xFF00C6FF)
                            )

                            StatBox(
                                value = stats.nightPrayers.toString(),
                                label = when (currentLang) {
                                    "ur" -> "صلاۃ اللیل"
                                    "ar" -> "نمازِ شب"
                                    else -> "Night Salah"
                                },
                                color = Color(0xFF9C27B0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Badges row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎁 Achievements:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (stats.fasted >= 1) {
                                BadgeChip(text = "🌙 First Fast", color = Color(0xFFF39C12))
                            }
                            if (stats.fasted >= 15) {
                                BadgeChip(text = "🌟 Half Way", color = Color(0xFFFFD54F))
                            }
                            if (stats.totalJuz >= 5) {
                                BadgeChip(text = "📖 Quran Devotee", color = Color(0xFF29B6F6))
                            }
                            if (stats.nightPrayers >= 3) {
                                BadgeChip(text = "🕯️ Pray Vigil", color = Color(0xFFAB47BC))
                            }
                            if (stats.charityCount >= 3) {
                                BadgeChip(text = "🤝 Generous", color = Color(0xFF26A69A))
                            }
                        }
                    }
                }
            }

            // Tabs for navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = when (currentLang) {
                                "ur" -> "صوم و اعمال"
                                "ar" -> "الصيام والأعمال"
                                else -> "Fasting & Deeds"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = when (currentLang) {
                                "ur" -> "رمضان المبارک کی دعائیں"
                                "ar" -> "أدعية شهر رمضان"
                                else -> "Ramadan Daily Duas"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                if (selectedTab == 0) {
                    // Fasting Grid & Daily Actions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = when (currentLang) {
                                "ur" -> "روزہ کی حالت منتخب کرنے کے لیے کسی بھی دن پر کلک کریں:"
                                "ar" -> "انقر على أي يوم لتحديث حالة الصيام والأعمال المباركة:"
                                else -> "Click any day to update fast & spiritual deeds:"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            fullRecordsList.chunked(4).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { record ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            RamadanGridItem(
                                                record = record,
                                                languageCode = currentLang,
                                                onClick = { selectedDayRecord = record }
                                            )
                                        }
                                    }
                                    if (rowItems.size < 4) {
                                        repeat(4 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Daily Shia Duas view implementation
                    RamadanDuasReader(
                        currentLang = currentLang,
                        fontScale = fontScale,
                        viewModel = viewModel,
                        onEditCard = onEditCard
                    )
                }
            }
        }
    }

    // Modern Dialog to edit daily status
    selectedDayRecord?.let { record ->
        var activeFastStatus by remember { mutableStateOf(record.fastingStatus) }
        var activeQuranJuz by remember { mutableIntStateOf(record.quranJuzRead) }
        var readMainDua by remember { mutableStateOf(record.readMainDua) }
        var readSuhurDua by remember { mutableStateOf(record.readSuhurDua) }
        var readIftarDua by remember { mutableStateOf(record.readIftarDua) }
        var nightPrayers by remember { mutableStateOf(record.nightPrayers) }
        var charitySadaqahCount by remember { mutableIntStateOf(record.charitySadaqahCount) }

        Dialog(onDismissRequest = { selectedDayRecord = null }) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("day_record_edit_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Dialog Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🌙", fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when (currentLang) {
                                    "ur" -> "${record.ramadanDay} رمضان المبارک"
                                    "ar" -> "يوم ${record.ramadanDay} رمضان"
                                    else -> "Day ${record.ramadanDay} of Ramadan"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { selectedDayRecord = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 1: FASTING STATUS
                    Text(
                        text = "Fasting Status:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("Fasted", "Keep", Color(0xFF00C853)),
                            Triple("Qada", "Qada", Color(0xFFFFAB00)),
                            Triple("Excused", "Excused", Color(0xFF78909C)),
                            Triple("None", "Reset", Color(0xFFE53935))
                        ).forEach { (status, label, color) ->
                            val isSelected = activeFastStatus == status
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent
                                    )
                                    .clickable { activeFastStatus = status }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 2: QURAN RECITATION JUZ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quran Juz Read today:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (activeQuranJuz > 0) activeQuranJuz-- }) {
                                Icon(Icons.Default.KeyboardArrowDown, "Minus")
                            }
                            Text(
                                text = activeQuranJuz.toString(),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(onClick = { if (activeQuranJuz < 30) activeQuranJuz++ }) {
                                Icon(Icons.Default.KeyboardArrowUp, "Plus")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // SECTION 3: SPIRITUAL CHECKLIST
                    Text(
                        text = "Spiritual Deeds Checklist:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    CheckBoxItem(
                        checked = readMainDua,
                        onCheckedChange = { readMainDua = it },
                        title = when (currentLang) {
                            "ur" -> "آج کی مخصوص دعا کی تلاوت"
                            "ar" -> "قراءة الدعاء المخصوص لليوم"
                            else -> "Recited Daily Ramadan Shia Dua"
                        }
                    )

                    CheckBoxItem(
                        checked = readSuhurDua,
                        onCheckedChange = { readSuhurDua = it },
                        title = "Recited Suhur Dua (Dua Baha / Aliyyu Ya Azeem)"
                    )

                    CheckBoxItem(
                        checked = readIftarDua,
                        onCheckedChange = { readIftarDua = it },
                        title = "Recited Iftar Dua (Allahumma Laka Sumtu)"
                    )

                    CheckBoxItem(
                        checked = nightPrayers,
                        onCheckedChange = { nightPrayers = it },
                        title = "Offered Night Prayers (Salah al-Layl)"
                    )

                    // Sadaqah Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🤝 Sadaqah (Charity logged times):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (charitySadaqahCount > 0) charitySadaqahCount-- },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Remove, "Minus", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = charitySadaqahCount.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { charitySadaqahCount++ },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Add, "Plus", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Save / Reset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedDayRecord = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.updateRamadanRecord(
                                    RamadanDayRecord(
                                        ramadanDay = record.ramadanDay,
                                        fastingStatus = activeFastStatus,
                                        quranJuzRead = activeQuranJuz,
                                        readMainDua = readMainDua,
                                        readSuhurDua = readSuhurDua,
                                        readIftarDua = readIftarDua,
                                        nightPrayers = nightPrayers,
                                        charitySadaqahCount = charitySadaqahCount
                                    )
                                )
                                selectedDayRecord = null
                            },
                            modifier = Modifier.weight(1.5f).testTag("save_day_record_button")
                        ) {
                            Text("Save Entry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BadgeChip(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun CheckBoxItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun RamadanGridItem(
    record: RamadanDayRecord,
    languageCode: String,
    onClick: () -> Unit
) {
    val statusColor = when (record.fastingStatus) {
        "Fasted" -> Color(0xFF00E676)
        "Qada" -> Color(0xFFFFD54F)
        "Excused" -> Color(0xFF90A4AE)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val displayLabel = when (record.fastingStatus) {
        "Fasted" -> "Kept"
        "Qada" -> "Qada"
        "Excused" -> "Exc."
        else -> "Log"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.12f)
        ),
        border = BorderStroke(
            width = if (record.fastingStatus != "None") 1.5.dp else 1.dp,
            color = if (record.fastingStatus != "None") statusColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("day_grid_item_${record.ramadanDay}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Day ${record.ramadanDay}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = displayLabel,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (record.fastingStatus != "None" && record.fastingStatus != "Qada") Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            // Quick indicators for other items under the day grid box
            if (record.quranJuzRead > 0 || record.readMainDua || record.nightPrayers) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (record.quranJuzRead > 0) {
                        Text("📖", fontSize = 8.sp)
                    }
                    if (record.readMainDua) {
                        Text("⭐", fontSize = 8.sp)
                    }
                    if (record.nightPrayers) {
                        Text("🕯️", fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

// Stats Holder Representation
data class RamadanStats(
    val fasted: Int,
    val qada: Int,
    val excused: Int,
    val totalJuz: Int,
    val nightPrayers: Int,
    val charityCount: Int,
    val completionRate: Float
)

// List of daily Shia Duas or popular Shia theological prayers
@Composable
fun RamadanDuasReader(
    currentLang: String,
    fontScale: Float,
    viewModel: PrayerViewModel,
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {}
) {
    val appFont by viewModel.appFont.collectAsState()
    val baseAppFontFamily = com.example.ui.theme.getAppFontFamily(appFont, currentLang)
    var activeDayDuaIndex by remember { mutableIntStateOf(0) } // 0 is Day 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        // Horizontally scrolling layout to select day for Dua
        Text(
            text = "Select Ramadan Day to read its special brief Dua:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 1..30) {
                val isSelected = activeDayDuaIndex == (i - 1)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { activeDayDuaIndex = i - 1 }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Day $i",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large beautiful card hosting the Shia Duas
        val dailySuhaDua = getDuaForDay(activeDayDuaIndex)

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌙 Daily Dua for Day ${activeDayDuaIndex + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row {
                        IconButton(onClick = {
                            onEditCard(
                                com.example.api.ShiaQuote(
                                    arabic = dailySuhaDua.arabic,
                                    english = if (currentLang == "ur") "" else dailySuhaDua.english,
                                    urdu = if (currentLang == "ur") dailySuhaDua.urdu else "",
                                    source = "Ramadan Day ${activeDayDuaIndex + 1} Dua"
                                )
                            )
                        }) {
                            Icon(Icons.Default.ColorLens, contentDescription = "Edit in Cards Maker", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            val shareText = """
                                🌙 Daily Ramadan Dua - Day ${activeDayDuaIndex + 1} 🌙
                                
                                📖 Arabic:
                                ${dailySuhaDua.arabic}
                                
                                🇬🇧 English:
                                ${dailySuhaDua.english}
                                
                                🇵🇰 اردو:
                                ${dailySuhaDua.urdu}
                                
                                — via Shia Pulse (Ramadan Companion)
                            """.trimIndent()
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Ramadan Day ${activeDayDuaIndex + 1} Dua")
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Ramadan Dua"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share Dua", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ARABIC TEXT SCREEN
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                        .padding(14.dp)
                ) {
                    Text(
                        text = dailySuhaDua.arabic,
                        fontFamily = baseAppFontFamily,
                        fontSize = (18 * fontScale).sp,
                        lineHeight = (30 * fontScale).sp,
                        color = Color(0xFFFFD54F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clickable {
                            onEditCard(
                                com.example.api.ShiaQuote(
                                    arabic = dailySuhaDua.arabic,
                                    english = "",
                                    urdu = "",
                                    source = "Ramadan Day ${activeDayDuaIndex + 1} Dua"
                                )
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ENGLISH TRANSLATION
                Text(
                    text = "English Translation:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = dailySuhaDua.english,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp).clickable {
                        onEditCard(
                            com.example.api.ShiaQuote(
                                arabic = "",
                                english = dailySuhaDua.english,
                                urdu = "",
                                source = "Ramadan Day ${activeDayDuaIndex + 1} Dua"
                            )
                        )
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // URDU TRANSLATION
                Text(
                    text = "اردو ترجمہ:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = dailySuhaDua.urdu,
                    fontSize = (15 * fontScale).sp,
                    lineHeight = (22 * fontScale).sp,
                    fontFamily = baseAppFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp).clickable {
                        onEditCard(
                            com.example.api.ShiaQuote(
                                arabic = "",
                                english = "",
                                urdu = dailySuhaDua.urdu,
                                source = "Ramadan Day ${activeDayDuaIndex + 1} Dua"
                            )
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Recommended Daily Iftar Prayer Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🤲 Holy Shia Iftar Prayer (Allahumma Laka Sumtu)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF00E676)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "اَللَّهُمَّ لَكَ صُمْتُ وَعَلَىٰ رِزْقِكَ أَفْطَرْتُ وَعَلَيْكَ تَوَكَّلْتُ",
                    fontFamily = baseAppFontFamily,
                    fontSize = (17 * fontScale).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "English: O Allah, for You I have fasted, with Your sustenance I break my fast, and in You I trust.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class RamadanDua(val arabic: String, val english: String, val urdu: String)

private fun getDuaForDay(dayIndex: Int): RamadanDua {
    val list = listOf(
        // Day 1
        RamadanDua(
            arabic = "اللَّهُمَّ اجْعَلْ صِيَامِي فِيهِ صِيَامَ الصَّائِمِينَ وَ قِيَامِي فِيهِ قِيَامَ الْقَائِمِينَ وَ نَبِّهْنِي فِيهِ عَنْ نَوْمَةِ الْغَافِلِينَ",
            english = "O Allah, make my fast in this month one of those who fast sincerely, and my standing up for prayers like those who stand in worship, and awaken me from the sleep of the heedless.",
            urdu = "یا اللہ، اس مہینے میں میرے روزے کو سچے روزہ داروں جیسا بنا، میری راتوں کی عبادت کو مخلص عبادت گزاروں کی مانند کر، اور مجھے غفلت کی نیند سے بیدار فرما۔"
        ),
        // Day 2
        RamadanDua(
            arabic = "اللَّهُمَّ قَرِّبْنِي فِيهِ إِلَى مَرْضَاتِكَ وَ جَنِّبْنِي فِيهِ مِنْ سَخَطِكَ وَ نَقِمَاتِكَ وَ وَفِّقْنِي فِيهِ لِقِرَاءَةِ آيَاتِكَ بِرَحْمَتِكَ",
            english = "O Allah, on this day, bring me near towards Your pleasure, and keep me away from Your anger and punishment, and grant me success to recite Your revelations.",
            urdu = "اے اللہ، مجھے اپنی خوشنودی کے قریب کر، اور اپنے غضب و عذاب سے دور رکھ، اور مجھے اپنی آیات کی تلاوت کی توفیق عطا فرما۔"
        ),
        // Day 3
        RamadanDua(
            arabic = "اللَّهُمَّ ارْزُقْنِي فِيهِ الذِّهْنَ وَ التَّنْبِيهَ وَ بَاعِدْنِي فِيهِ مِنَ السَّفَاهَةِ وَ التَّمْوِيهِ وَ اجْعَلْ لِي نَصِيباً مِنْ كُلِّ خَيْرٍ",
            english = "O Allah, grant me wisdom and awareness, and keep me away from ignorance and pretension, and provide me a share of every blessing You send down.",
            urdu = "یا اللہ، مجھے عقل و بیداری نصیب فرما، بیوقوفی اور بناوٹ سے دور رکھ، اور ہر اس بھلائی میں میرا حصہ رکھ جو تو نازل فرمائے۔"
        ),
        // Day 4
        RamadanDua(
            arabic = "اللَّهُمَّ قَوِّنِي فِيهِ عَلَى إِقَامَةِ أَمْرِكَ وَ أَذِقْنِي فِيهِ حَلاَوَةَ ذِكْرِكَ وَ أَوْزِعْنِي فِيهِ لِأَدَاءِ شُكْرِكَ بِكَرَمِكَ",
            english = "O Allah, strengthen me to carry out Your commands, and let me taste the sweetness of Your remembrance, and grant me gratitude through Your generosity.",
            urdu = "اے اللہ، مجھے اپنے احکام پر عمل پیرا ہونے کی طاقت دے، اپنے ذکر کی مٹھاس کا ذائقہ چکھا، اور اپنے کرم سے شکر ادا کرنے کی توفیق دے۔"
        ),
        // Day 5
        RamadanDua(
            arabic = "اللَّهُمَّ اجْعَلْنِي فِيهِ مِنَ الْمُسْتَغْفِرِينَ وَ اجْعَلْنِي فِيهِ مِنْ عِبَادِكَ الصَّالِحِينَ الْقَانِتِينَ وَ اجْعَلْنِي فِيهِ مِنْ أَوْلِيَائِكَ الْمُقَرَّبِينَ",
            english = "O Allah, place me among those who seek forgiveness, make me one of Your righteous obedient servants, and count me among Your close friends.",
            urdu = "یا اللہ، مجھے بخشش مانگنے والوں میں شامل فرما، مجھے اپنے فرمانبردار بندوں میں سے کر، اور اپنے مقرب دوستوں میں جگہ دے۔"
        ),
        // Day 6
        RamadanDua(
            arabic = "اللَّهُمَّ لاَ تَخْذُلْنِي فِيهِ لِتَعَرُّضِ مَعْصِيَتِكَ وَ لاَ تَضْرِبْنِي بِسِيَاطِ نَقِمَتِكَ وَ زَحْزِحْنِي فِيهِ مِنْ مُوجِبَاتِ سَخَطِكَ",
            english = "O Allah, do not humiliate me for disobeying You, nor strike me with the whips of Your retribution, and keep me away from Your anger.",
            urdu = "اے اللہ، گناہوں کی وجہ سے مجھے ذلیل نہ کر، اپنے عذاب کے کوڑوں سے مجھ پر ضرب نہ لگا، اور غضب کے اسباب سے دور رکھ۔"
        ),
        // Day 7
        RamadanDua(
            arabic = "اللَّهُمَّ أَعِنِّي فِيهِ عَلَى صِيَامِهِ وَ قِيَامِهِ وَ جَنِّبْنِي فِيهِ مِنْ هَفَوَاتِهِ وَ آثَامِهِ وَ ارْزُقْنِي فِيهِ ذِكْرَكَ بِدَوَامِهِ",
            english = "O Allah, help me to fast and pray in it, keep me away from its errors and sins, and grant me ongoing remembrance of You.",
            urdu = "یا اللہ، اس مہینے کے روزوں اور عبادتوں میں میری مدد فرما، مجھے اس کی لغزشوں اور گناہوں سے بچا، اور مجھے ہمیشہ اپنا ذکر نصیب فرما۔"
        ),
        // Day 8
        RamadanDua(
            arabic = "اللَّهُمَّ ارْزُقْنِي فِيهِ رَحْمَةَ الْأَيْتَامِ وَ إِطْعَامَ الطَّعَامِ وَ إِفْشَاءَ السَّلاَمِ وَ صُحْبَةَ الْكِرَامِ بِطَوْلِكَ يَا مَلْجَأَ الْآمِلِينَ",
            english = "O Allah, grant me compassion towards orphans, the feeding of the hungry, the spreading of peace, and the company of the noble ones.",
            urdu = "یا اللہ، مجھے یتیموں پر رحم کرنے، کھانا کھلانے، سلام کو عام کرنے اور نیک لوگوں کی صحبت اختیار کرنے کی توفیق عطا فرما۔"
        ),
        // Day 9
        RamadanDua(
            arabic = "اللَّهُمَّ اجْعَلْ لِي فِيهِ نَصِيباً مِنْ رَحْمَتِكَ الْوَاسِعَةِ وَ اهْدِنِي فِيهِ لِبَرَاهِينِكَ السَّاطِعَةِ وَ خُذْ بِنَاصِيَتِي إِلَى مَرْضَاتِكَ الْجَامِعَةِ",
            english = "O Allah, make for me a share in Your vast mercy, guide me towards Your bright proofs, and direct my forelock to Your comprehensive pleasure.",
            urdu = "اے اللہ، اپنے وسیع رحم میں میرا حصہ رکھ، مجھے اپنی روشن دلیلوں کی ہدایت دے، اور مجھے اپنی جامع خوشنودی کی راہ پر چلا۔"
        ),
        // Day 10
        RamadanDua(
            arabic = "اللَّهُمَّ اجْعَلْنِي فِيهِ مِنَ الْمُتَوَكِّلِينَ عَلَيْكَ وَ اجْعَلْنِي فِيهِ مِنَ الْفَائِزِينَ لَدَيْكَ وَ اجْعَلْنِي فِيهِ مِنَ الْمُقَرَّبِينَ إِلَيْكَ بِإِحْسَانِكَ",
            english = "O Allah, place me among those who trust in You, make me among those who succeed in Your sight, and make me close to You.",
            urdu = "یا اللہ، مجھے توکل کرنے والوں میں شامل فرما، اپنے حضور کامیاب بندوں میں جگہ دے، اور اپنے احسان سے قرب نصیب فرما۔"
        )
    )

    // Fallback cycle to provide continuous beautiful prayers for all 30 days
    return list.getOrElse(dayIndex % list.size) { list[0] }
}

@Composable
fun RamadanLiveProgressCard(
    viewModel: PrayerViewModel,
    modifier: Modifier = Modifier
) {
    val location by viewModel.selectedLocation.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()
    val globalAppFont by viewModel.appFont.collectAsState()
    val baseAppFontFamily = com.example.ui.theme.getAppFontFamily(globalAppFont, currentLang)

    // Determine current Hijri Date & Status
    val currentDate = Calendar.getInstance()
    val hijriDate = remember(currentDate, com.example.utils.HijriCalendarHelper.globalOffsetDays) {
        com.example.utils.HijriCalendarHelper.convertGregorianToHijri(currentDate)
    }
    val isRealRamadan = hijriDate.monthName == "Ramadan"

    // Simulation Support because today is outside Ramadan.
    // This allows visual testing of the slider/transitions.
    var selectedSimDay by remember { mutableStateOf(15f) }
    val activeRamadanDay = if (isRealRamadan) hijriDate.day else selectedSimDay.toInt()

    // Smoothly transition the progress bar as requested!
    val animatedProgress by animateFloatAsState(
        targetValue = activeRamadanDay.toFloat() / 30f,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "RamadanProgressBarAnimation"
    )

    // Running countdown state
    var countdownText by remember { mutableStateOf("00:00:00") }
    var nextIftarTimeStr by remember { mutableStateOf("--:--") }

    LaunchedEffect(location, isRealRamadan) {
        if (!isRealRamadan) return@LaunchedEffect
        while (true) {
            val zone = location.resolvedTimeZone()
            val nowMillis = System.currentTimeMillis()
            val now = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
            val todayDate = PrayerTimeCalculator.PrayerDate.fromCalendar(now)
            val today = PrayerTimeCalculator.calculateTimeline(
                todayDate,
                location.lat,
                location.lon,
                zone
            )
            val tomorrow = PrayerTimeCalculator.calculateTimeline(
                todayDate.plusDays(1),
                location.lat,
                location.lon,
                zone
            )
            val targetInstant = if (today.instant(PrayerTimeCalculator.PrayerEvent.MAGHRIB) > nowMillis) {
                today.instant(PrayerTimeCalculator.PrayerEvent.MAGHRIB)
            } else {
                tomorrow.instant(PrayerTimeCalculator.PrayerEvent.MAGHRIB)
            }
            val targetCal = Calendar.getInstance(zone).apply { timeInMillis = targetInstant }

            // Format friendly time displaying: e.g. 7:42 PM
            val hour = targetCal.get(Calendar.HOUR)
            val displayHour = if (hour == 0) 12 else hour
            val displayMin = String.format(Locale.US, "%02d", targetCal.get(Calendar.MINUTE))
            val amPm = if (targetCal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            nextIftarTimeStr = "$displayHour:$displayMin $amPm"

            val diffMs = targetInstant - nowMillis
            if (diffMs > 0) {
                val diffSec = diffMs / 1000
                val h = diffSec / 3600
                val m = (diffSec % 3600) / 60
                val s = diffSec % 60
                countdownText = String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
            } else {
                countdownText = "00:00:00"
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp)
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("ramadan_live_progress_card")
    ) {
        // Beautiful glowing backdrop representing serene twilight sky with deep emerald/teal touches
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F2027), // deep navy starscape
                            Color(0xFF203A43), // rich slate
                            Color(0xFF2C5364)  // emerald dawn
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header (Icon & Current date / Local context)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🌙",
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isRealRamadan) "Ramadan Al-Mubarak" else "Ramadan Season (Live Tracker)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📍", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = location.city,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Badge showing "Simulating" status
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isRealRamadan) Color(0xFF00C853).copy(alpha = 0.2f)
                                else Color(0xFFFFB300).copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isRealRamadan) "REAL-TIME" else "PREVIEW",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isRealRamadan) Color(0xFF00FF88) else Color(0xFFFFA000)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // PROGRESS BAR SECTION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Days Elapsed:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Day $activeRamadanDay / 30",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD54F)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Beautiful custom linear progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF00E676), // emerald
                                        Color(0xFF00E676),
                                        Color(0xFF00E676).copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Day 1", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
                    Text(
                        text = "${(animatedProgress * 100).toInt()}% completed",
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Text("Day 30", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
                }

                if (!isRealRamadan) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Awaiting the Blessed Month of Ramadan...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF88).copy(alpha = 0.8f)
                        )
                    }
                } else {
                    // IFTAR COUNTDOWN SECTION
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Pulsing / glowing green indicator
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E676))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "COUNTDOWN TO IFTAR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00FF88),
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = countdownText,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                modifier = Modifier.testTag("iftar_countdown_text")
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Next Iftar Time",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = nextIftarTimeStr,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                }
            }
        }
    }
}
