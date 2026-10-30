package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.QuranAyah
import com.example.data.QuranAyahMetadata
import com.example.data.QuranAyahMetadataRepository
import com.example.data.QuranCollection
import com.example.data.QuranStudyState
import com.example.data.QuranStudyStore
import com.example.data.QuranSurahInfo
import com.example.data.QuranWord
import com.example.ui.theme.AmiriFont
import com.example.ui.theme.NoorehudaFont
import com.example.ui.theme.NotoNastaliqUrduFont
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun QuranStudyAyahDialog(
    info: QuranSurahInfo,
    ayah: QuranAyah,
    languageCode: String,
    palette: QuranReadingPalette,
    arabicFontType: String,
    arabicFontScale: Float,
    translationFontScale: Float,
    words: List<QuranWord>?,
    wordsLoading: Boolean,
    wordsFailed: Boolean,
    note: String,
    collections: List<QuranCollection>,
    isBookmarked: Boolean,
    onDismiss: () -> Unit,
    onBookmark: () -> Unit,
    onPlayAyah: () -> Unit,
    onPlayWord: (String) -> Unit,
    onRetryWords: () -> Unit,
    onSaveNote: (String) -> Unit,
    onToggleCollection: (String) -> Unit,
    onNavigateAyah: (Int) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val reference = "${info.number}:${ayah.number}"
    var noteDraft by remember(reference) { mutableStateOf(note) }
    var metadata by remember(reference) { mutableStateOf<QuranAyahMetadata?>(null) }
    var metadataLoading by remember(reference) { mutableStateOf(true) }
    var metadataFailed by remember(reference) { mutableStateOf(false) }
    var metadataRetry by remember(reference) { mutableStateOf(0) }

    LaunchedEffect(reference, metadataRetry) {
        metadataLoading = true
        metadataFailed = false
        try {
            metadata = withContext(Dispatchers.IO) {
                QuranAyahMetadataRepository.load(context, info.number, ayah.number)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            metadataFailed = true
        } finally {
            metadataLoading = false
        }
    }

    val shareText = buildString {
        append(ayah.arabic)
        append("\n\n")
        append(ayah.english)
        append("\n\n")
        append(ayah.urdu)
        append("\n\n")
        append(info.name)
        append(" ")
        append(reference)
        append(" · ShiaPulse")
    }

    fun copyText(text: String) {
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(context, quranStudyText("copied", languageCode), Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = palette.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            quranStudyText("study_ayah", languageCode),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.text
                        )
                        Text(
                            "${info.name} · $reference",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.secondaryText
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = quranStudyText("close", languageCode),
                            tint = palette.text
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = ayah.arabic,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = if (arabicFontType == "Indo-Pak") NoorehudaFont else AmiriFont,
                            fontSize = (28 * arabicFontScale).sp,
                            lineHeight = (46 * arabicFontScale).sp,
                            textAlign = TextAlign.Right,
                            color = palette.arabic
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onPlayAyah) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(quranStudyText("play", languageCode))
                        }
                        OutlinedButton(onClick = onBookmark) {
                            Icon(
                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(quranStudyText("bookmark", languageCode))
                        }
                        OutlinedButton(onClick = {
                            runCatching {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, quranStudyText("share", languageCode)))
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(quranStudyText("share", languageCode))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(onClick = { copyText(ayah.arabic) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(quranStudyText("copy_arabic", languageCode))
                        }
                        TextButton(onClick = { copyText(ayah.english + "\n\n" + ayah.urdu) }) {
                            Text(quranStudyText("copy_translations", languageCode))
                        }
                        TextButton(onClick = { copyText(shareText) }) {
                            Text(quranStudyText("copy_both", languageCode))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateAyah(ayah.number - 1) },
                            enabled = ayah.number > 1
                        ) {
                            Text(quranStudyText("previous_ayah", languageCode))
                        }
                        Text(reference, color = palette.secondaryText, fontWeight = FontWeight.SemiBold)
                        OutlinedButton(
                            onClick = { onNavigateAyah(ayah.number + 1) },
                            enabled = ayah.number < info.verseCount
                        ) {
                            Text(quranStudyText("next_ayah", languageCode))
                        }
                    }

                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                quranStudyText("ayah_details", languageCode),
                                fontWeight = FontWeight.Bold,
                                color = palette.accent
                            )
                            Text(
                                quranStudyText("revelation", languageCode) + ": " +
                                    quranStudyText(
                                        if (info.revelationType.equals("Meccan", ignoreCase = true)) "meccan" else "medinan",
                                        languageCode
                                    ),
                                color = palette.secondaryText
                            )
                            when {
                                metadataLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(quranStudyText("metadata_loading", languageCode), color = palette.secondaryText)
                                }
                                metadataFailed -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        quranStudyText("metadata_error", languageCode),
                                        modifier = Modifier.weight(1f),
                                        color = palette.secondaryText,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    TextButton(onClick = { metadataRetry++ }) {
                                        Text(quranStudyText("retry", languageCode))
                                    }
                                }
                                metadata != null -> {
                                    val data = metadata!!
                                    val location = listOfNotNull(
                                        data.juzNumber?.let { quranStudyText("juz", languageCode) + " $it" },
                                        data.pageNumber?.let { quranStudyText("page", languageCode) + " $it" },
                                        data.hizbNumber?.let { quranStudyText("hizb", languageCode) + " $it" },
                                        data.rubElHizbNumber?.let { quranStudyText("rub_hizb", languageCode) + " $it" },
                                        data.rukuNumber?.let { quranStudyText("ruku", languageCode) + " $it" },
                                        data.manzilNumber?.let { quranStudyText("manzil", languageCode) + " $it" }
                                    ).joinToString(" · ")
                                    if (location.isNotBlank()) Text(location, color = palette.text)
                                    val hasSajdah = data.sajdahNumber != null ||
                                        (!data.sajdahType.isNullOrBlank() && !data.sajdahType.equals("false", ignoreCase = true))
                                    if (hasSajdah) {
                                        Text(
                                            quranStudyText("sajdah", languageCode) +
                                                (data.sajdahNumber?.let { " $it" } ?: ""),
                                            color = palette.accent,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        quranStudyText("metadata_source", languageCode),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.secondaryText
                                    )
                                }
                            }
                        }
                    }

                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Ali Quli Qarai · English", fontWeight = FontWeight.Bold, color = palette.accent)
                            SelectionContainer {
                                Text(
                                    ayah.english,
                                    fontSize = (16 * translationFontScale).sp,
                                    lineHeight = (25 * translationFontScale).sp,
                                    color = palette.text
                                )
                            }
                            Text("Syed Zeeshan Haider Jawadi · اردو", fontWeight = FontWeight.Bold, color = palette.accent)
                            SelectionContainer {
                                Text(
                                    ayah.urdu,
                                    fontFamily = NotoNastaliqUrduFont,
                                    fontSize = (20 * translationFontScale).sp,
                                    lineHeight = (35 * translationFontScale).sp,
                                    textAlign = TextAlign.Right,
                                    color = palette.text
                                )
                            }
                        }
                    }

                    QuranTafsirSection(
                        info = info,
                        ayah = ayah,
                        languageCode = languageCode,
                        palette = palette
                    )

                    when {
                        wordsLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(quranStudyText("word_loading", languageCode), color = palette.secondaryText)
                        }
                        wordsFailed -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                quranStudyText("word_error", languageCode),
                                modifier = Modifier.weight(1f),
                                color = palette.secondaryText,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = onRetryWords) { Text(quranStudyText("retry", languageCode)) }
                        }
                        !words.isNullOrEmpty() -> QuranWordByWordSection(
                            words = words.orEmpty(),
                            languageCode = languageCode,
                            palette = palette,
                            onPlayWord = onPlayWord
                        )
                    }

                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(quranStudyText("my_notes", languageCode), fontWeight = FontWeight.Bold, color = palette.accent)
                            OutlinedTextField(
                                value = noteDraft,
                                onValueChange = { noteDraft = it.take(4000) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 6,
                                placeholder = { Text(quranStudyText("note_hint", languageCode)) }
                            )
                            Button(onClick = { onSaveNote(noteDraft) }) {
                                Text(quranStudyText("save_note", languageCode))
                            }
                        }
                    }

                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(quranStudyText("collections", languageCode), fontWeight = FontWeight.Bold, color = palette.accent)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                collections.forEach { collection ->
                                    FilterChip(
                                        selected = reference in collection.references,
                                        onClick = { onToggleCollection(collection.id) },
                                        label = { Text(quranCollectionName(collection, languageCode)) }
                                    )
                                }
                            }
                            Text(
                                quranStudyText("collection_hint", languageCode),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.secondaryText
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun QuranStudyLibraryPanel(
    surahs: List<QuranSurahInfo>,
    studyState: QuranStudyState,
    languageCode: String,
    onOpenReference: (Int, Int) -> Unit,
    onCreateCollection: (String) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onRemoveReference: (String, String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var newCollectionName by rememberSaveable { mutableStateOf("") }
    val normalizedQuery = query.trim().lowercase()
    val notes = studyState.notes.entries.filter { (reference, text) ->
        normalizedQuery.isBlank() || reference.contains(normalizedQuery, ignoreCase = true) ||
            text.contains(normalizedQuery, ignoreCase = true)
    }.sortedBy { it.key }
    val collections = studyState.collections.filter { collection ->
        normalizedQuery.isBlank() || quranCollectionName(collection, languageCode).contains(normalizedQuery, ignoreCase = true) ||
            collection.references.any { it.contains(normalizedQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                placeholder = { Text(quranStudyText("search_study", languageCode)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
        }
        item {
            Text(quranStudyText("my_notes", languageCode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (notes.isEmpty()) {
            item { Text(quranStudyText("no_notes", languageCode), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(notes.size) { index ->
                val (reference, text) = notes[index]
                QuranStudyNoteCard(
                    reference = reference,
                    text = text,
                    surahs = surahs,
                    languageCode = languageCode,
                    onOpen = { openQuranReference(reference, surahs, onOpenReference) },
                    onDelete = { onDeleteNote(reference) }
                )
            }
        }
        item {
            Column {
                Text(quranStudyText("collections", languageCode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it.take(60) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(quranStudyText("collection_name", languageCode)) },
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (newCollectionName.isNotBlank()) {
                            onCreateCollection(newCollectionName)
                            newCollectionName = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = quranStudyText("add_collection", languageCode))
                    }
                }
            }
        }
        if (collections.isEmpty()) {
            item { Text(quranStudyText("no_collections", languageCode), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(collections.size) { index ->
                val collection = collections[index]
                QuranCollectionCard(
                    collection = collection,
                    surahs = surahs,
                    languageCode = languageCode,
                    onOpenReference = onOpenReference,
                    onDeleteCollection = { onDeleteCollection(collection.id) },
                    onRemoveReference = { reference -> onRemoveReference(collection.id, reference) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun QuranStudyNoteCard(
    reference: String,
    text: String,
    surahs: List<QuranSurahInfo>,
    languageCode: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
                    Text(quranReferenceLabel(reference, surahs, languageCode), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = quranStudyText("delete", languageCode))
                }
            }
            Text(text, maxLines = 5, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuranCollectionCard(
    collection: QuranCollection,
    surahs: List<QuranSurahInfo>,
    languageCode: String,
    onOpenReference: (Int, Int) -> Unit,
    onDeleteCollection: () -> Unit,
    onRemoveReference: (String) -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    quranCollectionName(collection, languageCode),
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )
                Text(collection.references.size.toString(), color = MaterialTheme.colorScheme.primary)
                if (!collection.isBuiltIn) {
                    IconButton(onClick = onDeleteCollection) {
                        Icon(Icons.Default.Delete, contentDescription = quranStudyText("delete", languageCode))
                    }
                }
            }
            if (collection.references.isEmpty()) {
                Text(quranStudyText("empty_collection", languageCode), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                collection.references.sorted().forEach { reference ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { openQuranReference(reference, surahs, onOpenReference) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(quranReferenceLabel(reference, surahs, languageCode), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { onRemoveReference(reference) }) {
                            Icon(Icons.Default.Close, contentDescription = quranStudyText("remove", languageCode), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun openQuranReference(
    reference: String,
    surahs: List<QuranSurahInfo>,
    onOpenReference: (Int, Int) -> Unit
) {
    val parts = reference.split(":")
    val surahNumber = parts.getOrNull(0)?.toIntOrNull() ?: return
    val ayahNumber = parts.getOrNull(1)?.toIntOrNull() ?: return
    val info = surahs.firstOrNull { it.number == surahNumber } ?: return
    if (ayahNumber in 1..info.verseCount) onOpenReference(surahNumber, ayahNumber)
}

private fun quranReferenceLabel(reference: String, surahs: List<QuranSurahInfo>, languageCode: String): String {
    val surahNumber = reference.substringBefore(":").toIntOrNull()
    val info = surahs.firstOrNull { it.number == surahNumber }
    val name = info?.let { if (languageCode == "en") it.name else it.nameAr } ?: quranStudyText("surah", languageCode)
    return "$name · $reference"
}

internal fun quranCollectionName(collection: QuranCollection, languageCode: String): String =
    if (!collection.isBuiltIn) collection.name else quranStudyText(
        when (collection.id) {
            QuranStudyStore.FAVORITES -> "favorites"
            QuranStudyStore.STUDY_LATER -> "study_later"
            QuranStudyStore.MEMORIZATION -> "memorization"
            QuranStudyStore.DUA_AYAHS -> "dua_ayahs"
            else -> "collection"
        },
        languageCode
    )

internal fun quranStudyText(key: String, languageCode: String): String = when (languageCode) {
    "ur" -> when (key) {
        "study_ayah" -> "آیت کا مطالعہ"
        "previous_ayah" -> "پچھلی آیت"
        "next_ayah" -> "اگلی آیت"
        "tafsir" -> "تفسیر"
        "author" -> "مصنف"
        "language" -> "زبان"
        "source" -> "ماخذ"
        "source_terms" -> "ماخذ کی شرائط"
        "tafsir_loading" -> "تفسیر لوڈ ہو رہی ہے…"
        "tafsir_error" -> "تفسیر لوڈ نہیں ہو سکی۔ محفوظ متن نہ ہو تو انٹرنیٹ درکار ہے۔"
        "tafsir_unavailable" -> "اس ماخذ میں اس آیت کے لیے کوئی mapped تفسیر دستیاب نہیں۔"
        "shared_tafsir_block" -> "مشترکہ تفسیری حصہ ان آیات سے منسلک ہے"
        "copy_tafsir" -> "تفسیر کاپی"
        "share_tafsir" -> "تفسیر شیئر"
        "open_source" -> "ماخذ کا صفحہ کھولیں"
        "tafsir_cache_note" -> "پہلی بار لوڈ ہونے کے بعد یہ متن آف لائن مطالعے کے لیے device پر محفوظ ہے۔"
        "tafsir_format_note" -> "اصل عبارت نہیں بدلی گئی؛ صرف presentation formatting normal کی گئی ہے۔"
        "search_cached_tafsir" -> "محفوظ شدہ تفاسیر میں تلاش کریں"
        "all_sources" -> "تمام ماخذ"
        "all_languages" -> "تمام زبانیں"
        "english" -> "انگریزی"
        "arabic" -> "عربی"
        "cached_tafsir_only" -> "تلاش صرف device پر cache کی گئی verified تفسیر میں ہوتی ہے؛ نیا متن ڈاؤن لوڈ نہیں ہوتا۔"
        "tafsir_search_error" -> "تفسیر کی تلاش مکمل نہیں ہو سکی۔"
        "matches" -> "نتائج"
        "surahs" -> "سورہیں"
        "showing_first" -> "ابتدائی"
        "no_cached_tafsir_matches" -> "کوئی نتیجہ نہیں۔ Study Ayah میں تفسیر load کر کے اسے cache کریں۔"
        "my_study" -> "میرا مطالعہ"
        "my_notes" -> "میرے نوٹس"
        "notes" -> "نوٹس"
        "collections" -> "Collections"
        "add_collection" -> "Collection بنائیں"
        "collection_name" -> "نئی collection کا نام"
        "search_study" -> "نوٹس اور collections تلاش کریں"
        "no_notes" -> "ابھی کوئی نوٹ نہیں۔ Study Ayah سے نوٹ شامل کریں۔"
        "no_collections" -> "کوئی collection نہیں ملی۔"
        "empty_collection" -> "اس collection میں ابھی کوئی آیت نہیں۔"
        "note_hint" -> "اس آیت کے بارے میں اپنا نوٹ لکھیں…"
        "save_note" -> "نوٹ محفوظ کریں"
        "copied" -> "کاپی ہو گیا"
        "copy_arabic" -> "عربی کاپی"
        "copy_translations" -> "ترجمے کاپی"
        "copy_both" -> "سب کاپی"
        "share" -> "شیئر"
        "play" -> "تلاوت"
        "bookmark" -> "Bookmark"
        "close" -> "بند کریں"
        "ayah_details" -> "آیت کی تفصیل"
        "revelation" -> "نزول"
        "meccan" -> "مکی"
        "medinan" -> "مدنی"
        "metadata_loading" -> "آیت کی location لوڈ ہو رہی ہے…"
        "metadata_error" -> "Location کے لیے انٹرنیٹ درکار ہے۔"
        "metadata_source" -> "Location data: Quran Foundation"
        "juz" -> "پارہ"
        "page" -> "صفحہ"
        "hizb" -> "حزب"
        "rub_hizb" -> "ربع حزب"
        "ruku" -> "رکوع"
        "manzil" -> "منزل"
        "sajdah" -> "سجدہ آیت"
        "retry" -> "دوبارہ کوشش"
        "word_loading" -> "کلمات کا ڈیٹا لوڈ ہو رہا ہے…"
        "word_error" -> "کلمات کا ڈیٹا دستیاب نہیں۔"
        "collection_hint" -> "منتخب کردہ collections میں یہ آیت محفوظ ہوگی۔"
        "favorites" -> "پسندیدہ آیات"
        "study_later" -> "بعد میں مطالعہ"
        "memorization" -> "حفظ"
        "dua_ayahs" -> "دعا کی آیات"
        "collection" -> "Collection"
        "delete" -> "حذف کریں"
        "remove" -> "ہٹائیں"
        "surah" -> "سورۃ"
        else -> key
    }
    "ar" -> when (key) {
        "study_ayah" -> "دراسة الآية"
        "previous_ayah" -> "الآية السابقة"
        "next_ayah" -> "الآية التالية"
        "tafsir" -> "التفسير"
        "author" -> "المؤلف"
        "language" -> "اللغة"
        "source" -> "المصدر"
        "source_terms" -> "شروط المصدر"
        "tafsir_loading" -> "جارٍ تحميل التفسير…"
        "tafsir_error" -> "تعذر تحميل التفسير. يلزم الاتصال إذا لم يكن النص محفوظاً."
        "tafsir_unavailable" -> "لا يوجد تفسير مرتبط بهذه الآية في هذا المصدر."
        "shared_tafsir_block" -> "كتلة التفسير المشتركة مرتبطة بالآيات"
        "copy_tafsir" -> "نسخ التفسير"
        "share_tafsir" -> "مشاركة التفسير"
        "open_source" -> "فتح صفحة المصدر"
        "tafsir_cache_note" -> "بعد أول تحميل يُحفظ النص على الجهاز للقراءة دون اتصال."
        "tafsir_format_note" -> "لم تتغير عبارة المصدر؛ طُبّع تنسيق العرض فقط."
        "search_cached_tafsir" -> "البحث في التفاسير المحفوظة"
        "all_sources" -> "كل المصادر"
        "all_languages" -> "كل اللغات"
        "english" -> "الإنجليزية"
        "arabic" -> "العربية"
        "cached_tafsir_only" -> "البحث في التفسير الموثق المحفوظ فقط؛ لا يُنزّل نص جديد."
        "tafsir_search_error" -> "تعذر إكمال البحث في التفسير."
        "matches" -> "نتائج"
        "surahs" -> "سور"
        "showing_first" -> "تظهر أول"
        "no_cached_tafsir_matches" -> "لا توجد نتائج. حمّل التفسير من دراسة الآية ليُحفظ أولاً."
        "my_study" -> "دراستي"
        "my_notes" -> "ملاحظاتي"
        "notes" -> "الملاحظات"
        "collections" -> "المجموعات"
        "add_collection" -> "إنشاء مجموعة"
        "collection_name" -> "اسم المجموعة الجديدة"
        "search_study" -> "البحث في الملاحظات والمجموعات"
        "no_notes" -> "لا توجد ملاحظات بعد. أضف ملاحظة من دراسة الآية."
        "no_collections" -> "لا توجد مجموعات."
        "empty_collection" -> "لا توجد آيات في هذه المجموعة بعد."
        "note_hint" -> "اكتب ملاحظتك عن هذه الآية…"
        "save_note" -> "حفظ الملاحظة"
        "copied" -> "تم النسخ"
        "copy_arabic" -> "نسخ العربية"
        "copy_translations" -> "نسخ الترجمات"
        "copy_both" -> "نسخ الكل"
        "share" -> "مشاركة"
        "play" -> "التلاوة"
        "bookmark" -> "إشارة مرجعية"
        "close" -> "إغلاق"
        "ayah_details" -> "تفاصيل الآية"
        "revelation" -> "النزول"
        "meccan" -> "مكية"
        "medinan" -> "مدنية"
        "metadata_loading" -> "جارٍ تحميل موقع الآية…"
        "metadata_error" -> "يلزم الاتصال لبيانات الموقع."
        "metadata_source" -> "بيانات الموقع: Quran Foundation"
        "juz" -> "جزء"
        "page" -> "صفحة"
        "hizb" -> "حزب"
        "rub_hizb" -> "ربع الحزب"
        "ruku" -> "ركوع"
        "manzil" -> "منزل"
        "sajdah" -> "آية سجدة"
        "retry" -> "إعادة المحاولة"
        "word_loading" -> "جارٍ تحميل بيانات الكلمات…"
        "word_error" -> "بيانات الكلمات غير متاحة."
        "collection_hint" -> "ستُحفظ هذه الآية في المجموعات المحددة."
        "favorites" -> "الآيات المفضلة"
        "study_later" -> "للدراسة لاحقاً"
        "memorization" -> "الحفظ"
        "dua_ayahs" -> "آيات الدعاء"
        "collection" -> "مجموعة"
        "delete" -> "حذف"
        "remove" -> "إزالة"
        "surah" -> "سورة"
        else -> key
    }
    else -> when (key) {
        "study_ayah" -> "Study Ayah"
        "previous_ayah" -> "Previous ayah"
        "next_ayah" -> "Next ayah"
        "tafsir" -> "Tafsir"
        "author" -> "Author"
        "language" -> "Language"
        "source" -> "Source"
        "source_terms" -> "Source terms"
        "tafsir_loading" -> "Loading sourced Tafsir…"
        "tafsir_error" -> "Tafsir could not be loaded. Internet is needed if it is not cached."
        "tafsir_unavailable" -> "No commentary is mapped to this ayah in the selected source."
        "shared_tafsir_block" -> "This shared commentary block is mapped to"
        "copy_tafsir" -> "Copy Tafsir"
        "share_tafsir" -> "Share Tafsir"
        "open_source" -> "Open source page"
        "tafsir_cache_note" -> "After the first load, this text is saved on the device for offline reading."
        "tafsir_format_note" -> "Source wording is retained; only presentation formatting is normalized."
        "search_cached_tafsir" -> "Search cached Tafsir"
        "all_sources" -> "All sources"
        "all_languages" -> "All languages"
        "english" -> "English"
        "arabic" -> "Arabic"
        "cached_tafsir_only" -> "Searches verified Tafsir already cached on this device; no new text is downloaded."
        "tafsir_search_error" -> "The Tafsir search could not be completed."
        "matches" -> "matches"
        "surahs" -> "Surahs"
        "showing_first" -> "showing first"
        "no_cached_tafsir_matches" -> "No matches. Load Tafsir in Study Ayah to cache it first."
        "my_study" -> "My Study"
        "my_notes" -> "My Notes"
        "notes" -> "Notes"
        "collections" -> "Collections"
        "add_collection" -> "Create collection"
        "collection_name" -> "New collection name"
        "search_study" -> "Search notes and collections"
        "no_notes" -> "No notes yet. Add one from Study Ayah."
        "no_collections" -> "No collections found."
        "empty_collection" -> "No ayahs in this collection yet."
        "note_hint" -> "Write your personal note about this ayah…"
        "save_note" -> "Save note"
        "copied" -> "Copied"
        "copy_arabic" -> "Copy Arabic"
        "copy_translations" -> "Copy translations"
        "copy_both" -> "Copy all"
        "share" -> "Share"
        "play" -> "Recite"
        "bookmark" -> "Bookmark"
        "close" -> "Close"
        "ayah_details" -> "Ayah details"
        "revelation" -> "Revelation"
        "meccan" -> "Meccan"
        "medinan" -> "Medinan"
        "metadata_loading" -> "Loading ayah location…"
        "metadata_error" -> "Internet is needed for location data."
        "metadata_source" -> "Location data: Quran Foundation"
        "juz" -> "Juz"
        "page" -> "Page"
        "hizb" -> "Hizb"
        "rub_hizb" -> "Rub al-Hizb"
        "ruku" -> "Ruku"
        "manzil" -> "Manzil"
        "sajdah" -> "Sajdah ayah"
        "retry" -> "Retry"
        "word_loading" -> "Loading word data…"
        "word_error" -> "Word data is unavailable."
        "collection_hint" -> "This ayah will be saved in the selected collections."
        "favorites" -> "Favorite Ayahs"
        "study_later" -> "Study Later"
        "memorization" -> "Memorization"
        "dua_ayahs" -> "Dua Ayahs"
        "collection" -> "Collection"
        "delete" -> "Delete"
        "remove" -> "Remove"
        "surah" -> "Surah"
        else -> key
    }
}
