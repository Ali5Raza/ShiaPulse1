package com.example.ui.academy

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/** A small, asset-backed translation value. English is the safe fallback. */
data class LocalizedText(val values: Map<String, String>) {
    fun get(languageCode: String): String =
        values[languageCode]?.takeIf { it.isNotBlank() }
            ?: values["en"]
            ?: values.values.firstOrNull()
            .orEmpty()
}

fun tr(languageCode: String, english: String, urdu: String, arabic: String): String =
    when (languageCode) {
        "ur" -> urdu
        "ar" -> arabic
        else -> english
    }

data class AcademyStep(
    val id: Int,
    val title: LocalizedText,
    val description: LocalizedText,
    val arabic: String = "",
    val source: String = ""
)

data class KidsDua(
    val id: Int,
    val title: LocalizedText,
    val arabic: String,
    val transliteration: String,
    val translation: LocalizedText,
    val source: String
)

data class AcademyLesson(
    val id: Int,
    val title: LocalizedText,
    val description: LocalizedText,
    val pages: List<LocalizedText>,
    val question: LocalizedText,
    val options: List<LocalizedText>,
    val correctIndex: Int,
    val badge: LocalizedText,
    val source: String
)

data class QuizQuestion(
    val id: Int,
    val category: String,
    val question: LocalizedText,
    val options: List<LocalizedText>,
    val correctIndex: Int
)

data class TrueFalseQuestion(
    val id: Int,
    val statement: LocalizedText,
    val answer: Boolean
)

data class AcademyContent(
    val version: Int = 0,
    val wudu: List<AcademyStep> = emptyList(),
    val salah: List<AcademyStep> = emptyList(),
    val duas: List<KidsDua> = emptyList(),
    val aqaid: List<AcademyLesson> = emptyList(),
    val stories: List<AcademyLesson> = emptyList(),
    val quiz: List<QuizQuestion> = emptyList(),
    val trueFalse: List<TrueFalseQuestion> = emptyList(),
    val loadError: Boolean = false
) {
    val isUsable: Boolean
        get() = !loadError && wudu.isNotEmpty() && salah.isNotEmpty() &&
            duas.isNotEmpty() && aqaid.isNotEmpty() && stories.isNotEmpty() &&
            quiz.size >= 10 && trueFalse.size >= 8 &&
            quiz.any { it.category == "wudu" } && quiz.any { it.category == "salah" }
}

/** Loads the single bundled source of truth for every Academy learning module. */
object AcademyContentLoader {
    private const val TAG = "AcademyContent"
    const val ASSET_PATH = "academy/academy_content.json"

    fun load(context: Context): AcademyContent = runCatching {
        val json = context.assets.open(ASSET_PATH).bufferedReader(Charsets.UTF_8).use { it.readText() }
        parse(JSONObject(json))
    }.getOrElse {
        Log.e(TAG, "Unable to load bundled Academy content", it)
        AcademyContent(loadError = true)
    }

    private fun parse(root: JSONObject): AcademyContent {
        val wudu = root.getJSONArray("wudu").mapObject { it.toStep() }
        val salah = root.getJSONArray("salah").mapObject { it.toStep() }
        val duas = root.getJSONArray("duas").mapObject { item ->
            KidsDua(
                id = item.getInt("id"),
                title = item.localized("title"),
                arabic = item.getString("arabic"),
                transliteration = item.getString("transliteration"),
                translation = item.localized("translation"),
                source = item.optString("source")
            )
        }
        val aqaid = root.getJSONArray("aqaid").mapObject { it.toLesson() }
        val stories = root.getJSONArray("stories").mapObject { it.toLesson() }
        val quiz = root.getJSONArray("quiz").mapObject { item ->
            QuizQuestion(
                id = item.getInt("id"),
                category = item.getString("category"),
                question = item.localized("question"),
                options = item.getJSONArray("options").mapLocalized(),
                correctIndex = item.getInt("correctIndex")
            )
        }
        val trueFalse = root.getJSONArray("trueFalse").mapObject { item ->
            TrueFalseQuestion(
                id = item.getInt("id"),
                statement = item.localized("statement"),
                answer = item.getBoolean("answer")
            )
        }

        return AcademyContent(
            version = root.getInt("version"),
            wudu = wudu,
            salah = salah,
            duas = duas,
            aqaid = aqaid,
            stories = stories,
            quiz = quiz,
            trueFalse = trueFalse
        )
    }

    private fun JSONObject.toStep() = AcademyStep(
        id = getInt("id"),
        title = localized("title"),
        description = localized("description"),
        arabic = optString("arabic"),
        source = optString("source")
    )

    private fun JSONObject.toLesson() = AcademyLesson(
        id = getInt("id"),
        title = localized("title"),
        description = localized("description"),
        pages = getJSONArray("pages").mapLocalized(),
        question = localized("question"),
        options = getJSONArray("options").mapLocalized(),
        correctIndex = getInt("correctIndex"),
        badge = localized("badge"),
        source = optString("source")
    )

    private fun JSONObject.localized(key: String): LocalizedText {
        val value = getJSONObject(key)
        val translations = buildMap {
            listOf("en", "ur", "ar").forEach { language ->
                value.optString(language).takeIf { it.isNotBlank() }?.let { put(language, it) }
            }
        }
        require(translations.containsKey("en")) { "Missing English translation for $key" }
        return LocalizedText(translations)
    }

    private fun JSONArray.mapLocalized(): List<LocalizedText> = buildList {
        for (index in 0 until length()) {
            val value = getJSONObject(index)
            add(
                LocalizedText(
                    buildMap {
                        listOf("en", "ur", "ar").forEach { language ->
                            value.optString(language).takeIf { it.isNotBlank() }?.let { put(language, it) }
                        }
                    }
                )
            )
        }
    }

    private fun <T> JSONArray.mapObject(transform: (JSONObject) -> T): List<T> = buildList {
        for (index in 0 until length()) add(transform(getJSONObject(index)))
    }
}
