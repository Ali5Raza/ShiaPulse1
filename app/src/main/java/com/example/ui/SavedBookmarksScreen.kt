package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.data.MasoomeenData
import com.example.data.ZiyaratItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedBookmarksScreen(
    viewModel: PrayerViewModel,
    onBack: () -> Unit,
    languageCode: String,
    modifier: Modifier = Modifier,
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {}
) {
    val favoriteQuotes by viewModel.favoriteQuotes.collectAsState()
    val favoriteDuaIds by viewModel.favoriteDuaIds.collectAsState()
    val allZiyarats by viewModel.allZiyarats.collectAsState()
    
    val isUrdu = languageCode == "ur"
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var shareQuote by remember { mutableStateOf<com.example.api.ShiaQuote?>(null) }
    
    // Bookmark and Reading states
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = My Bookmarks, 1 = Daily Quotes
    var bookmarkSectionFilter by remember { mutableStateOf("All") } // "All", "Duas", "Ziyarats"
    var quoteSectionFilter by remember { mutableStateOf("All") } // "All", "Ayats", "Hadees"
    
    var selectedZiyaratForReading by remember { mutableStateOf<ZiyaratItem?>(null) }
    var selectedDuaForReading by remember { mutableStateOf<ShiaDua?>(null) }

    // Load up Duas from assets for resolving references
    val loadedDuas = remember {
        val fromJson = loadDuasFromAssets(context)
        if (fromJson.isNotEmpty()) fromJson else shiaDuasList
    }
    
    val bookmarkedDuas = remember(loadedDuas, favoriteDuaIds) {
        loadedDuas.filter { favoriteDuaIds.contains(it.id) }
    }
    
    val bookmarkedZiyarats = remember(allZiyarats) {
        allZiyarats.filter { it.isFavorite }
    }

    val readingDua = selectedDuaForReading
    if (readingDua != null) {
        DuaReadingView(
            viewModel = viewModel,
            dua = readingDua,
            onBack = { selectedDuaForReading = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(com.example.utils.LocalizationUtility.get("saved_items", languageCode), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Secondary level tabs
                 TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = com.example.utils.LocalizationUtility.get("my_bookmarks", languageCode),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = com.example.utils.LocalizationUtility.get("daily_quotes", languageCode),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    )
                }
                
                if (selectedTab == 0) {
                    // "My Bookmarks" Tab content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Chips to filter local Bookmarks
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            listOf("All", "Duas", "Ziyarats").forEach { filterOpt ->
                                val selected = filterOpt == bookmarkSectionFilter
                                val label = when (filterOpt) {
                                    "Duas" -> com.example.utils.LocalizationUtility.get("tab_duas", languageCode)
                                    "Ziyarats" -> com.example.utils.LocalizationUtility.get("tab_ziyarats", languageCode)
                                    else -> com.example.utils.LocalizationUtility.get("tab_all", languageCode)
                                }
                                FilterChip(
                                    selected = selected,
                                    onClick = { bookmarkSectionFilter = filterOpt },
                                    label = { Text(label, fontWeight = FontWeight.Bold) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                        
                        // Get merged or filtered list of bookmarks
                        val showDuas = bookmarkSectionFilter == "All" || bookmarkSectionFilter == "Duas"
                        val showZiyarats = bookmarkSectionFilter == "All" || bookmarkSectionFilter == "Ziyarats"
                        
                        val isSectionEmpty = (showDuas && bookmarkedDuas.isEmpty()) && (showZiyarats && bookmarkedZiyarats.isEmpty())
                        
                        if (isSectionEmpty) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.BookmarkBorder, contentDescription = "No bookmarks", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(56.dp))
                                    Text(
                                        text = com.example.utils.LocalizationUtility.get("no_saved_duas", languageCode),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                // 1. Render Bookmarked Duas
                                if (showDuas && bookmarkedDuas.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = com.example.utils.LocalizationUtility.get("bookmarked_duas", languageCode),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    items(bookmarkedDuas) { dua ->
                                        DuaListTile(
                                            dua = dua,
                                            isFavorite = true,
                                            onFavoriteToggle = { viewModel.toggleDuaFavorite(dua.id) },
                                            onSelect = { selectedDuaForReading = dua }
                                        )
                                    }
                                }
                                
                                // 2. Render Bookmarked Ziyarats
                                if (showZiyarats && bookmarkedZiyarats.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = com.example.utils.LocalizationUtility.get("bookmarked_ziyarats", languageCode),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                        )
                                    }
                                    items(bookmarkedZiyarats) { ziyarat ->
                                        Card(
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedZiyaratForReading = ziyarat }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = ziyarat.masoomIndex.toString(),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                        val masoomName = remember(ziyarat.masoomIndex) {
                                                            MasoomeenData.list.find { it.index == ziyarat.masoomIndex }?.name ?: "Holy Ahlul Bayt (as)"
                                                        }
                                                        Text(
                                                            text = masoomName,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { viewModel.toggleZiyaratFavorite(ziyarat) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Favorite,
                                                            contentDescription = "Favorite",
                                                            tint = Color(0xFFE94057),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                
                                                Text(
                                                    text = if (languageCode == "ur") ziyarat.titleUr else ziyarat.titleEn,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                
                                                Text(
                                                    text = ziyarat.arText.take(100) + "...",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                    textAlign = TextAlign.Right,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    // "Daily Quotes" Tab content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Chips to filter local Quotes
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            listOf("All", "Ayats", "Hadees").forEach { filterOpt ->
                                val selected = filterOpt == quoteSectionFilter
                                FilterChip(
                                    selected = selected,
                                    onClick = { quoteSectionFilter = filterOpt },
                                    label = { Text(filterOpt, fontWeight = FontWeight.Bold) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                        
                        val filteredQuotes = favoriteQuotes.filter { quote ->
                            val isAyat = quote.source.lowercase().contains("surah")
                            when (quoteSectionFilter) {
                                "Ayats" -> isAyat
                                "Hadees" -> !isAyat
                                else -> true
                            }
                        }

                        if (filteredQuotes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (favoriteQuotes.isEmpty()) com.example.utils.LocalizationUtility.get("no_saved_quotes", languageCode) else "No items found in this section",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                items(filteredQuotes, key = { it.english }) { quote ->
                                    Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = quote.arabic,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        val urduTrans = com.example.api.UrduTranslations.getUrdu(
                                            com.example.api.ShiaQuote(quote.arabic, quote.english, quote.source),
                                            context
                                        )
                                        if (isUrdu) {
                                            Text(
                                                text = urduTrans,
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Text(
                                                text = quote.english,
                                                style = LocalTextStyle.current.copy(lineHeight = 22.sp),
                                                fontSize = 15.sp
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = quote.source,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                modifier = Modifier.weight(1f)
                                            )
                                            
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        com.example.utils.ShareService.shareQuoteText(
                                                            context = context,
                                                            quote = com.example.api.ShiaQuote(quote.arabic, quote.english, quote.source)
                                                        )
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = "Share Text",
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                
                                                IconButton(
                                                    onClick = { 
                                                        shareQuote = com.example.api.ShiaQuote(quote.arabic, quote.english, quote.source)
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Brush,
                                                        contentDescription = "Share Card",
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { 
                                                        viewModel.toggleFavoriteCustom(quote.arabic, quote.english, quote.source)
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(20.dp)
                                                    )
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
        }
    }
    
    // Popup Ziyarat Dialog when clicked
    val readingZiyarat = selectedZiyaratForReading
    if (readingZiyarat != null) {
        ZiyaratDetailDialog(
            ziyarat = readingZiyarat,
            languageCode = languageCode,
            viewModel = viewModel,
            onDismiss = { selectedZiyaratForReading = null },
            onEditCard = onEditCard
        )
    }

    val quoteToShare = shareQuote
    if (quoteToShare != null) {
        AyatImageShareDialog(
            ayat = quoteToShare,
            onDismiss = { shareQuote = null },
            onShare = { includeArabic, includeEnglish, includeUrdu ->
                com.example.utils.ShareService.shareQuoteCard(
                    context = context,
                    quote = quoteToShare,
                    includeArabic = includeArabic,
                    includeEnglish = includeEnglish,
                    includeUrdu = includeUrdu,
                    fileNamePrefix = "shiapulse_quote",
                    chooserTitle = "Share Card",
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
