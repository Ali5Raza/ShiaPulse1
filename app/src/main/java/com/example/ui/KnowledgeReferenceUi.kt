package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KnowledgeReference
import com.example.data.KnowledgeReferenceType

@Composable
fun KnowledgeReferenceList(
    references: List<KnowledgeReference>,
    languageCode: String,
    modifier: Modifier = Modifier,
    onOpenQuran: ((surahNumber: Int, ayahNumber: Int) -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        references.forEach { reference ->
            KnowledgeReferenceCard(
                reference = reference,
                languageCode = languageCode,
                onOpenQuran = onOpenQuran
            )
        }
    }
}

@Composable
fun KnowledgeReferenceCard(
    reference: KnowledgeReference,
    languageCode: String,
    modifier: Modifier = Modifier,
    onOpenQuran: ((surahNumber: Int, ayahNumber: Int) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val hasInternalQuran = reference.type == KnowledgeReferenceType.QURAN &&
        reference.surahNumber != null && reference.ayahNumber != null && onOpenQuran != null
    val hasLink = !hasInternalQuran &&
        (reference.url.startsWith("https://") || reference.url.startsWith("http://"))
    val canOpen = hasInternalQuran || hasLink
    val customNote = reference.localizedNote(languageCode)
    val relationNote = when {
        customNote.isNotBlank() -> customNote
        reference.type == KnowledgeReferenceType.QURAN && !reference.direct -> when (languageCode) {
            "ur" -> "متعلقہ آیت: اس آیت میں فہرست کی یہی صورت لفظاً موجود ہونے کا دعویٰ نہیں کیا گیا۔"
            "ar" -> "آية مرتبطة بالسياق؛ لا يُدَّعى أنها تذكر صيغة القائمة نفسها حرفياً."
            else -> "Related verse: it supports the attribute or context without claiming to quote this exact list form."
        }
        else -> ""
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (canOpen) {
                    Modifier.clickable {
                        if (hasInternalQuran) {
                            onOpenQuran?.invoke(reference.surahNumber!!, reference.ayahNumber!!)
                        } else {
                            runCatching { uriHandler.openUri(reference.url) }
                        }
                    }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = when (reference.type) {
                    KnowledgeReferenceType.QURAN -> Icons.Default.MenuBook
                    KnowledgeReferenceType.HADITH -> Icons.Default.AccountBox
                    KnowledgeReferenceType.AQWAL -> Icons.Default.Star
                    KnowledgeReferenceType.HISTORY -> Icons.Default.History
                    KnowledgeReferenceType.DUA -> Icons.Default.MenuBook
                    KnowledgeReferenceType.GENERAL -> Icons.Default.Info
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = referenceTypeLabel(reference.type, languageCode),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = reference.citation,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (relationNote.isNotBlank()) {
                    Text(
                        text = relationNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                if (canOpen) {
                    Text(
                        text = localizedReferenceText(
                            if (hasInternalQuran) "read_in_app_quran" else "open_source",
                            languageCode
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (canOpen) {
                Icon(
                    imageVector = if (hasInternalQuran) Icons.Default.MenuBook else Icons.Default.OpenInNew,
                    contentDescription = localizedReferenceText(
                        if (hasInternalQuran) "read_in_app_quran" else "open_source",
                        languageCode
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CompactReferenceLink(
    url: String,
    languageCode: String,
    modifier: Modifier = Modifier,
    label: String = localizedReferenceText("source", languageCode)
) {
    if (url.isBlank()) return
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { runCatching { uriHandler.openUri(url) } }
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ReferenceIntroCard(
    text: String,
    languageCode: String,
    modifier: Modifier = Modifier,
    reference: KnowledgeReference? = null
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.22f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 17.sp
        )
        reference?.let {
            Row(
                modifier = Modifier.clickable { runCatching { uriHandler.openUri(it.url) } },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBox,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${it.citation} · ${localizedReferenceText("open_source", languageCode)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PendingReferenceText(languageCode: String, modifier: Modifier = Modifier) {
    Text(
        text = localizedReferenceText("pending", languageCode),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

fun localizedReferenceText(key: String, languageCode: String): String = when (languageCode) {
    "ur" -> when (key) {
        "source" -> "ماخذ و حوالہ"
        "open_source" -> "ماخذ کھولیں"
        "read_in_app_quran" -> "ایپ کے قرآن میں پڑھیں"
        "pending" -> "اس متن کا مستند حوالہ ابھی زیرِ جائزہ ہے؛ غیر مصدقہ روایت کے طور پر پیش نہیں کیا گیا۔"
        "quran" -> "قرآن"
        "hadith" -> "حدیث"
        "aqwal" -> "قولِ معصوم"
        "history" -> "تاریخی ماخذ"
        "dua" -> "دعا و زیارت"
        else -> "حوالہ"
    }
    "ar" -> when (key) {
        "source" -> "المصدر والمرجع"
        "open_source" -> "فتح المصدر"
        "read_in_app_quran" -> "اقرأ في قرآن التطبيق"
        "pending" -> "المرجع الموثق لهذا النص قيد المراجعة؛ لا يُعرض كرواية مؤكدة."
        "quran" -> "القرآن"
        "hadith" -> "الحديث"
        "aqwal" -> "قول المعصوم"
        "history" -> "مصدر تاريخي"
        "dua" -> "الدعاء والزيارة"
        else -> "المرجع"
    }
    else -> when (key) {
        "source" -> "Source & reference"
        "open_source" -> "Open source"
        "read_in_app_quran" -> "Read in app Quran"
        "pending" -> "A reliable citation for this text is still under review; it is not presented as a verified narration."
        "quran" -> "Qur’an"
        "hadith" -> "Hadith"
        "aqwal" -> "Aqwal-e-Masoom"
        "history" -> "Historical source"
        "dua" -> "Dua & Ziyarat"
        else -> "Reference"
    }
}

private fun referenceTypeLabel(type: KnowledgeReferenceType, languageCode: String): String =
    localizedReferenceText(
        when (type) {
            KnowledgeReferenceType.QURAN -> "quran"
            KnowledgeReferenceType.HADITH -> "hadith"
            KnowledgeReferenceType.AQWAL -> "aqwal"
            KnowledgeReferenceType.HISTORY -> "history"
            KnowledgeReferenceType.DUA -> "dua"
            KnowledgeReferenceType.GENERAL -> "general"
        },
        languageCode
    )
