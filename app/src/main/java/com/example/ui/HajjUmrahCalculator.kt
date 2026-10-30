package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmiriFont
import androidx.compose.ui.platform.LocalUriHandler

@Composable
fun HajjUmrahTab(
    viewModel: PrayerViewModel,
    languageCode: String,
    onOpenQuran: (surahNumber: Int, ayahNumber: Int) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = when (languageCode) {
        "ur" -> listOf("عمرہ", "حج", "سفر", "معلومات")
        "ar" -> listOf("العمرة", "الحج", "الرحلة", "معلومات")
        else -> listOf("Umrah", "Hajj", "Journey", "Info")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> UmrahGuide(viewModel, languageCode, onOpenQuran)
                1 -> HajjGuide(viewModel, languageCode, onOpenQuran)
                2 -> HajjMapVisualization(languageCode)
                3 -> InfoGuide(languageCode)
            }
        }
    }
}

@Composable
fun HajjMapVisualization(languageCode: String) {
    val stops = remember { hajjJourneyStops() }
    val accents = listOf(
        MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary,
        MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary,
        MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 720.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = when (languageCode) {
                    "ur" -> "حج کا مرحلہ وار سفر"
                    "ar" -> "رحلة مناسك الحج خطوة بخطوة"
                    else -> "Hajj Ritual Journey"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (languageCode) {
                    "ur" -> "نمبروں اور تیروں کو follow کریں؛ مقامات کی تکرار سفر کی اصل ترتیب دکھاتی ہے۔"
                    "ar" -> "اتبع الأرقام والأسهم؛ ويكرر ظهور المواقع لتوضيح الترتيب الفعلي للرحلة."
                    else -> "Follow the numbers and arrows; repeated locations reflect the actual ritual sequence."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (languageCode) {
                                    "ur" -> "رهنما خاکہ • جغرافیایی پیمانہ نہیں"
                                    "ar" -> "مخطط إرشادي • ليس بمقياس جغرافي"
                                    else -> "SCHEMATIC ROUTE • NOT TO SCALE"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    stops.forEachIndexed { index, stop ->
                        HajjJourneyStopRow(
                            stop = stop,
                            languageCode = languageCode,
                            accent = accents[index % accents.size].first,
                            onAccent = accents[index % accents.size].second,
                            showConnector = index < stops.lastIndex
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (languageCode) {
                            "ur" -> "مکہ اور منیٰ دوبارہ کیوں؟"
                            "ar" -> "لماذا تتكرر مكة ومنى؟"
                            else -> "Why do Makkah and Mina appear again?"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = when (languageCode) {
                            "ur" -> "منیٰ کے 10 ذوالحجہ کے اعمال کے بعد حاجی مکہ میں طواف، نماز، سعی اور طواف النساء کے لیے جاتا ہے، پھر واجب راتوں اور باقی جمرات کے لیے منیٰ واپس آتا ہے۔ یہ خاکہ مقامات کا جغرافیایی نقشہ نہیں، اعمال کی ترتیب ہے۔"
                            "ar" -> "بعد أعمال منى يوم العاشر، يذهب الحاج إلى مكة للطواف والصلاة والسعي وطواف النساء، ثم يعود إلى منى لليالي الواجبة وبقية الجمرات. هذا ترتيب للأعمال لا خريطة جغرافية."
                            else -> "After the Mina rites on the 10th, the pilgrim travels to Makkah for Tawaf, prayer, Sa‘i and Tawaf al-Nisa, then returns to Mina for the required nights and remaining Jamarat. This is a ritual sequence, not a geographic map."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        lineHeight = 20.sp
                    )
                    GuideSourceLink(
                        url = "https://www.sistani.org/english/book/47/2089/",
                        languageCode = languageCode
                    )
                }
            }
        }
    }
}

@Composable
private fun HajjJourneyStopRow(
    stop: HajjJourneyStop,
    languageCode: String,
    accent: Color,
    onAccent: Color,
    showConnector: Boolean
) {
    Column {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accent, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stop.order.toString(),
                    color = onAccent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    accent.copy(alpha = 0.28f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        stop.location.get(languageCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            stop.date.get(languageCode),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stop.ritual.get(languageCode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        if (showConnector) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(8.dp)
                            .background(accent.copy(alpha = 0.45f))
                    )
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(8.dp)
                            .background(accent.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}

@Composable
fun UmrahGuide(
    viewModel: PrayerViewModel,
    languageCode: String,
    onOpenQuran: (surahNumber: Int, ayahNumber: Int) -> Unit
) {
    val hajjUmrahItems by viewModel.allHajjUmrahStates.collectAsState()
    
    val steps = remember { umrahGuideSteps() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            text = when (languageCode) {
                "ur" -> "عمرہ کا قدم بہ قدم طریقہ اور چیک لسٹ"
                "ar" -> "دليل خطوات العمرة"
                else -> "Umrah — Step-by-Step Checklist"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        GuideScopeNotice(languageCode)
        steps.forEachIndexed { index, step ->
            val isChecked = hajjUmrahItems.find { it.type == "UMRAH" && it.stepId == step.id }?.isCompleted == true
            StepCard(
                step = step,
                displayNumber = index + 1,
                languageCode = languageCode,
                isChecked = isChecked,
                onCheckedChange = { checked -> viewModel.toggleHajjUmrahItem("UMRAH", step.id, checked) },
                onOpenQuran = onOpenQuran
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun HajjGuide(
    viewModel: PrayerViewModel,
    languageCode: String,
    onOpenQuran: (surahNumber: Int, ayahNumber: Int) -> Unit
) {
    val hajjUmrahItems by viewModel.allHajjUmrahStates.collectAsState()
    
    val steps = remember { hajjGuideSteps() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            text = when (languageCode) {
                "ur" -> "حج (تمتع) کا طریقہ اور چیک لسٹ"
                "ar" -> "دليل خطوات حج التمتع"
                else -> "Hajj al-Tamattu — Step-by-Step Checklist"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        GuideScopeNotice(languageCode)
        steps.forEachIndexed { index, step ->
            val isChecked = hajjUmrahItems.find { it.type == "HAJJ" && it.stepId == step.id }?.isCompleted == true
            StepCard(
                step = step,
                displayNumber = index + 1,
                languageCode = languageCode,
                isChecked = isChecked,
                onCheckedChange = { checked -> viewModel.toggleHajjUmrahItem("HAJJ", step.id, checked) },
                onOpenQuran = onOpenQuran
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GuideScopeNotice(languageCode: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = when (languageCode) {
                    "ur" -> "شیعہ فقہی رہنمائی: ہر مرحلے کے ساتھ قرآن، روایت، دعا یا مستند فقہی ماخذ منسلک ہے۔ اپنی خاص صورتِ حال کے لیے اپنے مرجع کی رہنمائی لیں۔"
                    "ar" -> "دليل وفق الفقه الشيعي: ترفق بكل خطوة آية أو رواية أو دعاء أو مصدر فقهي موثوق. راجع مرجعك في الحالات الخاصة."
                    else -> "Shia fiqh guide: every step includes a Qur’an, Hadith, Dua or trusted juristic reference. Consult your Marja for personal circumstances."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun InfoGuide(languageCode: String) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = when (languageCode) {
                        "ur" -> "حج کی اقسام اور اس گائیڈ کا دائرہ"
                        "ar" -> "أقسام الحج ونطاق هذا الدليل"
                        else -> "Types of Hajj and this guide’s scope"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (languageCode) {
                        "ur" -> "حج کی تین اقسام ہیں: تمتع، افراد اور قران۔ تمتع میں عمرہ کے بعد احرام سے نکل کر حج کا نیا احرام باندھا جاتا ہے؛ قران میں حج و عمرہ ایک ہی احرام اور قربانی کے ساتھ ہوتے ہیں؛ افراد حج تمتع کے عمرہ کے سلسلے کے بغیر ہے۔ کون سا حج واجب ہے، یہ رہائش اور ذاتی حالات کے مطابق طے ہوتا ہے (سیستانی، احکام 143–146)۔\n\nیہ چیک لسٹ خصوصاً عمرہ و حجِ تمتع کے لیے ہے، اور جہاں ضروری ہو عمرہ مفردہ کا فرق الگ ظاہر کرتی ہے۔"
                        "ar" -> "أقسام الحج ثلاثة: التمتع والإفراد والقِران. في التمتع يتحلل بعد العمرة ثم يحرم للحج؛ وفي القِران يجمع الحج والعمرة في إحرام واحد مع الهدي؛ والإفراد حج من دون تسلسل عمرة التمتع. ويتحدد الواجب بحسب السكن والحالات الشخصية (السيستاني، الأحكام 143–146).\n\nهذه القائمة مخصصة لعمرة وحج التمتع، وتوضح أحكام العمرة المفردة حيث يلزم."
                        else -> "The three types are Tamattu, Ifrad and Qiran. In Tamattu, Umrah is completed and Ihram is left before a new Ihram for Hajj; Qiran combines Hajj and Umrah in one Ihram with Hady; Ifrad is Hajj without the Tamattu Umrah sequence. Which is obligatory depends on residence and personal circumstances (Sistani, Rules 143–146).\n\nThis checklist is specifically for Umrah and Hajj al-Tamattu, while identifying Umrah Mufradah differences where needed."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 21.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                GuideSourceLink(
                    url = "https://www.sistani.org/english/book/47/2088/",
                    languageCode = languageCode
                )
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = when (languageCode) {
                        "ur" -> "لازمی اعمال — واجب اور رکن میں فرق"
                        "ar" -> "الأعمال الواجبة — الفرق بين الواجب والركن"
                        else -> "Required acts — Wajib is not always Rukn"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (languageCode) {
                        "ur" -> "عمرہ تمتع کے پانچ اعمال: میقات سے احرام، طواف، نمازِ طواف، سعی اور تقصیر۔ عمرہ مفردہ میں طواف النساء اور اس کی نماز بھی واجب ہے۔\n\nحج تمتع کے تیرہ واجب اعمال: مکہ سے احرام، وقوفِ عرفات، وقوفِ مزدلفہ، جمرہ عقبہ کی رمی، قربانی، حلق/تقصیر، طواف حج، اس کی نماز، سعی، طواف النساء، اس کی نماز، منیٰ میں مقررہ راتیں، اور تینوں جمرات کی رمی۔\n\nطواف النساء اور اس کی نماز واجب ہیں لیکن مذکورہ فقہی ماخذ کے مطابق ارکانِ حج میں شامل نہیں؛ انھیں اختیاری نہ سمجھیں۔"
                        "ar" -> "أعمال عمرة التمتع خمسة: الإحرام من الميقات، والطواف، وصلاة الطواف، والسعي، والتقصير. وفي العمرة المفردة يجب أيضاً طواف النساء وصلاته.\n\nواجبات حج التمتع ثلاثة عشر: الإحرام من مكة، والوقوف بعرفات، والوقوف بمزدلفة، ورمي جمرة العقبة، والهدي، والحلق/التقصير، وطواف الحج، وصلاته، والسعي، وطواف النساء، وصلاته، والمبيت بمنى في الليالي المقررة، ورمي الجمرات الثلاث.\n\nطواف النساء وصلاته واجبان، لكنهما ليسا من أركان الحج في المصدر الفقهي المذكور؛ فلا يجوز اعتبارهما اختياريين."
                        else -> "Five acts of Umrah al-Tamattu: Ihram from Miqat, Tawaf, Tawaf prayer, Sa‘i and Taqsir. Umrah Mufradah additionally requires Tawaf al-Nisa and its prayer.\n\nThirteen obligations of Hajj al-Tamattu: Ihram from Makkah, Wuquf at Arafat, Wuquf at Muzdalifah, Rami of Jamrat al-Aqabah, Hady, Halq/Taqsir, Tawaf al-Hajj, its prayer, Sa‘i, Tawaf al-Nisa, its prayer, the required Mina nights, and Rami of the three Jamarat.\n\nTawaf al-Nisa and its prayer are obligatory but are not among the arkan in the cited juristic manual; they must not be treated as optional."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    lineHeight = 21.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                GuideSourceLink(
                    url = "https://www.sistani.org/english/book/47/2089/",
                    languageCode = languageCode
                )
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = when (languageCode) {
                        "ur" -> "مستند ماخذ اور احتیاط"
                        "ar" -> "المصادر الموثوقة والتنبيه"
                        else -> "Trusted sources and caution"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (languageCode) {
                        "ur" -> "فقہی ترتیب اور احکام کے لیے آیت اللہ سیستانی کی Hajj Rituals، مستحب دعاؤں کے لیے اسی کتاب کا Recommended acts and supplications، اور دعائے عرفہ کے لیے امام حسینؑ کا مکمل متن استعمال کیا گیا ہے۔ یہ مختصر رہنمائی کسی خاص شرعی صورت میں مرجع کے حکم کا بدل نہیں۔"
                        "ar" -> "اعتمد الدليل كتاب Hajj Rituals للسيستاني في الترتيب والأحكام، وقسم Recommended acts and supplications للأدعية، والنص الكامل لدعاء الإمام الحسين (ع) يوم عرفة. هذا الملخص لا يغني عن حكم المرجع في الحالات الخاصة."
                        else -> "The legal sequence uses Ayatullah Sistani’s Hajj Rituals; recommended duas use its Recommended acts and supplications section; and Imam Hussain’s complete Dua Arafah is linked directly. This summary does not replace a Marja’s ruling for individual circumstances."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 21.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                GuideSourceLink(
                    url = "https://www.sistani.org/english/book/47/",
                    languageCode = languageCode,
                    label = when (languageCode) {
                        "ur" -> "مکمل احکامِ حج"
                        "ar" -> "أحكام الحج كاملة"
                        else -> "Complete Hajj rulings"
                    }
                )
                GuideSourceLink(
                    url = "https://www.sistani.org/english/book/47/2114/",
                    languageCode = languageCode,
                    label = when (languageCode) {
                        "ur" -> "مستحب اعمال اور دعائیں"
                        "ar" -> "الأعمال والأدعية المستحبة"
                        else -> "Recommended acts and supplications"
                    }
                )
                GuideSourceLink(
                    url = "https://www.duas.org/dua-arafah-imam-husain.html",
                    languageCode = languageCode,
                    label = when (languageCode) {
                        "ur" -> "مکمل دعائے عرفہ — امام حسینؑ"
                        "ar" -> "دعاء عرفة كاملاً — الإمام الحسين (ع)"
                        else -> "Complete Dua Arafah — Imam Hussain (as)"
                    }
                )
            }
        }
    }
}

@Composable
fun StepCard(
    step: GuideStep,
    displayNumber: Int,
    languageCode: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenQuran: (surahNumber: Int, ayahNumber: Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            displayNumber.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title.get(languageCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        RequirementBadge(step.requirement, languageCode)
                        Spacer(modifier = Modifier.height(7.dp))
                        Text(
                            text = step.description.get(languageCode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
                Checkbox(checked = isChecked, onCheckedChange = onCheckedChange)
            }

            step.note?.let { note ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        note.get(languageCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        lineHeight = 18.sp
                    )
                }
            }

            if (step.references.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                GuideSectionLabel(
                    when (languageCode) {
                        "ur" -> "قرآن، روایت اور مستند ماخذ"
                        "ar" -> "القرآن والروايات والمصادر الموثوقة"
                        else -> "Qur’an, Hadith and trusted sources"
                    }
                )
                step.references.forEach { reference ->
                    GuideReferenceRow(reference, languageCode, onOpenQuran)
                }
            }

            if (step.duas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                GuideSectionLabel(
                    when (languageCode) {
                        "ur" -> "دعائیں اور اذکار"
                        "ar" -> "الأدعية والأذكار"
                        else -> "Duas and Adhkar"
                    }
                )
                step.duas.forEach { dua ->
                    GuideDuaRow(dua, languageCode)
                }
            }
        }
    }
}

@Composable
private fun RequirementBadge(requirement: GuideRequirement, languageCode: String) {
    val label = when (requirement) {
        GuideRequirement.OBLIGATORY -> when (languageCode) {
            "ur" -> "واجب"
            "ar" -> "واجب"
            else -> "Obligatory"
        }
        GuideRequirement.RECOMMENDED -> when (languageCode) {
            "ur" -> "مستحب"
            "ar" -> "مستحب"
            else -> "Recommended"
        }
        GuideRequirement.CONDITIONAL -> when (languageCode) {
            "ur" -> "صورت کے مطابق واجب"
            "ar" -> "واجب بحسب الحالة"
            else -> "Conditionally obligatory"
        }
    }
    val container = when (requirement) {
        GuideRequirement.OBLIGATORY -> MaterialTheme.colorScheme.primaryContainer
        GuideRequirement.RECOMMENDED -> MaterialTheme.colorScheme.secondaryContainer
        GuideRequirement.CONDITIONAL -> MaterialTheme.colorScheme.tertiaryContainer
    }
    Surface(color = container, shape = RoundedCornerShape(50)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GuideSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun GuideReferenceRow(
    reference: GuideReference,
    languageCode: String,
    onOpenQuran: (surahNumber: Int, ayahNumber: Int) -> Unit
) {
    val icon = when (reference.type) {
        GuideReferenceType.QURAN -> Icons.Default.MenuBook
        GuideReferenceType.HADITH -> Icons.Default.AccountBox
        GuideReferenceType.IMAM -> Icons.Default.Star
        GuideReferenceType.FIQH -> Icons.Default.Gavel
    }
    Row(
        modifier = Modifier.padding(bottom = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                reference.citation.get(languageCode),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                lineHeight = 18.sp
            )
            Text(
                reference.summary.get(languageCode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
            if (reference.type == GuideReferenceType.QURAN &&
                reference.surahNumber != null && reference.ayahNumber != null
            ) {
                GuideQuranLink(
                    surahNumber = reference.surahNumber,
                    ayahNumber = reference.ayahNumber,
                    languageCode = languageCode,
                    onOpenQuran = onOpenQuran
                )
            } else {
                GuideSourceLink(reference.url, languageCode)
            }
        }
    }
}

@Composable
private fun GuideQuranLink(
    surahNumber: Int,
    ayahNumber: Int,
    languageCode: String,
    onOpenQuran: (surahNumber: Int, ayahNumber: Int) -> Unit
) {
    Text(
        text = localizedReferenceText("read_in_app_quran", languageCode) + " →",
        modifier = Modifier
            .padding(top = 3.dp, bottom = 2.dp)
            .clickable { onOpenQuran(surahNumber, ayahNumber) },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        textDecoration = TextDecoration.Underline
    )
}

@Composable
private fun GuideDuaRow(dua: GuideDua, languageCode: String) {
    Row(
        modifier = Modifier.padding(bottom = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                dua.title.get(languageCode),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )
            dua.arabic?.let { arabic ->
                Text(
                    arabic,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = AmiriFont,
                    fontSize = 18.sp,
                    lineHeight = 29.sp,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                dua.body.get(languageCode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
            Text(
                dua.citation.get(languageCode),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 3.dp),
                lineHeight = 16.sp
            )
            GuideSourceLink(dua.url, languageCode)
        }
    }
}

@Composable
private fun GuideSourceLink(
    url: String,
    languageCode: String,
    label: String? = null
) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = label ?: when (languageCode) {
            "ur" -> "ماخذ کھولیں ↗"
            "ar" -> "فتح المصدر ↗"
            else -> "Open source ↗"
        },
        modifier = Modifier
            .padding(top = 3.dp, bottom = 2.dp)
            .clickable { uriHandler.openUri(url) },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        textDecoration = TextDecoration.Underline
    )
}
