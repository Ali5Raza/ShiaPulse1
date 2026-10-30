package com.example.api

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.BuildConfig

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object DailyContentClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    private var cachedQuotes: List<ShiaQuote>? = null
    private var cachedAyats: List<ShiaQuote>? = null
    private var cachedMasoomQuotes: Map<Int, List<ShiaQuote>>? = null

    suspend fun getDailyShiaQuote(
        context: android.content.Context,
        topic: String? = null,
        masoomIndex: Int = 0,
        dayStamp: Int? = null
    ): ShiaQuote = withContext(Dispatchers.IO) {
        if (cachedQuotes == null) {
            cachedQuotes = com.example.ui.admin.ContentSyncManager.loadContent(context, "quotes.json", "quotes_cached.json")
            com.example.ui.admin.ContentSyncManager.syncContentFromFirebase(context, com.example.ui.admin.ContentType.HADITH, "quotes_cached.json") {
                cachedQuotes = it
            }
        }
        val items = cachedQuotes?.takeIf { it.isNotEmpty() } ?: LocalQuotes.quotes
        val calendar = java.util.Calendar.getInstance()
        val selectionDayStamp = dayStamp
            ?: calendar.get(java.util.Calendar.YEAR) * 1000 + calendar.get(java.util.Calendar.DAY_OF_YEAR)

        var selectionItems = items
        var selectionMasoomIndex = masoomIndex
        if (masoomIndex in 1..14 && items.none { sourceMatchesMasoom(it.source, masoomIndex) }) {
            // quotes.json does not attribute direct sayings to every Infallible. Use
            // the canonical localized biography sayings rather than an unrelated quote.
            val wisdomItems = cachedMasoomQuotes?.get(masoomIndex).orEmpty().ifEmpty {
                if (cachedMasoomQuotes == null) {
                    cachedMasoomQuotes = loadMasoomQuotes(context)
                }
                cachedMasoomQuotes?.get(masoomIndex).orEmpty()
            }
            if (wisdomItems.isNotEmpty()) {
                selectionItems = wisdomItems
                selectionMasoomIndex = 0 // This pool is already restricted to the Masoom.
            }
        }
        return@withContext selectDailyContent(selectionItems, topic, selectionDayStamp, selectionMasoomIndex)
    }

    suspend fun getDailyQuranAyat(
        context: android.content.Context,
        topic: String? = null,
        dayStamp: Int? = null
    ): ShiaQuote = withContext(Dispatchers.IO) {
        if (cachedAyats == null) {
            cachedAyats = com.example.ui.admin.ContentSyncManager.loadContent(context, "ayats.json", "ayats_cached.json")
            com.example.ui.admin.ContentSyncManager.syncContentFromFirebase(context, com.example.ui.admin.ContentType.AYAT, "ayats_cached.json") {
                cachedAyats = it
            }
        }
        val items = cachedAyats?.takeIf { it.isNotEmpty() } ?: LocalAyats.ayats
        val calendar = java.util.Calendar.getInstance()
        val selectionDayStamp = dayStamp
            ?: calendar.get(java.util.Calendar.YEAR) * 1000 + calendar.get(java.util.Calendar.DAY_OF_YEAR)
        return@withContext selectDailyContent(items, topic, selectionDayStamp)
    }

    private val topicKeywords = mapOf(
        "ghadir" to listOf("ghadir", "wilayat", "mawla", "perfected", "announce", "loves us", "our love"),
        "ahlulbayt" to listOf("ahlul", "progeny", "household", "fatima", "hussain", "purify", "loves us", "our love", "our traditions"),
        "patience" to listOf("patience", "sabr", "hardship", "oppressed", "difficulty", "misery", "persecution", "adversity"),
        "forgiveness" to listOf("forgive", "forgives", "forgiveness", "mercy", "repent", "sin", "sins", "pardon", "istighfar"),
        "hope" to listOf("hope", "hopeful", "ease", "relief", "reappearance", "mahdi", "trust"),
        "remembrance" to listOf("remember", "remembrance", "grateful", "dhikr"),
        "prayer" to listOf("prayer", "prayers", "salah", "worship", "worshipped"),
        "character" to listOf("character", "anger", "moral", "morals", "manners", "kindness", "goodness", "patience", "humble"),
        "knowledge" to listOf("knowledge", "learn", "learning", "wisdom", "wise", "understanding", "intellect", "ignorance", "ignorant"),
        "kindness" to listOf("kind", "kindness", "parents", "families", "weak", "charity", "brother", "serve"),
        "trust" to listOf("trust", "rely", "relied", "reliance", "tawakkul", "allah is sufficient", "sufficient for us", "thinking well of allah", "satisfaction with the decree", "certainty"),
        "gratitude" to listOf("grateful", "gratitude", "thank", "thanked", "thankful", "shukr", "blessing", "blessings", "favor")
    )

    private fun loadMasoomQuotes(context: android.content.Context): Map<Int, List<ShiaQuote>> =
        com.example.data.MasoomeenAssetLoader.loadMasoomeenWisdom(context).associate { wisdom ->
            wisdom.masoomIndex to wisdom.sayingsEn.mapIndexedNotNull { index, english ->
                if (english.isBlank()) {
                    null
                } else {
                    ShiaQuote(
                        arabic = wisdom.sayingsAr.getOrNull(index).orEmpty(),
                        english = english,
                        source = wisdom.nameEn,
                        urdu = wisdom.sayingsUr.getOrNull(index).orEmpty()
                    )
                }
            }
        }

    /**
     * Selects verified content by occasion speaker first, then topic relevance.
     * If no quote is attributed to that Masoom, the existing topic pool is used.
     */
    internal fun selectDailyContent(
        items: List<ShiaQuote>,
        topic: String?,
        dayStamp: Int,
        masoomIndex: Int = 0
    ): ShiaQuote {
        val personItems = if (masoomIndex in 1..14) {
            items.filter { sourceMatchesMasoom(it.source, masoomIndex) }
        } else {
            emptyList()
        }
        val selectionPool = personItems.ifEmpty { items }
        val normalizedTopic = topic?.trim()?.lowercase()
        val keywords = topicKeywords[normalizedTopic.orEmpty()].orEmpty()
        if (keywords.isEmpty()) return selectionPool[dayStamp % selectionPool.size]

        val scoredItems = selectionPool.map { quote ->
            val content = quote.english.lowercase()
            val source = quote.source.lowercase()
            val score = keywords.sumOf { keyword ->
                (if (content.containsWholeKeyword(keyword)) 2 else 0) +
                    (if (source.containsWholeKeyword(keyword)) 1 else 0)
            }
            quote to score
        }
        val bestScore = scoredItems.maxOfOrNull { it.second } ?: 0
        if (bestScore == 0) return selectionPool[dayStamp % selectionPool.size]

        val relevantItems = scoredItems.filter { it.second == bestScore }.map { it.first }
        return relevantItems[dayStamp % relevantItems.size]
    }

    private fun sourceMatchesMasoom(source: String, masoomIndex: Int): Boolean {
        val attribution = source.substringBefore("-").lowercase().trim()
        return when (masoomIndex) {
            1 -> attribution.containsWholeKeyword("prophet muhammad")
            2 -> attribution == "imam ali" || attribution.startsWith("imam ali (") ||
                attribution.containsWholeKeyword("imam ali ibn abi talib")
            3 -> attribution.containsWholeKeyword("lady fatima") ||
                attribution.containsWholeKeyword("fatima zahra")
            4 -> (attribution.containsWholeKeyword("imam hasan") ||
                attribution.containsWholeKeyword("imam hassan")) && "askari" !in attribution
            5 -> attribution.containsWholeKeyword("imam hussain") ||
                attribution.containsWholeKeyword("imam hussein") ||
                attribution.containsWholeKeyword("imam husayn")
            6 -> attribution.containsWholeKeyword("imam sajjad") ||
                attribution.containsWholeKeyword("imam zayn") ||
                attribution.containsWholeKeyword("imam ali sajjad") ||
                attribution.containsWholeKeyword("imam ali ibn al-hussain") ||
                attribution.containsWholeKeyword("imam ali ibn al-husayn")
            7 -> attribution.containsWholeKeyword("imam baqir") ||
                attribution.containsWholeKeyword("imam muhammad baqir")
            8 -> attribution.containsWholeKeyword("imam sadiq") ||
                attribution.containsWholeKeyword("imam ja'far") ||
                attribution.containsWholeKeyword("imam jafar")
            9 -> attribution.containsWholeKeyword("imam kadhim") ||
                attribution.containsWholeKeyword("imam kazim") ||
                attribution.containsWholeKeyword("imam musa kadhim") ||
                attribution.containsWholeKeyword("imam musa kazim")
            10 -> attribution.containsWholeKeyword("imam ridha") ||
                attribution.containsWholeKeyword("imam reza") ||
                attribution.containsWholeKeyword("imam ali ridha") ||
                attribution.containsWholeKeyword("imam ali reza")
            11 -> attribution.containsWholeKeyword("imam jawad") ||
                attribution.containsWholeKeyword("imam taqi") ||
                attribution.containsWholeKeyword("imam muhammad jawad") ||
                attribution.containsWholeKeyword("imam muhammad taqi")
            12 -> attribution.containsWholeKeyword("imam hadi") ||
                attribution.containsWholeKeyword("imam naqi") ||
                attribution.containsWholeKeyword("imam ali naqi")
            13 -> attribution.containsWholeKeyword("imam askari") ||
                ("askari" in attribution &&
                    (attribution.containsWholeKeyword("imam hasan") ||
                        attribution.containsWholeKeyword("imam hassan")))
            14 -> attribution.containsWholeKeyword("imam mahdi") ||
                attribution.containsWholeKeyword("imam muhammad mahdi")
            else -> false
        }
    }

    private fun String.containsWholeKeyword(keyword: String): Boolean =
        Regex("\\b${Regex.escape(keyword)}\\b").containsMatchIn(this)

    private fun loadJsonList(context: android.content.Context, filename: String): List<ShiaQuote>? {
        return try {
            val jsonStr = context.assets.open(filename).bufferedReader().use { it.readText() }
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, ShiaQuoteJson::class.java)
            val adapter = moshi.adapter<List<ShiaQuoteJson>>(listType)
            val parsedList = adapter.fromJson(jsonStr)
            parsedList?.map { ShiaQuote(arabic = it.arabic, english = it.english, source = it.source, urdu = it.urdu) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



}
data class ShiaQuoteJson(
    @Json(name = "arabic") val arabic: String = "",
    @Json(name = "english") val english: String = "",
    @Json(name = "source") val source: String = "",
    @Json(name = "urdu") val urdu: String = ""
)
