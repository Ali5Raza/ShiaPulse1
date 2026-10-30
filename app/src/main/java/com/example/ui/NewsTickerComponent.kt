package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NewsTickerBar(
    viewModel: PrayerViewModel,
    onItemClick: (TickerItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isTickerActive by viewModel.isTickerActive.collectAsState()
    val tickerItems by viewModel.tickerItems.collectAsState()
    val speed by viewModel.tickerSpeed.collectAsState()
    val mutedCategories by viewModel.tickerMutedCategories.collectAsState()
    val isRefreshing by viewModel.isRefreshingTicker.collectAsState()

    if (!isTickerActive || tickerItems.isEmpty()) return

    // Filter out muted items
    val activeItems = remember(tickerItems, mutedCategories) {
        tickerItems.filter { !mutedCategories.contains(it.category) }
    }

    if (activeItems.isEmpty()) return

    val currentLang by viewModel.appLanguage.collectAsState()

    var isPaused by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Determine scroll speed (delay duration between virtual manual micro-scrolls)
    val scrollDelay = when (speed) {
        "Slow" -> 55L
        "Fast" -> 20L
        else -> 35L // Normal
    }

    // Auto-scroll loop using LazyListState
    LaunchedEffect(isPaused, activeItems, scrollDelay) {
        if (!isPaused && activeItems.isNotEmpty()) {
            while (true) {
                delay(scrollDelay)
                // Scroll small amount to create smooth motion
                try {
                    lazyListState.firstVisibleItemIndex
                    lazyListState.scrollBy(2.5f)
                } catch (e: Exception) {
                    // Fail-safe
                }
                
                // Teleport to start to repeat scroll seamlessly
                val lastIndex = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (lastIndex >= activeItems.size - 1) {
                    try {
                        lazyListState.scrollToItem(0, 0)
                    } catch (e: Exception) { }
                }
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(44.dp)
            .testTag("news_events_ticker_bar"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    isPaused = !isPaused
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "LATEST" Badge
            Box(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                            )
                        )
                    )
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Playback state",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (currentLang) {
                            "ur" -> "تازہ ترین"
                            "ar" -> "الهامش"
                            "fa" -> "خبر فوری"
                            "hi" -> "मुख्य समाचार"
                            else -> "LIVE"
                        },
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            // The continuous scrolling lazy row
            LazyRow(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                userScrollEnabled = true, // Users can still drag manually
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                // Duplicate items (x3) to make the marquee flow infinitely
                val tripledList = activeItems + activeItems + activeItems
                itemsIndexed(tripledList) { index, item ->
                    TickerItemRow(item = item, currentLang = currentLang) {
                        onItemClick(item)
                    }
                    if (index < tripledList.size - 1) {
                        Text(
                            text = "  •  ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            // Quick customization icon
            IconButton(
                onClick = { onItemClick(TickerItem("CUSTOMIZE_TICKER", "", TickerCategory.APP)) },
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Ticker settings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun TickerItemRow(
    item: TickerItem,
    currentLang: String,
    onClick: () -> Unit
) {
    val categoryIcon = when (item.category) {
        TickerCategory.HISTORY -> Icons.Default.DateRange
        TickerCategory.RELIGIOUS -> Icons.Default.Notifications
        TickerCategory.COMMUNITY -> Icons.Default.Place
        TickerCategory.EDUCATIONAL -> Icons.Default.Book
        TickerCategory.APP -> Icons.Default.Settings
        TickerCategory.ALERT -> Icons.Default.Warning
    }

    val categoryColor = when (item.category) {
        TickerCategory.HISTORY -> Color(0xFFFF9800)
        TickerCategory.RELIGIOUS -> Color(0xFF4CAF50)
        TickerCategory.COMMUNITY -> Color(0xFF00BCD4)
        TickerCategory.EDUCATIONAL -> Color(0xFF673AB7)
        TickerCategory.APP -> Color(0xFF9C27B0)
        TickerCategory.ALERT -> Color(0xFFF44336)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(categoryColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = item.category.name,
                tint = categoryColor,
                modifier = Modifier.size(11.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(6.dp))
        
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TickerDetailDialog(
    item: TickerItem,
    languageCode: String,
    onDismiss: () -> Unit,
    onMuteCategory: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = when (languageCode) {
                        "ur" -> "ٹھیک ہے"
                        "ar" -> "حسنًا"
                        else -> "Dismiss"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onMuteCategory,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(
                    text = when (languageCode) {
                        "ur" -> "اس زمرے کو چھپائیں"
                        "ar" -> "كتم هذا التصنيف"
                        else -> "Mute Category"
                    }
                )
            }
        },
        icon = {
            val icon = when (item.category) {
                TickerCategory.HISTORY -> Icons.Default.DateRange
                TickerCategory.RELIGIOUS -> Icons.Default.Notifications
                TickerCategory.COMMUNITY -> Icons.Default.Place
                TickerCategory.EDUCATIONAL -> Icons.Default.Book
                TickerCategory.APP -> Icons.Default.Settings
                TickerCategory.ALERT -> Icons.Default.Warning
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = item.fullTitle.ifEmpty { item.category.getLabel(languageCode) },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = item.fullDetails,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (item.bulletPoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = when (languageCode) {
                            "ur" -> "اہم نکات:"
                            "ar" -> "النقاط الرئيسية:"
                            else -> "Key Information:"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    item.bulletPoints.forEach { pt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = pt,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Source: ${item.source}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    
                    if (item.dateString.isNotEmpty()) {
                        Text(
                            text = item.dateString,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (languageCode) {
                                "ur" -> "عارضی طور پر حذف کریں (۲۴ گھنٹے)"
                                "ar" -> "إخفاء مؤقت (٢٤ ساعة)"
                                else -> "Dismiss message (24 hours)"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun TickerSettingsOverlay(
    viewModel: PrayerViewModel,
    languageCode: String,
    onDismiss: () -> Unit
) {
    val isTickerActive by viewModel.isTickerActive.collectAsState()
    val tickerSpeed by viewModel.tickerSpeed.collectAsState()
    val mutedCategories by viewModel.tickerMutedCategories.collectAsState()
    val isRefreshing by viewModel.isRefreshingTicker.collectAsState()
    val submittedEvents by viewModel.submittedTickerEvents.collectAsState()

    var showSubmitDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
text = "Ticker Settings & Control",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = { viewModel.refreshTickerEvents() }) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // General Switch Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
text = "Display News Ticker",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
text = "Toggling this hides the continuous marquee ticker completely",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isTickerActive,
                        onCheckedChange = { viewModel.setTickerActive(it) }
                    )
                }
            }

            if (isTickerActive) {
                Spacer(modifier = Modifier.height(18.dp))

                // Scroll Speed Selector
                Text(
text = "Ticker Motion Speed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Slow", "Normal", "Fast").forEach { speed ->
                        val isSelected = tickerSpeed == speed
                        Button(
                            onClick = { viewModel.setTickerSpeed(speed) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = speed,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Category Muting / Filtering
                Text(
text = "Category Filters (Mute / Unmute Feed)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TickerCategory.values().forEach { cat ->
                        val isMuted = mutedCategories.contains(cat)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleTickerMuteCategory(cat) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (!isMuted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (!isMuted) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val icon = when (cat) {
                                        TickerCategory.HISTORY -> Icons.Default.DateRange
                                        TickerCategory.RELIGIOUS -> Icons.Default.Notifications
                                        TickerCategory.COMMUNITY -> Icons.Default.Place
                                        TickerCategory.EDUCATIONAL -> Icons.Default.Book
                                        TickerCategory.APP -> Icons.Default.Settings
                                        TickerCategory.ALERT -> Icons.Default.Warning
                                    }
                                    Icon(icon, contentDescription = null, size = 18.dp, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = cat.getLabel(languageCode),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (!isMuted) "Active" else "Muted",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (!isMuted) Color(0xFF4CAF50) else Color.Red,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Checkbox(
                                        checked = !isMuted,
                                        onCheckedChange = { viewModel.toggleTickerMuteCategory(cat) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- User Submissions & Admin Panel Simulation ---
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "📢 Administrative Dashboard & Community News",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Submit events to the feed, and review submissions in the Moderator Admin Console queue below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showSubmitDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Submit")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Submit News / Event for Review")
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Admin moderator console
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Moderator Approval Queue (${submittedEvents.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text("ADMIN SIMULATOR", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }

                        if (submittedEvents.isEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No pending community submissions in audit queue. Submit a test event above to inspect the live admin workflow.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                            submittedEvents.forEach { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = item.fullTitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = item.category.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = item.fullDetails,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = { viewModel.rejectSubmittedEvent(item.id) },
                                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Reject", modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reject")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = { viewModel.approveSubmittedEvent(item.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = "Approve", modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Approve & Publish", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSubmitDialog) {
        var eventTitle by remember { mutableStateOf("") }
        var eventText by remember { mutableStateOf("") }
        var eventDetails by remember { mutableStateOf("") }
        var selectedCat by remember { mutableStateOf(TickerCategory.RELIGIOUS) }

        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = {
                Text(
                    text = "Submit Local Shia Event",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (eventText.isNotEmpty() && eventTitle.isNotEmpty()) {
                            viewModel.submitUserEvent(
                                text = "📢 $eventText",
                                category = selectedCat,
                                title = eventTitle,
                                details = eventDetails
                            )
                            showSubmitDialog = false
                        }
                    }
                ) {
                    Text("Submit Content")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("Cancel")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Fill in the event fields. Moderated submissions will queue in the Admin dashboard panel below for instant publish preview.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("Event Title / Header") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = eventText,
                        onValueChange = { eventText = it },
                        label = { Text("Ticker Scroll Headline (Max 100 chars)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    OutlinedTextField(
                        value = eventDetails,
                        onValueChange = { eventDetails = it },
                        label = { Text("Full Context / Details") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Text("Select Category:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(TickerCategory.RELIGIOUS, TickerCategory.HISTORY, TickerCategory.COMMUNITY).forEach { cat ->
                            val isChosen = selectedCat == cat
                            FilterChip(
                                selected = isChosen,
                                onClick = { selectedCat = cat },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }
            }
        )
    }
}

// Helper Extension to support size styling safely
@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size)
    )
}
