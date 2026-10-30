package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.EventType
import com.example.data.ExpandedMasoomData
import com.example.data.IslamicEventManager
import com.example.data.MasoomeenAssetLoader
import com.example.data.MasoomeenData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasoomeenScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    languageCode: String = "en",
    initialSelectedMasoomId: String? = null,
    onOpenQuran: ((surahNumber: Int, ayahNumber: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedMasoom by remember { mutableStateOf<ExpandedMasoomData?>(null) }
    
    BackHandler(enabled = selectedMasoom != null) {
        selectedMasoom = null
    }

    val masoomeenList = remember(languageCode) {
        MasoomeenAssetLoader.loadMasoomeenData(context, languageCode)
    }
    LaunchedEffect(initialSelectedMasoomId, masoomeenList) {
        if (initialSelectedMasoomId != null) {
            selectedMasoom = masoomeenList.firstOrNull {
                it.id.toString() == initialSelectedMasoomId
            }
        }
    }
    val eventsRevision by IslamicEventManager.eventsRevision.collectAsState()

    val filteredMasoomeen = remember(searchQuery, masoomeenList) {
        if (searchQuery.isBlank()) {
            masoomeenList
        } else {
            masoomeenList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.nameAr.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            PremiumScreenTopBar(
                title = if (languageCode == "ur") "14 معصومین علیہم السلام" else if (languageCode == "ar") "١٤ المعصومين (ع)" else "The 14 Masoomeen",
                subtitle = if (languageCode == "ur") "اہلبیت علیہم السلام کی پاکیزہ زندگی اور سوانح" else if (languageCode == "ar") "سيرة وتاريخ المعصومين الأربعة عشر من آل محمد" else "Biography and history of the Holy Ahlul Bayt (as)",
                onBack = onBack,
                backModifier = Modifier.testTag("masoomeen_back")
            )
        },
        modifier = modifier.testTag("masoomeen_root")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (languageCode == "ur") "نام یا لقب تلاش کریں..." else if (languageCode == "ar") "ابحث عن الاسم أو اللقب..." else "Search by name or title...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            // Masoomeen Custom Grid Display
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMasoomeen) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMasoom = item }
                            .testTag("masoom_item_${item.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // ID Badge
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .align(Alignment.Start),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.id.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Arabic script
                            Text(
                                text = item.nameAr,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            // Localized Name
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Title details
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog displaying complete historical bio and metadata
    selectedMasoom?.let { item ->
        val lunarEvents = remember(item.id, eventsRevision) {
            MasoomeenData.getEventsForMasoom(context, item.id)
        }
        Dialog(
            onDismissRequest = { selectedMasoom = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .padding(vertical = 12.dp)
                    .testTag("masoomeen_detail_dialog")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header (Fixed)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (languageCode == "ur") "معصوم نمبر #${item.id}" else if (languageCode == "ar") "المعصوم رقم #${item.id}" else "INFALLIBLE MASOOM #${item.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.nameAr,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            textAlign = TextAlign.Center
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Born details mapping
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (languageCode == "ur") "ولادت باسعادت" else if (languageCode == "ar") "الولادة المباركة" else "SACRED BIRTH",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = item.born,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (languageCode == "ur") "شہادت / گراں سفر" else if (languageCode == "ar") "الشهادة / الغيبة" else "MARTYDOM / STATUS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (item.id == 14) Color(0xFF00BFA5) else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = item.martyrdom,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        // Shrine/Place detail row
                        Column {
                            Text(
                                text = if (languageCode == "ur") "روضہ اطہر / زیارت گاہ" else if (languageCode == "ar") "المرقد والمقام" else "ZIYARAH SHRINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Shrine Location",
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = item.burial,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Biography
                        ExpandableSection(
                            title = if (languageCode == "ur") "سوانح حیات" else if (languageCode == "ar") "السيرة الروحية" else "Biography",
                            icon = Icons.Default.Info
                        ) {
                            Text(
                                text = item.biography,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }

                        // Family Tree
                        ExpandableSection(
                            title = if (languageCode == "ur") "شجرہ نسب" else if (languageCode == "ar") "شجرة العائلة" else "Family Tree",
                            icon = Icons.Default.AccountTree
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "• " + (if (languageCode == "ur") "والد: " else if (languageCode == "ar") "الأب: " else "Father: ") + item.familyTree.father, style = MaterialTheme.typography.bodySmall)
                                Text(text = "• " + (if (languageCode == "ur") "والدہ: " else if (languageCode == "ar") "الأم: " else "Mother: ") + item.familyTree.mother, style = MaterialTheme.typography.bodySmall)
                                Text(text = "• " + (if (languageCode == "ur") "زوجہ: " else if (languageCode == "ar") "الزوجة: " else "Spouse: ") + item.familyTree.spouse, style = MaterialTheme.typography.bodySmall)
                                if (item.familyTree.children.isNotEmpty()) {
                                    Text(text = "• " + (if (languageCode == "ur") "اولاد: " else if (languageCode == "ar") "الأولاد: " else "Children: ") + item.familyTree.children.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        // Timeline
                        if (item.timeline.isNotEmpty()) {
                            ExpandableSection(
                                title = if (languageCode == "ur") "ٹائم لائن" else if (languageCode == "ar") "الجدول الزمني" else "Timeline",
                                icon = Icons.Default.Timeline
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item.timeline.forEach { tl ->
                                        Row {
                                            Text(text = "${tl.year}: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text(text = tl.event, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        // Places
                        if (item.places.isNotEmpty()) {
                            ExpandableSection(
                                title = if (languageCode == "ur") "مقامات" else if (languageCode == "ar") "الأماكن" else "Places",
                                icon = Icons.Default.LocationOn
                            ) {
                                Text(
                                    text = item.places.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Books
                        if (item.books.isNotEmpty()) {
                            ExpandableSection(
                                title = if (languageCode == "ur") "کتب و تعلیمات" else if (languageCode == "ar") "الكتب والتعاليم" else "Books & Teachings",
                                icon = Icons.Default.MenuBook
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    item.books.forEach { book ->
                                        Text(text = "📖 $book", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // Important Events
                        if (item.importantEvents.isNotEmpty()) {
                            ExpandableSection(
                                title = if (languageCode == "ur") "اہم واقعات" else if (languageCode == "ar") "أحداث هامة" else "Important Events",
                                icon = Icons.Default.Event
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    item.importantEvents.forEach { ev ->
                                        Text(text = "▪ $ev", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // Sayings & Wisdom
                        if (item.sayings.isNotEmpty()) {
                            ExpandableSection(
                                title = if (languageCode == "ur") "اقوال و احادیث" else if (languageCode == "ar") "أقوال وحكم" else "Sayings & Wisdom",
                                icon = Icons.Default.Star
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    item.sayings.forEachIndexed { sayingIndex, saying ->
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "“ $saying ”",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 18.sp
                                            )
                                            val references = item.sayingReferences[sayingIndex].orEmpty()
                                            if (references.isEmpty()) {
                                                PendingReferenceText(languageCode)
                                            } else {
                                                KnowledgeReferenceList(
                                                    references = references,
                                                    languageCode = languageCode,
                                                    onOpenQuran = onOpenQuran
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Canonical lunar events linked by masoomIndex in islamic_events.json
                        if (lunarEvents.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (languageCode == "ur") "قمری کیلنڈر کی تواریخ" else if (languageCode == "ar") "أحداث التقويم الهجري" else "LUNAR CALENDAR EVENTS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    lunarEvents.forEach { ev ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (ev.eventType == EventType.WILADAT) Color(0xFF00BFA5) else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = ev.localizedTitle(languageCode),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = com.example.utils.LocalizationUtility.translateHijriDateString(ev.dateStringHijri, languageCode),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom close button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { selectedMasoom = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = if (languageCode == "ur") "بند کریں" else if (languageCode == "ar") "إغلاق" else "Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) } // default expanded for visibility

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(modifier = Modifier.padding(top = 4.dp)) {
                    content()
                }
            }
        }
    }
}
