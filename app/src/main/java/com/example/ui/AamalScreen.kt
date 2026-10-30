package com.example.ui
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookOnline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import org.json.JSONObject
import java.io.InputStreamReader



object AamalAssetLoader {
    fun loadAamal(context: android.content.Context): List<AamalItem> {
        val result = mutableListOf<AamalItem>()
        try {
            val assetManager = context.assets
            val aamalFiles = assetManager.list("Supplications/Aamal") ?: emptyArray()
            
            for (file in aamalFiles) {
                if (file.endsWith(".json")) {
                    val inputStream = assetManager.open("Supplications/Aamal/$file")
                    val jsonStr = InputStreamReader(inputStream).readText()
                    val jsonObj = JSONObject(jsonStr)
                    
                    val actsList = parseAamalActs(jsonObj)

                    result.add(
                        AamalItem(
                            id = jsonObj.optString("id", ""),
                            title = jsonObj.optString("title", ""),
                            titleUr = if (jsonObj.has("titleUr")) jsonObj.optString("titleUr") else null,
                            titleAr = if (jsonObj.has("titleAr")) jsonObj.optString("titleAr") else null,
                            category = jsonObj.optString("category", ""),
                            categoryUr = if (jsonObj.has("categoryUr")) jsonObj.optString("categoryUr") else null,
                            categoryAr = if (jsonObj.has("categoryAr")) jsonObj.optString("categoryAr") else null,
                            description = jsonObj.optString("description", ""),
                            descriptionUr = if (jsonObj.has("descriptionUr")) jsonObj.optString("descriptionUr") else null,
                            descriptionAr = if (jsonObj.has("descriptionAr")) jsonObj.optString("descriptionAr") else null,
                            relevance = jsonObj.optString("relevance", ""),
                            acts = actsList,
                            relevanceUr = jsonObj.optString("relevanceUr").takeIf { it.isNotBlank() },
                            relevanceAr = jsonObj.optString("relevanceAr").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AamalScreen(
    currentLang: String,
    onBack: () -> Unit,
    initialAamalId: String? = null
) {
    val context = LocalContext.current
    var aamalList by remember { mutableStateOf(com.example.ui.admin.AamalSyncManager.loadAamal(context)) }
    var selectedAamal by remember { mutableStateOf<AamalItem?>(null) }
    var hasOpenedInitial by remember(initialAamalId) { mutableStateOf(false) }

    LaunchedEffect(initialAamalId, aamalList, hasOpenedInitial) {
        if (!hasOpenedInitial && initialAamalId != null && selectedAamal == null) {
            aamalList.firstOrNull { it.id == initialAamalId }?.let { initial ->
                selectedAamal = initial
                hasOpenedInitial = true
            }
        }
    }
    
    BackHandler(enabled = selectedAamal != null) {
        selectedAamal = null
    }
    
    LaunchedEffect(Unit) {
        com.example.ui.admin.AamalSyncManager.syncContentFromFirebase(context) { updatedList ->
            aamalList = updatedList
        }
    }
    
    val isUrdu = currentLang == "ur"
    val isArabic = currentLang == "ar"
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PremiumScreenTopBar(
                title = if (isUrdu) "اعمال" else if (isArabic) "الأعمال" else "Aamal",
                onBack = {
                    if (selectedAamal != null) selectedAamal = null
                    else onBack()
                }
            )
        }
    ) { padding ->
        val currentAamal = selectedAamal
        Box(modifier = Modifier.padding(padding)) {
            if (currentAamal == null) {
                Column {
                    val categories = remember(aamalList) {
                        listOf("All") + aamalList.map { it.category }.distinct().sorted()
                    }
                    var selectedCategoryTab by remember { mutableStateOf(0) }
                    
                    if (categories.size > 1) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedCategoryTab,
                            containerColor = MaterialTheme.colorScheme.background,
                            edgePadding = 20.dp,
                            divider = {},
                            indicator = { tabPositions ->
                                if (selectedCategoryTab < tabPositions.size) {
                                    TabRowDefaults.Indicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedCategoryTab]),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        ) {
                            categories.forEachIndexed { index, category ->
                                Tab(
                                    selected = selectedCategoryTab == index,
                                    onClick = { selectedCategoryTab = index },
                                    text = { 
                                        Text(
                                            text = category,
                                            fontWeight = if (selectedCategoryTab == index) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedCategoryTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ) 
                                    }
                                )
                            }
                        }
                    }
                    
                    val filteredList = remember(selectedCategoryTab, aamalList) {
                        if (selectedCategoryTab == 0) aamalList
                        else aamalList.filter { it.category == categories[selectedCategoryTab] }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredList) { aamal ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAamal = aamal },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookOnline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isUrdu && aamal.titleUr != null) aamal.titleUr else if (isArabic && aamal.titleAr != null) aamal.titleAr else aamal.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isUrdu && aamal.categoryUr != null) aamal.categoryUr else if (isArabic && aamal.categoryAr != null) aamal.categoryAr else aamal.category,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isUrdu && aamal.descriptionUr != null) aamal.descriptionUr else if (isArabic && aamal.descriptionAr != null) aamal.descriptionAr else aamal.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                    } // End of Column wrapper
                }
            } else {
                val item = currentAamal
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = if (isUrdu && item.titleUr != null) item.titleUr else if (isArabic && item.titleAr != null) item.titleAr else item.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = item.relevance,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                    }
                    items(item.acts) { act ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = act.actNumber.toString(),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isUrdu && act.titleUr != null) act.titleUr else if (isArabic && act.titleAr != null) act.titleAr else act.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isUrdu && act.descriptionUr != null) act.descriptionUr else if (isArabic && act.descriptionAr != null) act.descriptionAr else act.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                if (act.arabicRecitation != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = act.arabicRecitation,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Right
                                            )
                                            if (isUrdu && act.urduTranslation != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = act.urduTranslation,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Right
                                                )
                                            } else if (act.translation != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = act.translation,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            
                                            if (act.transliteration != null) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = act.transliteration,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontStyle = FontStyle.Italic
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
