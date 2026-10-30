package com.example.ui
import kotlinx.coroutines.isActive
import androidx.compose.foundation.gestures.animateScrollBy
import com.example.data.MyCollection
import com.example.data.ZiyaratItem
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.utils.LocalizationUtility
import com.example.ui.theme.AmiriFont
import com.example.ui.theme.NotoNaskhArabicFont
import com.example.ui.theme.NotoNastaliqUrduFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray

data class DuaLine(
    val arabic: String,
    val translation: String, // English translation
    val transliteration: String,
    val urduTranslation: String? = null,
    val farsiTranslation: String? = null
)

data class ShiaDua(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val relevance: String,
    val audioUrl: String,
    val lines: List<DuaLine>,
    val titleUr: String? = null,
    val titleAr: String? = null,
    val descriptionUr: String? = null,
    val descriptionAr: String? = null,
    val categoryUr: String? = null,
    val categoryAr: String? = null,
    val relevanceUr: String? = null,
    val relevanceAr: String? = null
)

// Real-time translation client using Gemini 3.5 Flash API
suspend fun translateWithGemini(arabicText: String, englishText: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
    val languageCode = when (targetLanguage.lowercase()) {
        "ur", "urdu" -> "ur"
        "ar", "arabic" -> "ar"
        else -> "en"
    }
    val apiKey = com.example.BuildConfig.GEMINI_API_KEY
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER")) {
        return@withContext com.example.utils.LocalizationUtility.get("ai_unavailable", languageCode)
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // Prompt optimized for direct, beautiful Quranic/Dua translation
    val prompt = "Translate this line from an Islamic prayer or supplication into the language \"$targetLanguage\". Provide ONLY the translated text. Do not provide any introduction, explanation, transliteration, phonetic notation, or notes. Just output the translation itself.\nArabic Verse: \"$arabicText\"\nEnglish Meaning: \"$englishText\""

    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

    val requestJson = JSONObject().apply {
        put("contents", JSONArray().put(JSONObject().apply {
            put("parts", JSONArray().put(JSONObject().apply {
                put("text", prompt)
            }))
        }))
    }

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = requestJson.toString().toRequestBody(mediaType)

    val request = Request.Builder()
        .url(url)
        .post(body)
        .build()

    try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext String.format(
                    java.util.Locale.getDefault(),
                    com.example.utils.LocalizationUtility.get("ai_http_error", languageCode),
                    response.code
                )
            }
            val responseString = response.body?.string()
                ?: return@withContext com.example.utils.LocalizationUtility.get("ai_no_response", languageCode)
            val jsonObject = JSONObject(responseString)
            val candidates = jsonObject.getJSONArray("candidates")
            val candidate = candidates.getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val resultText = parts.getJSONObject(0).getString("text").trim()
            return@withContext resultText
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        return@withContext com.example.utils.LocalizationUtility.get("ai_network_error", languageCode)
    }
}

fun normalizeCategory(cat: String?): String {
    if (cat.isNullOrBlank()) return "Duas"
    val lower = cat.lowercase()
    if (lower.contains("ziyarat")) return "Ziyarats"
    if (lower.contains("munajat")) return "Munajats"
    if (lower.contains("amaal") || lower.contains("amal")) return "Amaal"
    if (lower.contains("prayer") || lower.contains("namaz") || lower.contains("salah")) return "Prayers"
    
    // Default all other types (Rajab, Ramadhan, Quranic, Salawat, Famed Supplications, etc) to "Duas"
    return "Duas"
}

private fun JSONObject.optionalLocalizedText(key: String, legacyKey: String? = null): String? {
    val value = optString(key, "").trim()
    if (value.isNotEmpty()) return value
    if (legacyKey == null) return null
    return optString(legacyKey, "").trim().takeIf { it.isNotEmpty() }
}

private fun String?.orLocalizedFallback(fallback: String?): String? =
    this?.takeIf { it.isNotBlank() } ?: fallback

/** Remote content keeps its own text while older records inherit bundled translations. */
private fun ShiaDua.withLocalizationFallback(fallback: ShiaDua?): ShiaDua {
    if (fallback == null) return this
    return copy(
        titleUr = titleUr.orLocalizedFallback(fallback.titleUr),
        titleAr = titleAr.orLocalizedFallback(fallback.titleAr),
        descriptionUr = descriptionUr.orLocalizedFallback(fallback.descriptionUr),
        descriptionAr = descriptionAr.orLocalizedFallback(fallback.descriptionAr),
        categoryUr = categoryUr.orLocalizedFallback(fallback.categoryUr),
        categoryAr = categoryAr.orLocalizedFallback(fallback.categoryAr),
        relevanceUr = relevanceUr.orLocalizedFallback(fallback.relevanceUr),
        relevanceAr = relevanceAr.orLocalizedFallback(fallback.relevanceAr)
    )
}

fun loadDuasFromAssets(context: android.content.Context): List<ShiaDua> {
    val allDuas = mutableListOf<ShiaDua>()
    val folders = listOf("Supplications/Duas", "Supplications/Munajats", "Supplications/Ziyarats")
    
    for (folder in folders) {
        try {
            val files = context.assets.list(folder)
            if (files != null) {
                for (file in files) {
                    if (file.endsWith(".json")) {
                        try {
                            val inputStream = context.assets.open("$folder/$file")
                            val jsonString = inputStream.bufferedReader().use { it.readText() }
                            val dua = parseSingleDuaJson(jsonString)
                            if (dua != null) {
                                // Optionally set the category based on the folder if missing
                                val defaultCategory = folder.substringAfterLast("/")
                                val finalDua = if (dua.category == "General Supplications" || dua.category.isEmpty()) {
                                    dua.copy(category = defaultCategory)
                                } else {
                                    dua
                                }
                                allDuas.add(finalDua)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("DuasScreen", "Error parsing $file in $folder: ${e.message}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DuasScreen", "Error loading from $folder: ${e.message}", e)
        }
    }
    
    // Fallback if folders are empty for some reason, try to load legacy duas.json
    if (allDuas.isEmpty()) {
        try {
            val inputStream = context.assets.open("duas.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            return parseDuasJson(jsonString)
        } catch (e: Exception) {
            android.util.Log.e("DuasScreen", "Error loading duas.json: ${e.message}", e)
        }
    }
    
    return allDuas
}

fun parseSingleDuaJson(jsonString: String): ShiaDua? {
    try {
        val duaObj = org.json.JSONObject(jsonString)
        val linesArray = duaObj.getJSONArray("lines")
        val linesList = mutableListOf<DuaLine>()
        for (j in 0 until linesArray.length()) {
            val lineObj = linesArray.getJSONObject(j)
            linesList.add(
                DuaLine(
                    arabic = lineObj.optString("arabic", ""),
                    translation = lineObj.optString("translation", ""),
                    transliteration = lineObj.optString("transliteration", ""),
                    urduTranslation = lineObj.optString("urduTranslation").takeIf { it.isNotEmpty() },
                    farsiTranslation = lineObj.optString("farsiTranslation").takeIf { it.isNotEmpty() }
                )
            )
        }
        return ShiaDua(
            id = duaObj.getString("id"),
            title = duaObj.getString("title"),
            category = normalizeCategory(duaObj.optString("category", "General Supplications")),
            description = duaObj.optString("description", "A beautiful Islamic supplication."),
            relevance = duaObj.optString("relevance", "Anytime"),
            audioUrl = duaObj.optString("audioUrl", ""),
            lines = linesList,
            titleUr = duaObj.optionalLocalizedText("titleUr"),
            titleAr = duaObj.optionalLocalizedText("titleAr", "arabicTitle"),
            descriptionUr = duaObj.optionalLocalizedText("descriptionUr"),
            descriptionAr = duaObj.optionalLocalizedText("descriptionAr"),
            categoryUr = duaObj.optionalLocalizedText("categoryUr"),
            categoryAr = duaObj.optionalLocalizedText("categoryAr"),
            relevanceUr = duaObj.optionalLocalizedText("relevanceUr"),
            relevanceAr = duaObj.optionalLocalizedText("relevanceAr")
        )
    } catch (e: Exception) {
        return null
    }
}

fun parseDuasJson(jsonString: String): List<ShiaDua> {
    val jsonArray = org.json.JSONArray(jsonString)
    val list = mutableListOf<ShiaDua>()
    for (i in 0 until jsonArray.length()) {
        val duaObj = jsonArray.getJSONObject(i)
        val linesArray = duaObj.getJSONArray("lines")
        val linesList = mutableListOf<DuaLine>()
        for (j in 0 until linesArray.length()) {
            val lineObj = linesArray.getJSONObject(j)
            linesList.add(
                DuaLine(
                    arabic = lineObj.optString("arabic", ""),
                    translation = lineObj.optString("translation", ""),
                    transliteration = lineObj.optString("transliteration", ""),
                    urduTranslation = lineObj.optString("urduTranslation").takeIf { it.isNotEmpty() },
                    farsiTranslation = lineObj.optString("farsiTranslation").takeIf { it.isNotEmpty() }
                )
            )
        }
        list.add(
            ShiaDua(
                id = duaObj.getString("id"),
                title = duaObj.getString("title"),
                category = normalizeCategory(duaObj.optString("category", "General Supplications")),
                description = duaObj.optString("description", "A beautiful Islamic supplication."),
                relevance = duaObj.optString("relevance", "Anytime"),
                audioUrl = duaObj.optString("audioUrl", ""),
                lines = linesList,
                titleUr = duaObj.optionalLocalizedText("titleUr"),
                titleAr = duaObj.optionalLocalizedText("titleAr", "arabicTitle"),
                descriptionUr = duaObj.optionalLocalizedText("descriptionUr"),
                descriptionAr = duaObj.optionalLocalizedText("descriptionAr"),
                categoryUr = duaObj.optionalLocalizedText("categoryUr"),
                categoryAr = duaObj.optionalLocalizedText("categoryAr"),
                relevanceUr = duaObj.optionalLocalizedText("relevanceUr"),
                relevanceAr = duaObj.optionalLocalizedText("relevanceAr")
            )
        )
    }
    return list
}

fun loadDuas(context: android.content.Context): List<ShiaDua> {
    val baseList = loadDuasFromAssets(context)
    val cacheFile = java.io.File(context.filesDir, "duas_cached.json")
    
    if (cacheFile.exists()) {
        try {
            val jsonString = cacheFile.readText()
            val cachedList = parseDuasJson(jsonString)
            if (cachedList.isNotEmpty()) {
                android.util.Log.d("DuasScreen", "Loaded ${cachedList.size} Duas from internal storage cache.")
                
                // Merge cachedList and baseList. Local assets overwrite cached items with the same id.
                val mergedMap = mutableMapOf<String, ShiaDua>()
                cachedList.forEach { mergedMap[it.id] = it }
                baseList.forEach { mergedMap[it.id] = it }
                
                return mergedMap.values.toList()
            }
        } catch (e: java.lang.Exception) {
            android.util.Log.e("DuasScreen", "Error reading cached duas: ${e.message}", e)
        }
    }
    
    if (baseList.isNotEmpty()) {
        saveDuasToCache(context, baseList)
    }
    return baseList
}

fun saveDuasToCache(context: android.content.Context, duas: List<ShiaDua>) {
    try {
        val jsonArray = org.json.JSONArray()
        for (dua in duas) {
            val duaObj = org.json.JSONObject().apply {
                put("id", dua.id)
                put("title", dua.title)
                put("category", dua.category)
                put("description", dua.description)
                put("relevance", dua.relevance)
                put("audioUrl", dua.audioUrl)
                if (dua.titleUr != null) put("titleUr", dua.titleUr)
                if (dua.titleAr != null) put("titleAr", dua.titleAr)
                if (dua.descriptionUr != null) put("descriptionUr", dua.descriptionUr)
                if (dua.descriptionAr != null) put("descriptionAr", dua.descriptionAr)
                if (dua.categoryUr != null) put("categoryUr", dua.categoryUr)
                if (dua.categoryAr != null) put("categoryAr", dua.categoryAr)
                if (dua.relevanceUr != null) put("relevanceUr", dua.relevanceUr)
                if (dua.relevanceAr != null) put("relevanceAr", dua.relevanceAr)
                
                val linesArray = org.json.JSONArray()
                for (line in dua.lines) {
                    val lineObj = org.json.JSONObject().apply {
                        put("arabic", line.arabic)
                        put("translation", line.translation)
                        put("transliteration", line.transliteration)
                        if (line.urduTranslation != null) put("urduTranslation", line.urduTranslation)
                        if (line.farsiTranslation != null) put("farsiTranslation", line.farsiTranslation)
                    }
                    linesArray.put(lineObj)
                }
                put("lines", linesArray)
            }
            jsonArray.put(duaObj)
        }
        val cacheFile = java.io.File(context.filesDir, "duas_cached.json")
        cacheFile.writeText(jsonArray.toString())
        android.util.Log.d("DuasScreen", "Successfully cached ${duas.size} Duas to local storage.")
    } catch (e: java.lang.Exception) {
        android.util.Log.e("DuasScreen", "Error saving duas to cache: ${e.message}", e)
    }
}

fun parseSnapshotToDuas(snapshot: com.google.firebase.database.DataSnapshot): List<ShiaDua> {
    val list = mutableListOf<ShiaDua>()
    var targetSnapshot = snapshot
    if (snapshot.hasChild("duas")) {
        targetSnapshot = snapshot.child("duas")
    } else if (snapshot.hasChild("duas_data")) {
        targetSnapshot = snapshot.child("duas_data")
    }
    for (child in targetSnapshot.children) {
        val id = child.child("id").getValue(String::class.java) ?: continue
        val title = child.child("title").getValue(String::class.java) ?: ""
        val category = normalizeCategory(child.child("category").getValue(String::class.java) ?: "General Supplications")
        val description = child.child("description").getValue(String::class.java) ?: ""
        val relevance = child.child("relevance").getValue(String::class.java) ?: "Anytime"
        val audioUrl = child.child("audioUrl").getValue(String::class.java) ?: ""
        
        val linesList = mutableListOf<DuaLine>()
        val linesSnapshot = child.child("lines")
        for (lineChild in linesSnapshot.children) {
            val arabic = lineChild.child("arabic").getValue(String::class.java) ?: ""
            val translation = lineChild.child("translation").getValue(String::class.java) ?: ""
            val transliteration = lineChild.child("transliteration").getValue(String::class.java) ?: ""
            val urduTranslation = lineChild.child("urduTranslation").getValue(String::class.java)
            val farsiTranslation = lineChild.child("farsiTranslation").getValue(String::class.java)
            linesList.add(DuaLine(arabic, translation, transliteration, urduTranslation, farsiTranslation))
        }
        list.add(
            ShiaDua(
                id = id,
                title = title,
                category = category,
                description = description,
                relevance = relevance,
                audioUrl = audioUrl,
                lines = linesList,
                titleUr = child.child("titleUr").getValue(String::class.java),
                titleAr = child.child("titleAr").getValue(String::class.java)
                    ?: child.child("arabicTitle").getValue(String::class.java),
                descriptionUr = child.child("descriptionUr").getValue(String::class.java),
                descriptionAr = child.child("descriptionAr").getValue(String::class.java),
                categoryUr = child.child("categoryUr").getValue(String::class.java),
                categoryAr = child.child("categoryAr").getValue(String::class.java),
                relevanceUr = child.child("relevanceUr").getValue(String::class.java),
                relevanceAr = child.child("relevanceAr").getValue(String::class.java)
            )
        )
    }
    return list
}

fun syncDuasFromFirebase(context: android.content.Context, onSyncComplete: (List<ShiaDua>) -> Unit) {
    val prefs = context.getSharedPreferences("sync_prefs", android.content.Context.MODE_PRIVATE)
    val localVersion = prefs.getLong("duas_version", 0L)

    try {
        val database = com.google.firebase.database.FirebaseDatabase.getInstance("https://shiapulse-pk-default-rtdb.asia-southeast1.firebasedatabase.app")
        val versionRef = database.reference.child("version")
        
        android.util.Log.d("DuasScreen", "Starting Firebase sync check. Local version: $localVersion")
        
        versionRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(versionSnapshot: com.google.firebase.database.DataSnapshot) {
                val remoteVersion = versionSnapshot.getValue(Long::class.java) ?: 0L
                android.util.Log.d("DuasScreen", "Firebase version: $remoteVersion, Local version: $localVersion")
                
                if (remoteVersion > localVersion || (localVersion == 0L && remoteVersion == 0L)) {
                    android.util.Log.d("DuasScreen", "New version available or first sync. Starting download...")
                    
                    val rootRef = database.reference
                    rootRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                        override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                            var syncedDuas: List<ShiaDua> = emptyList()
                            val jsonStringValue = snapshot.child("duas_json_string").getValue(String::class.java)
                                ?: snapshot.child("duas_json").getValue(String::class.java)
                            
                            if (!jsonStringValue.isNullOrEmpty()) {
                                try {
                                    syncedDuas = parseDuasJson(jsonStringValue)
                                    android.util.Log.d("DuasScreen", "Successfully parsed ${syncedDuas.size} duas from Firebase JSON string.")
                                } catch (e: Exception) {
                                    android.util.Log.e("DuasScreen", "Validation failed: Error parsing JSON string from Firebase: ${e.message}", e)
                                }
                            }
                            
                            if (syncedDuas.isEmpty()) {
                                try {
                                    syncedDuas = parseSnapshotToDuas(snapshot)
                                    android.util.Log.d("DuasScreen", "Successfully parsed ${syncedDuas.size} duas from Firebase nodes.")
                                } catch (e: Exception) {
                                    android.util.Log.e("DuasScreen", "Validation failed: Error parsing Firebase nodes: ${e.message}", e)
                                }
                            }
                            
                            if (syncedDuas.isNotEmpty()) {
                                // Validation check: ensure at least one valid Dua exists and basic fields are present
                                val isValid = syncedDuas.all { it.id.isNotBlank() && it.title.isNotBlank() }
                                if (isValid) {
                                    val baseList = loadDuasFromAssets(context)
                                    val mergedMap = mutableMapOf<String, ShiaDua>()
                                    baseList.forEach { mergedMap[it.id] = it }
                                    syncedDuas.forEach { synced ->
                                        mergedMap[synced.id] = synced.withLocalizationFallback(mergedMap[synced.id])
                                    }
                                    val finalMerged = mergedMap.values.toList()

                                    saveDuasToCache(context, finalMerged)
                                    val newVersionToSave = if (remoteVersion > 0L) remoteVersion else 1L
                                    prefs.edit().putLong("duas_version", newVersionToSave).apply()
                                    android.util.Log.d("DuasScreen", "Cache updated successfully. New local version: $newVersionToSave")
                                    onSyncComplete(finalMerged)
                                } else {
                                    android.util.Log.e("DuasScreen", "Validation failed: Downloaded data has invalid structure or missing required fields.")
                                }
                            } else {
                                android.util.Log.e("DuasScreen", "Validation failed: Firebase returned empty or invalid duas data.")
                            }
                        }
                        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                            android.util.Log.e("DuasScreen", "Firebase data download failed: ${error.message}")
                        }
                    })
                } else {
                    android.util.Log.d("DuasScreen", "Sync skipped. Local data is up to date (version $localVersion).")
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                android.util.Log.e("DuasScreen", "Firebase version check failed: ${error.message}")
            }
        })
    } catch (e: Exception) {
        android.util.Log.e("DuasScreen", "Error starting Firebase sync: ${e.message}", e)
    }
}

val shiaDuasList: List<ShiaDua> = emptyList()

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DuasScreen(
    viewModel: PrayerViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onOpenZiyaratsMap: (() -> Unit)? = null,
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {},
    initialDuaId: String? = null,
    initialTab: String? = null
) {
    val context = LocalContext.current
    val currentLang by viewModel.appLanguage.collectAsState()
    val globalAppFont by viewModel.appFont.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val prefs = remember { context.getSharedPreferences("recent_searches", android.content.Context.MODE_PRIVATE) }
    var recentSearches by remember { 
        mutableStateOf(context.getSharedPreferences("shia_tracker_prefs", android.content.Context.MODE_PRIVATE).getStringSet("dua_searches", emptySet<String>())?.toList()?.reversed() ?: emptyList<String>()) 
    }
    
    fun saveRecentSearch(query: String) {
        if (query.isNotBlank()) {
            val updated = (listOf(query) + recentSearches).distinct().take(5)
            recentSearches = updated
            prefs.edit().putStringSet("dua_searches", updated.toSet()).apply()
        }
    }

    var selectedCategoryTab by remember(initialTab) { mutableStateOf(initialTab ?: "Categories") }
    var currentSortOption by remember { mutableStateOf("Most Popular") }
    var isAudioPlayingGlobal by remember { mutableStateOf(false) }
    var showExitAudioDialog by remember { mutableStateOf(false) }
    var shouldPauseAudio by remember { mutableStateOf(false) }
    val allCollections by viewModel.allCollections.collectAsState()
    val allZiyarats by viewModel.allZiyarats.collectAsState()
    var activeCollectionDetail by remember { mutableStateOf<MyCollection?>(null) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var newCollectionDescription by remember { mutableStateOf("") }
    var selectedZiyaratForReading by remember { mutableStateOf<ZiyaratItem?>(null) }
    var activeDuaForCollectionDialog by remember { mutableStateOf<ShiaDua?>(null) }

    var loadedDuas by remember { mutableStateOf<List<ShiaDua>>(emptyList()) }
    var activeReadingDua by remember(initialDuaId) { mutableStateOf<ShiaDua?>(null) }

    LaunchedEffect(Unit) {
        val baseDuas = loadDuas(context)
        loadedDuas = baseDuas
        if (initialDuaId != null) {
            activeReadingDua = baseDuas.find { it.id == initialDuaId }
        }
        
        syncDuasFromFirebase(context) { syncedList ->
            if (syncedList.isNotEmpty()) {
                loadedDuas = syncedList
                val currentReadingDua = activeReadingDua
                if (currentReadingDua == null && initialDuaId != null) {
                    activeReadingDua = syncedList.find { it.id == initialDuaId }
                } else if (currentReadingDua != null) {
                    syncedList.find { it.id == currentReadingDua.id }?.let {
                        activeReadingDua = it
                    }
                }
            }
        }
    }

    val favoriteDuaIds by viewModel.favoriteDuaIds.collectAsState()

    // Dynamic categories extraction
    val activeCategories = remember(loadedDuas) {
        loadedDuas.map { it.category }.distinct().filter { it.isNotBlank() }.sorted()
    }

    // Filter and sort list based on checks
    val filteredDuas = remember(loadedDuas, searchQuery, selectedCategoryTab, currentSortOption, favoriteDuaIds) {
        val filtered = loadedDuas.filter { dua ->
            val matchesSearch = dua.title.contains(searchQuery, ignoreCase = true) || 
                                dua.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedCategoryTab) {
                "Categories" -> if (searchQuery.isNotEmpty()) true else false
                "Favorites" -> favoriteDuaIds.contains(dua.id)
                "Collections" -> false // Collections are handled separately
                "All" -> true
                "Ziyarat", "Ziyarats", "Holy Ziyarats" -> dua.category.contains("Ziyarat", ignoreCase = true)
                else -> dua.category.equals(selectedCategoryTab, ignoreCase = true)
            }
            matchesSearch && matchesCategory
        }

        when (currentSortOption) {
            "Alphabetical" -> filtered.sortedBy { it.title }
            "Shortest Duration" -> filtered.sortedBy { it.lines.size }
            "Longest Duration" -> filtered.sortedByDescending { it.lines.size }
            "Most Popular" -> {
                val popularIds = mapOf(
                    "kumayl" to 100,
                    "ashura" to 95,
                    "tawassul" to 90,
                    "ahad" to 85,
                    "nudba" to 80,
                    "jawshan_kabeer" to 78,
                    "faraj" to 75,
                    "sabah" to 70,
                    "hujjat" to 65,
                    "noor" to 62,
                    "ghareeq" to 61,
                    "nade_ali" to 60,
                    "hazeen" to 59,
                    "jawshan_sagheer" to 58,
                    "aliyyu" to 57,
                    "ziyarat_aminullah" to 56,
                    "dua_mujeer" to 55,
                    "ziyarat_aal_yaseen" to 54,
                    "ziyarat_arbaeen" to 53,
                    "dua_mashlool" to 52,
                    "dua_simaat" to 51,
                    "munajat_imamali" to 50,
                    "hadith_kisa" to 49,
                    "ziyarat_reza" to 48
                )
                filtered.sortedWith(
                    compareByDescending<ShiaDua> { favoriteDuaIds.contains(it.id) }
                        .thenByDescending { popularIds[it.id] ?: 0 }
                        .thenBy { it.title }
                )
            }
            else -> filtered
        }
    }

    if (activeReadingDua == null && selectedCategoryTab != "Categories") {
        BackHandler {
            selectedCategoryTab = "Categories"
        }
    }

    val readingDua = activeReadingDua
    if (readingDua != null) {
        BackHandler {
            if (isAudioPlayingGlobal) {
                showExitAudioDialog = true
            } else {
                activeReadingDua = null
            }
        }
        DuaReadingView(
            viewModel = viewModel,
            dua = readingDua,
            onBack = {
                if (isAudioPlayingGlobal) {
                    showExitAudioDialog = true
                } else {
                    activeReadingDua = null
                }
            },
            onPlaybackEnded = {
                val currentIndex = filteredDuas.indexOf(readingDua)
                if (currentIndex != -1 && currentIndex < filteredDuas.size - 1) {
                    activeReadingDua = filteredDuas[currentIndex + 1]
                }
            },
            onEditCard = onEditCard,
            onIsPlayingChanged = { isPlaying ->
                isAudioPlayingGlobal = isPlaying
            },
            shouldPause = shouldPauseAudio,
            onPauseHandled = {
                shouldPauseAudio = false
                activeReadingDua = null
            }
        )

        if (showExitAudioDialog) {
            AlertDialog(
                onDismissRequest = { showExitAudioDialog = false },
                title = {
                    Text(
                        text = if (currentLang == "ur") "دعا کی آڈیو چل رہی ہے" else "Dua Audio Playing",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Text(
                        text = if (currentLang == "ur") 
                            "کیا آپ دعا کی آڈیو کو بند کرنا چاہتے ہیں یا اسے پس منظر میں چلتا رہنے دینا چاہتے ہیں؟"
                        else 
                            "Would you like to stop the audio or keep it playing in the background?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            shouldPauseAudio = true
                            showExitAudioDialog = false
                        }
                    ) {
                        Text(
                            text = if (currentLang == "ur") "آڈیو بند کریں" else "Stop Audio",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                showExitAudioDialog = false
                                activeReadingDua = null
                            }
                        ) {
                            Text(
                                text = if (currentLang == "ur") "چلتی رہے" else "Keep Playing",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { showExitAudioDialog = false }
                        ) {
                            Text(
                                text = if (currentLang == "ur") "منسوخ کریں" else "Cancel",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    } else if (selectedCategoryTab == "Sacred Ziyarats") {
        ZiyaratsScreen(
            viewModel = viewModel,
            onBack = { selectedCategoryTab = "Categories" },
            languageCode = currentLang,
            modifier = modifier,
            onEditCard = onEditCard,
            onOpenZiyaratsMap = onOpenZiyaratsMap,
            supplicationZiyarats = loadedDuas.filter { it.category.contains("Ziyarat", ignoreCase = true) },
            onOpenDua = { dua -> 
                activeReadingDua = dua
            }
        )
    } else {
        Scaffold(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // 1. Clean, compact library header with the original titles and navigation.
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    if (selectedCategoryTab != "Categories") {
                                        selectedCategoryTab = "Categories"
                                    } else {
                                        onBack()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (selectedCategoryTab) {
                                    "Categories" -> "الدعاء والزيارة"
                                    "Favorites" -> "پسندیدہ دعائیں"
                                    "Duas" -> "دعائیں اور مناجات"
                                    "Ziyarats" -> "الزیارات المقدسہ"
                                    "Munajat" -> "مناجات ائمہ"
                                    "Amaal" -> "اعمال و وظائف"
                                    "All" -> "تمام ادعیہ و زیارات"
                                    else -> "الدعاء والزيارة"
                                },
                                fontFamily = AmiriFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = when (selectedCategoryTab) {
                                    "Categories" -> "Dua & Ziyarat"
                                    "Favorites" -> "My Favorites"
                                    "Duas" -> "Holy Duas"
                                    "Ziyarats" -> "Ziyarats"
                                    "Munajat" -> "Munajat"
                                    "Amaal" -> "Amaal & Special"
                                    "All" -> "All Supplications"
                                    else -> "Dua & Ziyarat"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.4).sp
                            )
                            Text(
                                text = when (selectedCategoryTab) {
                                    "Categories" -> "Supplications & Holy Salutations"
                                    "Favorites" -> "Your bookmarked supplications"
                                    "Duas" -> "Beautiful Islamic prayers & supplications"
                                    "Ziyarats" -> "Explore holy shrines & visitations"
                                    "Munajat" -> "Whispered intimate conversations"
                                    "Amaal" -> "Actions, prayers & daily recommendations"
                                    "All" -> "Complete index of devotional Shia supplications"
                                    else -> "Supplications & Holy Salutations"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 2. Glass-morphic search bar with subtle shadow
                item {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { saveRecentSearch(searchQuery.trim()) }),
                                    textStyle = TextStyle(
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 14.dp),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search Duas & Supplications...",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                fontSize = 15.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        androidx.compose.animation.AnimatedVisibility(visible = searchQuery.isEmpty() && recentSearches.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentSearches.forEach { search ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.clickable { searchQuery = search }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = "Recent",
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = search,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }



                // Beautiful, modern Sort Dropdown Row
                if (selectedCategoryTab != "Categories" || searchQuery.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Sort Supplications:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            
                            var expanded by remember { mutableStateOf(false) }
                            val sortOptions = listOf("Most Popular", "Alphabetical", "Shortest Duration", "Longest Duration")
                            
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                        .clickable { expanded = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sort,
                                        contentDescription = "Sort Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = currentSortOption,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    sortOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    text = option,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = if (option == currentSortOption) FontWeight.Bold else FontWeight.Normal
                                                ) 
                                            },
                                            onClick = {
                                                currentSortOption = option
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Map Banner for Holy Ziyarats category
                if (selectedCategoryTab == "Ziyarats" && onOpenZiyaratsMap != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { onOpenZiyaratsMap() }
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Map Button",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Interactive Ziyarats Map",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Explore holy shrines & historical sites globally.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Go to Map",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Main Dua list items (Sleek, beautifully styled cards)
                if (selectedCategoryTab == "Categories" && searchQuery.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val categoryColors = listOf(
                                Color(0xFF1F5F4F), Color(0xFF8B6B22), Color(0xFF2C3E50), Color(0xFF4A235A),
                                MaterialTheme.colorScheme.primary, Color(0xFFC0392B), Color(0xFF5D6D7E), Color(0xFF8E44AD)
                            )
                            val categoryIcons = listOf(
                                Icons.Default.MenuBook, Icons.Default.Mosque, Icons.Default.NightsStay, Icons.Default.LibraryBooks,
                                Icons.Default.Favorite, Icons.Default.Star, Icons.Default.Book, Icons.Default.Bookmark
                            )
                            
                            val allCards = mutableListOf<@androidx.compose.runtime.Composable () -> Unit>()
                            
                            // Merged Ziyarats Folder Card
                            val supplicationZiyaratCount = loadedDuas.count { it.category.contains("Ziyarat", ignoreCase = true) }
                            val totalZiyaratsCount = (if (allZiyarats.isNotEmpty()) allZiyarats.size else 14) + supplicationZiyaratCount
                            
                            allCards.add {
                                FolderCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    titleEng = "Ziyarats",
                                    titleUr = "زیارات (مقدسہ و مجموعہ)",
                                    icon = Icons.Default.Mosque,
                                    color = Color(0xFF8B6B22),
                                    count = totalZiyaratsCount,
                                    onClick = { selectedCategoryTab = "Sacred Ziyarats" }
                                )
                            }
                            
                            // 3. Ziyarats Map Folder Card
                            allCards.add {
                                FolderCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    titleEng = "Ziyarats Map",
                                    titleUr = "زیارات کا نقشہ",
                                    icon = Icons.Default.LocationOn,
                                    color = Color(0xFF1565C0),
                                    count = 14,
                                    onClick = { onOpenZiyaratsMap?.invoke() }
                                )
                            }
                            
                            // 4. Dynamic Categories (excluding duplicates)
                            activeCategories.filter { !it.contains("ziyarat", ignoreCase = true) }.forEachIndexed { index, category ->
                                allCards.add {
                                    val icon = when(category.lowercase()) {
                                        "duas" -> Icons.Default.MenuBook
                                        "munajats" -> Icons.Default.NightsStay
                                        "amaal" -> Icons.Default.LibraryBooks
                                        "prayers" -> Icons.Default.PanTool
                                        else -> categoryIcons[index % categoryIcons.size]
                                    }
                                    val color = when(category.lowercase()) {
                                        "duas" -> Color(0xFF1F5F4F)
                                        "munajats" -> Color(0xFF2C3E50)
                                        "amaal" -> Color(0xFF4A235A)
                                        else -> categoryColors[index % categoryColors.size]
                                    }
                                    val urduText = when(category.lowercase()) {
                                        "duas" -> "دعائیں"
                                        "munajats" -> "مناجات"
                                        "amaal" -> "اعمال / دیگر"
                                        "prayers" -> "نمازیں"
                                        else -> category
                                    }
                                    FolderCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        titleEng = category,
                                        titleUr = urduText,
                                        icon = icon,
                                        color = color,
                                        count = loadedDuas.count { it.category == category },
                                        onClick = { selectedCategoryTab = category }
                                    )
                                }
                            }
                            
                            // 4. My Collections Card
                            allCards.add {
                                FolderCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    titleEng = "My Collections",
                                    titleUr = "میری کلیکشنز",
                                    icon = Icons.Default.Folder,
                                    color = Color(0xFFC0392B),
                                    count = allCollections.size,
                                    onClick = { selectedCategoryTab = "Collections" }
                                )
                            }
                            
                            // 5. All Items Card
                            allCards.add {
                                FolderCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    titleEng = "All Items",
                                    titleUr = "تمام فہرست",
                                    icon = Icons.Default.FormatListBulleted,
                                    color = MaterialTheme.colorScheme.primary,
                                    count = loadedDuas.size + allZiyarats.size,
                                    onClick = { selectedCategoryTab = "All" }
                                )
                            }
                            
                            // Chunk by 2 to render in rows
                            allCards.chunked(2).forEach { rowCards ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        rowCards[0]()
                                    }
                                    if (rowCards.size > 1) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            rowCards[1]()
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedCategoryTab != "Categories") {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable {
                                    if (selectedCategoryTab == "Collections" && activeCollectionDetail != null) {
                                        activeCollectionDetail = null
                                    } else {
                                        selectedCategoryTab = "Categories"
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedCategoryTab == "Collections" && activeCollectionDetail != null) "Back to Folders" else "Back to Categories",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = if (selectedCategoryTab == "Collections" && activeCollectionDetail != null) "فولڈرز پر واپس جائیں" else "زمرہ جات پر واپس جائیں",
                                    fontFamily = NotoNastaliqUrduFont,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                if (selectedCategoryTab == "Categories" && searchQuery.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Showing search results across all categories:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (selectedCategoryTab == "Collections") {
                    val isUrdu = currentLang == "ur"
                    val collectionDetail = activeCollectionDetail
                    if (collectionDetail != null) {
                        // Display items inside activeCollectionDetail
                        val currentCollection = allCollections.find { it.id == collectionDetail.id } ?: collectionDetail
                        val collectionDuaIds = currentCollection.duaIds.split(",").filter { it.isNotBlank() }.toSet()
                        val collectionDuas = loadedDuas.filter { collectionDuaIds.contains(it.id) }
                        val collectionZiyaratIds = currentCollection.ziyaratIds.split(",").filter { it.isNotBlank() }.toSet()
                        val collectionZiyarats = allZiyarats.filter { collectionZiyaratIds.contains(it.id) }

                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(
                                    text = currentCollection.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (currentCollection.description.isNotBlank()) {
                                    Text(
                                        text = currentCollection.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        if (collectionDuas.isEmpty() && collectionZiyarats.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = "Empty Folder",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Text(
                                            text = if (isUrdu) "اس کلیکشن میں کوئی چیز نہیں ہے۔" else "This collection is empty.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = if (isUrdu) "دعا یا زیارت پڑھتے ہوئے 'کلیکشن میں شامل کریں' استعمال کریں۔" else "Add items from Duas or Ziyarats screen.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            if (collectionDuas.isNotEmpty()) {
                                item {
                                    Text(
                                        text = if (isUrdu) "دعائیں" else "Duas / Supplications",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                                items(collectionDuas) { dua ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { activeReadingDua = dua }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = dua.title,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = dua.category,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                    )
                                                }
                                                Row {
                                                    IconButton(onClick = { viewModel.toggleDuaFavorite(dua.id) }) {
                                                        Icon(
                                                            imageVector = if (favoriteDuaIds.contains(dua.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                            contentDescription = "Favorite",
                                                            tint = if (favoriteDuaIds.contains(dua.id)) Color(0xFFC0392B) else MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    IconButton(onClick = { viewModel.removeDuaFromCollection(currentCollection, dua.id) }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (collectionZiyarats.isNotEmpty()) {
                                item {
                                    Text(
                                        text = if (isUrdu) "زیارات" else "Ziyarats / Salutations",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                                items(collectionZiyarats) { ziyarat ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedZiyaratForReading = ziyarat }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (isUrdu) ziyarat.titleUr else ziyarat.titleEn,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    val masoomName = remember(ziyarat.masoomIndex) {
                                                        com.example.data.MasoomeenData.list.find { it.index == ziyarat.masoomIndex }?.name ?: "Holy Infallible (as)"
                                                    }
                                                    Text(
                                                        text = masoomName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                    )
                                                }
                                                IconButton(onClick = { viewModel.removeZiyaratFromCollection(currentCollection, ziyarat.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // activeCollectionDetail is null -> List of collections/folders
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUrdu) "میری مذہبی کلیکشنز" else "My Religious Folders",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Button(
                                    onClick = { showCreateCollectionDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isUrdu) "نیا فولڈر" else "New Folder",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (allCollections.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = "No folders",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Text(
                                            text = if (isUrdu) "کوئی کلیکشن نہیں ملی" else "No Collections Yet",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = if (isUrdu) 
                                                "اپنے پسندیدہ دعاؤں اور زیارات کو جمع کرنے کے لیے ایک نیا فولڈر بنائیں، جیسے 'رمضان' یا 'جمعہ'۔" 
                                                else "Create a custom folder (e.g., 'Laylatul Qadr' or 'Friday Devotions') to group specific prayers together for easy access.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(allCollections) { collection ->
                                val duaCount = collection.duaIds.split(",").filter { it.isNotBlank() }.size
                                val ziyaratCount = collection.ziyaratIds.split(",").filter { it.isNotBlank() }.size
                                val totalCount = duaCount + ziyaratCount

                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { activeCollectionDetail = collection }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.FolderSpecial,
                                                        contentDescription = "Folder",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = collection.name,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    if (collection.description.isNotBlank()) {
                                                        Text(
                                                            text = collection.description,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                            maxLines = 1
                                                        )
                                                    }
                                                    Text(
                                                        text = if (isUrdu) 
                                                            "$totalCount اشیاء ($duaCount دعائیں، $ziyaratCount زیارات)"
                                                            else "$totalCount items ($duaCount Duas, $ziyaratCount Ziyarats)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF8B6B22)
                                                    )
                                                }
                                            }

                                            IconButton(onClick = { viewModel.deleteCollection(collection) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Folder",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (selectedCategoryTab != "Categories" && filteredDuas.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "No Results",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "No supplications found in this category.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredDuas) { dua ->
                            val isFavorite = favoriteDuaIds.contains(dua.id)
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                DuaListTile(
                                    dua = dua,
                                    isFavorite = isFavorite,
                                    onFavoriteToggle = { viewModel.toggleDuaFavorite(dua.id) },
                                    onSelect = { 
                                        saveRecentSearch(searchQuery.trim())
                                        activeReadingDua = dua 
                                    },
                                    onAddToCollection = { activeDuaForCollectionDialog = dua }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val collectionDua = activeDuaForCollectionDialog
    if (collectionDua != null) {
        AddToCollectionDialog(
            duaId = collectionDua.id,
            ziyaratId = null,
            allCollections = allCollections,
            onAdd = { collection: MyCollection -> viewModel.addDuaToCollection(collection, collectionDua.id) },
            onRemove = { collection: MyCollection -> viewModel.removeDuaFromCollection(collection, collectionDua.id) },
            onCreateCollection = { name: String, desc: String -> viewModel.createCollection(name, desc) },
            onDismiss = { activeDuaForCollectionDialog = null }
        )
    }

    if (showCreateCollectionDialog) {
        val isUrdu = currentLang == "ur"
        AlertDialog(
            onDismissRequest = { showCreateCollectionDialog = false },
            title = { Text(if (isUrdu) "نیا فولڈر بنائیں" else "New Folder / Collection", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        label = { Text(if (isUrdu) "نام" else "Folder Name") },
                        placeholder = { Text(if (isUrdu) "مثال: لیلۃ القدر" else "e.g., Laylatul Qadr") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCollectionDescription,
                        onValueChange = { newCollectionDescription = it },
                        label = { Text(if (isUrdu) "تفصیل (اختیاری)" else "Description (Optional)") },
                        placeholder = { Text(if (isUrdu) "مثال: اعمال و دعائیں" else "e.g., Friday morning supplications") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCollectionName.isNotBlank()) {
                            viewModel.createCollection(newCollectionName.trim(), newCollectionDescription.trim())
                            newCollectionName = ""
                            newCollectionDescription = ""
                            showCreateCollectionDialog = false
                        }
                    }
                ) {
                    Text(if (isUrdu) "بنائیں" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCollectionDialog = false }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    val readingZiyarat = selectedZiyaratForReading
    if (readingZiyarat != null) {
        ZiyaratDetailDialog(
            ziyarat = readingZiyarat,
            languageCode = currentLang,
            viewModel = viewModel,
            onDismiss = { selectedZiyaratForReading = null }
        )
    }
}

@Composable
fun DuaListTile(
    dua: ShiaDua,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onSelect: () -> Unit,
    onAddToCollection: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(20.dp))
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = dua.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    // Estimated Read Time Badge
                    val totalWords = dua.lines.sumOf { it.arabic.split("\\s+".toRegex()).size }
                    val readTimeMins = (totalWords / 45).coerceAtLeast(1)
                    Surface(
                        color = Color(0xFFD4AF37).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Estimated Read Time",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$readTimeMins min read",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onAddToCollection != null) {
                        IconButton(
                            onClick = onAddToCollection,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Add to Collection",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isFavorite) Color(0xFFD4AF37) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = dua.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = dua.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dua.relevance,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Read Now", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Book,
                        contentDescription = "Go",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaReadingView(
    viewModel: PrayerViewModel,
    dua: ShiaDua,
    onBack: () -> Unit,
    onPlaybackEnded: () -> Unit = {},
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {},
    onIsPlayingChanged: (Boolean) -> Unit = {},
    shouldPause: Boolean = false,
    onPauseHandled: () -> Unit = {}
) {
    val favoriteDuaIds by viewModel.favoriteDuaIds.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()
    val appFont by viewModel.appFont.collectAsState()
    val isFavorite = favoriteDuaIds.contains(dua.id)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val duaBookmarks by viewModel.duaBookmarks.collectAsState()
    val savedLineIndex = duaBookmarks[dua.id]
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = savedLineIndex ?: 0)

    val scrollProgress by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems <= 1) {
                0f
            } else {
                val visibleItems = listState.layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) {
                    0f
                } else {
                    val lastVisibleItem = visibleItems.last()
                    if (lastVisibleItem.index == totalItems - 1 && 
                        lastVisibleItem.offset + lastVisibleItem.size <= listState.layoutInfo.viewportEndOffset) {
                        1f
                    } else {
                        val firstVisibleItem = listState.firstVisibleItemIndex
                        val firstVisibleItemOffset = listState.firstVisibleItemScrollOffset.toFloat()
                        val firstVisibleItemSize = visibleItems.first().size.toFloat()
                        val fraction = if (firstVisibleItemSize > 0f) firstVisibleItemOffset / firstVisibleItemSize else 0f
                        val progress = (firstVisibleItem + fraction) / (totalItems - 1)
                        progress.coerceIn(0f, 1f)
                    }
                }
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index > 0 || (savedLineIndex != null && index == 0)) {
                    viewModel.setDuaBookmark(dua.id, index)
                }
            }
    }

    LaunchedEffect(dua.id) {
        var elapsedSeconds = 0
        val prefs = context.getSharedPreferences("dua_goals", android.content.Context.MODE_PRIVATE)
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        
        while (true) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds++
            
            // Log dua reading after 5 seconds
            if (elapsedSeconds == 5) {
                val todayStr = dateFormat.format(java.util.Date())
                viewModel.logDuaReading(dua.id, todayStr)
            }
            
            // Periodically save duration every 10 seconds to avoid lost data on sudden exit
            if (elapsedSeconds % 10 == 0) {
                val todayStr = dateFormat.format(java.util.Date())
                val currentSeconds = prefs.getInt("duration_seconds_$todayStr", 0)
                prefs.edit().putInt("duration_seconds_$todayStr", currentSeconds + 10).apply()
            }
        }
    }

    val fontScale = remember {
        val prefs = context.getSharedPreferences("shia_tracker_prefs", android.content.Context.MODE_PRIVATE)
        prefs.getFloat("arabic_urdu_font_scale", 1.0f)
    }

    // Dynamic sizing states
    var arabicFontSize by remember { mutableStateOf(18f * fontScale) }
    var translationFontSize by remember { mutableStateOf(12f * fontScale) }

    // Multi-Language Selection Filters (Toggle States AR, TR, EN, UR)
    var visibleLanguages by remember(currentLang) {
        mutableStateOf(
            if (currentLang == "ur") setOf("AR", "UR")
            else if (currentLang == "ar") setOf("AR")
            else setOf("AR", "TR", "EN")
        )
    }

    // Local Repeat Recitations count tracking state per line index
    val repeatCounts = remember { mutableStateMapOf<Int, Int>() }

    // AI Translation tracking state
    val aiTranslations = remember { mutableStateMapOf<String, String>() }
    val aiTranslatingState = remember { mutableStateMapOf<String, Boolean>() }
    var isTranslatingAll by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    var isFocusMode by remember { mutableStateOf(false) }
    var arabicFontType by remember { mutableStateOf(context.getSharedPreferences("shia_tracker_prefs", android.content.Context.MODE_PRIVATE).getString("arabic_font_type", "Indo-Pak") ?: "Indo-Pak") }
    var isAutoScrollActive by remember { mutableStateOf(false) }


    val focusedIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
            var closestIndex = -1
            var minDiff = Int.MAX_VALUE
            for (item in layoutInfo.visibleItemsInfo) {
                val itemCenter = item.offset + item.size / 2
                val diff = kotlin.math.abs(viewportCenter - itemCenter)
                if (diff < minDiff) {
                    minDiff = diff
                    closestIndex = item.index
                }
            }
            closestIndex
        }
    }


    var currentPlaybackPosition by remember { mutableStateOf(0L) }
    var currentPlaybackDuration by remember { mutableStateOf(0L) }
    var requestedSeekPosition by remember { mutableStateOf<Long?>(null) }
    var manualPlaybackOffset by remember { mutableStateOf(0L) }
    
    LaunchedEffect(currentPlaybackPosition) {
        if (currentPlaybackPosition == 0L) {
            manualPlaybackOffset = 0L
        }
    }
    
    val highlightedLineIndex = remember(currentPlaybackPosition, currentPlaybackDuration, manualPlaybackOffset, dua.lines) {
        if (currentPlaybackDuration <= 0L || dua.lines.isEmpty()) return@remember -1
        val totalChars = dua.lines.sumOf { it.arabic.length.toLong() }
        if (totalChars <= 0) return@remember -1
        
        val adjustedPlaybackPosition = (currentPlaybackPosition - manualPlaybackOffset)
            .coerceIn(0L, currentPlaybackDuration)
        
        var accumulatedTime = 0L
        var foundIndex = -1
        for (i in dua.lines.indices) {
            val lineDuration = (dua.lines[i].arabic.length.toDouble() / totalChars * currentPlaybackDuration).toLong()
            if (adjustedPlaybackPosition in accumulatedTime until (accumulatedTime + lineDuration)) {
                foundIndex = i
                break
            }
            accumulatedTime += lineDuration
        }
        foundIndex
    }

    LaunchedEffect(highlightedLineIndex) {
        if (highlightedLineIndex >= 0) {
            // Only scroll if it's currently not visible or we want to keep it centered
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val isVisible = visibleItems.any { it.index == highlightedLineIndex }
            if (!isVisible) {
                listState.animateScrollToItem(highlightedLineIndex)
            }
        }
    }


    LaunchedEffect(isAutoScrollActive) {
        if (isAutoScrollActive) {
            while(isActive) {
                listState.animateScrollBy(2.0f)
                kotlinx.coroutines.delay(32)
            }
        }
    }

    val prefs = remember { context.getSharedPreferences("shia_tracker_prefs", android.content.Context.MODE_PRIVATE) }
    var readingTheme by remember { mutableStateOf(context.getSharedPreferences("shia_tracker_prefs", android.content.Context.MODE_PRIVATE).getString("reading_theme", "Cream") ?: "Cream") }

    val bgThemeColor = when (readingTheme) {
        "Sepia" -> Color(0xFFF4ECD8)
        "Dark" -> Color(0xFF121212)
        else -> Color(0xFFFAF6EE)
    }
    val textThemeColor = when (readingTheme) {
        "Sepia" -> Color(0xFF433422)
        "Dark" -> Color(0xFFE0E0E0)
        else -> Color(0xFF0F2E27)
    }
    val arabicThemeColor = when (readingTheme) {
        "Sepia" -> Color(0xFF5D4037)
        "Dark" -> Color(0xFF81C784)
        else -> Color(0xFF1B4D3E)
    }
    val cardBgThemeColor = when (readingTheme) {
        "Sepia" -> Color(0xFFEFE5CD)
        "Dark" -> Color(0xFF1E1E1E)
        else -> Color.White
    }
    val cardBorderThemeColor = when (readingTheme) {
        "Sepia" -> Color(0xFFD4BFA7).copy(alpha = 0.5f)
        "Dark" -> Color(0xFF333333)
        else -> Color(0xFF0F2E27).copy(alpha = 0.08f)
    }

    Scaffold(
        bottomBar = {
            if (dua.audioUrl.isNotBlank()) {
                Box(
                    modifier = if (isFocusMode) Modifier.height(0.dp).alpha(0f) else Modifier
                ) {
                    com.example.ui.components.AudioPlayerComponent(
                        audioUrl = dua.audioUrl,
                        onProgressUpdate = { pos, dur ->
                            currentPlaybackPosition = pos
                            currentPlaybackDuration = dur
                        },
                        onPlaybackEnded = onPlaybackEnded,
                        onIsPlayingChanged = onIsPlayingChanged,
                        shouldPause = shouldPause,
                        onPauseHandled = onPauseHandled,
                        seekToPosition = requestedSeekPosition,
                        onSeekHandled = { requestedSeekPosition = null },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        },
        topBar = {
            androidx.compose.animation.AnimatedVisibility(visible = !isFocusMode) {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                text = dua.title,
                                fontWeight = FontWeight.ExtraBold,
                                color = textThemeColor,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Reading Devotional Supplication",
                                style = MaterialTheme.typography.bodySmall,
                                color = textThemeColor.copy(alpha = 0.6f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Go back",
                                tint = textThemeColor
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isFocusMode = true }) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Focus Mode",
                                tint = textThemeColor
                            )
                        }
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = textThemeColor
                            )
                        }
                        IconButton(onClick = { viewModel.toggleDuaFavorite(dua.id) }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isFavorite) Color(0xFFD4AF37) else textThemeColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = bgThemeColor
                    )
                )
            }
        },
        floatingActionButton = {
            androidx.compose.animation.AnimatedVisibility(visible = isFocusMode) {
                FloatingActionButton(
                    onClick = { isFocusMode = false },
                    containerColor = textThemeColor,
                    contentColor = bgThemeColor
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Focus Mode"
                    )
                }
            }
        },
        containerColor = bgThemeColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 3. Language Selection & Theme Filters
            androidx.compose.animation.AnimatedVisibility(visible = showSettings) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Show Filters:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textThemeColor.copy(alpha = 0.7f)
                    )
                    
                    val options = listOf(
                        "AR" to "Arabic",
                        "TR" to "Transliteration",
                        "EN" to "English",
                        "UR" to "Urdu"
                    )

                    options.forEach { (code, label) ->
                        val isActive = visibleLanguages.contains(code)
                        val gradient = if (isActive) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0F2E27), // Deep emerald green
                                    Color(0xFF1B4D3E)  // Jade green
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    cardBgThemeColor,
                                    cardBgThemeColor
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .shadow(if (isActive) 4.dp else 1.dp, CircleShape)
                                .background(gradient, CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = if (isActive) Color.Transparent else textThemeColor.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    visibleLanguages = if (isActive) {
                                        if (visibleLanguages.size > 1) visibleLanguages - code else visibleLanguages
                                    } else {
                                        visibleLanguages + code
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = code,
                                color = if (isActive) Color(0xFFD4AF37) else textThemeColor.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Reading Theme:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textThemeColor.copy(alpha = 0.7f)
                    )

                    val themes = listOf(
                        "Cream" to Color(0xFFFAF6EE),
                        "Sepia" to Color(0xFFF4ECD8),
                        "Dark" to Color(0xFF121212)
                    )

                    themes.forEach { (tName, tColor) ->
                        val isSelected = readingTheme == tName
                        Box(
                            modifier = Modifier
                                .shadow(if (isSelected) 4.dp else 1.dp, CircleShape)
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF0F2E27), Color(0xFF1B4D3E))
                                        )
                                    } else {
                                        Brush.horizontalGradient(colors = listOf(cardBgThemeColor, cardBgThemeColor))
                                    },
                                    CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFFD4AF37) else textThemeColor.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    readingTheme = tName
                                    prefs.edit().putString("reading_theme", tName).apply()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(tColor)
                                        .border(0.5.dp, Color.Gray, CircleShape)
                                )
                                Text(
                                    text = if (tName == "Dark") "Night" else tName,
                                    color = if (isSelected) Color(0xFFD4AF37) else textThemeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Professional Custom Font Size Sliders Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgThemeColor),
                    border = BorderStroke(1.dp, cardBorderThemeColor)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Font Sizer",
                                tint = textThemeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Adjust Readability",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textThemeColor
                            )
                        }

                        // Arabic Font Size Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Arabic",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textThemeColor.copy(alpha = 0.6f),
                                modifier = Modifier.width(60.dp)
                            )
                            Slider(
                                value = arabicFontSize,
                                onValueChange = { arabicFontSize = it },
                                valueRange = 18f..40f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFD4AF37),
                                    activeTrackColor = arabicThemeColor,
                                    inactiveTrackColor = textThemeColor.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.weight(1f).height(32.dp)
                            )
                            Text(
                                text = "${arabicFontSize.toInt()}sp",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textThemeColor,
                                modifier = Modifier.width(35.dp),
                                textAlign = TextAlign.End
                            )
                        }

                        // Translation Font Size Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Meaning",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textThemeColor.copy(alpha = 0.6f),
                                modifier = Modifier.width(60.dp)
                            )
                            Slider(
                                value = translationFontSize,
                                onValueChange = { translationFontSize = it },
                                valueRange = 11f..25f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFD4AF37),
                                    activeTrackColor = arabicThemeColor,
                                    inactiveTrackColor = textThemeColor.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.weight(1f).height(32.dp)
                            )
                            Text(
                                text = "${translationFontSize.toInt()}sp",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textThemeColor,
                                modifier = Modifier.width(35.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                // New Controls
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Scroll", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textThemeColor)
                    Switch(
                        checked = isAutoScrollActive,
                        onCheckedChange = { isAutoScrollActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD4AF37), checkedTrackColor = arabicThemeColor)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Arabic Script: $arabicFontType", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textThemeColor)
                    Switch(
                        checked = (arabicFontType == "Indo-Pak"),
                        onCheckedChange = {
                            val newType = if(it) "Indo-Pak" else "Uthmani"
                            arabicFontType = newType
                            prefs.edit().putString("arabic_font_type", newType).apply()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD4AF37), checkedTrackColor = arabicThemeColor)
                    )
                }

            }
            }

            // AI Bulk Translation integration block styled professionally
            if (visibleLanguages.contains("UR")) {
                val untranslatedIndices = dua.lines.indices.filter { idx ->
                    val line = dua.lines[idx]
                    line.urduTranslation == null && !aiTranslations.containsKey("${idx}_Urdu")
                }

                if (untranslatedIndices.isNotEmpty()) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isTranslatingAll = true
                                untranslatedIndices.forEach { idx ->
                                    val line = dua.lines[idx]
                                    aiTranslatingState["${idx}_Urdu"] = true
                                    val res = translateWithGemini(line.arabic, line.translation, "Urdu")
                                    aiTranslations["${idx}_Urdu"] = res
                                    aiTranslatingState["${idx}_Urdu"] = false
                                }
                                isTranslatingAll = false
                            }
                        },
                        enabled = !isTranslatingAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F2E27).copy(alpha = 0.1f),
                            contentColor = Color(0xFF0F2E27)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .border(1.dp, Color(0xFF0F2E27).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    ) {
                        if (isTranslatingAll) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF0F2E27))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("A.I. Translating Lines...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("✨ Translate Missing Urdu Lines with Gemini AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

            // 4. Dua Cards Main Content LazyColumn
            
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                ) {
                    itemsIndexed(dua.lines) { index, line ->
                        val isBookmarkedLine = savedLineIndex == index
                    val isAudioHighlighted = highlightedLineIndex == index
                    val isCenterFocused = isFocusMode && (focusedIndex == index)
                    
                    val cardAlpha by animateFloatAsState(targetValue = if (isFocusMode && !isCenterFocused && !isAudioHighlighted) 0.4f else 1f, label = "cardAlpha")
                    val cardScale by animateFloatAsState(targetValue = if (isCenterFocused || isAudioHighlighted) 1.02f else 1f, label = "cardScale")
                    val cardElevation by animateDpAsState(targetValue = if (isCenterFocused || isAudioHighlighted) 8.dp else 3.dp, label = "cardElevation")
                    val borderWidth by animateDpAsState(targetValue = if (isCenterFocused || isAudioHighlighted) 2.dp else 1.dp, label = "borderWidth")

                    // Glass-effect card container with rounded corners, shadows, light border
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(cardAlpha)
                            .scale(cardScale)
                            .shadow(cardElevation, RoundedCornerShape(24.dp))
                            .border(
                                width = borderWidth,
                                color = if (isAudioHighlighted || isCenterFocused) Color(0xFF4CAF50).copy(alpha = 0.8f) else if (isBookmarkedLine) Color(0xFFD4AF37).copy(alpha = 0.5f) else cardBorderThemeColor,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable {
                                isFocusMode = !isFocusMode
                                if (dua.audioUrl.isNotBlank() && currentPlaybackDuration > 0L) {
                                    val totalChars = dua.lines.sumOf { it.arabic.length.toLong() }
                                    if (totalChars > 0) {
                                        var expectedStartTime = 0L
                                        for (i in 0 until index) {
                                            val lineDuration = (dua.lines[i].arabic.length.toDouble() / totalChars * currentPlaybackDuration).toLong()
                                            expectedStartTime += lineDuration
                                        }
                                        manualPlaybackOffset = currentPlaybackPosition - expectedStartTime
                                    }
                                }
                            },

                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAudioHighlighted) {
                                if (readingTheme == "Dark") Color(0xFF1B3B24) else Color(0xFFE8F5E9)
                            } else if (isBookmarkedLine) {
                                if (readingTheme == "Dark") Color(0xFF2C2514) else Color(0xFFFBF1D5)
                            } else cardBgThemeColor
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            // Top interactive row containing bookmark / repeat badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = textThemeColor.copy(alpha = 0.08f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = textThemeColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Custom Card Maker action
                                    IconButton(
                                        onClick = {
                                            val urTranslation = line.urduTranslation ?: aiTranslations["${index}_Urdu"] ?: ""
                                            onEditCard(
                                                com.example.api.ShiaQuote(
                                                    arabic = line.arabic,
                                                    english = line.translation,
                                                    urdu = urTranslation,
                                                    source = dua.title
                                                )
                                            )
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ColorLens,
                                            contentDescription = "Edit in Cards Maker",
                                            tint = if (readingTheme == "Dark") Color(0xFFBCAAA4) else Color(0xFF8B7355),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Bookmark/Resume Action
                                    IconButton(
                                        onClick = { 
                                            if (isBookmarkedLine) {
                                                viewModel.setDuaBookmark(dua.id, -1)
                                            } else {
                                                viewModel.setDuaBookmark(dua.id, index) 
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isBookmarkedLine) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Bookmark resume point",
                                            tint = if (isBookmarkedLine) Color(0xFFD4AF37) else textThemeColor.copy(alpha = 0.4f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Interactive repeat count badge (Tap to increment)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(textThemeColor.copy(alpha = 0.06f))
                                            .clickable {
                                                val count = repeatCounts[index] ?: 0
                                                repeatCounts[index] = count + 1
                                            }
                                            .border(1.dp, textThemeColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Autorenew,
                                                contentDescription = "Repeat Count",
                                                tint = textThemeColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Repeat: ${repeatCounts[index] ?: 0}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textThemeColor
                                            )
                                        }
                                    }
                                }
                            }

                            // 1. Beautiful large Arabic text at the top
                            if (visibleLanguages.contains("AR")) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = line.arabic,
                                    fontSize = arabicFontSize.sp,
                                    fontFamily = if (arabicFontType == "Indo-Pak") com.example.ui.theme.NoorehudaFont else AmiriFont,
                                    fontWeight = FontWeight.Bold,
                                    color = arabicThemeColor,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                            }

                            // 2. Soft, italic style Transliteration right below it
                            if (visibleLanguages.contains("TR")) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = line.transliteration,
                                    fontSize = (translationFontSize - 1).sp,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Medium,
                                    color = if (readingTheme == "Dark") Color(0xFFCEB195) else Color(0xFF8B7355),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                            }

                            // 3. Clean, readable Nastaliq Urdu Translation
                            if (visibleLanguages.contains("UR")) {
                                val urduText = line.urduTranslation ?: aiTranslations["${index}_Urdu"]
                                val isAi = line.urduTranslation == null && urduText != null
                                val isTranslating = aiTranslatingState["${index}_Urdu"] ?: false

                                if (urduText != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = if (isAi) {
                                                    if (readingTheme == "Dark") Color(0xFF1B2C27) else Color(0xFFE3ECE9)
                                                } else {
                                                    if (readingTheme == "Dark") Color(0xFF262016) else Color(0xFFF3ECE1)
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (isAi) "A.I. Translation" else "Urdu Translation",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (readingTheme == "Dark") Color(0xFFCEB195) else Color(0xFF8B7355)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = urduText,
                                            fontSize = (arabicFontSize * 0.85f).sp,
                                            fontFamily = NotoNastaliqUrduFont,
                                            fontWeight = FontWeight.Bold,
                                            color = textThemeColor,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                } else if (isTranslating) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = textThemeColor)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("A.I. Translating line...", fontSize = 11.sp, color = textThemeColor.copy(alpha = 0.6f))
                                    }
                                }
                            }

                            // 4. Modern sans-serif English Translation at the bottom
                            if (visibleLanguages.contains("EN")) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = line.translation,
                                    fontSize = translationFontSize.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = textThemeColor.copy(alpha = 0.85f),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                    }
                }

            // Subtle Progress Indicator at the bottom of the long Dua
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgThemeColor)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reading Progress",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textThemeColor.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${(scrollProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (readingTheme == "Dark") Color(0xFF81C784) else Color(0xFF1B4D3E)
                        )
                    }
                    // Progress track & bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(textThemeColor.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(scrollProgress)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFD4AF37), arabicThemeColor)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCollectionDialog(
    duaId: String?,
    ziyaratId: String?,
    allCollections: List<MyCollection>,
    onAdd: (MyCollection) -> Unit,
    onRemove: (MyCollection) -> Unit,
    onCreateCollection: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add to Collection", fontWeight = FontWeight.Bold, color = Color(0xFF0F2E27)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select a religious folder/collection to group this prayer:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                if (allCollections.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No custom collections yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allCollections) { collection ->
                            val isAdded = if (duaId != null) {
                                collection.duaIds.split(",").contains(duaId)
                            } else if (ziyaratId != null) {
                                collection.ziyaratIds.split(",").contains(ziyaratId)
                            } else {
                                false
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAdded) Color(0xFF0F2E27).copy(alpha = 0.06f) else Color(0xFFF5F5F5)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isAdded) Color(0xFF0F2E27).copy(alpha = 0.3f) else Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isAdded) {
                                            onRemove(collection)
                                        } else {
                                            onAdd(collection)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (isAdded) Icons.Default.FolderSpecial else Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = if (isAdded) Color(0xFFD4AF37) else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = collection.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F2E27)
                                        )
                                    }
                                    Checkbox(
                                        checked = isAdded,
                                        onCheckedChange = { checked ->
                                            if (checked) onAdd(collection) else onRemove(collection)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF0F2E27)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (showCreateDialog) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F9F9), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "New Collection",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F2E27)
                        )
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("Folder Name (e.g. Laylatul Qadr)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 13.sp)
                        )
                        OutlinedTextField(
                            value = newDesc,
                            onValueChange = { newDesc = it },
                            placeholder = { Text("Description (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 13.sp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { showCreateDialog = false }) {
                                Text("Cancel", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    if (newName.isNotBlank()) {
                                        onCreateCollection(newName.trim(), newDesc.trim())
                                        newName = ""
                                        newDesc = ""
                                        showCreateDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2E27)),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("Create", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF0F2E27)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F2E27)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create New Collection")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2E27))
            ) {
                Text("Done")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}


@Composable
fun FolderCard(
    modifier: Modifier = Modifier,
    titleEng: String,
    titleUr: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    count: Int,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.let {
        (0.299f * it.red + 0.587f * it.green + 0.114f * it.blue) < 0.5f
    }
    val accent = if (isDark) MaterialTheme.colorScheme.primary else color
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = if (isDark) 0.22f else 0.16f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
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
                        .background(accent.copy(alpha = 0.11f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = titleEng,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.09f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$count",
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column {
                Text(
                    text = titleUr,
                    fontFamily = NotoNastaliqUrduFont,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = titleEng,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
