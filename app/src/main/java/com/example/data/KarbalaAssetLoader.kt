package com.example.data

import android.content.Context
import org.json.JSONObject
import java.io.InputStreamReader

data class KarbalaFullData(
    val timeline: List<KarbalaTimelineItem>,
    val companions: List<KarbalaCompanion>,
    val shuhada: List<KarbalaShuhadaItem>,
    val familyTree: List<FamilyTreeMember>,
    val arbaeenRoute: List<ArbaeenRouteItem>,
    val ziyaratGuide: List<ZiyaratGuideItem>,
    val historicalPlaces: List<HistoricalPlaceItem>
)

data class KarbalaShuhadaItem(
    val nameEn: String,
    val nameUr: String,
    val nameAr: String
)

data class FamilyTreeMember(
    val nameEn: String,
    val relationEn: String
)

data class KarbalaTimelineItem(
    val id: String,
    val date: String,
    val eventEn: String,
    val eventUr: String,
    val eventAr: String,
    val descEn: String,
    val descUr: String,
    val descAr: String,
    val lat: Double,
    val lon: Double,
    val dateUr: String = "",
    val dateAr: String = "",
    val sourceLabel: String = "",
    val sourceUrl: String = ""
) {
    fun localizedDate(languageCode: String): String = when (languageCode) {
        "ur" -> dateUr.ifBlank { date }
        "ar" -> dateAr.ifBlank { date }
        else -> date
    }

    fun localizedTitle(languageCode: String): String = when (languageCode) {
        "ur" -> eventUr
        "ar" -> eventAr
        else -> eventEn
    }

    fun localizedDescription(languageCode: String): String = when (languageCode) {
        "ur" -> descUr
        "ar" -> descAr
        else -> descEn
    }
}

data class KarbalaCompanion(
    val nameEn: String,
    val nameUr: String,
    val nameAr: String,
    val roleEn: String,
    val descEn: String,
    val descUr: String,
    val roleUr: String = "",
    val roleAr: String = "",
    val descAr: String = "",
    val sourceUrl: String = ""
) {
    fun localizedName(languageCode: String): String = when (languageCode) {
        "ur" -> nameUr
        "ar" -> nameAr
        else -> nameEn
    }

    fun localizedRole(languageCode: String): String = when (languageCode) {
        "ur" -> roleUr.ifBlank { roleEn }
        "ar" -> roleAr.ifBlank { roleEn }
        else -> roleEn
    }

    fun localizedDescription(languageCode: String): String = when (languageCode) {
        "ur" -> descUr
        "ar" -> descAr.ifBlank { descEn }
        else -> descEn
    }
}

data class ArbaeenRouteItem(
    val nameEn: String,
    val nameUr: String,
    val descEn: String,
    val lat: Double,
    val lon: Double
)

data class ZiyaratGuideItem(
    val titleEn: String,
    val titleUr: String,
    val descEn: String
)

data class HistoricalPlaceItem(
    val nameEn: String,
    val nameUr: String,
    val descEn: String
)

object KarbalaAssetLoader {
    fun loadKarbalaData(context: Context): KarbalaFullData {
        try {
            val inputStream = context.assets.open("karbala.json")
            val jsonString = InputStreamReader(inputStream).readText()
            val jsonObj = JSONObject(jsonString)
            
            val tlArr = jsonObj.getJSONArray("timeline")
            val timeline = mutableListOf<KarbalaTimelineItem>()
            for (i in 0 until tlArr.length()) {
                val item = tlArr.getJSONObject(i)
                timeline.add(
                    KarbalaTimelineItem(
                        id = item.getString("id"),
                        date = item.getString("date"),
                        eventEn = item.getString("eventEn"),
                        eventUr = item.getString("eventUr"),
                        eventAr = item.getString("eventAr"),
                        descEn = item.getString("descEn"),
                        descUr = item.getString("descUr"),
                        descAr = item.getString("descAr"),
                        lat = item.getDouble("lat"),
                        lon = item.getDouble("lon"),
                        dateUr = item.optString("dateUr"),
                        dateAr = item.optString("dateAr"),
                        sourceLabel = item.optString("sourceLabel"),
                        sourceUrl = item.optString("sourceUrl")
                    )
                )
            }
            
            val compArr = jsonObj.getJSONArray("companions")
            val companions = mutableListOf<KarbalaCompanion>()
            for (i in 0 until compArr.length()) {
                val item = compArr.getJSONObject(i)
                companions.add(
                    KarbalaCompanion(
                        nameEn = item.getString("nameEn"),
                        nameUr = item.getString("nameUr"),
                        nameAr = item.getString("nameAr"),
                        roleEn = item.getString("roleEn"),
                        descEn = item.getString("descEn"),
                        descUr = item.getString("descUr"),
                        roleUr = item.optString("roleUr"),
                        roleAr = item.optString("roleAr"),
                        descAr = item.optString("descAr"),
                        sourceUrl = item.optString("sourceUrl")
                    )
                )
            }
            
            val shuhadaArr = jsonObj.optJSONArray("shuhada") ?: org.json.JSONArray()
            val shuhadaList = mutableListOf<KarbalaShuhadaItem>()
            for (i in 0 until shuhadaArr.length()) {
                val item = shuhadaArr.getJSONObject(i)
                shuhadaList.add(
                    KarbalaShuhadaItem(
                        nameEn = item.getString("nameEn"),
                        nameUr = item.getString("nameUr"),
                        nameAr = item.getString("nameAr")
                    )
                )
            }
            
            val famArr = jsonObj.optJSONArray("family_tree") ?: org.json.JSONArray()
            val familyTree = mutableListOf<FamilyTreeMember>()
            for (i in 0 until famArr.length()) {
                val item = famArr.getJSONObject(i)
                familyTree.add(
                    FamilyTreeMember(
                        nameEn = item.getString("nameEn"),
                        relationEn = item.getString("relationEn")
                    )
                )
            }
            
            val arbArr = jsonObj.getJSONArray("arbaeen_route")
            val arbaeen = mutableListOf<ArbaeenRouteItem>()
            for (i in 0 until arbArr.length()) {
                val item = arbArr.getJSONObject(i)
                arbaeen.add(
                    ArbaeenRouteItem(
                        nameEn = item.getString("nameEn"),
                        nameUr = item.getString("nameUr"),
                        descEn = item.getString("descEn"),
                        lat = item.getDouble("lat"),
                        lon = item.getDouble("lon")
                    )
                )
            }
            
            val ziyArr = jsonObj.getJSONArray("ziyarat_guide")
            val ziyarats = mutableListOf<ZiyaratGuideItem>()
            for (i in 0 until ziyArr.length()) {
                val item = ziyArr.getJSONObject(i)
                ziyarats.add(
                    ZiyaratGuideItem(
                        titleEn = item.getString("titleEn"),
                        titleUr = item.getString("titleUr"),
                        descEn = item.getString("descEn")
                    )
                )
            }
            
            val histArr = jsonObj.getJSONArray("historical_places")
            val places = mutableListOf<HistoricalPlaceItem>()
            for (i in 0 until histArr.length()) {
                val item = histArr.getJSONObject(i)
                places.add(
                    HistoricalPlaceItem(
                        nameEn = item.getString("nameEn"),
                        nameUr = item.getString("nameUr"),
                        descEn = item.getString("descEn")
                    )
                )
            }
            
            return KarbalaFullData(timeline, companions, shuhadaList, familyTree, arbaeen, ziyarats, places)
        } catch (e: Exception) {
            e.printStackTrace()
            return KarbalaFullData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        }
    }
}
