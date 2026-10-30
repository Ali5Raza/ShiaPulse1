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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.QuranAyahMetadata
import com.example.data.QuranAyahMetadataRepository
import com.example.data.QuranNavigationRepository
import com.example.data.QuranNavigationTarget
import com.example.data.QuranNavigationType
import com.example.data.QuranProgressStore
import com.example.data.QuranReadingPosition
import com.example.data.QuranSurahInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class QuranNavigationRequest(
    val type: QuranNavigationType,
    val number: Int
)

/** Metadata-driven navigation; the reader remains the only place where Quran text is displayed. */
@Composable
fun QuranNavigationPanel(
    surahs: List<QuranSurahInfo>,
    languageCode: String,
    currentSurahNumber: Int?,
    currentAyahNumber: Int?,
    lastRead: QuranReadingPosition?,
    onOpenReference: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val positionSurah = currentSurahNumber ?: lastRead?.surahNumber
    val positionAyah = if (currentSurahNumber != null) currentAyahNumber else lastRead?.ayahNumber
    var metadata by remember { mutableStateOf<QuranAyahMetadata?>(null) }
    var metadataLoading by remember { mutableStateOf(false) }
    var metadataFailed by remember { mutableStateOf(false) }
    var metadataRetry by remember { mutableStateOf(0) }
    var selectedTypeName by rememberSaveable { mutableStateOf(QuranNavigationType.JUZ.name) }
    val selectedType = QuranNavigationType.entries.firstOrNull { it.name == selectedTypeName }
        ?: QuranNavigationType.JUZ
    var unitInput by rememberSaveable { mutableStateOf("1") }
    var request by remember { mutableStateOf<QuranNavigationRequest?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentJuz by remember { mutableStateOf<QuranNavigationTarget?>(null) }
    var currentHizb by remember { mutableStateOf<QuranNavigationTarget?>(null) }

    LaunchedEffect(positionSurah, positionAyah, metadataRetry) {
        metadata = null
        currentJuz = null
        currentHizb = null
        metadataFailed = false
        metadataLoading = false
        if (positionSurah == null || positionAyah == null) return@LaunchedEffect
        metadataLoading = true
        try {
            metadata = withContext(Dispatchers.IO) {
                QuranAyahMetadataRepository.load(context, positionSurah, positionAyah)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            metadataFailed = true
        } finally {
            metadataLoading = false
        }
    }

    LaunchedEffect(metadata?.juzNumber, metadata?.hizbNumber) {
        val current = metadata ?: return@LaunchedEffect
        current.juzNumber?.let { juz ->
            try {
                currentJuz = withContext(Dispatchers.IO) {
                    QuranNavigationRepository.resolve(context, QuranNavigationType.JUZ, juz)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                currentJuz = null
            }
        }
        current.hizbNumber?.let { hizb ->
            try {
                currentHizb = withContext(Dispatchers.IO) {
                    QuranNavigationRepository.resolve(context, QuranNavigationType.HIZB, hizb)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                currentHizb = null
            }
        }
    }

    LaunchedEffect(request) {
        val currentRequest = request ?: return@LaunchedEffect
        try {
            val target = withContext(Dispatchers.IO) {
                QuranNavigationRepository.resolve(
                    context,
                    currentRequest.type,
                    currentRequest.number
                )
            }
            request = null
            onOpenReference(target.surahNumber, target.ayahNumber)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            request = null
            errorMessage = quranNavigationText("navigation_error", languageCode)
        }
    }

    val globalAyahId = remember(surahs, positionSurah, positionAyah) {
        if (positionSurah == null || positionAyah == null) {
            null
        } else {
            surahs.filter { it.number < positionSurah }.sumOf { it.verseCount } + positionAyah
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                quranNavigationText("title", languageCode),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                quranNavigationText("subtitle", languageCode),
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
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        quranNavigationText("current_position", languageCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (positionSurah == null || positionAyah == null) {
                        Text(quranNavigationText("no_position", languageCode))
                    } else {
                        val info = surahs.firstOrNull { it.number == positionSurah }
                        Text(
                            "${info?.name ?: quranNavigationText("surah", languageCode)} " +
                                "$positionSurah:$positionAyah",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        when {
                            metadataLoading -> CircularProgressIndicator()
                            metadataFailed -> {
                                Text(
                                    quranNavigationText("metadata_error", languageCode),
                                    color = MaterialTheme.colorScheme.error
                                )
                                TextButton(onClick = { metadataRetry++ }) {
                                    Text(quranNavigationText("retry", languageCode))
                                }
                            }
                            metadata != null -> {
                                val current = metadata
                                if (current != null) {
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        current.juzNumber?.let {
                                            NavigationValueChip("juz", it, languageCode) {
                                                selectedTypeName = QuranNavigationType.JUZ.name
                                                unitInput = it.toString()
                                            }
                                        }
                                        current.hizbNumber?.let {
                                            NavigationValueChip("hizb", it, languageCode) {
                                                selectedTypeName = QuranNavigationType.HIZB.name
                                                unitInput = it.toString()
                                            }
                                        }
                                        current.rubElHizbNumber?.let {
                                            NavigationValueChip("rub_hizb", it, languageCode) {
                                                selectedTypeName = QuranNavigationType.RUB_HIZB.name
                                                unitInput = it.toString()
                                            }
                                        }
                                        current.rukuNumber?.let {
                                            NavigationValueChip("ruku", it, languageCode) {
                                                selectedTypeName = QuranNavigationType.RUKU.name
                                                unitInput = it.toString()
                                            }
                                        }
                                        current.manzilNumber?.let {
                                            NavigationValueChip("manzil", it, languageCode) {
                                                selectedTypeName = QuranNavigationType.MANZIL.name
                                                unitInput = it.toString()
                                            }
                                        }
                                        current.pageNumber?.let {
                                            NavigationValueChip("page", it, languageCode) {
                                                selectedTypeName = QuranNavigationType.PAGE.name
                                                unitInput = it.toString()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (globalAyahId != null) {
                            currentJuz?.let { target ->
                                NavigationProgressLine(
                                    label = quranNavigationText("juz", languageCode),
                                    target = target,
                                    globalAyahId = globalAyahId
                                )
                            }
                            currentHizb?.let { target ->
                                NavigationProgressLine(
                                    label = quranNavigationText("hizb", languageCode),
                                    target = target,
                                    globalAyahId = globalAyahId
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        quranNavigationText("select_unit", languageCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedTypeName == "SURAH",
                            onClick = {
                                selectedTypeName = "SURAH"
                                errorMessage = null
                            },
                            label = { Text(quranNavigationText("surah", languageCode)) }
                        )
                        QuranNavigationType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedTypeName == type.name,
                                onClick = {
                                    selectedTypeName = type.name
                                    errorMessage = null
                                },
                                label = { Text(navigationTypeLabel(type, languageCode)) }
                            )
                        }
                    }
                    if (selectedTypeName == "SURAH") {
                        OutlinedTextField(
                            value = unitInput,
                            onValueChange = { unitInput = it.filter(Char::isDigit).take(3) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(quranNavigationText("surah_number", languageCode)) },
                            supportingText = {
                                val number = unitInput.toIntOrNull()
                                Text(
                                    surahs.firstOrNull { it.number == number }?.let {
                                        "${it.name} · ${it.verseCount} ${quranNavigationText("verses", languageCode)}"
                                    } ?: quranNavigationText("surah_range", languageCode)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    } else {
                        OutlinedTextField(
                            value = unitInput,
                            onValueChange = { unitInput = it.filter(Char::isDigit).take(3) },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    "${navigationTypeLabel(selectedType, languageCode)} " +
                                        quranNavigationText("number", languageCode)
                                )
                            },
                            supportingText = {
                                Text("1–${selectedType.maximum}")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = {
                            val number = unitInput.toIntOrNull()
                            if (selectedTypeName == "SURAH") {
                                if (number == null || surahs.none { it.number == number }) {
                                    errorMessage = quranNavigationText("invalid_number", languageCode)
                                } else {
                                    errorMessage = null
                                    onOpenReference(number, 1)
                                }
                            } else if (number == null || number !in 1..selectedType.maximum) {
                                errorMessage = quranNavigationText("invalid_number", languageCode)
                            } else {
                                errorMessage = null
                                request = QuranNavigationRequest(selectedType, number)
                            }
                        },
                        enabled = request == null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (request != null) {
                            CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(quranNavigationText("go_first_ayah", languageCode))
                    }
                }
            }
        }

        item {
            HorizontalDivider()
            Text(
                quranNavigationText("source_note", languageCode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NavigationValueChip(
    key: String,
    value: Int,
    languageCode: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text("${quranNavigationText(key, languageCode)} $value") }
    )
}

@Composable
private fun NavigationProgressLine(
    label: String,
    target: QuranNavigationTarget,
    globalAyahId: Int
) {
    val position = (globalAyahId - target.firstVerseId + 1).coerceIn(1, target.totalVerses)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            "$label ${target.number} · $position/${target.totalVerses}",
            style = MaterialTheme.typography.labelLarge
        )
        LinearProgressIndicator(
            progress = { position.toFloat() / target.totalVerses.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun navigationTypeLabel(type: QuranNavigationType, languageCode: String): String =
    quranNavigationText(
        when (type) {
            QuranNavigationType.JUZ -> "juz"
            QuranNavigationType.HIZB -> "hizb"
            QuranNavigationType.RUB_HIZB -> "rub_hizb"
            QuranNavigationType.RUKU -> "ruku"
            QuranNavigationType.MANZIL -> "manzil"
            QuranNavigationType.PAGE -> "page"
        },
        languageCode
    )

internal fun quranNavigationText(key: String, languageCode: String): String = when (languageCode) {
    "ur" -> when (key) {
        "title" -> "قرآن نیویگیشن"
        "source_short" -> "Metadata · پہلی بار online، پھر offline"
        "subtitle" -> "سورۃ، جز، حزب، ربع الحزب، رکوع، منزل اور صفحہ سے سیدھا مطلوبہ آیت پر جائیں۔"
        "current_position" -> "موجودہ مقام"
        "no_position" -> "موجودہ مقام دیکھنے کے لیے کوئی سورۃ کھولیں یا تلاوت جاری رکھیں۔"
        "select_unit" -> "نیویگیشن کی قسم منتخب کریں"
        "surah" -> "سورۃ"
        "juz" -> "جز"
        "hizb" -> "حزب"
        "rub_hizb" -> "ربع الحزب"
        "ruku" -> "رکوع"
        "manzil" -> "منزل"
        "page" -> "صفحہ"
        "number" -> "نمبر"
        "surah_number" -> "سورۃ نمبر"
        "surah_range" -> "1–114 میں سے سورۃ نمبر درج کریں"
        "verses" -> "آیات"
        "go_first_ayah" -> "پہلی آیت پر جائیں"
        "invalid_number" -> "منتخب حد کے اندر درست نمبر درج کریں۔"
        "navigation_error" -> "یہ metadata لوڈ نہیں ہو سکی۔ انٹرنیٹ کے ساتھ دوبارہ کوشش کریں۔"
        "metadata_error" -> "موجودہ مقام کی metadata دستیاب نہیں۔"
        "retry" -> "دوبارہ کوشش"
        "source_note" -> "Navigation metadata: Quran Foundation. پہلی بار منتخب unit کے لیے انٹرنیٹ درکار ہے؛ محفوظ شدہ targets آف لائن کھلتے ہیں۔ قرآن کا متن پہلے سے bundled Tanzil/Qarai/Jawadi assets سے ہے۔"
        else -> key
    }
    "ar" -> when (key) {
        "title" -> "التنقل في القرآن"
        "source_short" -> "البيانات · أولاً عبر الإنترنت ثم دون اتصال"
        "subtitle" -> "انتقل مباشرة إلى أول آية حسب السورة أو الجزء أو الحزب أو ربع الحزب أو الركوع أو المنزل أو الصفحة."
        "current_position" -> "الموضع الحالي"
        "no_position" -> "افتح سورة أو تابع القراءة لعرض الموضع الحالي."
        "select_unit" -> "اختر نوع التنقل"
        "surah" -> "سورة"
        "juz" -> "جزء"
        "hizb" -> "حزب"
        "rub_hizb" -> "ربع الحزب"
        "ruku" -> "ركوع"
        "manzil" -> "منزل"
        "page" -> "صفحة"
        "number" -> "رقم"
        "surah_number" -> "رقم السورة"
        "surah_range" -> "أدخل رقم سورة بين 1 و114"
        "verses" -> "آيات"
        "go_first_ayah" -> "الانتقال إلى أول آية"
        "invalid_number" -> "أدخل رقماً صالحاً ضمن النطاق المحدد."
        "navigation_error" -> "تعذر تحميل هذه البيانات. حاول مجدداً مع اتصال بالإنترنت."
        "metadata_error" -> "بيانات الموضع الحالي غير متاحة."
        "retry" -> "إعادة المحاولة"
        "source_note" -> "بيانات التنقل: Quran Foundation. يلزم الإنترنت عند أول تحميل للوحدة، وتعمل الأهداف المحفوظة دون اتصال. نص القرآن من ملفات Tanzil/Qarai/Jawadi المضمنة."
        else -> key
    }
    else -> when (key) {
        "title" -> "Quran navigation"
        "source_short" -> "Metadata · online first, then offline"
        "subtitle" -> "Jump directly by Surah, Juz, Hizb, Rubʿ al-Hizb, Ruku, Manzil or Page."
        "current_position" -> "Current position"
        "no_position" -> "Open a Surah or continue reading to see your current position."
        "select_unit" -> "Choose a navigation unit"
        "surah" -> "Surah"
        "juz" -> "Juz"
        "hizb" -> "Hizb"
        "rub_hizb" -> "Rubʿ al-Hizb"
        "ruku" -> "Ruku"
        "manzil" -> "Manzil"
        "page" -> "Page"
        "number" -> "number"
        "surah_number" -> "Surah number"
        "surah_range" -> "Enter a Surah number from 1–114"
        "verses" -> "ayat"
        "go_first_ayah" -> "Go to first ayah"
        "invalid_number" -> "Enter a valid number within the selected range."
        "navigation_error" -> "This metadata could not be loaded. Retry with an internet connection."
        "metadata_error" -> "Current-position metadata is unavailable."
        "retry" -> "Retry"
        "source_note" -> "Navigation metadata: Quran Foundation. Internet is needed for a unit's first load; saved targets work offline. Quran text remains in the bundled Tanzil/Qarai/Jawadi assets."
        else -> key
    }
}
