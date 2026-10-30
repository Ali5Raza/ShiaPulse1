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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class NameOfAllah(
    val id: Int,
    val arabic: String,
    val transliteration: String,
    val english: String,
    val urdu: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamesOfAllahScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    languageCode: String = "en",
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {},
    onOpenQuran: ((surahNumber: Int, ayahNumber: Int) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf<NameOfAllah?>(null) }
    var activeCounter by remember { mutableStateOf(0) }

    val names = remember {
        listOf(
            NameOfAllah(1, "الرَّحْمَنُ", "Ar-Rahman", "The Beneficent", "نہایت مہربان"),
            NameOfAllah(2, "الرَّحِيمُ", "Ar-Rahim", "The Merciful", "نہایت رحم کرنے والا"),
            NameOfAllah(3, "الْمَلِكُ", "Al-Malik", "The King / Sovereign", "حقیقی بادشاہ"),
            NameOfAllah(4, "الْقُدُّوسُ", "Al-Quddus", "The Most Sacred", "انتہائی پاکیزہ"),
            NameOfAllah(5, "السَّلَامُ", "As-Salam", "The Giver of Peace", "سراسر سلامتی والا"),
            NameOfAllah(6, "الْمُؤْمِنُ", "Al-Mu'min", "The Infuser of Faith", "امن و امان دینے والا"),
            NameOfAllah(7, "الْمُهَيْمِنُ", "Al-Muhaymin", "The Preserver / Guardian", "نگہبان و پاسبان"),
            NameOfAllah(8, "الْعَزِيزُ", "Al-Aziz", "The Mighty One", "سب پر غالب"),
            NameOfAllah(9, "الْجَبَّارُ", "Al-Jabbar", "The Omnipotent", "زبردست عمال رکھنے والا"),
            NameOfAllah(10, "الْمُتَكَبِّرُ", "Al-Mutakabbir", "The Supreme / Majestic", "بڑائی والا"),
            NameOfAllah(11, "الْخَالِقُ", "Al-Khaliq", "The Creator", "پیدا کرنے والا"),
            NameOfAllah(12, "الْبَارِئُ", "Al-Bari'", "The Maker / Evolver", "درست بنانے والا"),
            NameOfAllah(13, "الْمُصَوِّرُ", "Al-Musawwir", "The Fashioner", "خوبصورت بنانے والا"),
            NameOfAllah(14, "الْغَفَّارُ", "Al-Ghaffar", "The All-Forgiving", "بہت زیادہ بخشنے والا"),
            NameOfAllah(15, "الْقَهَّارُ", "Al-Qahhar", "The Subduer", "سب کو بس میں رکھنے والا"),
            NameOfAllah(16, "الْوَهَّابُ", "Al-Wahhab", "The Giver of All", "سب کچھ عطا کرنے والا"),
            NameOfAllah(17, "الرَّزَّاقُ", "Ar-Razzaq", "The Provider", "بہتر رزق دینے والا"),
            NameOfAllah(18, "الْفَتَّاحُ", "Al-Fattah", "The Opener", "مشکل کشا / کھولنے والا"),
            NameOfAllah(19, "الْعَلِيمُ", "Al-Alim", "The All-Knowing", "بہت علم والا"),
            NameOfAllah(20, "الْقَابِضُ", "Al-Qabid", "The Withholder / Constrictor", "تنگ کرنے والا"),
            NameOfAllah(21, "الْبَاسِطُ", "Al-Basit", "The Expander", "کشادہ کرنے والا"),
            NameOfAllah(22, "الْخَافِضُ", "Al-Khafid", "The Abaser", "پست کرنے والا"),
            NameOfAllah(23, "الرَّافِعُ", "Ar-Rafi", "The Exalter", "بلند کرنے والا"),
            NameOfAllah(24, "الْمُعِزُّ", "Al-Mu'izz", "The Bestower of Honor", "عزت دینے والا"),
            NameOfAllah(25, "الْمُذِلُّ", "Al-Mudhill", "The Dishonorer", "ذلت دینے والا"),
            NameOfAllah(26, "السَّمِيعُ", "As-Sami'", "The All-Hearing", "سب کچھ سننے والا"),
            NameOfAllah(27, "الْبَصِيرُ", "Al-Basir", "The All-Seeing", "سب کچھ دیکھنے والا"),
            NameOfAllah(28, "الْحَكَمُ", "Al-Hakam", "The Judge", "بہتر فیصلہ کرنے والا"),
            NameOfAllah(29, "الْعَدْلُ", "Al-Adl", "The Utterly Just", "سراسر عدل کرنے والا"),
            NameOfAllah(30, "اللَّطِيفُ", "Al-Latif", "The Subtle / Gentle", "نہایت مہربان و باریک بین"),
            NameOfAllah(31, "الْخَبِيرُ", "Al-Khabir", "The All-Aware", "ہر چیز سے واقف"),
            NameOfAllah(32, "الْحَلِيمُ", "Al-Halim", "The Forbearing", "بردبار اور نرم خو"),
            NameOfAllah(33, "الْعَظِيمُ", "Al-Azim", "The Magnificent", "بہت عظمت والا"),
            NameOfAllah(34, "الْغَفُورُ", "Al-Ghafur", "The Most Forgiving", "بہت بخشنے والا"),
            NameOfAllah(35, "الشَّكُورُ", "Ash-Shakur", "The Grateful", "قدردان"),
            NameOfAllah(36, "الْعَلِيُّ", "Al-Aliyy", "The Most High", "بہت بلند مرتبہ"),
            NameOfAllah(37, "الْكَبِيرُ", "Al-Kabir", "The Greatest", "سب سے بڑا کیبریا والا"),
            NameOfAllah(38, "الْحَفِيظُ", "Al-Hafiz", "The Preserver / Protector", "حفاظت کرنے والا"),
            NameOfAllah(39, "الْمُقِيتُ", "Al-Muqit", "The Nourisher", "توانائی فراہم کرنے والا"),
            NameOfAllah(40, "الْحَسِيبُ", "Al-Hasib", "The Accounter", "حساب لینے والا"),
            NameOfAllah(41, "الْجَلِيلُ", "Al-Jalil", "The Mighty / Glorious", "عظمت و جلال والا"),
            NameOfAllah(42, "الْكَرِيمُ", "Al-Karim", "The Most Generous", "بہت مہربان و سخی"),
            NameOfAllah(43, "الرَّقِيبُ", "Ar-Raqib", "The Watchful", "نگہبان"),
            NameOfAllah(44, "الْمُجِيبُ", "Al-Mujib", "The Responsive", "امیدیں پوری کرنے والا"),
            NameOfAllah(45, "الْوَاسِعُ", "Al-Wasi'", "The Vast / All-Encompassing", "کشادہ اور وسیع"),
            NameOfAllah(46, "الْحَكِيمُ", "Al-Hakim", "The Wise", "نہایت دانا اور حکمت والا"),
            NameOfAllah(47, "الْوَدُودُ", "Al-Wadud", "The Loving One", "محبت کرنے والا"),
            NameOfAllah(48, "الْمَجِيدُ", "Al-Majid", "The Majestic / Glorious", "بزرگی والا"),
            NameOfAllah(49, "الْبَاعِثُ", "Al-Ba'ith", "The Resurrector", "مردوں کو اٹھانے والا"),
            NameOfAllah(50, "الشَّهِيدُ", "Ash-Shahid", "The Witness", "گواہ اور حاضر"),
            NameOfAllah(51, "الْحَقُّ", "Al-Haqq", "The Truth", "سچا اور ثابت"),
            NameOfAllah(52, "الْوَكِيلُ", "Al-Wakil", "The Trustee", "کارساز اور مددگار"),
            NameOfAllah(53, "الْقَوِيُّ", "Al-Qawiyy", "The All-Strong", "بے پناہ طاقتور"),
            NameOfAllah(54, "الْمَتِينُ", "Al-Matin", "The Firm", "مستحکم قوت والا"),
            NameOfAllah(55, "الْوَلِيُّ", "Al-Waliyy", "The Protector / Supporter", "حمایتی اور سرپرست"),
            NameOfAllah(56, "الْحَمِيدُ", "Al-Hamid", "The Praiseworthy", "لائقِ تعریف"),
            NameOfAllah(57, "الْمُحْصِي", "Al-Muhsi", "The Appraiser / Counter", "شمار کرنے والا"),
            NameOfAllah(58, "الْمُبْدِئُ", "Al-Mubdi'", "The Originator", "پہلی بار پیدا کرنے والا"),
            NameOfAllah(59, "الْمُعِيدُ", "Al-Mu'id", "The Restorer", "دوبارہ پیدا کرنے والا"),
            NameOfAllah(60, "الْمُحْيِي", "Al-Muhyi", "The Giver of Life", "زندگی بخشنے والا"),
            NameOfAllah(61, "الْمُمِيتُ", "Al-Mumit", "The Bringer of Death", "موت دینے والا"),
            NameOfAllah(62, "الْحَيُّ", "Al-Hayy", "The Ever-Living", "ہمیشہ زندہ رہنے والا"),
            NameOfAllah(63, "الْقَيُّومُ", "Al-Qayyum", "The Self-Subsisting", "سب کا تھامنے والا"),
            NameOfAllah(64, "الْوَاجِدُ", "Al-Wajid", "The Finder", "حاصل کرنے والا"),
            NameOfAllah(65, "الْمَاجِدُ", "Al-Majid", "The Noble", "بزرگ و برتر"),
            NameOfAllah(66, "الْوَاحِدُ", "Al-Wahid", "The One", "اکیلا اور تنہا"),
            NameOfAllah(67, "الأَحَدُ", "Al-Ahad", "The Unique", "واحد و یکتا"),
            NameOfAllah(68, "الصَّمَدُ", "As-Samad", "The Eternal / Self-Sufficient", "بے نیاز اور صمد"),
            NameOfAllah(69, "الْقَادِرُ", "Al-Qadir", "The Capable", "قادر مطلق"),
            NameOfAllah(70, "الْمُقْتَدِرُ", "Al-Muqtadir", "The Omnipotent", "کامل اقتدار والا"),
            NameOfAllah(71, "الْمُقَدِّمُ", "Al-Muqaddim", "The Expediter", "آگے کرنے والا"),
            NameOfAllah(72, "الْمُؤَخِّرُ", "Al-Mu'akhkhir", "The Delayer", "پیچھے کرنے والا"),
            NameOfAllah(73, "الأَوَّلُ", "Al-Awwal", "The First", "سب سے پہلے رہنے والا"),
            NameOfAllah(74, "الأَخِرُ", "Al-Akhir", "The Last", "ہمیشہ باقی رہنے والا"),
            NameOfAllah(75, "الظَّاهِرُ", "Az-Zahir", "The Manifest", "سب پر عیاں"),
            NameOfAllah(76, "الْبَاطِنُ", "Al-Batin", "The Hidden", "پوشیدہ اور باطن"),
            NameOfAllah(77, "الْوَالِي", "Al-Wali", "The Patron / Governor", "مالک و مختار"),
            NameOfAllah(78, "الْمُتَعَالِي", "Al-Muta'ali", "The Supreme Exalted", "نہایت بلند مرتبہ"),
            NameOfAllah(79, "الْبَرُّ", "Al-Barr", "The Source of All Goodness", "سب کا محسن"),
            NameOfAllah(80, "التَّوَّابُ", "At-Tawwab", "The Acceptor of Repentance", "توبہ قبول کرنے والا"),
            NameOfAllah(81, "الْمُنْتَقِمُ", "Al-Muntaqim", "The Avenger", "بدلہ لینے والا"),
            NameOfAllah(82, "الْعَفُوُّ", "Al-Afuww", "The Pardoner", "بہت زیادہ معاف کرنے والا"),
            NameOfAllah(83, "الرَّؤُوفُ", "Ar-Ra'uf", "The Most Kind", "نہایت شفقت کرنے والا"),
            NameOfAllah(84, "مَالِكُ الْمُلْكِ", "Malik-ul-Mulk", "Owner of All Sovereignty", "مالک کائنات"),
            NameOfAllah(85, "ذُو الْجَلَالِ وَالإِكْرَامِ", "Dhul-Jalal-wal-Ikram", "Lord of Majesty and Bounty", "جلال اور بخشش والا"),
            NameOfAllah(86, "الْمُقْسِطُ", "Al-Muqsit", "The Equitable One", "انصاف پسند"),
            NameOfAllah(87, "الْجَامِعُ", "Al-Jami'", "The Gatherer", "سب کو اکٹھا کرنے والا"),
            NameOfAllah(88, "الْغَنِيُّ", "Al-Ghaniyy", "The Rich One", "سب سے غنی و بے نیاز"),
            NameOfAllah(89, "الْمُغْنِي", "Al-Mughni", "The Enricher", "غنی کرنے والا"),
            NameOfAllah(90, "الْمَانِعُ", "Al-Mani'", "The Preventer", "روکنے والا"),
            NameOfAllah(91, "الضَّارُّ", "Ad-Darr", "The Distresser", "نقصان کا مالک"),
            NameOfAllah(92, "النَّافِعُ", "An-Nafi'", "The Propitious", "نفع کا مالک"),
            NameOfAllah(93, "النُّورُ", "An-Nur", "The Light", "سراسر روشنی و نور"),
            NameOfAllah(94, "الْهَادِي", "Al-Hadi", "The Guide", "راہِ راست دکھانے والا"),
            NameOfAllah(95, "الْبَدِيعُ", "Al-Badi'", "The Incomparable", "عدیم المثال"),
            NameOfAllah(96, "الْبَاقِي", "Al-Baqi", "The Everlasting", "ہمیشہ باقی رہنے والا"),
            NameOfAllah(97, "الْوَارِثُ", "Al-Warith", "The Supreme Inheritor", "سب کا وارث"),
            NameOfAllah(98, "الرَّشِيدُ", "Ar-Rashid", "The Guide to Path", "سیدھا راستہ دکھانے والا"),
            NameOfAllah(99, "الصَّبُورُ", "As-Sabur", "The Extolled Patient", "نہایت صبر و تحمل والا")
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
                    "ur" -> "اللہ کے 99 نام"
                    "ar" -> "أسماء الله الحسنى التسعة والتسعون"
                    else -> "99 Names of Allah"
                },
                subtitle = when (languageCode) {
                    "ur" -> "قرآنی حوالوں اور ذکر کے ساتھ اسماء الحسنیٰ"
                    "ar" -> "الأسماء الحسنى مع المراجع القرآنية والذكر"
                    else -> "Asma-ul-Husna with Qur’an references & tasbeeh"
                },
                onBack = onBack,
                backModifier = Modifier.testTag("allah_names_back")
            )
        },
        modifier = modifier.testTag("names_of_allah_root")
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
                            text = if (languageCode == "ur") "ذکر تسبیح اسماء الحسنیٰ" else "Tasbeeh of Sacred Names",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (languageCode == "ur") "کاؤنٹر: $activeCounter" else "Recitation Count: $activeCounter",
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
                    "ur" -> "ہر نام کے ساتھ قرآنی یا متعلقہ صفت کا حوالہ دیا گیا ہے۔ ‘متعلقہ آیت’ کا مطلب ہے کہ فہرست کی صورت لفظاً آیت میں موجود نہیں۔ کسی مقررہ تعداد کا فضیلت کا دعویٰ صرف مستند روایت کے ساتھ ہی پیش کیا جائے گا۔"
                    "ar" -> "يظهر مع كل اسم مرجع قرآني أو صفة مرتبطة. تعني ‘آية مرتبطة’ أن صيغة القائمة نفسها ليست منصوصة في الآية. لا ندعي فضلاً لعدد محدد من الذكر بلا رواية موثقة."
                    else -> "Each name includes a Qur’an or related-attribute reference. ‘Related verse’ means the exact list form is not quoted. No fixed-count virtue is claimed without a verified narration."
                },
                languageCode = languageCode,
                reference = KnowledgeNameReferences.asmaHusnaHadith
            )

            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (languageCode == "ur") "تلاش کریں..." else "Search name or meaning...",
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
                            .testTag("allah_name_item_${item.id}")
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
        val references = KnowledgeNameReferences.forAllahName(item.id)
        Dialog(onDismissRequest = { selectedName = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("allah_name_detail_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "NAME OF ALLAH (SWT) #${item.id}",
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
                                        source = "Name of Allah (SWT)"
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
                                text = "English Meaning",
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
                                            source = "Name of Allah (SWT)"
                                        )
                                    )
                                }
                            )
                        }

                        Column {
                            Text(
                                text = "Urdu Translation (ترجمہ)",
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
                                            source = "Name of Allah (SWT)"
                                        )
                                    )
                                }
                            )
                        }

                        Column {
                            Text(
                                text = when (languageCode) {
                                    "ur" -> "قرآن و حدیث کا حوالہ"
                                    "ar" -> "المرجع من القرآن والحديث"
                                    else -> "Qur’an & Hadith Reference"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            KnowledgeReferenceList(
                                references = references,
                                languageCode = languageCode,
                                onOpenQuran = onOpenQuran
                            )
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
                                            ?: "Asma-ul-Husna #${item.id}"
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
