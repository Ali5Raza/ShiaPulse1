package com.example.ui

import android.media.ToneGenerator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmiriFont
import kotlin.math.roundToInt

data class BeadStyleObj(val base: Color, val high: Color, val shadow: Color)

val tasbeehBeadStyles = listOf(
    BeadStyleObj(Color(0xFFE0E0E0), Color.White, Color(0xFF757575)),
    BeadStyleObj(Color(0xFF303030), Color(0xFF757575), Color.Black),
    BeadStyleObj(Color(0xFF4CAF50), Color(0xFFA5D6A7), Color(0xFF1B5E20)),
    BeadStyleObj(Color(0xFFFFB300), Color(0xFFFFE082), Color(0xFFFF6F00)),
    BeadStyleObj(Color(0xFF6D4C41), Color(0xFFA1887F), Color(0xFF3E2723))
)

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRealisticBead(
    center: Offset,
    radius: Float,
    style: BeadStyleObj
) {
    drawCircle(
        color = Color.Black.copy(alpha = 0.28f),
        radius = radius + 1.5.dp.toPx(),
        center = center + Offset(0f, 3.dp.toPx())
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(style.base, style.shadow),
            center = center,
            radius = radius * 1.2f
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(style.high.copy(alpha = 0.9f), Color.Transparent),
            center = center - Offset(radius * 0.35f, radius * 0.35f),
            radius = radius * 0.7f
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.55f),
        radius = radius * 0.15f,
        center = center
    )
}

private data class TasbeehPhrase(
    val arabic: String,
    val transliteration: String,
    val meaning: String,
    val phaseLabel: String
)

@Composable
fun TasbeehScreen(
    viewModel: PrayerViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val selectedTasbeeh by viewModel.selectedTasbeeh.collectAsState()
    val tasbeehCount by viewModel.tasbeehCount.collectAsState()
    val tasbeehCycle by viewModel.tasbeehCycle.collectAsState()
    val records by viewModel.tasbeehRecords.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()
    val globalAppFont by viewModel.appFont.collectAsState()
    val baseAppFontFamily = com.example.ui.theme.getAppFontFamily(globalAppFont, currentLang)

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val toneGenerator = remember {
        runCatching { ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 55) }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose { toneGenerator?.release() }
    }

    val sharedPrefs = remember {
        context.getSharedPreferences("tasbeeh_prefs", android.content.Context.MODE_PRIVATE)
    }
    var dailyGoal by remember { mutableIntStateOf(sharedPrefs.getInt("daily_goal", 33)) }
    var soundEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("sound_enabled", true)) }
    var hapticsEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("haptics_enabled", true)) }
    var selectedBeadStyle by remember { mutableIntStateOf(sharedPrefs.getInt("bead_style", 0)) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showPresetsDialog by remember { mutableStateOf(false) }

    val todayDateStr = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }
    val recordedTodayCount = records.find { it.dateString == todayDateStr }?.totalCount ?: 0
    var liveTodayCount by remember { mutableIntStateOf(recordedTodayCount) }
    LaunchedEffect(recordedTodayCount) {
        if (recordedTodayCount > liveTodayCount) liveTodayCount = recordedTodayCount
    }

    val isFatima = selectedTasbeeh == "Tasbih Lady Fatima (sa)"
    val isFreeMode = selectedTasbeeh == "Free Mode"
    val isComplete = when {
        isFatima -> tasbeehCycle == 3
        selectedTasbeeh == "Salawat (100x)" || selectedTasbeeh == "Istighfar (100x)" -> tasbeehCount >= 100
        else -> false
    }
    val targetGoal = when {
        isFatima && tasbeehCycle == 3 -> 100
        isFatima && tasbeehCycle == 0 -> 34
        isFatima -> 33
        isFreeMode -> dailyGoal
        else -> 100
    }
    val displayCount = if (isFatima && tasbeehCycle == 3) 100 else tasbeehCount
    val completedCycleCount = when {
        isFatima && tasbeehCycle == 0 -> tasbeehCount
        isFatima && tasbeehCycle == 1 -> 34 + tasbeehCount
        isFatima && tasbeehCycle == 2 -> 67 + tasbeehCount
        isFatima -> 100
        else -> tasbeehCount
    }
    val progress = when {
        isComplete -> 1f
        isFreeMode && dailyGoal > 0 -> {
            val remainder = tasbeehCount % dailyGoal
            if (tasbeehCount > 0 && remainder == 0) 1f else remainder.toFloat() / dailyGoal
        }
        targetGoal > 0 -> (tasbeehCount.toFloat() / targetGoal).coerceIn(0f, 1f)
        else -> 0f
    }
    val loopCount = if (isFreeMode && dailyGoal > 0) {
        if (tasbeehCount == 0) 1 else (tasbeehCount - 1) / dailyGoal + 1
    } else {
        1
    }
    val phrase = tasbeehPhrase(selectedTasbeeh, tasbeehCycle, currentLang)
    val currentStyle = tasbeehBeadStyles.getOrElse(selectedBeadStyle) { tasbeehBeadStyles.first() }
    val canDecrement = tasbeehCount > 0 && !(isFatima && tasbeehCycle == 3)

    fun incrementWithFeedback() {
        if (isComplete) return
        val previousCount = viewModel.tasbeehCount.value
        val previousCycle = viewModel.tasbeehCycle.value
        var milestone = false
        viewModel.incrementTasbeeh { milestone = true }
        val updatedCount = viewModel.tasbeehCount.value
        val didIncrement = previousCount != updatedCount || previousCycle != viewModel.tasbeehCycle.value
        if (!didIncrement) return
        liveTodayCount += 1
        if (isFreeMode && dailyGoal > 0 && updatedCount % dailyGoal == 0) milestone = true
        if (hapticsEnabled) {
            haptic.performHapticFeedback(
                if (milestone) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
            )
        }
        if (soundEnabled) {
            toneGenerator?.startTone(
                if (milestone) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE,
                if (milestone) 90 else 25
            )
        }
    }

    fun decrementWithFeedback() {
        if (!canDecrement) return
        viewModel.decrementTasbeeh()
        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (soundEnabled) toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 30)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        PremiumScreenTopBar(
            title = tasbeehText("title", currentLang),
            subtitle = tasbeehPresetTitle(selectedTasbeeh, currentLang),
            onBack = onBack,
            actions = {
                IconButton(onClick = {
                    soundEnabled = !soundEnabled
                    sharedPrefs.edit().putBoolean("sound_enabled", soundEnabled).apply()
                }) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = tasbeehText(if (soundEnabled) "sound_on" else "sound_off", currentLang),
                        tint = if (soundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {
                    hapticsEnabled = !hapticsEnabled
                    sharedPrefs.edit().putBoolean("haptics_enabled", hapticsEnabled).apply()
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = tasbeehText(if (hapticsEnabled) "haptics_on" else "haptics_off", currentLang),
                        tint = if (hapticsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.09f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                        Text(
                            text = phrase.phaseLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = phrase.arabic,
                        fontFamily = AmiriFont,
                        fontSize = 28.sp,
                        lineHeight = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = phrase.transliteration,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = phrase.meaning,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = baseAppFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TasbeehStatCard(
                    label = tasbeehText("today", currentLang),
                    value = liveTodayCount.toString(),
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f)
                )
                TasbeehStatCard(
                    label = tasbeehText(if (isFreeMode) "loop" else "cycle", currentLang),
                    value = if (isFreeMode) loopCount.toString() else completedCycleCount.toString(),
                    icon = Icons.Default.Refresh,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            TasbeehCircleView(
                progress = progress,
                beadPosition = completedCycleCount,
                currentStyle = currentStyle,
                onBeadClick = { incrementWithFeedback() },
                onDecrement = { decrementWithFeedback() },
                enabled = !isComplete,
                counterDescription = tasbeehText("tap", currentLang),
                modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isComplete) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = displayCount.toString(),
                        fontSize = if (isComplete) 48.sp else 60.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = if (isComplete) 54.sp else 66.sp
                    )
                    Text(
                        text = if (isComplete) tasbeehText("completed", currentLang) else "/ $targetGoal",
                        style = if (isComplete) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isComplete) {
                        Text(
                            text = "${(progress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }
            Text(
                text = tasbeehText(if (isComplete) "completed" else "tap", currentLang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TasbeehActionButton(
                    title = tasbeehText("undo", currentLang),
                    icon = Icons.Default.Undo,
                    onClick = { decrementWithFeedback() },
                    enabled = canDecrement,
                    modifier = Modifier.weight(1f)
                )
                TasbeehActionButton(
                    title = tasbeehText("reset", currentLang),
                    icon = Icons.Default.Refresh,
                    onClick = { showResetDialog = true },
                    modifier = Modifier.weight(1f)
                )
                if (isFreeMode) {
                    TasbeehActionButton(
                        title = tasbeehText("goal", currentLang),
                        icon = Icons.Default.Edit,
                        onClick = { showGoalDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            AnimatedVisibility(visible = isComplete) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tasbeehText("completed", currentLang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tasbeehText("complete_message", currentLang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { viewModel.resetTasbeeh() }) {
                            Text(tasbeehText("start_again", currentLang), maxLines = 2, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tasbeehText("current_dhikr", currentLang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { showPresetsDialog = true }) {
                    Text(tasbeehText("change", currentLang))
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable {
                    showPresetsDialog = true
                },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tasbeehPresetTitle(selectedTasbeeh, currentLang),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                isFatima -> "34 + 33 + 33 = 100"
                                isFreeMode -> "${tasbeehText("goal", currentLang)}: $dailyGoal"
                                else -> "100 ×"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = tasbeehText("change", currentLang),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isFatima) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { phase ->
                        TasbeehPhasePill(
                            phase = phase,
                            currentPhase = tasbeehCycle,
                            currentLang = currentLang,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = tasbeehText("bead_style", currentLang),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
            ) {
                items(tasbeehBeadStyles.size) { index ->
                    val style = tasbeehBeadStyles[index]
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .clickable {
                                selectedBeadStyle = index
                                sharedPrefs.edit().putInt("bead_style", index).apply()
                            }
                            .border(
                                width = if (selectedBeadStyle == index) 2.dp else 0.dp,
                                color = if (selectedBeadStyle == index) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .padding(5.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRealisticBead(
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.minDimension * 0.33f,
                                style = style
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showGoalDialog) {
        var tempGoal by remember { mutableStateOf(dailyGoal.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text(tasbeehText("set_goal", currentLang), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempGoal,
                    onValueChange = { input ->
                        tempGoal = input.filter { char -> char.isDigit() }.take(5)
                    },
                    label = { Text(tasbeehText("goal_label", currentLang)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    tempGoal.toIntOrNull()?.let { goal ->
                        val validGoal = goal.coerceIn(1, 10000)
                        dailyGoal = validGoal
                        sharedPrefs.edit().putInt("daily_goal", validGoal).apply()
                    }
                    showGoalDialog = false
                }) {
                    Text(tasbeehText("save", currentLang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text(tasbeehText("cancel", currentLang))
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(tasbeehText("reset_title", currentLang), fontWeight = FontWeight.Bold) },
            text = { Text(tasbeehText("reset_message", currentLang)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.resetTasbeeh()
                    showResetDialog = false
                }) {
                    Text(tasbeehText("reset", currentLang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(tasbeehText("cancel", currentLang))
                }
            }
        )
    }

    if (showPresetsDialog) {
        AlertDialog(
            onDismissRequest = { showPresetsDialog = false },
            title = { Text(tasbeehText("select_dhikr", currentLang), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    viewModel.tasbeehPresets.forEach { preset ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                                viewModel.selectTasbeeh(preset)
                                showPresetsDialog = false
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTasbeeh == preset,
                                onClick = {
                                    viewModel.selectTasbeeh(preset)
                                    showPresetsDialog = false
                                }
                            )
                            Text(
                                text = tasbeehPresetTitle(preset, currentLang),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetsDialog = false }) {
                    Text(tasbeehText("close", currentLang))
                }
            }
        )
    }
}

@Composable
private fun TasbeehStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TasbeehActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 11.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TasbeehPhasePill(
    phase: Int,
    currentPhase: Int,
    currentLang: String,
    modifier: Modifier = Modifier
) {
    val isCurrent = currentPhase == phase
    val isComplete = currentPhase > phase
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = when {
            isCurrent -> MaterialTheme.colorScheme.primaryContainer
            isComplete -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (phase == 0) "34" else "33",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent || isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = tasbeehPhaseShort(phase, currentLang),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun tasbeehPhaseShort(phase: Int, languageCode: String): String = when (languageCode) {
    "ur" -> listOf("تکبیر", "تحمید", "تسبیح").getOrElse(phase) { "مکمل" }
    "ar" -> listOf("التكبير", "التحميد", "التسبيح").getOrElse(phase) { "تم" }
    else -> listOf("Takbir", "Hamd", "Tasbih").getOrElse(phase) { "Done" }
}

private fun tasbeehPresetTitle(preset: String, languageCode: String): String {
    return when (preset) {
        "Tasbih Lady Fatima (sa)" -> when (languageCode) {
            "ur" -> "تسبیح حضرت فاطمہ زہراؑ"
            "ar" -> "تسبيح السيدة فاطمة الزهراء (ع)"
            else -> "Tasbih of Lady Fatima (sa)"
        }
        "Salawat (100x)" -> when (languageCode) {
            "ur" -> "صلوات · 100"
            "ar" -> "الصلوات · 100"
            else -> "Salawat · 100"
        }
        "Istighfar (100x)" -> when (languageCode) {
            "ur" -> "استغفار · 100"
            "ar" -> "الاستغفار · 100"
            else -> "Istighfar · 100"
        }
        else -> when (languageCode) {
            "ur" -> "آزاد ذکر"
            "ar" -> "ذكر حر"
            else -> "Free Dhikr"
        }
    }
}

private fun tasbeehPhrase(preset: String, cycle: Int, languageCode: String): TasbeehPhrase {
    if (preset == "Tasbih Lady Fatima (sa)") {
        return when (cycle) {
            0 -> TasbeehPhrase(
                "اللهُ أَكْبَرُ",
                "Allahu Akbar",
                when (languageCode) {
                    "ur" -> "اللہ سب سے بڑا ہے"
                    "ar" -> "الله أكبر"
                    else -> "Allah is the Greatest"
                },
                "${tasbeehPhaseShort(0, languageCode)} · 34"
            )
            1 -> TasbeehPhrase(
                "الْحَمْدُ لِلَّهِ",
                "Alhamdulillah",
                when (languageCode) {
                    "ur" -> "تمام تعریفیں اللہ کے لیے ہیں"
                    "ar" -> "الحمد لله"
                    else -> "All praise belongs to Allah"
                },
                "${tasbeehPhaseShort(1, languageCode)} · 33"
            )
            2 -> TasbeehPhrase(
                "سُبْحَانَ اللَّهِ",
                "SubhanAllah",
                when (languageCode) {
                    "ur" -> "اللہ پاک ہے"
                    "ar" -> "سبحان الله"
                    else -> "Glory be to Allah"
                },
                "${tasbeehPhaseShort(2, languageCode)} · 33"
            )
            else -> TasbeehPhrase(
                "تَمَّتْ",
                "Completed",
                tasbeehText("completed", languageCode),
                "100 / 100"
            )
        }
    }
    return when (preset) {
        "Salawat (100x)" -> TasbeehPhrase(
            "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَآلِ مُحَمَّدٍ",
            "Salawat",
            when (languageCode) {
                "ur" -> "اے اللہ! محمد اور آل محمد پر رحمت نازل فرما"
                "ar" -> "اللهم صلّ على محمد وآل محمد"
                else -> "O Allah, bless Muhammad and the family of Muhammad"
            },
            "100 ×"
        )
        "Istighfar (100x)" -> TasbeehPhrase(
            "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
            "Istighfar",
            when (languageCode) {
                "ur" -> "میں اللہ سے مغفرت مانگتا ہوں اور اسی کی طرف توبہ کرتا ہوں"
                "ar" -> "أطلب مغفرة الله وأتوب إليه"
                else -> "I seek Allah’s forgiveness and turn to Him"
            },
            "100 ×"
        )
        else -> TasbeehPhrase(
            "ذِكْر",
            "Dhikr",
            when (languageCode) {
                "ur" -> "اپنے ہدف کے ساتھ اللہ کا ذکر کریں"
                "ar" -> "اذكر الله وفق هدفك"
                else -> "Remember Allah at your own pace and target"
            },
            tasbeehText("goal", languageCode)
        )
    }
}

private fun tasbeehText(key: String, languageCode: String): String {
    val values = when (languageCode) {
        "ur" -> mapOf(
            "title" to "ڈیجیٹل تسبیح",
            "today" to "آج",
            "cycle" to "موجودہ چکر",
            "loop" to "موجودہ راؤنڈ",
            "tap" to "ذکر کے لیے دائرے کو دبائیں یا سوائپ کریں",
            "completed" to "تسبیح مکمل ہو گئی",
            "complete_message" to "آپ نے یہ چکر مکمل کر لیا ہے۔ نیا چکر شروع کر سکتے ہیں۔",
            "start_again" to "دوبارہ شروع کریں",
            "undo" to "ایک پیچھے",
            "reset" to "ری سیٹ",
            "goal" to "ہدف",
            "current_dhikr" to "موجودہ ذکر",
            "change" to "تبدیل کریں",
            "bead_style" to "دانوں کا انداز",
            "set_goal" to "روزانہ کا ہدف",
            "goal_label" to "ہدف کی تعداد",
            "save" to "محفوظ کریں",
            "cancel" to "منسوخ",
            "reset_title" to "کاؤنٹر ری سیٹ کریں؟",
            "reset_message" to "صرف موجودہ کاؤنٹر ری سیٹ ہوگا۔ محفوظ شدہ روزانہ کل تعداد نہیں بدلے گی۔",
            "select_dhikr" to "ذکر منتخب کریں",
            "close" to "بند کریں",
            "sound_on" to "آواز فعال ہے",
            "sound_off" to "آواز بند ہے",
            "haptics_on" to "ارتعاش فعال ہے",
            "haptics_off" to "ارتعاش بند ہے"
        )
        "ar" -> mapOf(
            "title" to "المسبحة الرقمية",
            "today" to "اليوم",
            "cycle" to "الدورة الحالية",
            "loop" to "الجولة الحالية",
            "tap" to "اضغط على الدائرة للذكر",
            "completed" to "اكتملت التسبيحة",
            "complete_message" to "أكملت هذه الدورة. يمكنك بدء دورة جديدة.",
            "start_again" to "ابدأ من جديد",
            "undo" to "تراجع",
            "reset" to "إعادة",
            "goal" to "الهدف",
            "current_dhikr" to "الذكر الحالي",
            "change" to "تغيير",
            "bead_style" to "نمط الخرز",
            "set_goal" to "الهدف اليومي",
            "goal_label" to "عدد الهدف",
            "save" to "حفظ",
            "cancel" to "إلغاء",
            "reset_title" to "إعادة ضبط العدّاد؟",
            "reset_message" to "سيُعاد ضبط العدّاد الحالي فقط، ولن يتغير المجموع اليومي المحفوظ.",
            "select_dhikr" to "اختر الذكر",
            "close" to "إغلاق",
            "sound_on" to "الصوت مفعّل",
            "sound_off" to "الصوت متوقف",
            "haptics_on" to "الاهتزاز مفعّل",
            "haptics_off" to "الاهتزاز متوقف"
        )
        else -> mapOf(
            "title" to "Digital Tasbeeh",
            "today" to "Today",
            "cycle" to "This cycle",
            "loop" to "Current loop",
            "tap" to "Tap or swipe the circle to count",
            "completed" to "Tasbeeh completed",
            "complete_message" to "You completed this round. Start another whenever you are ready.",
            "start_again" to "Start again",
            "undo" to "Undo",
            "reset" to "Reset",
            "goal" to "Goal",
            "current_dhikr" to "Current dhikr",
            "change" to "Change",
            "bead_style" to "Bead style",
            "set_goal" to "Set daily goal",
            "goal_label" to "Target count",
            "save" to "Save",
            "cancel" to "Cancel",
            "reset_title" to "Reset counter?",
            "reset_message" to "Only the current counter resets. Your saved daily total will not change.",
            "select_dhikr" to "Choose a dhikr",
            "close" to "Close",
            "sound_on" to "Sound is on",
            "sound_off" to "Sound is off",
            "haptics_on" to "Vibration is on",
            "haptics_off" to "Vibration is off"
        )
    }
    return values[key] ?: key
}
