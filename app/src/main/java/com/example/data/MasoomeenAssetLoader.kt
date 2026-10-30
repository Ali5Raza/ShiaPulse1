package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader

data class ExpandedMasoomData(
    val id: Int,
    val nameAr: String,
    val name: String,
    val title: String,
    val born: String,
    val martyrdom: String,
    val burial: String,
    val biography: String,
    val familyTree: FamilyTree,
    val timeline: List<TimelineEvent>,
    val places: List<String>,
    val books: List<String>,
    val importantEvents: List<String>,
    val sayings: List<String> = emptyList(),
    /** References keyed by the zero-based saying index; missing indices remain under review. */
    val sayingReferences: Map<Int, List<KnowledgeReference>> = emptyMap()
)

/** Localized sayings from the same canonical masoomeen.json used by the biography screen. */
data class MasoomWisdom(
    val masoomIndex: Int,
    val nameEn: String,
    val nameUr: String,
    val nameAr: String,
    val sayingsEn: List<String>,
    val sayingsUr: List<String>,
    val sayingsAr: List<String>
)

data class FamilyTree(
    val father: String,
    val mother: String,
    val spouse: String,
    val children: List<String>
)

data class TimelineEvent(
    val year: String,
    val event: String
)

object MasoomeenAssetLoader {
    fun loadMasoomeenData(context: Context, langCode: String): List<ExpandedMasoomData> {
        val result = mutableListOf<ExpandedMasoomData>()
        try {
            val inputStream = context.assets.open("masoomeen.json")
            val jsonString = InputStreamReader(inputStream).readText()
            val jsonArray = JSONArray(jsonString)
            
            val targetLang = if (langCode == "ur" || langCode == "ar" || langCode == "en") langCode else "en"
            
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val id = item.getInt("id")
                val langObj = item.getJSONObject(targetLang)
                val arObj = item.getJSONObject("ar")
                
                val familyTreeObj = langObj.getJSONObject("familyTree")
                val childrenArray = familyTreeObj.getJSONArray("children")
                val childrenList = mutableListOf<String>()
                for (j in 0 until childrenArray.length()) {
                    childrenList.add(childrenArray.getString(j))
                }
                
                val familyTree = FamilyTree(
                    father = familyTreeObj.getString("father"),
                    mother = familyTreeObj.getString("mother"),
                    spouse = familyTreeObj.getString("spouse"),
                    children = childrenList
                )
                
                val timelineArray = langObj.getJSONArray("timeline")
                val timelineList = mutableListOf<TimelineEvent>()
                for (j in 0 until timelineArray.length()) {
                    val tlItem = timelineArray.getJSONObject(j)
                    timelineList.add(TimelineEvent(
                        year = tlItem.getString("year"),
                        event = tlItem.getString("event")
                    ))
                }
                
                val placesArray = langObj.getJSONArray("places")
                val placesList = mutableListOf<String>()
                for (j in 0 until placesArray.length()) {
                    placesList.add(placesArray.getString(j))
                }
                
                val booksArray = langObj.getJSONArray("books")
                val booksList = mutableListOf<String>()
                for (j in 0 until booksArray.length()) {
                    booksList.add(booksArray.getString(j))
                }
                
                val eventsArray = langObj.getJSONArray("importantEvents")
                val eventsList = mutableListOf<String>()
                for (j in 0 until eventsArray.length()) {
                    eventsList.add(eventsArray.getString(j))
                }
                
                val sayingsList = mutableListOf<String>()
                if (langObj.has("sayings")) {
                    val sayingsArray = langObj.getJSONArray("sayings")
                    for (j in 0 until sayingsArray.length()) {
                        sayingsList.add(sayingsArray.getString(j))
                    }
                }

                val sayingReferences = mutableMapOf<Int, MutableList<KnowledgeReference>>()
                val referenceArray = item.optJSONArray("sayingReferences")
                if (referenceArray != null) {
                    for (j in 0 until referenceArray.length()) {
                        val referenceObj = referenceArray.getJSONObject(j)
                        val sayingIndex = referenceObj.optInt("sayingIndex", -1)
                        if (sayingIndex !in sayingsList.indices) continue
                        val referenceType = runCatching {
                            KnowledgeReferenceType.valueOf(
                                referenceObj.optString("type", "general").uppercase()
                            )
                        }.getOrDefault(KnowledgeReferenceType.GENERAL)
                        sayingReferences.getOrPut(sayingIndex) { mutableListOf() }.add(
                            KnowledgeReference(
                                type = referenceType,
                                citation = referenceObj.optString("citation"),
                                url = referenceObj.optString("url"),
                                direct = referenceObj.optBoolean("direct", true),
                                noteEn = referenceObj.optString("noteEn"),
                                noteUr = referenceObj.optString("noteUr"),
                                noteAr = referenceObj.optString("noteAr")
                            )
                        )
                    }
                }
                
                result.add(
                    ExpandedMasoomData(
                        id = id,
                        nameAr = arObj.getString("name"),
                        name = langObj.getString("name"),
                        title = langObj.getString("title"),
                        born = langObj.getString("born"),
                        martyrdom = langObj.getString("martyrdom"),
                        burial = langObj.getString("burial"),
                        biography = langObj.getString("biography"),
                        familyTree = familyTree,
                        timeline = timelineList,
                        places = placesList,
                        books = booksList,
                        importantEvents = eventsList,
                        sayings = sayingsList,
                        sayingReferences = sayingReferences.mapValues { entry -> entry.value.toList() }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun loadMasoomeenWisdom(context: Context): List<MasoomWisdom> {
        val result = mutableListOf<MasoomWisdom>()
        try {
            val jsonString = context.assets.open("masoomeen.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val en = item.getJSONObject("en")
                val ur = item.getJSONObject("ur")
                val ar = item.getJSONObject("ar")
                result.add(
                    MasoomWisdom(
                        masoomIndex = item.getInt("id"),
                        nameEn = en.optString("name"),
                        nameUr = ur.optString("name"),
                        nameAr = ar.optString("name"),
                        sayingsEn = en.localizedSayings(),
                        sayingsUr = ur.localizedSayings(),
                        sayingsAr = ar.localizedSayings()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun JSONObject.localizedSayings(): List<String> {
        val array = optJSONArray("sayings") ?: return emptyList()
        return List(array.length()) { index -> array.optString(index) }
            .filter { it.isNotBlank() }
    }
}
