package com.example.ui

import org.json.JSONObject

fun parseSingleDuaJson(jsonString: String): com.example.data.ShiaDua? {
    try {
        val duaObj = JSONObject(jsonString)
        val linesArray = duaObj.getJSONArray("lines")
        val linesList = mutableListOf<com.example.data.DuaLine>()
        for (j in 0 until linesArray.length()) {
            val lineObj = linesArray.getJSONObject(j)
            linesList.add(
                com.example.data.DuaLine(
                    arabic = lineObj.optString("arabic", ""),
                    translation = lineObj.optString("translation", ""),
                    transliteration = lineObj.optString("transliteration", ""),
                    urduTranslation = lineObj.optString("urduTranslation").takeIf { it.isNotEmpty() },
                    farsiTranslation = lineObj.optString("farsiTranslation").takeIf { it.isNotEmpty() }
                )
            )
        }
        return com.example.data.ShiaDua(
            id = duaObj.getString("id"),
            title = duaObj.getString("title"),
            category = duaObj.optString("category", "General Supplications"),
            description = duaObj.optString("description", "A beautiful Islamic supplication."),
            relevance = duaObj.optString("relevance", "Anytime"),
            audioUrl = duaObj.optString("audioUrl", ""),
            lines = linesList
        )
    } catch (e: Exception) {
        return null
    }
}
