package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class QuranReadingPalette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val secondaryText: Color,
    val arabic: Color,
    val border: Color,
    val accent: Color,
    val onAccent: Color
)

fun quranReadingPalette(theme: String): QuranReadingPalette = when (theme) {
    "Sepia" -> QuranReadingPalette(
        background = Color(0xFFF4ECD8),
        surface = Color(0xFFEFE5CD),
        text = Color(0xFF433422),
        secondaryText = Color(0xFF6E5B45),
        arabic = Color(0xFF5D4037),
        border = Color(0xFFD4BFA7),
        accent = Color(0xFF433422),
        onAccent = Color(0xFFFAF6EE)
    )
    "Dark" -> QuranReadingPalette(
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        text = Color(0xFFE0E0E0),
        secondaryText = Color(0xFFBDBDBD),
        arabic = Color(0xFF81C784),
        border = Color(0xFF333333),
        accent = Color(0xFFD4AF37),
        onAccent = Color(0xFF121212)
    )
    else -> QuranReadingPalette(
        background = Color(0xFFFAF6EE),
        surface = Color.White,
        text = Color(0xFF0F2E27),
        secondaryText = Color(0xFF53675F),
        arabic = Color(0xFF1B4D3E),
        border = Color(0xFFDDE5E0),
        accent = Color(0xFF0F2E27),
        onAccent = Color(0xFFFAF6EE)
    )
}

@Composable
internal fun QuranReadingSettingsControls(
    languageCode: String,
    palette: QuranReadingPalette,
    viewingMode: Int,
    onViewingModeChange: (Int) -> Unit,
    showEnglish: Boolean,
    onShowEnglishChange: (Boolean) -> Unit,
    showUrdu: Boolean,
    onShowUrduChange: (Boolean) -> Unit,
    readingTheme: String,
    onReadingThemeChange: (String) -> Unit,
    arabicFontType: String,
    onArabicFontTypeChange: (String) -> Unit,
    arabicFontScale: Float,
    onArabicScale: (Float) -> Unit,
    translationFontScale: Float,
    onTranslationScale: (Float) -> Unit,
    keepScreenAwake: Boolean,
    onKeepAwake: (Boolean) -> Unit,
    hifzMode: Boolean,
    onHifzMode: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    autoScrollActive: Boolean? = null,
    onAutoScrollChange: (Boolean) -> Unit = {}
) {
    Column(
        modifier = modifier
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.text.copy(alpha = 0.05f))
                .padding(4.dp)
        ) {
            listOf("bilingual", "arabic_only", "translations").forEachIndexed { index, key ->
                val selected = viewingMode == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) palette.accent else Color.Transparent)
                        .clickable { onViewingModeChange(index) }
                        .padding(horizontal = 3.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = quranReadingText(key, languageCode),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) palette.onAccent else palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = quranReadingText("filters", languageCode),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = palette.secondaryText
            )
            FilterChip(
                selected = showEnglish,
                onClick = { onShowEnglishChange(!showEnglish) },
                label = { Text("English") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = palette.accent,
                    selectedLabelColor = palette.onAccent
                )
            )
            FilterChip(
                selected = showUrdu,
                onClick = { onShowUrduChange(!showUrdu) },
                label = { Text(quranReadingText("urdu", languageCode)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = palette.accent,
                    selectedLabelColor = palette.onAccent
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.text.copy(alpha = 0.05f))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = quranReadingText("theme", languageCode),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = palette.secondaryText,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            listOf(
                Triple("Cream", Color(0xFFFAF6EE), "cream"),
                Triple("Sepia", Color(0xFFF4ECD8), "sepia"),
                Triple("Dark", Color(0xFF121212), "night")
            ).forEach { (theme, color, labelKey) ->
                val selected = readingTheme == theme
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) palette.accent else Color.Transparent)
                        .clickable { onReadingThemeChange(theme) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(0.5.dp, palette.secondaryText, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = quranReadingText(labelKey, languageCode),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) palette.onAccent else palette.text,
                        maxLines = 1
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FormatSize,
                        contentDescription = null,
                        tint = palette.text,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        quranReadingText("readability", languageCode),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.text
                    )
                }
                QuranReadabilitySlider(
                    title = quranReadingText("arabic", languageCode),
                    value = arabicFontScale,
                    baseFontSize = 26,
                    palette = palette,
                    onValueChangeFinished = onArabicScale
                )
                QuranReadabilitySlider(
                    title = quranReadingText("meaning", languageCode),
                    value = translationFontScale,
                    baseFontSize = 16,
                    palette = palette,
                    onValueChangeFinished = onTranslationScale
                )
            }
        }

        QuranReadingSettingSwitch(
            title = quranReadingText("arabic_script", languageCode) + ": " +
                quranReadingText(if (arabicFontType == "Indo-Pak") "indo_pak" else "uthmani", languageCode),
            checked = arabicFontType == "Indo-Pak",
            onCheckedChange = { onArabicFontTypeChange(if (it) "Indo-Pak" else "Uthmani") },
            palette = palette
        )
        if (autoScrollActive != null) {
            QuranReadingSettingSwitch(
                title = quranReadingText("auto_scroll", languageCode),
                checked = autoScrollActive,
                onCheckedChange = onAutoScrollChange,
                palette = palette
            )
        }
        QuranReadingSettingSwitch(
            title = quranToolText("keep_awake", languageCode),
            checked = keepScreenAwake,
            onCheckedChange = onKeepAwake,
            palette = palette
        )
        QuranReadingSettingSwitch(
            title = quranToolText("hifz_mode", languageCode),
            checked = hifzMode,
            onCheckedChange = onHifzMode,
            palette = palette
        )
        Text(
            text = quranToolText("hifz_note", languageCode),
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondaryText,
            lineHeight = 20.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun QuranReadabilitySlider(
    title: String,
    value: Float,
    baseFontSize: Int,
    palette: QuranReadingPalette,
    onValueChangeFinished: (Float) -> Unit
) {
    var draftValue by remember(value) { mutableFloatStateOf(value) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = palette.secondaryText,
            modifier = Modifier.width(66.dp)
        )
        Slider(
            value = draftValue,
            onValueChange = { draftValue = it },
            onValueChangeFinished = { onValueChangeFinished(draftValue) },
            valueRange = 0.8f..1.5f,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.arabic,
                inactiveTrackColor = palette.text.copy(alpha = 0.1f)
            ),
            modifier = Modifier.weight(1f).height(34.dp)
        )
        Text(
            text = "${(draftValue * baseFontSize).roundToInt()}sp",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = palette.text,
            modifier = Modifier.width(38.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun QuranReadingSettingSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    palette: QuranReadingPalette
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.text
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.accent,
                    checkedTrackColor = palette.arabic.copy(alpha = 0.55f)
                )
            )
        }
    }
}

internal fun quranReadingText(key: String, languageCode: String): String = when (languageCode) {
    "ur" -> when (key) {
        "bilingual" -> "عربی و ترجمہ"
        "arabic_only" -> "صرف عربی"
        "translations" -> "ترجمے"
        "filters" -> "ترجمے:"
        "urdu" -> "اردو"
        "theme" -> "تھیم"
        "cream" -> "کریم"
        "sepia" -> "سیپیا"
        "night" -> "نائٹ"
        "readability" -> "متن کی وضاحت"
        "arabic" -> "عربی"
        "meaning" -> "ترجمہ"
        "arabic_script" -> "عربی فونٹ"
        "indo_pak" -> "انڈو پاک"
        "uthmani" -> "عثمانی"
        "auto_scroll" -> "خودکار سکرول"
        "focus_mode" -> "فوکس ریڈنگ"
        else -> key
    }
    "ar" -> when (key) {
        "bilingual" -> "العربية والترجمة"
        "arabic_only" -> "العربية فقط"
        "translations" -> "الترجمات"
        "filters" -> "الترجمات:"
        "urdu" -> "الأردية"
        "theme" -> "المظهر"
        "cream" -> "كريمي"
        "sepia" -> "بني فاتح"
        "night" -> "ليلي"
        "readability" -> "وضوح القراءة"
        "arabic" -> "العربية"
        "meaning" -> "المعنى"
        "arabic_script" -> "الخط العربي"
        "indo_pak" -> "هندي باكستاني"
        "uthmani" -> "عثماني"
        "auto_scroll" -> "تمرير تلقائي"
        "focus_mode" -> "قراءة مركزة"
        else -> key
    }
    else -> when (key) {
        "bilingual" -> "Bilingual"
        "arabic_only" -> "Arabic Only"
        "translations" -> "Translations"
        "filters" -> "Filters:"
        "urdu" -> "Urdu"
        "theme" -> "Theme:"
        "cream" -> "Cream"
        "sepia" -> "Sepia"
        "night" -> "Night"
        "readability" -> "Adjust Readability"
        "arabic" -> "Arabic"
        "meaning" -> "Meaning"
        "arabic_script" -> "Arabic Script"
        "indo_pak" -> "Indo-Pak"
        "uthmani" -> "Uthmani"
        "auto_scroll" -> "Auto-Scroll"
        "focus_mode" -> "Focus Reading"
        else -> key
    }
}
