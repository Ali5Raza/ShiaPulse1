package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class NameOfMuhammad(
    val id: Int,
    val arabic: String,
    val transliteration: String,
    val english: String,
    val urdu: String,
    val description: String,
    val descriptionUrdu: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamesOfMuhammadScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    languageCode: String = "en",
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {},
    onOpenQuran: ((surahNumber: Int, ayahNumber: Int) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf<NameOfMuhammad?>(null) }
    var activeCounter by remember { mutableStateOf(0) }

    val names = remember {
        listOf(
            NameOfMuhammad(1, "مُحَمَّدٌ", "Muhammad", "The Highly Praised", "جس کی بہت تعریف کی گئی ہو", "The primary sacred name of the Holy Prophet, representing perfect attributes.", "حضورِ اکرم صلی اللہ علیہ وآلہ وسلم کا اسمِ مبارک جو تمام تعریفوں کے جامع ہونے کا نشان ہے۔"),
            NameOfMuhammad(2, "أَحْمَدٌ", "Ahmad", "The Most Praiseworthy", "سب سے زیادہ تعریف کرنے والا", "Mentioned in Surah As-Saff by Prophet Isa (as) forecasting his arrival.", "قرآنِ پاک میں حضرت عیسیٰ علیہ السلام کی زبانی بشارت کے طور پر مذکور نام۔"),
            NameOfMuhammad(3, "حَامِدٌ", "Hamid", "The Praiser of Allah", "تعریف کرنے والا", "The one who performs absolute and perfect praise of Allah Almighty.", "اللہ تعالیٰ کی کثرت اور بہترین انداز میں حمد و ثنا کرنے والا۔"),
            NameOfMuhammad(4, "مَحْمُودٌ", "Mahmud", "The Praised One", "پسندیدہ اور لائقِ تعریف", "Reflects the supreme spiritual status of Maqam-e-Mahmud.", "مقامِ محمود پر فائز ہستی، جن کی کائنات کی ہر مخلوق ثنا کرتی ہے۔"),
            NameOfMuhammad(5, "أَبُو الْقَاسِمِ", "Abul Qasim", "Father of Qasim (Kunya)", "ابو القاسم (کنیت)", "The Prophet’s well-known agnomen (kunya), recorded in standard biographies of the Fourteen Infallibles.", "نبی کریم صلی اللہ علیہ وآلہ وسلم کی معروف کنیت، جو معصومین کی معیاری سیرت میں درج ہے۔"),
            NameOfMuhammad(6, "عَاقِبٌ", "Aqib", "The Successor / Last", "پیچھے آنے والا", "The one who came after all prophets, leaving no prophet after him.", "تمام انبیاء کے آخر میں تشریف لانے والا، جن کے بعد کوئی نبی نہیں۔"),
            NameOfMuhammad(7, "فَاتِحٌ", "Fatih", "The Opener / Conqueror", "کھولنے والا / فتح کرنے والا", "The opener of the doors of divine mercy, guidance, and spiritual victory.", "رحمت الٰہی کے دروازے اور علم کے بند راستے کھولنے والا۔"),
            NameOfMuhammad(8, "خَاتَمٌ", "Khatam", "The Seal", "مہرِ رسالت", "The Seal of all Messengers and Prophets, completing the chain.", "سلسلہ نبوت کو مکمل کر کے اس پر آخری مہر ثبت فرمانے والا۔"),
            NameOfMuhammad(9, "حَاشِرٌ", "Hashir", "The Gatherer", "جمع کرنے والا", "The one at whose feet and call all of humanity will gather on Judgment Day.", "قیامت کے دن جن کے قدموں میں تمام خلقت کا حشر کیا جائے گا۔"),
            NameOfMuhammad(10, "مَاحِي", "Mahi", "The Eraser of Disbelief", "مٹانے والا", "The chosen one through whom Allah erases polytheism and transgression.", "وہ مقدس ہستی جن کے ذریعے اللہ کفر اور شرک کے اندھیروں کو مٹاتا ہے۔"),
            NameOfMuhammad(11, "دَاعِي", "Da'i", "The Caller to Allah", "اللہ کی طرف بلانے والا", "Inviting humanity to the path of eternal light, peace, and salvation.", "انسانیت کو حق اور نجات کے ابدی راستے کی طرف دعوت دینے والا۔"),
            NameOfMuhammad(12, "سِرَاجٌ مُنِيرٌ", "Siraj-um-Munir", "The Glowing Lamp", "روشن چراغ", "Directly named in the Holy Quran as a source of absolute light and guidance.", "قرآن کریم میں روشن اور منور چراغ کے لقب سے پکارا گیا اسم مبارک۔"),
            NameOfMuhammad(13, "بَشِيرٌ", "Bashir", "The Giver of Glad Tidings", "خوشخبری دینے والا", "Forewarning believers of the beautiful rewards of Paradise and close comfort.", "ایمان والوں کو جنت کی لازوال نعمتوں اور قربِ الٰہی کی خوشخبری دینے والا۔"),
            NameOfMuhammad(14, "نَذِيرٌ", "Nadhir", "The Warner", "ڈرانے والا", "Warning humanity against deviation, loss of soul, and divine anger.", "خالق سے دوری اور گمراہی کے نقصانات سے بروقت خبردار کرنے والا۔"),
            NameOfMuhammad(15, "مُنْذِرٌ", "Mundhir", "The Warner", "خبردار کرنے والا", "One who calls people away from loss and warns every community to return to Allah.", "لوگوں کو گمراہی اور نقصان سے بچانے کے لیے اللہ کی طرف لوٹنے کی تنبیہ کرنے والا۔"),
            NameOfMuhammad(16, "أُمِّيٌّ", "Ummi", "The Untaught Prophet", "امی (کسی انسانی معلم سے نہ پڑھا ہوا)", "Taught by Allah rather than by a human teacher, as described in the Qur’an.", "کسی انسان کے بجائے اللہ کی تعلیم یافتہ، جیسا کہ قرآن میں بیان ہوا ہے۔"),
            NameOfMuhammad(17, "صَادِقٌ", "Sadiq", "The Truthful One", "سچا", "Famous throughout Arabia even before the declaration of prophet-hood.", "بچپن اور جوانی سے ہی صادق اور امانت کے اعلیٰ مقام پر فائز۔"),
            NameOfMuhammad(18, "أَمِينٌ", "Amin", "The Trustworthy", "امانت دار", "The protector of trusts, secrets, and spiritual messages perfectly.", "ہر امانت اور راز کی حفاظت کرنے والا سب سے بہترین امین۔"),
            NameOfMuhammad(19, "شَكُورٌ", "Shakur", "The Grateful", "شکر گزار", "Perfect in expressing boundless gratitude through every worship.", "اللہ تعالیٰ کی ہر نعمت پر ہر گھڑی شکر ادا کرنے والے۔"),
            NameOfMuhammad(20, "مُطَهَّرٌ", "Mutahhar", "The Purified", "پاک و پاکیزہ", "Completely purified from all defects, faults, and lower desires.", "ہر قسم کی لغزش اور باطنی و ظاہری عیوب سے مکمل پاک ہستی۔"),
            NameOfMuhammad(21, "طَيِّبٌ", "Tayyib", "The Pure / Good", "پاکیزہ اور نیک", "A traditional honorific emphasizing purity and goodness.", "پاکیزگی اور نیکی کو بیان کرنے والا روایتی لقب۔"),
            NameOfMuhammad(22, "مُجْتَبَىٰ", "Mujtaba", "The Chosen / Elected", "منتخب اور برگزیدہ", "An honorific meaning chosen or elected.", "منتخب یا برگزیدہ ہونے کے مفہوم والا لقب۔"),
            NameOfMuhammad(23, "مُصْطَفَىٰ", "Mustafa", "The Chosen One", "چنا ہوا", "Selected by Allah to carry the final and most complete guidance.", "الٰہی احکام اور پیغامِ ہدایت پہنچانے کے لیے برگزیدہ ہستی۔"),
            NameOfMuhammad(24, "مُرْتَضَىٰ", "Murtada", "The Well-Pleasing", "جس سے اللہ راضی ہو", "An honorific expressing Allah’s pleasure and approval.", "اللہ کی رضا اور پسندیدگی کا اظہار کرنے والا لقب۔"),
            NameOfMuhammad(25, "رَؤُوفٌ", "Ra'uf", "The Compassionate", "نہایت شفیق", "Filled with boundless love and care for his followers both here and there.", "اپنی امت سے بے پناہ پیار کرنے والا، رحمت کے سمندر۔"),
            NameOfMuhammad(26, "رَحِيمٌ", "Rahim", "The Merciful", "مہربان اور رحیم", "Exhibiting deep forgiveness and support for the struggling souls.", "کمزور گنہگاروں کی شفاعت فرمانے والا شفیق غمخوار۔"),
            NameOfMuhammad(27, "رَحْمَةٌ لِّلْعَالَمِينَ", "Rahmat-ul-lil-Aalameen", "Mercy to the Worlds", "تمام جہانوں کے لیے رحمت", "The Qur’an describes the Prophet as a mercy sent by Allah to all the worlds.", "قرآن نبی کریم صلی اللہ علیہ وآلہ وسلم کو اللہ کی جانب سے تمام جہانوں کے لیے رحمت قرار دیتا ہے۔"),
            NameOfMuhammad(28, "طٰهٰ", "Taha", "Taha (Qur’anic Address)", "طہٰ (قرآنی خطاب)", "The opening letters of Surah Taha, also counted among the Prophet’s titles in transmitted reports.", "سورۃ طہٰ کے ابتدائی حروف، جنہیں روایات میں نبی کریم صلی اللہ علیہ وآلہ وسلم کے القاب میں بھی شمار کیا گیا ہے۔"),
            NameOfMuhammad(29, "يٰسٓ", "Yaseen", "Yasin (Qur’anic Address)", "یٰسٓ (قرآنی خطاب)", "The opening letters of Surah Yasin, also counted among the Prophet’s titles in transmitted reports.", "سورۃ یٰسٓ کے ابتدائی حروف، جنہیں روایات میں نبی کریم صلی اللہ علیہ وآلہ وسلم کے القاب میں بھی شمار کیا گیا ہے۔"),
            NameOfMuhammad(30, "مُزَّمِّلٌ", "Muzzammil", "The Enwrapped One", "چادر اوڑھنے والا", "Comforted by Allah during early heavy spiritual revelations.", "ابتدائی نزولِ وحی کے وقت چادر مبارک لپیٹنے والا پیارا لقب۔"),
            NameOfMuhammad(31, "مُدَّثِّرٌ", "Muddaththir", "The Cloaked One", "کملی والا", "Addressed by Allah to rise and deliver the warnings to the world.", "سرگوشیوں اور وحی کے غار کے بعد کائنات کو بیدار کرنے والا۔"),
            NameOfMuhammad(32, "شَافِعٌ", "Shafi’", "The Intercessor", "شفاعت کرنے والا", "A title associated with the Prophet’s intercession by Allah’s permission.", "اللہ کی اجازت سے نبی کریم صلی اللہ علیہ وآلہ وسلم کی شفاعت سے وابستہ لقب۔"),
            NameOfMuhammad(33, "شَفِيعٌ", "Shafi'", "The Helper / Mediator", "سفارش کرنے والا", "The supreme advocate representing weak souls at the Divine Court.", "بارگاہ الٰہی میں عاجز بندوں کی شفاعت لانے والی برگزیدہ ہستی۔"),
            NameOfMuhammad(34, "مُشَفَّعٌ", "Mushaffa’", "Whose Intercession Is Accepted", "جس کی شفاعت قبول ہو", "The one whose intercession is accepted by Allah’s permission.", "وہ ہستی جس کی شفاعت اللہ کی اجازت سے قبول کی جاتی ہے۔")
        )
    }

    val filteredNames = remember(searchQuery, names) {
        if (searchQuery.isBlank()) {
            names
        } else {
            names.filter {
                it.transliteration.contains(searchQuery, ignoreCase = true) ||
                it.english.contains(searchQuery, ignoreCase = true) ||
                it.arabic.contains(searchQuery) ||
                it.urdu.contains(searchQuery)
            }
        }
    }

    Scaffold(
        topBar = {
            PremiumScreenTopBar(
                title = when (languageCode) {
                    "ur" -> "اسمائے محمدؐ (اسمائے گرامی)"
                    "ar" -> "أسماء وألقاب النبي محمد (ص)"
                    else -> "Names of Prophet Muhammad (saws)"
                },
                subtitle = when (languageCode) {
                    "ur" -> "قرآن اور روایات کے حوالوں کے ساتھ مبارک اسماء و القاب"
                    "ar" -> "الأسماء والألقاب مع مراجع من القرآن والروايات"
                    else -> "Sacred names & titles with Qur’an and hadith references"
                },
                onBack = onBack,
                backModifier = Modifier.testTag("muhammad_names_back")
            )
        },
        modifier = modifier.testTag("names_of_muhammad_root")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mini Interactive Tasbeeh Counter at the top
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (languageCode == "ur") "درود و ذکرِ اسمائے گرامی" else "Salawat & Sacred Name Counter",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (languageCode == "ur") "تعداد: $activeCounter" else "Recitations: $activeCounter",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Count button
                        Button(
                            onClick = { activeCounter++ },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Count")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+1")
                        }

                        // Reset button
                        IconButton(
                            onClick = { activeCounter = 0 },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Counter",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            ReferenceIntroCard(
                text = when (languageCode) {
                    "ur" -> "ہر نام یا لقب کے ساتھ دستیاب قرآنی، حدیث یا سیرت کا حوالہ دیکھیں۔ جن روایتی القاب کا مستند حوالہ ابھی جائزے میں ہے، انہیں واضح طور پر نشان زد کیا گیا ہے۔"
                    "ar" -> "راجع المرجع القرآني أو الروائي أو السيري المتاح لكل اسم ولقب. الألقاب التي ما زال مصدرها قيد المراجعة موضحة بوضوح."
                    else -> "Open each name for its available Qur’an, hadith, or biographical reference. Traditional honorifics still under source review are marked rather than presented as verified."
                },
                languageCode = languageCode,
                reference = KnowledgeNameReferences.prophetNamesOverview
            )

            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (languageCode == "ur") "نام یا ترجمہ تلاش کریں..." else "Search sacred name...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            // Names Grid with 2 columns to fit nicely
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredNames) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedName = item }
                            .testTag("muhammad_name_item_${item.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // ID Badge
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .align(Alignment.Start),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.id.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Arabic script
                            Text(
                                text = item.arabic,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            // Transliteration
                            Text(
                                text = item.transliteration,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            // Translation based on app language
                            Text(
                                text = if (languageCode == "ur") item.urdu else item.english,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // Beautiful Name Detail Dialog
    selectedName?.let { item ->
        val references = KnowledgeNameReferences.forProphetName(item.id)
        Dialog(onDismissRequest = { selectedName = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("muhammad_name_detail_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "NAME OF PROPHET MUHAMMAD (SAWS) #${item.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.arabic,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                onEditCard(
                                    com.example.api.ShiaQuote(
                                        arabic = item.arabic,
                                        english = "",
                                        urdu = "",
                                        source = "Name of Prophet Muhammad (PBUH)"
                                    )
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.transliteration,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column {
                            Text(
                                text = "Meaning / Attributes",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.english,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable {
                                    onEditCard(
                                        com.example.api.ShiaQuote(
                                            arabic = "",
                                            english = item.english,
                                            urdu = "",
                                            source = "Name of Prophet Muhammad (PBUH)"
                                        )
                                    )
                                }
                            )
                        }

                        Column {
                            Text(
                                text = "Urdu Meaning (مفہوم)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.urdu,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable {
                                    onEditCard(
                                        com.example.api.ShiaQuote(
                                            arabic = "",
                                            english = "",
                                            urdu = item.urdu,
                                            source = "Name of Prophet Muhammad (PBUH)"
                                        )
                                    )
                                }
                            )
                        }

                        Column {
                            Text(
                                text = when (languageCode) {
                                    "ur" -> "مفہوم اور سیرت"
                                    "ar" -> "المعنى والسياق"
                                    else -> "Meaning & Context"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.descriptionUrdu,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }

                        Column {
                            Text(
                                text = localizedReferenceText("source", languageCode),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (references.isEmpty()) {
                                PendingReferenceText(languageCode)
                            } else {
                                KnowledgeReferenceList(
                                    references = references,
                                    languageCode = languageCode,
                                    onOpenQuran = onOpenQuran
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                onEditCard(
                                    com.example.api.ShiaQuote(
                                        arabic = item.arabic,
                                        english = if (languageCode == "ur") "" else item.english,
                                        urdu = if (languageCode == "ur") item.urdu else "",
                                        source = references.firstOrNull()?.citation
                                            ?: "Traditional title of Prophet Muhammad (saws) — citation under review"
                                    )
                                )
                                selectedName = null
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ColorLens, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Edit in Cards Maker")
                        }

                        Button(
                            onClick = { selectedName = null },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (languageCode == "ur") "بند کریں" else "Close")
                        }
                    }
                }
            }
        }
    }
}
