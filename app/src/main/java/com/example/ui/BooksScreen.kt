package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit

// Data Classes for Shia Book System
data class BookChapter(
    val title: String,
    val arabicText: String? = null,
    val englishText: String,
    val summaryTopic: String
)

data class ShiaBook(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val category: String, // "General", "Theology", "Spirituality", "History"
    val chapters: List<BookChapter>
)

// List of high quality Shi'ite reference books
val shiaBooksList = listOf(
    ShiaBook(
        id = "nahj_al_balagha",
        title = "Nahj al-Balagha (Peak of Eloquence)",
        author = "Imam Ali ibn Abi Talib (as)",
        description = "A collection of sermons, letters, and sayings of the Commander of the Faithful, compile by Al-Sharif al-Radi.",
        category = "spirituality",
        chapters = listOf(
            BookChapter(
                title = "Sermon 1: The Creation & Angels",
                arabicText = "الْحَمْدُ للهِ الَّذِي لاَ يَبْلُغُ مِدْحَتَهُ الْقَائِلُونَ، وَلاَ يُحْصِي نَعْمَاءَهُ الْعَادُّونَ، وَلاَ يُؤَدِّي حَقَّهُ الُْمجْتَهِدُونَ... الَّذِي فطر الخَلائِقَ بِقُدْرَتِهِ، وَنَشَرَ الرِّياحَ بِرَحْمَتِهِ...",
                englishText = "Praise is due to Allah whose praise cannot be fully described by speakers, whose bounties cannot be numbered by enumerators, and whose claim cannot be satisfied by those who attempt. He who initiated creation by His power, dispersed the winds with His mercy, and keyed the earth with rocks.",
                summaryTopic = "Creation of universe, angels, Adam, and the greatness of Allah"
            ),
            BookChapter(
                title = "Letter 31: Wisdom for the Youth (To Imam Hasan)",
                arabicText = "مِنَ الْوَالِدِ الْفَانِ، الْمُقِرِّ لِلزَّمَانِ... أَيْ بُنَيَّ، إِنِّي لَمَّا رَأَيْتُكَ بَعْضِي، بَلْ رَأَيْتُكَ كُلِّي...",
                englishText = "My son, let your self be the measure of your dealings with others. Love for others what you love for yourself, and hate for them what you hate for yourself. Do not oppress, just as you do not wish to be oppressed. Be good to others just as you wish others to be good to you.",
                summaryTopic = "Ethical guidelines, moral traits, character building, self-discipline"
            ),
            BookChapter(
                title = "Saying 1: On Trials & Self-Control",
                arabicText = "كُنْ فِي الْفِتْنَةِ كَابْنِ اللَّبُونِ; لاَ ظَهْرٌ فَيُرْكَبَ، وَلاَ ضَرْعٌ فَيُحْلَبَ.",
                englishText = "During civil strife and trials, behave like an immature camel which has neither a back strong enough for riding, nor an udder full enough for milking (remain neutral and do not let others exploit you).",
                summaryTopic = "Patience during trials, self-preservation, neutral stance in conflict"
            ),
            BookChapter(
                title = "Saying 150: Guidelines for the Faithful",
                arabicText = "لاَ تَكُنْ مِمَّنْ يَرْجُو الاْخِرَةَ بِغَيْرِ الْعَمَلِ، وَيُؤَخِّرُ التَّوْبَةَ بِطُولِ الاَْمَلِ...",
                englishText = "Do not be like one who hopes for the hereafter without deeds, delays repentance due to distant hopes, talks like the pious in this world but acts like the worldly, and remains unsatisfied even with plenty.",
                summaryTopic = "Hypocrisy, sincerity, repentance, avoiding long worldly hopes"
            )
        )
    ),
    ShiaBook(
        id = "sahifa_sajjadiyya",
        title = "Al-Sahifa al-Sajjadiyya (The Psalms of Islam)",
        author = "Imam Ali ibn al-Husayn Zayn al-Abidin (as)",
        description = "The oldest prayer manual in Islamic sources and a seminal work of Shia Islamic spirituality.",
        category = "spirituality",
        chapters = listOf(
            BookChapter(
                title = "Supplication 1: In Praise of God",
                arabicText = "الْحَمْدُ للهِ الأوَّلِ بِلا أَوَّل كَانَ قَبْلَهُ، وَالآخِرِ بِلا آخِر يَكُونُ بَعْدَهُ... الَّذِي لا تَرَاهُ عُيُونُ النَّاظِرِينَ...",
                englishText = "Praise belongs to God, the First, before whom there was no first, and the Last, after whom there is no last. Whom the eyes of onlookers see not, and whom the descriptions of glorifiers reach not. He created the creations by His power and split them according to His wish.",
                summaryTopic = "Infinite nature of God, divine praise, purpose of creation"
            ),
            BookChapter(
                title = "Supplication 20: Makarim al-Akhlaq (Noble Moral Traits)",
                arabicText = "اللَّهُمَّ صَلِّ عَلَى مُحَمَّد وَآلِهِ، وَبَلِّغْ بِإيمَانِي أكْمَلَ الإيمَانِ، وَاجْعَلْ يَقِينِي أفْضَلَ الْيَقِينِ...",
                englishText = "O Allah, bless Muhammad and his Household, and cause my faith to reach the most perfect faith, make my certainty the most excellent certainty, and lift my intention to the best of intentions, and my deeds to the best of actions!",
                summaryTopic = "Moral perfection, perfect certainty, spiritual purity, righteous actions"
            ),
            BookChapter(
                title = "Supplication 15: In Sickness and Distress",
                arabicText = "اللَّهُمَّ لَكَ الْحَمْدُ عَلَى مَا لَمْ أزَلْ أَصِحُّ بِهِ مِنْ جَسَدِي، وَلَكَ الْحَمْدُ عَلَى مَا أحْدَثْتَ بِي مِنْ عِلَّةٍ...",
                englishText = "O Allah, to You belongs praise for the healthy state of body in which I have lived, and to You belongs praise for the illness You have brought upon me. For I know not which of the two states is more deserving of thanks!",
                summaryTopic = "Gratitude in illness, purification from sins, patience of Zayn al-Abidin as"
            )
        )
    ),
    ShiaBook(
        id = "lantern_path",
        title = "Lantern of the Path",
        author = "Imam Ja'far al-Sadiq (as)",
        description = "Deep mystical insights and practical guidance on spiritual purification, meditation, and manners of worship.",
        category = "spirituality",
        chapters = listOf(
            BookChapter(
                title = "Chapter 1: On Sincerity & Intention",
                arabicText = "أعْظَمُ النَّاسِ خُسْراناً مَنِ اشْتَغَلَ بِغَيْرِ اللهِ عَنِ اللهِ...",
                englishText = "The greatest loser is he who occupies himself with other than Allah. Sincerity lies in making your intention purely for Him, guarding the heart against vanity, and behaving in private exactly as you behave in public.",
                summaryTopic = "Sincerity in intentions, pure devotion, defeating spiritual showing-off (Riya)"
            ),
            BookChapter(
                title = "Chapter 2: On Contemplation & Reflection",
                arabicText = "فِكْرُ سَاعَة خَيْرٌ مِن عِبَادَةِ سَبْعينَ سَنَة، فالتَّفَكُّرُ يُبْصِرُكَ...",
                englishText = "Reflecting for an hour in silence is better than seventy years of rote worship. Contemplation unveils the wonders of the Creator's design, brings the heart back to life, and prompts true repentance.",
                summaryTopic = "Value of silent meditation, observing creation, intellectual growth"
            )
        )
    ),
    ShiaBook(
        id = "shiite_anthology",
        title = "A Shi'ite Anthology",
        author = "Allamah Muhammad Husayn Tabataba'i",
        description = "A collection of core Shia scholarly texts and traditions exploring Monotheism, Imamat, and the spiritual life of Shi'ism.",
        category = "theology",
        chapters = listOf(
            BookChapter(
                title = "Section 1: The Absolute Unity of Essence (Tawhid)",
                englishText = "The Shia view of Monotheism is that Allah's Essence is absolute and simple. His attributes are identical with His essence; He has no partners, no spatial limits, and cannot be captured by the human imagination or compared to creations.",
                summaryTopic = "Absolute monotheism (Tawhid), God's simple essence, identical attributes"
            ),
            BookChapter(
                title = "Section 2: The Need for Imamat",
                englishText = "Humanity is in continuous need of divine guidance. Since the Prophethood has ended, there must exist a divinely protected (Ma'sum) guide at all times to preserve the inner truths of the Quran, guide souls spiritually, and maintain justice.",
                summaryTopic = "Philosophy of Imamat, divine designation, necessity of an infallible guide"
            )
        )
    )
)

// Gemini Study Assistant Call
suspend fun queryGeminiScholar(
    bookTitle: String,
    chapterTitle: String,
    textSegment: String,
    userQuestion: String,
    languageCode: String = "en"
): String = withContext(Dispatchers.IO) {
    val apiKey = try {
        com.example.BuildConfig.GEMINI_API_KEY
    } catch (e: Exception) {
        ""
    }

    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER")) {
        return@withContext com.example.utils.LocalizationUtility.get("ai_unavailable", languageCode)
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val responseLanguage = when (languageCode) {
        "ur" -> "Urdu"
        "ar" -> "Arabic"
        else -> "English"
    }
    val prompt = """
        You are a careful academic Shia Islamic study companion, not a marja or an infallible authority.
        Respond entirely in $responseLanguage unless quoting the original Arabic text.
        Analyze and provide spiritual and historical explanations for this literature:
        Book: "$bookTitle"
        Chapter: "$chapterTitle"
        Text Segment: "$textSegment"
        
        The user has asked the following study question about this text:
        "$userQuestion"
        
        Provide a detailed explanation structured beautifully as follows:
        1. Context & Significance: Explain the setting and core message of this passage in Shi'ite theology/scholarly circles.
        2. Spiritual & Moral Lessons: Provide 2-3 specific bullet points summarizing the practical wisdom.
        3. Scholarly Analysis: Answer the user's specific question analytically based on authentic Shia traditions.
        
        Never invent Qur'an verses, hadith, citations, narrators, dates, or rulings. Clearly state uncertainty and do not issue a fatwa; direct binding rulings to a qualified Shia marja.
        Keep your tone humble, highly academic, respectful, and objective. Maximum 250 words. Do not use generic explanations; speak directly to the depth of classical Ahlul Bayt literature.
    """.trimIndent()

    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

    val requestJson = JSONObject().apply {
        put("contents", JSONArray().put(JSONObject().apply {
            put("parts", JSONArray().put(JSONObject().apply {
                put("text", prompt)
            }))
        }))
    }

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = requestJson.toString().toRequestBody(mediaType)

    val request = Request.Builder()
        .url(url)
        .post(body)
        .build()

    try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext String.format(
                    java.util.Locale.getDefault(),
                    com.example.utils.LocalizationUtility.get("ai_http_error", languageCode),
                    response.code
                )
            }
            val responseString = response.body?.string()
                ?: return@withContext com.example.utils.LocalizationUtility.get("ai_no_response", languageCode)
            val jsonObject = JSONObject(responseString)
            val resultText = jsonObject.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text").trim()
            return@withContext resultText
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        return@withContext com.example.utils.LocalizationUtility.get("ai_network_error", languageCode)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onEditCard: (com.example.api.ShiaQuote) -> Unit = {},
    currentLang: String = "en"
) {
    val baseAppFontFamily = androidx.compose.ui.text.font.FontFamily.Serif
    val coroutineScope = rememberCoroutineScope()
    var searchKeywords by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedBook by remember { mutableStateOf<ShiaBook?>(null) }
    var activeChapter by remember { mutableStateOf<BookChapter?>(null) }
    
    BackHandler(enabled = activeChapter != null || selectedBook != null) {
        if (activeChapter != null) {
            activeChapter = null
        } else if (selectedBook != null) {
            selectedBook = null
        }
    }
    
    // AI Chat/Question States
    var aiQuestion by remember { mutableStateOf("") }
    var aiAnswer by remember { mutableStateOf("") }
    var isCheckingWithAi by remember { mutableStateOf(false) }

    // Text adjustment states for the reader
    var textScaleFactor by remember { mutableFloatStateOf(16f) }

    val categories = listOf("All", "Spirituality", "Theology")

    val filteredBooks = remember(searchKeywords, selectedCategory) {
        shiaBooksList.filter { book ->
            val matchesCategory = selectedCategory == "All" || book.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = book.title.contains(searchKeywords, ignoreCase = true) || 
                                book.author.contains(searchKeywords, ignoreCase = true) ||
                                book.description.contains(searchKeywords, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val currentChapter = activeChapter
    val currentBook = selectedBook
    if (currentChapter != null && currentBook != null) {
        // Deep Reading view with custom settings and Gemini AI Scholarly Companion
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            currentChapter.title,
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            activeChapter = null 
                            aiQuestion = ""
                            aiAnswer = ""
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { textScaleFactor = (textScaleFactor - 2f).coerceAtLeast(12f) }) {
                            Icon(Icons.Default.Star, contentDescription = "Font smaller") // Visual hint smaller 
                            Text("A-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { textScaleFactor = (textScaleFactor + 2f).coerceAtMost(28f) }) {
                            Icon(Icons.Default.Add, contentDescription = "Font larger")
                            Text("A+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )
            },
            modifier = modifier
        ) { innerPadding ->
            val clipboardManager = LocalClipboardManager.current
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Book Info Header block Description
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "${currentBook.title} — ${currentBook.author}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reference Source attributed: al-islam.org",
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // Arabic verse/text if present
                currentChapter.arabicText?.let { arabic ->
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "ORIGINAL TEXT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row {
                                        IconButton(
                                            onClick = {
                                                onEditCard(
                                                    com.example.api.ShiaQuote(
                                                        arabic = arabic,
                                                        english = activeChapter?.englishText ?: "",
                                                        source = "${selectedBook?.title} - ${activeChapter?.title}"
                                                    )
                                                )
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ColorLens, "Edit in Cards Maker", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { clipboardManager.setText(AnnotatedString(arabic)) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Share, "Copy", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = arabic,
                                    fontSize = (textScaleFactor + 4f).sp,
                                    fontFamily = baseAppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = (textScaleFactor * 1.7f).sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // English Meaning block
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "ENGLISH SCRIPTURE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                IconButton(
                                    onClick = { clipboardManager.setText(AnnotatedString(currentChapter.englishText)) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Share, "Copy", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = currentChapter.englishText,
                                fontSize = textScaleFactor.sp,
                                lineHeight = (textScaleFactor * 1.55f).sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Interactive AI Scholar Companion Panel utilizing Gemini
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "✨ Gemini Shia AI Study Scholar",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Ask a technical, historical, or spiritual question regarding this scripture, and obtain real-time Islamic commentary powered by the Gemini AI Companion.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                com.example.utils.LocalizationUtility.get("ai_disclaimer", currentLang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Interactive quick question chip pills
                            val standardQuestions = listOf(
                                "Explicate spiritual lessons",
                                "What is historical context?",
                                "What other books confirm this?"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                standardQuestions.forEach { question ->
                                    ElevatedAssistChip(
                                        onClick = { aiQuestion = question },
                                        label = { Text(question, style = MaterialTheme.typography.bodySmall) },
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = aiQuestion,
                                onValueChange = { aiQuestion = it },
                                placeholder = { Text("e.g. What is the inner meaning of this text?") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_book_companion_input"),
                                shape = RoundedCornerShape(16.dp),
                                trailingIcon = {
                                    if (aiQuestion.isNotBlank() && !isCheckingWithAi) {
                                        IconButton(onClick = { aiQuestion = "" }) {
                                            Icon(Icons.Default.Clear, "Clear")
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isCheckingWithAi = true
                                        aiAnswer = "Asking Shia Scholar Companion..."
                                        try {
                                            val question = aiQuestion.ifBlank { "Explicate spiritual lessons & main theme" }
                                            val answer = queryGeminiScholar(
                                                bookTitle = currentBook.title,
                                                chapterTitle = currentChapter.title,
                                                textSegment = currentChapter.englishText,
                                                userQuestion = question,
                                                languageCode = currentLang
                                            )
                                            if (activeChapter == currentChapter) aiAnswer = answer
                                        } finally {
                                            isCheckingWithAi = false
                                        }
                                    }
                                },
                                enabled = !isCheckingWithAi,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_ai_book_companion"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (isCheckingWithAi) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyzing Literature with Gemini...", style = MaterialTheme.typography.labelLarge)
                                } else {
                                    Text("✨ Seek Commentary & Explanation", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (aiAnswer.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "AI Scholarly Commentary:",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = aiAnswer,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Source Attribution block for strict credit preservation
                item {
                    Text(
                        text = "Source credit & compilations provided graciously by al-islam.org for educational purposes. May Allah reward their service.",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    } else if (currentBook != null) {
        // Chapters Index view for the selected book
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentBook.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = { selectedBook = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )
            },
            modifier = modifier
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Book Info Header Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Author: ${currentBook.author}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                currentBook.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Text(
                        "TABLE OF CONTENTS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                items(currentBook.chapters) { chapter ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeChapter = chapter }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    chapter.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (chapter.arabicText != null) {
                                    Text(
                                        "Contains Arabic Scripture & Translation",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                } else {
                                    Text(
                                        "Literature translation text",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Read chapter",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Main Books Browser List
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Islamic Library", 
                            fontWeight = FontWeight.Light, 
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineSmall
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                    )
                )
            },
            modifier = modifier
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Interactive Search input
                OutlinedTextField(
                    value = searchKeywords,
                    onValueChange = { searchKeywords = it },
                    placeholder = { Text("Search by book title or authors...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp)
                )

                // Category selection chip row banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // Books Display
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredBooks) { book ->
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBook = book }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "By ${book.author}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    IconButton(
                                        onClick = { selectedBook = book },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowRight, "Open Book")
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = book.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Source: al-islam.org",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    )
                                    
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${book.chapters.size} Chapters",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredBooks.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Info, "No matches", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No books match your criteria.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }

                    // Strict Source Attributions footer card
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Compilations & Credit Attributions:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "• Supplication & Ziyarat text are credit attributed to duas.org.\n• Theological & historical literature are credit attributed to al-islam.org.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
