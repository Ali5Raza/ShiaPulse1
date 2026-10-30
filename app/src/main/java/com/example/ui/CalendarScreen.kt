package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.EventType
import com.example.data.MasoomDetails
import com.example.data.MasoomeenData

@Composable
fun CalendarScreen(
    viewModel: PrayerViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    var displayedCalendar by remember { mutableStateOf(java.util.Calendar.getInstance()) }
    var selectedDayNum by remember {
        mutableStateOf<Int?>(java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH))
    }
    var activeTabIndex by remember { mutableStateOf(0) } // 0 = Shia, 1 = International
    var eventQuery by remember { mutableStateOf("") }
    var eventTypeFilter by remember { mutableStateOf<EventType?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val languageCode by viewModel.appLanguage.collectAsState()
    val eventsRevision by com.example.data.IslamicEventManager.eventsRevision.collectAsState()
    val allEvents = remember(context, eventsRevision) { MasoomeenData.getAllEvents(context) }
    val calendarLocale = remember(languageCode) { java.util.Locale.forLanguageTag(languageCode) }
    val filteredShiaEvents = remember(allEvents, eventQuery, eventTypeFilter, languageCode) {
        allEvents.filter { event ->
            val matchesType = eventTypeFilter == null || event.eventType == eventTypeFilter
            val matchesQuery = eventQuery.isBlank() ||
                event.localizedTitle(languageCode).contains(eventQuery, ignoreCase = true) ||
                event.localizedDescription(languageCode).contains(eventQuery, ignoreCase = true) ||
                event.title.contains(eventQuery, ignoreCase = true)
            matchesType && matchesQuery
        }
    }

    fun calendarText(key: String): String = com.example.utils.LocalizationUtility.get(key, languageCode)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Button & Tab Headers
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = calendarText("action_back"))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(calendarText("calendar_history"), style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Events List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                item {
                    val year = displayedCalendar.get(java.util.Calendar.YEAR)
                    val month = displayedCalendar.get(java.util.Calendar.MONTH)
                    val maxDays = displayedCalendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                    
                    val firstDayCal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.YEAR, year)
                        set(java.util.Calendar.MONTH, month)
                        set(java.util.Calendar.DAY_OF_MONTH, 1)
                    }
                    val firstDayOfWeek = firstDayCal.get(java.util.Calendar.DAY_OF_WEEK) // 1 = Sunday, ..., 7 = Saturday
                    
                    val monthName = java.text.SimpleDateFormat("MMMM yyyy", calendarLocale).format(displayedCalendar.time)

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Calendar header with navigation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    val nextCal = displayedCalendar.clone() as java.util.Calendar
                                    nextCal.add(java.util.Calendar.MONTH, -1)
                                    displayedCalendar = nextCal
                                    selectedDayNum = null
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = calendarText("calendar_previous_month"))
                                }
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = monthName.uppercase(calendarLocale),
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = calendarText("calendar_grid_subtitle"),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = {
                                    val nextCal = displayedCalendar.clone() as java.util.Calendar
                                    nextCal.add(java.util.Calendar.MONTH, 1)
                                    displayedCalendar = nextCal
                                    selectedDayNum = null
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = calendarText("calendar_next_month"))
                                }
                            }

                            TextButton(
                                onClick = {
                                    val today = java.util.Calendar.getInstance()
                                    displayedCalendar = today
                                    selectedDayNum = today.get(java.util.Calendar.DAY_OF_MONTH)
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(calendarText("calendar_today"))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Day of week headers
                            val daysOfWeek = localizedWeekdays(languageCode)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                daysOfWeek.forEach { dayName ->
                                    Text(
                                        text = dayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Days Grid
                            val totalCells = (firstDayOfWeek - 1) + maxDays
                            val rows = (totalCells + 6) / 7

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (r in 0 until rows) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        for (c in 0..6) {
                                            val cellIndex = r * 7 + c
                                            val dayNumber = cellIndex - (firstDayOfWeek - 2)
                                            
                                            if (dayNumber in 1..maxDays) {
                                                // Convert to Hijri:
                                                val hijriForDay = com.example.utils.HijriCalendarHelper.convertGregorianToHijri(year, month + 1, dayNumber)
                                                // Find Shia/Hijri events:
                                                val dayEvents = allEvents.filter { matchEvent(hijriForDay.day, hijriForDay.monthName, it) }
                                                // Find Gregorian events:
                                                val dayGregorianEvents = com.example.data.MasoomeenData.gregorianEvents.filter { it.day == dayNumber && it.month == (month + 1) }
                                                
                                                val hasWiladat = dayEvents.any { it.eventType == com.example.data.EventType.WILADAT }
                                                val hasShahadat = dayEvents.any { it.eventType == com.example.data.EventType.SHAHADAT }
                                                val hasGregorian = dayGregorianEvents.isNotEmpty()

                                                val isSelected = selectedDayNum == dayNumber

                                                val cellBg = when {
                                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                    hasShahadat -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.50f)
                                                    hasWiladat -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f)
                                                    hasGregorian -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.40f)
                                                    else -> MaterialTheme.colorScheme.surface
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(cellBg)
                                                        .clickable {
                                                            selectedDayNum = if (isSelected) null else dayNumber
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                        Text(
                                                            text = dayNumber.toString(),
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = hijriForDay.day.toString(),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.size(38.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = calendarText("calendar_hijri_notice"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 10.dp)
                            )

                            // Selected day events details
                            selectedDayNum?.let { dayNum ->
                                val hijriForSelected = com.example.utils.HijriCalendarHelper.convertGregorianToHijri(year, month + 1, dayNum)
                                val dayEvents = allEvents.filter { matchEvent(hijriForSelected.day, hijriForSelected.monthName, it) }
                                val dayGregorianEvents = com.example.data.MasoomeenData.gregorianEvents.filter { it.day == dayNum && it.month == (month + 1) }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                Spacer(modifier = Modifier.height(12.dp))

                                val selCal = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.YEAR, year)
                                    set(java.util.Calendar.MONTH, month)
                                    set(java.util.Calendar.DAY_OF_MONTH, dayNum)
                                }
                                val formattedGreg = com.example.utils.LocalizationUtility.formatGregorianDate(selCal, languageCode)
                                val formattedHijri = com.example.utils.LocalizationUtility.formatHijriDate(hijriForSelected, languageCode)

                                Text(
                                    text = "$formattedGreg ($formattedHijri)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )

                                if (dayEvents.isEmpty() && dayGregorianEvents.isEmpty()) {
                                    Text(
                                        text = calendarText("calendar_no_events"),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                } else {
                                    dayEvents.forEach { ev ->
                                        val isWiladat = ev.eventType == com.example.data.EventType.WILADAT
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isWiladat) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isWiladat) Icons.Default.Star else Icons.Default.Info,
                                                contentDescription = null,
                                                tint = if (isWiladat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = ev.localizedTitle(languageCode),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isWiladat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                                )
                                                Text(
                                                    text = ev.localizedDescription(languageCode),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                CompactReferenceLink(ev.sourceUrl, languageCode)
                                            }
                                        }
                                    }

                                    dayGregorianEvents.forEach { ev ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Public,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = ev.localizedTitle(languageCode),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Text(
                                                    text = ev.localizedDescription(languageCode),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    TabRow(
                        selectedTabIndex = activeTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Tab(
                            selected = activeTabIndex == 0,
                            onClick = { activeTabIndex = 0 },
                            text = { Text(calendarText("calendar_shia_events"), fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTabIndex == 1,
                            onClick = { activeTabIndex = 1 },
                            text = { Text(calendarText("calendar_international_events"), fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                item {
                    AnimatedVisibility(visible = activeTabIndex == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = eventQuery,
                                onValueChange = { eventQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text(calendarText("calendar_search_events")) }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = eventTypeFilter == null,
                                    onClick = { eventTypeFilter = null },
                                    label = { Text(calendarText("calendar_all_events")) }
                                )
                                FilterChip(
                                    selected = eventTypeFilter == EventType.WILADAT,
                                    onClick = { eventTypeFilter = EventType.WILADAT },
                                    label = { Text(calendarText("calendar_wiladat")) }
                                )
                                FilterChip(
                                    selected = eventTypeFilter == EventType.SHAHADAT,
                                    onClick = { eventTypeFilter = EventType.SHAHADAT },
                                    label = { Text(calendarText("calendar_shahadat")) }
                                )
                            }
                        }
                    }
                }

                if (activeTabIndex == 0) {
                    items(filteredShiaEvents) { event ->
                        val isWiladat = event.eventType == EventType.WILADAT
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isWiladat) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isWiladat) Icons.Default.Star else Icons.Default.Info,
                                    contentDescription = calendarText(if (isWiladat) "calendar_joyous" else "calendar_solemn"),
                                    tint = if (isWiladat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = event.localizedTitle(languageCode),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isWiladat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = com.example.utils.LocalizationUtility.translateHijriDateString(event.dateStringHijri, languageCode),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = event.localizedDescription(languageCode),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    CompactReferenceLink(event.sourceUrl, languageCode)
                                }
                            }
                        }
                    }
                    if (filteredShiaEvents.isEmpty()) {
                        item {
                            Text(
                                text = calendarText("calendar_no_matching_events"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(com.example.data.MasoomeenData.gregorianEvents) { event ->
                        val monthName = shortMonthName(event.month, calendarLocale)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = calendarText("calendar_international"),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = event.localizedTitle(languageCode),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${event.day} $monthName",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = event.localizedDescription(languageCode),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
}

private fun matchEvent(day: Int, monthName: String, event: com.example.data.MasoomEvent): Boolean {
    val month = com.example.utils.HijriCalendarHelper.MONTH_NAMES.indexOfFirst {
        it.equals(monthName.trim(), ignoreCase = true)
    } + 1
    return event.occursOn(day, month, monthName)
}

private fun localizedWeekdays(languageCode: String): List<String> = when (languageCode) {
    "ur" -> listOf("اتوار", "پیر", "منگل", "بدھ", "جمعرات", "جمعہ", "ہفتہ")
    "ar" -> listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    else -> listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
}

private fun shortMonthName(month: Int, locale: java.util.Locale): String {
    val calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.MONTH, (month - 1).coerceIn(0, 11))
    }
    return java.text.SimpleDateFormat("MMM", locale).format(calendar.time)
}
