package com.example.ui.academy

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun DailyDuasScreen(
    onBack: () -> Unit,
    currentLang: String,
    content: AcademyContent,
    repository: AcademyProgressRepository,
    speech: AcademySpeechController
) {
    var selectedDuaId by rememberSaveable { mutableStateOf(-1) }
    var filter by rememberSaveable { mutableStateOf("all") }
    val progress by repository.progress.collectAsState()
    val selectedDua = content.duas.firstOrNull { it.id == selectedDuaId }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun handleBack() {
        speech.stop()
        if (selectedDuaId >= 0) selectedDuaId = -1 else onBack()
    }

    BackHandler { handleBack() }
    LaunchedEffect(selectedDuaId) { speech.stop() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AcademyTopBar(
                title = selectedDua?.title?.get(currentLang) ?: tr(currentLang, "Daily Duas", "روزانہ دعائیں", "الأدعية اليومية"),
                onBack = ::handleBack,
                accent = Color(0xFFEF6C00),
                currentLang = currentLang
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            if (selectedDua == null) {
                val filteredDuas = when (filter) {
                    "favorites" -> content.duas.filter { it.id in progress.favoriteDuas }
                    "unread" -> content.duas.filter { it.id !in progress.readDuas }
                    else -> content.duas
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            tr(currentLang, "Learn a dua for every part of your day", "دن کے ہر حصے کے لیے دعا سیکھیں", "تعلم دعاءً لكل جزء من يومك"),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEF6C00),
                            lineHeight = 30.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            FilterChip(
                                selected = filter == "all",
                                onClick = { filter = "all" },
                                label = { Text(tr(currentLang, "All", "سب", "الكل")) }
                            )
                            Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = filter == "favorites",
                                onClick = { filter = "favorites" },
                                label = { Text(tr(currentLang, "Favorites", "پسندیدہ", "المفضلة")) }
                            )
                            Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = filter == "unread",
                                onClick = { filter = "unread" },
                                label = { Text(tr(currentLang, "Unread", "باقی", "غير المقروءة")) }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    if (filteredDuas.isEmpty()) {
                        item {
                            Text(
                                if (filter == "favorites") {
                                    tr(currentLang, "Tap the heart on a dua to save it here.", "دعا کو یہاں محفوظ کرنے کے لیے دل کا نشان دبائیں۔", "اضغط القلب على الدعاء لحفظه هنا.")
                                } else {
                                    tr(currentLang, "MashaAllah—every dua has been read!", "ما شاء اللہ — تمام دعائیں پڑھ لی گئیں!", "ما شاء الله، قرأت كل الأدعية!")
                                },
                                modifier = Modifier.padding(24.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(filteredDuas, key = { it.id }) { dua ->
                        val palette = academyPalette(dua.id)
                        val isRead = dua.id in progress.readDuas
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedDuaId = dua.id },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = palette.container),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        when (dua.id) {
                                            1 -> Icons.Rounded.WbSunny
                                            2 -> Icons.Rounded.NightsStay
                                            3, 4 -> Icons.Rounded.Restaurant
                                            5, 6 -> Icons.Rounded.Home
                                            7, 8 -> Icons.Rounded.Mosque
                                            9, 10 -> Icons.Rounded.MenuBook
                                            11 -> Icons.Rounded.FamilyRestroom
                                            13 -> Icons.Rounded.WaterDrop
                                            14 -> Icons.Rounded.Flight
                                            else -> Icons.Rounded.Favorite
                                        },
                                        contentDescription = null,
                                        tint = palette.accent,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        dua.title.get(currentLang),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = palette.accent,
                                        lineHeight = 25.sp
                                    )
                                    if (isRead) {
                                        Text(
                                            tr(currentLang, "Read • saved progress", "پڑھا گیا • محفوظ", "مقروء • محفوظ"),
                                            fontSize = 12.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                                if (dua.id in progress.favoriteDuas) {
                                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color(0xFFC2185B))
                                } else if (isRead) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                                }
                            }
                        }
                    }
                }
            } else {
                val dua = selectedDua
                val palette = academyPalette(dua.id)
                val isRead = dua.id in progress.readDuas
                Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Card(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.container)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                dua.arabic,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238),
                                textAlign = TextAlign.Center,
                                lineHeight = 46.sp
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                dua.transliteration,
                                fontSize = 17.sp,
                                color = palette.accent,
                                textAlign = TextAlign.Center,
                                lineHeight = 26.sp
                            )
                            Spacer(Modifier.height(18.dp))
                            Text(
                                "“${dua.translation.get(currentLang)}”",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238),
                                textAlign = TextAlign.Center,
                                lineHeight = 29.sp
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                tr(currentLang, "Source for parents: ", "والدین کے لیے ماخذ: ", "المصدر للوالدين: ") + dua.source,
                                fontSize = 12.sp,
                                color = Color(0xFF5F6368),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { repository.toggleDuaFavorite(dua.id) },
                            modifier = Modifier.size(58.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(
                                if (dua.id in progress.favoriteDuas) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = tr(currentLang, "Favorite", "پسندیدہ", "مفضلة"),
                                tint = Color(0xFFC2185B),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        AcademyAudioButton(
                            id = "dua-${dua.id}",
                            text = dua.arabic,
                            audioLanguage = "ar",
                            currentLang = currentLang,
                            speech = speech,
                            accent = palette.accent
                        )
                        IconButton(
                            onClick = {
                                val awarded = repository.markDuaRead(dua.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (awarded > 0) {
                                            "+$awarded " + tr(currentLang, "points • Dua marked as read", "پوائنٹس • دعا مکمل محفوظ", "نقاط • تم حفظ قراءة الدعاء")
                                        } else {
                                            tr(currentLang, "Dua is already in your saved progress", "یہ دعا پہلے سے محفوظ پیش رفت میں ہے", "هذا الدعاء محفوظ في تقدمك")
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.size(58.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(
                                if (isRead) Icons.Rounded.CheckCircle else Icons.Rounded.Check,
                                contentDescription = if (isRead) tr(currentLang, "Read", "پڑھا گیا", "مقروء") else tr(currentLang, "Mark as read", "پڑھا گیا نشان لگائیں", "تحديد كمقروء"),
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
