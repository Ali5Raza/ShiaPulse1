package com.example.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranAssetLoader
import com.example.data.QuranAyah
import com.example.data.QuranHifzRecord
import com.example.data.QuranHifzReviewOutcome
import com.example.data.QuranHifzState
import com.example.data.QuranHifzStore
import com.example.data.QuranLearningRepository
import com.example.data.QuranProgressStore
import com.example.data.QuranSurahInfo
import com.example.ui.components.AudioPlayerComponent
import com.example.ui.theme.NoorehudaFont
import com.example.ui.theme.NotoNastaliqUrduFont
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class QuranHifzTestMode {
    RECALL,
    TRANSLATION_HIDDEN,
    AUDIO_RECALL
}

private data class QuranHifzSession(
    val references: List<String>,
    val index: Int = 0,
    val mode: QuranHifzTestMode,
    val repeats: Int = 0
) {
    val currentReference: String get() = references[index]
}

/** Private Hifz revision queue and non-competitive practice sessions. */
@Composable
fun QuranHifzPanel(
    surahs: List<QuranSurahInfo>,
    languageCode: String,
    onOpenReference: (Int, Int) -> Unit
) {
    val hifzState by QuranHifzStore.state.collectAsState()
    var session by remember { mutableStateOf<QuranHifzSession?>(null) }
    var selectedMode by rememberSaveable { mutableStateOf(QuranHifzTestMode.RECALL.name) }
    val mode = QuranHifzTestMode.valueOf(selectedMode)
    val activeSession = session
    if (activeSession == null) {
        QuranHifzDashboard(
            surahs = surahs,
            state = hifzState,
            languageCode = languageCode,
            selectedMode = mode,
            onModeChange = { selectedMode = it.name },
            onStart = { references ->
                if (references.isNotEmpty()) {
                    session = QuranHifzSession(references = references, mode = mode)
                }
            },
            onOpenReference = onOpenReference
        )
    } else {
        QuranHifzTestSession(
            surahs = surahs,
            session = activeSession,
            languageCode = languageCode,
            onSessionChange = { session = it },
            onClose = { session = null },
            onOpenReference = onOpenReference
        )
    }
}

@Composable
private fun QuranHifzDashboard(
    surahs: List<QuranSurahInfo>,
    state: QuranHifzState,
    languageCode: String,
    selectedMode: QuranHifzTestMode,
    onModeChange: (QuranHifzTestMode) -> Unit,
    onStart: (List<String>) -> Unit,
    onOpenReference: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var rangeSurah by rememberSaveable { mutableStateOf("1") }
    var rangeFrom by rememberSaveable { mutableStateOf("1") }
    var rangeTo by rememberSaveable { mutableStateOf("7") }
    var rangeError by remember { mutableStateOf<String?>(null) }
    val due = state.dueRecords
    val today = QuranProgressStore.currentLocalEpochDay()
    val targetProgress = (state.reviewsToday.toFloat() / state.dailyTarget.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                quranHifzText("title", languageCode),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                quranHifzText("subtitle", languageCode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        quranHifzText("daily_target", languageCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${state.reviewsToday}/${state.dailyTarget} " +
                            quranHifzText("reviews_today", languageCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { targetProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(quranHifzText("target_per_day", languageCode))
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(onClick = {
                            QuranHifzStore.setDailyTarget(context, state.dailyTarget - 1)
                        }) { Text("−") }
                        Text(
                            state.dailyTarget.toString(),
                            modifier = Modifier.padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedButton(onClick = {
                            QuranHifzStore.setDailyTarget(context, state.dailyTarget + 1)
                        }) { Text("+") }
                    }
                    Text(
                        quranHifzText("private_note", languageCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${due.size} " + quranHifzText("due_revision", languageCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${state.records.size} " + quranHifzText("tracked_ayat", languageCode) +
                            " · ${state.totalMistakes} " + quranHifzText("mistakes", languageCode) +
                            " · ${state.totalRepeats} " + quranHifzText("repeats", languageCode),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { onStart(due.take(state.dailyTarget).map { it.reference }) },
                        enabled = due.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(quranHifzText("start_revision", languageCode))
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        quranHifzText("test_mode", languageCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    QuranHifzModeSelector(selectedMode, onModeChange, languageCode)
                    Button(
                        onClick = {
                            onStart(state.records.keys.shuffled().take(state.dailyTarget))
                        },
                        enabled = state.records.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(quranHifzText("random_test", languageCode))
                    }
                    if (state.records.isEmpty()) {
                        Text(
                            quranHifzText("random_empty", languageCode),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        quranHifzText("range_test", languageCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(quranHifzText("range_hint", languageCode))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HifzNumberField(
                            value = rangeSurah,
                            onValueChange = { rangeSurah = it.filter(Char::isDigit).take(3) },
                            label = quranHifzText("surah", languageCode),
                            modifier = Modifier.weight(1f)
                        )
                        HifzNumberField(
                            value = rangeFrom,
                            onValueChange = { rangeFrom = it.filter(Char::isDigit).take(3) },
                            label = quranHifzText("from", languageCode),
                            modifier = Modifier.weight(1f)
                        )
                        HifzNumberField(
                            value = rangeTo,
                            onValueChange = { rangeTo = it.filter(Char::isDigit).take(3) },
                            label = quranHifzText("to", languageCode),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    rangeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = {
                            val number = rangeSurah.toIntOrNull()
                            val from = rangeFrom.toIntOrNull()
                            val to = rangeTo.toIntOrNull()
                            val info = surahs.firstOrNull { it.number == number }
                            if (
                                info == null || from == null || to == null ||
                                from < 1 || to < from || to > info.verseCount
                            ) {
                                rangeError = quranHifzText("invalid_range", languageCode)
                            } else {
                                rangeError = null
                                onStart((from..to).map { "$number:$it" })
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(quranHifzText("start_range", languageCode))
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        quranHifzText("mastery_levels", languageCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (0..5).forEach { level ->
                            val count = state.records.values.count { it.masteryLevel == level }
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    "L$level · $count",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                    Text(
                        quranHifzText("mastery_note", languageCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                quranHifzText("revision_queue", languageCode),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        if (due.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        quranHifzText("queue_empty", languageCode),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(due, key = { it.reference }) { record ->
                QuranHifzQueueRow(
                    record = record,
                    today = today,
                    languageCode = languageCode,
                    onReview = { onStart(listOf(record.reference)) },
                    onOpen = {
                        val surah = record.reference.substringBefore(':').toIntOrNull()
                        val ayah = record.reference.substringAfter(':').toIntOrNull()
                        if (surah != null && ayah != null) onOpenReference(surah, ayah)
                    }
                )
            }
        }
        item {
            HorizontalDivider()
            Text(
                quranHifzText("scheduling_note", languageCode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuranHifzModeSelector(
    selectedMode: QuranHifzTestMode,
    onModeChange: (QuranHifzTestMode) -> Unit,
    languageCode: String
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        QuranHifzTestMode.entries.forEach { mode ->
            FilterChip(
                selected = selectedMode == mode,
                onClick = { onModeChange(mode) },
                label = { Text(quranHifzModeLabel(mode, languageCode)) }
            )
        }
    }
}

@Composable
private fun QuranHifzQueueRow(
    record: QuranHifzRecord,
    today: Long,
    languageCode: String,
    onReview: () -> Unit,
    onOpen: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    record.reference,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "L${record.masteryLevel} · " + if (record.dueDay < today) {
                        quranHifzText("overdue", languageCode)
                    } else {
                        quranHifzText("due_today", languageCode)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "${record.reviewCount} " + quranHifzText("reviews", languageCode) +
                    " · ${record.intervalDays} " + quranHifzText("day_interval", languageCode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onReview, modifier = Modifier.weight(1f)) {
                    Text(quranHifzText("review", languageCode))
                }
                OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
                    Text(quranHifzText("open", languageCode))
                }
            }
        }
    }
}

@Composable
private fun QuranHifzTestSession(
    surahs: List<QuranSurahInfo>,
    session: QuranHifzSession,
    languageCode: String,
    onSessionChange: (QuranHifzSession) -> Unit,
    onClose: () -> Unit,
    onOpenReference: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val reference = session.currentReference
    val surahNumber = reference.substringBefore(':').toIntOrNull() ?: 0
    val ayahNumber = reference.substringAfter(':').toIntOrNull() ?: 0
    val info = surahs.firstOrNull { it.number == surahNumber }
    var ayah by remember(reference) { mutableStateOf<QuranAyah?>(null) }
    var loading by remember(reference) { mutableStateOf(true) }
    var failed by remember(reference) { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    var revealed by remember(reference) { mutableStateOf(false) }
    var audioUrl by remember(reference) { mutableStateOf<String?>(null) }

    LaunchedEffect(reference, retry, info?.number) {
        if (info == null) {
            loading = false
            failed = true
            return@LaunchedEffect
        }
        loading = true
        failed = false
        try {
            ayah = withContext(Dispatchers.IO) {
                QuranAssetLoader.loadSurah(context, info).verses.firstOrNull { it.number == ayahNumber }
            }
            if (ayah == null) failed = true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        quranHifzText("test_session", languageCode),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${session.index + 1}/${session.references.size} · $reference · " +
                            "${session.repeats} " + quranHifzText("repeats", languageCode),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onClose) { Text(quranHifzText("close", languageCode)) }
            }
            LinearProgressIndicator(
                progress = { (session.index + 1).toFloat() / session.references.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            QuranHifzModeSelector(
                selectedMode = session.mode,
                onModeChange = {
                    revealed = false
                    onSessionChange(session.copy(mode = it))
                },
                languageCode = languageCode
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        failed || ayah == null -> {
                            Text(
                                quranHifzText("load_error", languageCode),
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = { retry++ }) {
                                Text(quranHifzText("retry", languageCode))
                            }
                        }
                        else -> {
                            val currentAyah = ayah
                            if (currentAyah != null) {
                                if (session.mode == QuranHifzTestMode.AUDIO_RECALL && !revealed) {
                                    Text(
                                        quranHifzText("audio_recall_prompt", languageCode),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (session.mode == QuranHifzTestMode.RECALL && !revealed) {
                                    Text(
                                        quranHifzText("recall_prompt", languageCode),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (session.mode == QuranHifzTestMode.TRANSLATION_HIDDEN && !revealed) {
                                    Text(
                                        quranHifzText("translation_prompt", languageCode),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                QuranHifzAyahContent(
                                    ayah = currentAyah,
                                    languageCode = languageCode,
                                    showArabic = when (session.mode) {
                                        QuranHifzTestMode.RECALL,
                                        QuranHifzTestMode.AUDIO_RECALL -> revealed
                                        QuranHifzTestMode.TRANSLATION_HIDDEN -> true
                                    },
                                    showTranslations = when (session.mode) {
                                        QuranHifzTestMode.RECALL -> true
                                        QuranHifzTestMode.TRANSLATION_HIDDEN,
                                        QuranHifzTestMode.AUDIO_RECALL -> revealed
                                    }
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                QuranHifzStore.recordRepeat(context, reference)
                                revealed = false
                                onSessionChange(session.copy(repeats = session.repeats + 1))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(quranHifzText("repeat_ayah", languageCode))
                        }
                        Button(
                            onClick = { revealed = !revealed },
                            enabled = ayah != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                quranHifzText(if (revealed) "hide_answer" else "reveal_answer", languageCode)
                            )
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            quranHifzText("audio_recall", languageCode),
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold
                        )
                        Button(onClick = {
                            audioUrl = QuranLearningRepository.ayahAudioUrl(surahNumber, ayahNumber)
                        }) {
                            Text(quranHifzText("play_audio", languageCode))
                        }
                    }
                    audioUrl?.let { url ->
                        key(session.repeats) {
                            AudioPlayerComponent(
                                audioUrl = url,
                                modifier = Modifier.fillMaxWidth(),
                                onPlaybackEnded = {},
                                onProgressUpdate = { _, _ -> },
                                autoPlay = true
                            )
                        }
                        Text(
                            quranHifzText("audio_source", languageCode),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            Text(
                quranHifzText("self_grade", languageCode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                quranHifzText("grade_hint", languageCode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuranHifzReviewOutcome.entries.forEach { outcome ->
                    Button(
                        onClick = {
                            val status = QuranHifzStore.recordReview(context, reference, outcome)
                            QuranProgressStore.setMemorisationStatus(
                                context,
                                surahNumber,
                                ayahNumber,
                                status
                            )
                            if (session.index + 1 >= session.references.size) {
                                onClose()
                            } else {
                                onSessionChange(
                                    session.copy(index = session.index + 1, repeats = 0)
                                )
                            }
                        },
                        enabled = revealed && ayah != null
                    ) {
                        Text(quranHifzOutcomeLabel(outcome, languageCode))
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { if (surahNumber > 0 && ayahNumber > 0) onOpenReference(surahNumber, ayahNumber) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(quranHifzText("open_reader", languageCode))
            }
            Text(
                quranHifzText("test_private_note", languageCode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuranHifzAyahContent(
    ayah: QuranAyah,
    languageCode: String,
    showArabic: Boolean,
    showTranslations: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showArabic) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    ayah.arabic,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = NoorehudaFont,
                    fontSize = 27.sp,
                    lineHeight = 44.sp,
                    textAlign = TextAlign.Right
                )
            }
        }
        if (showTranslations) {
            Text(
                ayah.english,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    ayah.urdu,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = NotoNastaliqUrduFont,
                    fontSize = 20.sp,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.Right
                )
            }
        }
        if (!showArabic && !showTranslations) {
            Text(
                quranHifzText("answer_hidden", languageCode),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HifzNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

private fun quranHifzModeLabel(mode: QuranHifzTestMode, languageCode: String): String =
    quranHifzText(
        when (mode) {
            QuranHifzTestMode.RECALL -> "recall_test"
            QuranHifzTestMode.TRANSLATION_HIDDEN -> "translation_hidden"
            QuranHifzTestMode.AUDIO_RECALL -> "audio_recall"
        },
        languageCode
    )

private fun quranHifzOutcomeLabel(
    outcome: QuranHifzReviewOutcome,
    languageCode: String
): String = quranHifzText(outcome.name.lowercase(), languageCode)

internal fun quranHifzText(key: String, languageCode: String): String = when (languageCode) {
    "ur" -> when (key) {
        "title" -> "حفظ 2.0"
        "subtitle" -> "ذاتی revision queue، spaced repetition اور خود جائزہ لینے والی practice۔"
        "daily_target" -> "روزانہ حفظ target"
        "reviews_today" -> "reviews آج"
        "target_per_day" -> "روزانہ scored reviews"
        "private_note" -> "یہ progress صرف اسی device پر ہے؛ کوئی ranking یا مقابلہ نہیں۔"
        "due_revision" -> "revision واجب"
        "tracked_ayat" -> "محفوظ tracked آیات"
        "mistakes" -> "غلطیاں"
        "repeats" -> "تکرار"
        "start_revision" -> "واجب revision شروع کریں"
        "test_mode" -> "Test کی قسم"
        "recall_test" -> "یاد کر کے test"
        "translation_hidden" -> "ترجمہ چھپا کر"
        "audio_recall" -> "Audio recall"
        "random_test" -> "Random test"
        "random_empty" -> "پہلے reader میں آیات کی memorisation status محفوظ کریں یا range test شروع کریں۔"
        "range_test" -> "Range test"
        "range_hint" -> "ایک ہی سورۃ کے اندر آغاز اور اختتام منتخب کریں۔"
        "surah" -> "سورۃ"
        "from" -> "سے"
        "to" -> "تک"
        "invalid_range" -> "درست سورۃ اور آیات کا range درج کریں۔"
        "start_range" -> "Range test شروع کریں"
        "mastery_levels" -> "Mastery levels"
        "mastery_note" -> "L0 نیا/کمزور سے L5 مضبوط حفظ تک؛ جوابات کے ساتھ levels اور review intervals بدلتے ہیں۔"
        "revision_queue" -> "Revision queue"
        "queue_empty" -> "آج کوئی revision باقی نہیں۔ Random یا range test سے practice جاری رکھ سکتے ہیں۔"
        "overdue" -> "وقت گزر گیا"
        "due_today" -> "آج واجب"
        "reviews" -> "reviews"
        "day_interval" -> "دن کا interval"
        "review" -> "Review"
        "open" -> "کھولیں"
        "scheduling_note" -> "Again آج دوبارہ، Hard مختصر interval، Good اور Easy آہستہ آہستہ طویل intervals رکھتے ہیں۔ اگلی revision مقامی تاریخ کے مطابق طے ہوتی ہے۔"
        "test_session" -> "حفظ test session"
        "close" -> "بند کریں"
        "load_error" -> "آیت لوڈ نہیں ہو سکی۔"
        "retry" -> "دوبارہ کوشش"
        "audio_recall_prompt" -> "آواز سن کر آیت یاد کریں، پھر جواب دکھائیں۔"
        "recall_prompt" -> "ترجمے کو قرآنی عربی متن کا بدل نہیں سمجھیں؛ عربی آیت یاد کر کے reveal کریں۔"
        "translation_prompt" -> "عربی پڑھ کر ترجمہ یاد کریں، پھر جواب دکھائیں۔"
        "answer_hidden" -> "جواب چھپا ہوا ہے۔"
        "repeat_ayah" -> "آیت دہرائیں"
        "reveal_answer" -> "جواب دکھائیں"
        "hide_answer" -> "جواب چھپائیں"
        "play_audio" -> "Audio چلائیں"
        "audio_source" -> "قاری: Mishari Rashid al-Afasy · Quran Foundation · URL-first، پھر cache"
        "self_grade" -> "اپنا جائزہ"
        "grade_hint" -> "جواب دیکھنے کے بعد ایمانداری سے درجہ دیں۔"
        "again" -> "Again"
        "hard" -> "Hard"
        "good" -> "Good"
        "easy" -> "Easy"
        "open_reader" -> "Reader میں کھولیں"
        "test_private_note" -> "Audio پہلے URL سے چلتا ہے اور موجودہ cache استعمال کرتا ہے۔ Test text وہی bundled قرآن و ترجمہ assets ہیں۔"
        else -> key
    }
    "ar" -> when (key) {
        "title" -> "الحفظ 2.0"
        "subtitle" -> "قائمة مراجعة خاصة مع تكرار متباعد وتقييم ذاتي."
        "daily_target" -> "هدف الحفظ اليومي"
        "reviews_today" -> "مراجعات اليوم"
        "target_per_day" -> "المراجعات المقيّمة يومياً"
        "private_note" -> "يبقى التقدم على هذا الجهاز فقط؛ لا ترتيب ولا منافسة."
        "due_revision" -> "مراجعات مستحقة"
        "tracked_ayat" -> "آيات متابعة"
        "mistakes" -> "أخطاء"
        "repeats" -> "تكرارات"
        "start_revision" -> "بدء المراجعة المستحقة"
        "test_mode" -> "نوع الاختبار"
        "recall_test" -> "اختبار التذكر"
        "translation_hidden" -> "إخفاء الترجمة"
        "audio_recall" -> "التذكر الصوتي"
        "random_test" -> "اختبار عشوائي"
        "random_empty" -> "احفظ حالة حفظ الآيات من القارئ أولاً أو ابدأ اختبار نطاق."
        "range_test" -> "اختبار نطاق"
        "range_hint" -> "اختر البداية والنهاية داخل سورة واحدة."
        "surah" -> "سورة"
        "from" -> "من"
        "to" -> "إلى"
        "invalid_range" -> "أدخل سورة ونطاق آيات صالحين."
        "start_range" -> "بدء اختبار النطاق"
        "mastery_levels" -> "مستويات الإتقان"
        "mastery_note" -> "من L0 للجديد أو الضعيف إلى L5 للحفظ المتين؛ تتغير المستويات والفواصل بحسب إجاباتك."
        "revision_queue" -> "قائمة المراجعة"
        "queue_empty" -> "لا توجد مراجعة مستحقة اليوم. يمكنك المتابعة باختبار عشوائي أو بنطاق."
        "overdue" -> "متأخرة"
        "due_today" -> "مستحقة اليوم"
        "reviews" -> "مراجعات"
        "day_interval" -> "فاصل بالأيام"
        "review" -> "مراجعة"
        "open" -> "فتح"
        "scheduling_note" -> "Again تعيد اليوم، Hard تمنح فاصلاً قصيراً، وGood وEasy تزيدان الفواصل تدريجياً حسب التاريخ المحلي."
        "test_session" -> "جلسة اختبار الحفظ"
        "close" -> "إغلاق"
        "load_error" -> "تعذر تحميل الآية."
        "retry" -> "إعادة المحاولة"
        "audio_recall_prompt" -> "استمع وتذكر الآية، ثم اكشف الإجابة."
        "recall_prompt" -> "الترجمة ليست بديلاً عن النص العربي؛ تذكر الآية العربية ثم اكشفها."
        "translation_prompt" -> "اقرأ العربية وتذكر الترجمة، ثم اكشف الإجابة."
        "answer_hidden" -> "الإجابة مخفية."
        "repeat_ayah" -> "تكرار الآية"
        "reveal_answer" -> "إظهار الإجابة"
        "hide_answer" -> "إخفاء الإجابة"
        "play_audio" -> "تشغيل الصوت"
        "audio_source" -> "القارئ: Mishari Rashid al-Afasy · Quran Foundation · من الرابط أولاً ثم من التخزين"
        "self_grade" -> "التقييم الذاتي"
        "grade_hint" -> "قيّم بصدق بعد رؤية الإجابة."
        "again" -> "Again"
        "hard" -> "Hard"
        "good" -> "Good"
        "easy" -> "Easy"
        "open_reader" -> "فتح في القارئ"
        "test_private_note" -> "يبدأ الصوت من الرابط ويستخدم التخزين الحالي. نصوص الاختبار من ملفات القرآن والترجمة المضمنة نفسها."
        else -> key
    }
    else -> when (key) {
        "title" -> "Hifz 2.0"
        "subtitle" -> "A private revision queue, spaced repetition and honest self-testing."
        "daily_target" -> "Daily Hifz target"
        "reviews_today" -> "reviews today"
        "target_per_day" -> "Scored reviews per day"
        "private_note" -> "Progress stays on this device; no ranking or competition."
        "due_revision" -> "due for revision"
        "tracked_ayat" -> "tracked ayat"
        "mistakes" -> "mistakes"
        "repeats" -> "repeats"
        "start_revision" -> "Start due revision"
        "test_mode" -> "Test mode"
        "recall_test" -> "Recall test"
        "translation_hidden" -> "Translation hidden"
        "audio_recall" -> "Audio recall"
        "random_test" -> "Random test"
        "random_empty" -> "Save ayah memorisation statuses in the reader first, or start a range test."
        "range_test" -> "Range test"
        "range_hint" -> "Choose a start and end within one Surah."
        "surah" -> "Surah"
        "from" -> "From"
        "to" -> "To"
        "invalid_range" -> "Enter a valid Surah and ayah range."
        "start_range" -> "Start range test"
        "mastery_levels" -> "Mastery levels"
        "mastery_note" -> "L0 is new/weak and L5 is firmly memorised; answers adjust levels and review intervals."
        "revision_queue" -> "Revision queue"
        "queue_empty" -> "No revision is due today. You can still practise with a random or range test."
        "overdue" -> "Overdue"
        "due_today" -> "Due today"
        "reviews" -> "reviews"
        "day_interval" -> "day interval"
        "review" -> "Review"
        "open" -> "Open"
        "scheduling_note" -> "Again returns today, Hard uses a short interval, and Good/Easy gradually lengthen intervals using your local date."
        "test_session" -> "Hifz test session"
        "close" -> "Close"
        "load_error" -> "The ayah could not be loaded."
        "retry" -> "Retry"
        "audio_recall_prompt" -> "Listen and recall the ayah, then reveal the answer."
        "recall_prompt" -> "A translation is not a substitute for the Arabic Quran; recall the Arabic ayah, then reveal it."
        "translation_prompt" -> "Read the Arabic and recall its translation, then reveal the answer."
        "answer_hidden" -> "The answer is hidden."
        "repeat_ayah" -> "Repeat ayah"
        "reveal_answer" -> "Reveal answer"
        "hide_answer" -> "Hide answer"
        "play_audio" -> "Play audio"
        "audio_source" -> "Reciter: Mishari Rashid al-Afasy · Quran Foundation · URL-first, then cache"
        "self_grade" -> "Self-grade"
        "grade_hint" -> "Grade honestly after viewing the answer."
        "again" -> "Again"
        "hard" -> "Hard"
        "good" -> "Good"
        "easy" -> "Easy"
        "open_reader" -> "Open in reader"
        "test_private_note" -> "Audio is URL-first and uses the existing cache. Test text comes from the same bundled Quran and translation assets."
        else -> key
    }
}
