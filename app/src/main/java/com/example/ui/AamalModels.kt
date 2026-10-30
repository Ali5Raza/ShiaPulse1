package com.example.ui

import org.json.JSONObject

/** Reads both the current acts schema and the existing bundled lines schema. */
fun parseAamalActs(json: JSONObject): List<AamalAct> {
    val acts = json.optJSONArray("acts")
    if (acts != null && acts.length() > 0) {
        return (0 until acts.length()).map { index ->
            val act = acts.getJSONObject(index)
            AamalAct(
                actNumber = act.optInt("actNumber", index + 1),
                title = act.optString("title", ""),
                titleUr = act.optString("titleUr").takeIf { it.isNotBlank() },
                titleAr = act.optString("titleAr").takeIf { it.isNotBlank() },
                description = act.optString("description", ""),
                descriptionUr = act.optString("descriptionUr").takeIf { it.isNotBlank() },
                descriptionAr = act.optString("descriptionAr").takeIf { it.isNotBlank() },
                arabicRecitation = act.optString("arabicRecitation").takeIf { it.isNotBlank() },
                translation = act.optString("translation").takeIf { it.isNotBlank() },
                urduTranslation = act.optString("urduTranslation").takeIf { it.isNotBlank() },
                transliteration = act.optString("transliteration").takeIf { it.isNotBlank() }
            )
        }
    }

    val lines = json.optJSONArray("lines") ?: return emptyList()
    return (0 until lines.length()).map { index ->
        val line = lines.getJSONObject(index)
        AamalAct(
            actNumber = line.optInt("actNumber", index + 1),
            title = line.optString("title").takeIf { it.isNotBlank() } ?: "Recitation ${index + 1}",
            titleUr = line.optString("titleUr").takeIf { it.isNotBlank() },
            titleAr = line.optString("titleAr").takeIf { it.isNotBlank() },
            description = line.optString("description", ""),
            descriptionUr = line.optString("descriptionUr").takeIf { it.isNotBlank() },
            descriptionAr = line.optString("descriptionAr").takeIf { it.isNotBlank() },
            arabicRecitation = line.optString("arabic").takeIf { it.isNotBlank() },
            translation = line.optString("translation").takeIf { it.isNotBlank() },
            urduTranslation = line.optString("urduTranslation").takeIf { it.isNotBlank() },
            transliteration = line.optString("transliteration").takeIf { it.isNotBlank() }
        )
    }
}

data class AamalAct(
    val actNumber: Int,
    val title: String,
    val titleUr: String? = null,
    val titleAr: String? = null,
    val description: String,
    val descriptionUr: String? = null,
    val descriptionAr: String? = null,
    val arabicRecitation: String? = null,
    val translation: String? = null,
    val urduTranslation: String? = null,
    val transliteration: String? = null
)

data class AamalItem(
    val id: String,
    val title: String,
    val titleUr: String? = null,
    val titleAr: String? = null,
    val category: String,
    val categoryUr: String? = null,
    val categoryAr: String? = null,
    val description: String,
    val descriptionUr: String? = null,
    val descriptionAr: String? = null,
    val relevance: String,
    val acts: List<AamalAct>,
    val relevanceUr: String? = null,
    val relevanceAr: String? = null
)
