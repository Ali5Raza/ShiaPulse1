package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: PrayerViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    onOpenQuran: ((surahNumber: Int, ayahNumber: Int) -> Unit)? = null
) {
    var subScreen by remember { mutableIntStateOf(0) } // 0 = Menu, 1 = Calendar, 2 = Duas, 3 = Khums, 4 = Settings, 5 = Books, 7 = Quran
    var quranRoute by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val context = LocalContext.current
    val currentLang by viewModel.appLanguage.collectAsState()
    val openQuran: (Int, Int) -> Unit = onOpenQuran ?: { surahNumber, ayahNumber ->
        quranRoute = surahNumber to ayahNumber
        subScreen = 7
    }

    when (subScreen) {
        0 -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Library & Tools",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                modifier = modifier
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        LibraryCard(
                            title = "Find Nearby Shia Mosques",
                            subtitle = "Locate Islamic centers near your current location.",
                            icon = Icons.Default.LocationOn,
                            onClick = {
                                val gmmIntentUri = Uri.parse("geo:0,0?q=Shia+mosques+near+me")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                try {
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    val fallbackUri = Uri.parse("https://www.google.com/maps/search/Shia+mosques+near+me")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
                                }
                            }
                        )
                    }

                    item {
                        LibraryCard(
                            title = "Islamic Calendar",
                            subtitle = "View major Islamic events & historically significant dates.",
                            icon = Icons.Default.DateRange,
                            onClick = { subScreen = 1 }
                        )
                    }

                    item {
                        LibraryCard(
                            title = "Duas & Ziyarat",
                            subtitle = "A collection of essential Shia supplications.",
                            icon = Icons.Default.List,
                            onClick = { subScreen = 2 }
                        )
                    }

                    item {
                        LibraryCard(
                            title = "14 Masoomeen Ziyarats",
                            subtitle = "Searchable database & audio clips of sacred Ziyarats linked to the Hijri calendar.",
                            icon = Icons.Default.List,
                            onClick = { subScreen = 6 }
                        )
                    }

                    item {
                        LibraryCard(
                            title = "Islamic Calculator",
                            subtitle = "A comprehensive calculator for Khums, Zakat, inheritance, traveler limitations, and dates.",
                            icon = Icons.Default.Edit,
                            onClick = { subScreen = 3 }
                        )
                    }

                }
            }
        }
        1 -> CalendarScreen(viewModel = viewModel, modifier = modifier, onBack = { subScreen = 0 })
        2 -> DuasScreen(viewModel = viewModel, onBack = { subScreen = 0 }, modifier = modifier)
        3 -> KhumsScreen(
            viewModel = viewModel,
            onBack = { subScreen = 0 },
            onOpenQuran = openQuran,
            modifier = modifier
        )

        6 -> ZiyaratsScreen(viewModel = viewModel, onBack = { subScreen = 0 }, modifier = modifier)
        7 -> quranRoute?.let { route ->
            QuranScreen(
                currentLang = currentLang,
                onBack = {
                    quranRoute = null
                    subScreen = 3
                },
                modifier = modifier,
                initialSurahNumber = route.first,
                initialAyahNumber = route.second
            )
        }
    }
}

@Composable
fun LibraryCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
