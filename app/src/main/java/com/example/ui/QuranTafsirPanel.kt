package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranAyah
import com.example.data.QuranSurahInfo
import com.example.data.QuranTafsirEntry
import com.example.data.QuranTafsirRepository
import com.example.data.QuranTafsirSearchPage
import com.example.data.QuranTafsirSource
import com.example.ui.theme.AmiriFont
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun QuranTafsirSection(
    info: QuranSurahInfo,
    ayah: QuranAyah,
    languageCode: String,
    palette: QuranReadingPalette
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val reference = "${info.number}:${ayah.number}"
    val preferences = remember {
        context.getSharedPreferences("quran_tafsir_preferences", Context.MODE_PRIVATE)
    }
    var selectedSourceId by rememberSaveable {
        mutableStateOf(
            preferences.getString("selected_source", QuranTafsirRepository.defaultSourceId)
                ?: QuranTafsirRepository.defaultSourceId
        )
    }
    var sourceMenuOpen by remember { mutableStateOf(false) }
    var entry by remember(reference, selectedSourceId) { mutableStateOf<QuranTafsirEntry?>(null) }
    var loading by remember(reference, selectedSourceId) { mutableStateOf(true) }
    var failed by remember(reference, selectedSourceId) { mutableStateOf(false) }
    var retry by remember(reference, selectedSourceId) { mutableIntStateOf(0) }
    val selectedSource = QuranTafsirRepository.source(selectedSourceId)

    LaunchedEffect(reference, selectedSourceId, retry) {
        loading = true
        failed = false
        entry = null
        try {
            entry = withContext(Dispatchers.IO) {
                QuranTafsirRepository.load(context, selectedSourceId, info.number, ayah.number)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                quranStudyText("tafsir", languageCode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.accent
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { sourceMenuOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        selectedSource.name,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = sourceMenuOpen,
                    onDismissRequest = { sourceMenuOpen = false }
                ) {
                    QuranTafsirRepository.sources.forEach { sourceOption ->
                        DropdownMenuItem(
                            text = { QuranTafsirSourceLabel(sourceOption) },
                            onClick = {
                                selectedSourceId = sourceOption.id
                                preferences.edit().putString("selected_source", sourceOption.id).apply()
                                sourceMenuOpen = false
                            }
                        )
                    }
                }
            }

            Text(
                quranStudyText("author", languageCode) + ": " + selectedSource.author,
                style = MaterialTheme.typography.bodySmall,
                color = palette.text
            )
            Text(
                quranStudyText("language", languageCode) + ": " + selectedSource.languageLabel,
                style = MaterialTheme.typography.bodySmall,
                color = palette.text
            )
            Text(
                quranStudyText("source", languageCode) + ": " + selectedSource.provider,
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondaryText
            )
            Text(
                selectedSource.termsLabel,
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryText
            )
            TextButton(onClick = { openQuranUri(context, selectedSource.termsUrl) }) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(quranStudyText("source_terms", languageCode))
            }

            when {
                loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(quranStudyText("tafsir_loading", languageCode), color = palette.secondaryText)
                }
                failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        quranStudyText("tafsir_error", languageCode),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryText
                    )
                    TextButton(onClick = { retry++ }) { Text(quranStudyText("retry", languageCode)) }
                }
                entry == null -> Text(
                    quranStudyText("tafsir_unavailable", languageCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryText
                )
                else -> {
                    val loadedEntry = entry!!
                    if (loadedEntry.mappedAyahs.size > 1) {
                        Text(
                            quranStudyText("shared_tafsir_block", languageCode) + ": " +
                                loadedEntry.mappedAyahs.joinToString(", ") { "${info.number}:$it" },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryText
                        )
                    }
                    SelectionContainer {
                        Text(
                            loadedEntry.text,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = if (selectedSource.language == "ar") AmiriFont else null,
                            fontSize = if (selectedSource.language == "ar") 20.sp else 16.sp,
                            lineHeight = if (selectedSource.language == "ar") 35.sp else 25.sp,
                            textAlign = if (selectedSource.language == "ar") TextAlign.Right else TextAlign.Start,
                            color = palette.text
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(tafsirShareText(loadedEntry, selectedSource, reference)))
                            Toast.makeText(
                                context,
                                quranStudyText("copied", languageCode),
                                Toast.LENGTH_SHORT
                            ).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(quranStudyText("copy_tafsir", languageCode))
                        }
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    tafsirShareText(loadedEntry, selectedSource, reference)
                                )
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(intent, quranStudyText("share_tafsir", languageCode))
                                )
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(quranStudyText("share_tafsir", languageCode))
                        }
                        TextButton(onClick = { openQuranUri(context, loadedEntry.sourceUrl) }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(quranStudyText("open_source", languageCode))
                        }
                    }
                    Text(
                        quranStudyText("tafsir_cache_note", languageCode),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryText
                    )
                    Text(
                        quranStudyText("tafsir_format_note", languageCode),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun QuranTafsirSourceLabel(source: QuranTafsirSource) {
    Column(modifier = Modifier.width(300.dp)) {
        Text(source.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(source.author, style = MaterialTheme.typography.bodySmall)
        Text(
            source.languageLabel + " · " + source.provider,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun QuranTafsirSearchPanel(
    surahs: List<QuranSurahInfo>,
    languageCode: String,
    onOpenReference: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var selectedSourceId by rememberSaveable { mutableStateOf("") }
    var selectedLanguage by rememberSaveable { mutableStateOf("") }
    var page by remember { mutableStateOf(QuranTafsirSearchPage(emptyList(), 0, 0)) }
    var searching by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(query, selectedSourceId, selectedLanguage) {
        val searchQuery = query.trim()
        if (searchQuery.length < 2) {
            page = QuranTafsirSearchPage(emptyList(), 0, 0)
            searching = false
            failed = false
            return@LaunchedEffect
        }
        searching = true
        failed = false
        delay(300)
        try {
            page = withContext(Dispatchers.IO) {
                QuranTafsirRepository.searchCached(
                    context = context,
                    query = searchQuery,
                    sourceId = selectedSourceId.ifBlank { null },
                    language = selectedLanguage.ifBlank { null }
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            page = QuranTafsirSearchPage(emptyList(), 0, 0)
            failed = true
        } finally {
            searching = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            placeholder = { Text(quranStudyText("search_cached_tafsir", languageCode)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = quranToolText("clear", languageCode))
                    }
                }
            },
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedSourceId.isBlank(),
                onClick = { selectedSourceId = "" },
                label = { Text(quranStudyText("all_sources", languageCode)) }
            )
            QuranTafsirRepository.sources.forEach { sourceOption ->
                FilterChip(
                    selected = selectedSourceId == sourceOption.id,
                    onClick = {
                        selectedSourceId = if (selectedSourceId == sourceOption.id) "" else sourceOption.id
                    },
                    label = { Text(sourceOption.id.substringAfter('.').uppercase()) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("" to "all_languages", "en" to "english", "ar" to "arabic").forEach { (code, key) ->
                FilterChip(
                    selected = selectedLanguage == code,
                    onClick = { selectedLanguage = code },
                    label = { Text(quranStudyText(key, languageCode)) }
                )
            }
        }
        Text(
            quranStudyText("cached_tafsir_only", languageCode),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (searching) {
            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = 12.dp).size(24.dp),
                strokeWidth = 2.dp
            )
        }
        if (failed) {
            Text(
                quranStudyText("tafsir_search_error", languageCode),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (query.trim().length >= 2 && !searching && !failed) {
            Text(
                "${page.totalMatches} " + quranStudyText("matches", languageCode) +
                    " · ${page.surahCount} " + quranStudyText("surahs", languageCode) +
                    if (page.totalMatches > page.results.size) {
                        " · " + quranStudyText("showing_first", languageCode) + " ${page.results.size}"
                    } else {
                        ""
                    },
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (query.trim().length >= 2 && !searching && page.results.isEmpty()) {
                item {
                    Text(
                        quranStudyText("no_cached_tafsir_matches", languageCode),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(page.results) { result ->
                val source = QuranTafsirRepository.source(result.sourceId)
                val info = surahs.firstOrNull { it.number == result.surahNumber }
                val surahName = info?.let {
                    if (languageCode == "en") it.name else it.nameAr
                } ?: quranStudyText("surah", languageCode)
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        TextButton(onClick = {
                            onOpenReference(result.surahNumber, result.ayahNumber)
                        }) {
                            Text("$surahName · ${result.surahNumber}:${result.ayahNumber}")
                        }
                        Text(
                            source.name + " · " + source.languageLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            quranTafsirSnippet(result.text, query),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 7,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = if (source.language == "ar") AmiriFont else null,
                            textAlign = if (source.language == "ar") TextAlign.Right else TextAlign.Start
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

private fun tafsirShareText(
    entry: QuranTafsirEntry,
    source: QuranTafsirSource,
    reference: String
): String = buildString {
    append(entry.text)
    append("\n\n")
    append(source.name)
    append("\n")
    append(source.author)
    append("\n")
    append(source.languageLabel)
    append(" · ")
    append(reference)
    append("\n")
    append(source.provider)
    append("\n")
    append(entry.sourceUrl)
    append("\nTerms: ")
    append(source.termsUrl)
    append("\nPresentation formatting normalized; source wording retained.")
}

private fun quranTafsirSnippet(text: String, query: String): String {
    val index = text.indexOf(query.trim(), ignoreCase = true)
    val start = if (index >= 0) (index - 120).coerceAtLeast(0) else 0
    val end = (start + 520).coerceAtMost(text.length)
    return buildString {
        if (start > 0) append('…')
        append(text.substring(start, end).trim())
        if (end < text.length) append('…')
    }
}

private fun openQuranUri(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
