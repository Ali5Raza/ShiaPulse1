package com.example.ui.admin

import android.content.Context
import android.util.Log
import com.example.ui.AamalAct
import com.example.ui.AamalAssetLoader
import com.example.ui.parseAamalActs
import com.example.ui.AamalItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader

object AamalSyncManager {

    private fun withBundledLocalizationFallback(
        context: Context,
        cached: List<AamalItem>
    ): List<AamalItem> {
        val bundledById = AamalAssetLoader.loadAamal(context).associateBy { it.id }
        return cached.map { item ->
            val bundled = bundledById[item.id]
            item.copy(
                acts = item.acts.ifEmpty { bundled?.acts.orEmpty() },
                categoryUr = item.categoryUr?.takeIf { it.isNotBlank() } ?: bundled?.categoryUr,
                categoryAr = item.categoryAr?.takeIf { it.isNotBlank() } ?: bundled?.categoryAr,
                relevanceUr = item.relevanceUr?.takeIf { it.isNotBlank() } ?: bundled?.relevanceUr,
                relevanceAr = item.relevanceAr?.takeIf { it.isNotBlank() } ?: bundled?.relevanceAr
            )
        }
    }

    fun loadAamal(context: Context): List<AamalItem> {
        val result = mutableListOf<AamalItem>()
        var jsonStr: String? = null

        // Try cache first
        val cacheFile = File(context.filesDir, "aamal_cache.json")
        if (cacheFile.exists()) {
            try {
                jsonStr = cacheFile.readText()
            } catch (e: Exception) {
                Log.e("AamalSync", "Error reading cache: ${e.message}")
            }
        }

        if (jsonStr != null) {
            try {
                // Determine if it's a JSON array (multiple aamal items in one file for admin uploads)
                val array = JSONArray(jsonStr)
                for (j in 0 until array.length()) {
                    val jsonObj = array.getJSONObject(j)
                    val actsList = parseAamalActs(jsonObj)

                    result.add(
                        AamalItem(
                            id = jsonObj.optString("id", ""),
                            title = jsonObj.optString("title", ""),
                            titleUr = if (jsonObj.has("titleUr")) jsonObj.optString("titleUr") else null,
                            titleAr = if (jsonObj.has("titleAr")) jsonObj.optString("titleAr") else null,
                            category = jsonObj.optString("category", ""),
                            categoryUr = if (jsonObj.has("categoryUr")) jsonObj.optString("categoryUr") else null,
                            categoryAr = if (jsonObj.has("categoryAr")) jsonObj.optString("categoryAr") else null,
                            description = jsonObj.optString("description", ""),
                            descriptionUr = if (jsonObj.has("descriptionUr")) jsonObj.optString("descriptionUr") else null,
                            descriptionAr = if (jsonObj.has("descriptionAr")) jsonObj.optString("descriptionAr") else null,
                            relevance = jsonObj.optString("relevance", ""),
                            acts = actsList,
                            relevanceUr = jsonObj.optString("relevanceUr").takeIf { it.isNotBlank() },
                            relevanceAr = jsonObj.optString("relevanceAr").takeIf { it.isNotBlank() }
                        )
                    )
                }
                if (result.isNotEmpty()) return withBundledLocalizationFallback(context, result)
            } catch (e: Exception) {
                Log.e("AamalSync", "Failed to parse cache: ${e.message}")
            }
        }

        // Fallback to assets
        result.clear()
        try {
            val assetManager = context.assets
            val aamalFiles = assetManager.list("Supplications/Aamal") ?: emptyArray()
            
            for (file in aamalFiles) {
                if (file.endsWith(".json")) {
                    val inputStream = assetManager.open("Supplications/Aamal/$file")
                    val fileJsonStr = InputStreamReader(inputStream).readText()
                    val jsonObj = JSONObject(fileJsonStr)
                    
                    val actsList = parseAamalActs(jsonObj)

                    result.add(
                        AamalItem(
                            id = jsonObj.optString("id", ""),
                            title = jsonObj.optString("title", ""),
                            titleUr = if (jsonObj.has("titleUr")) jsonObj.optString("titleUr") else null,
                            titleAr = if (jsonObj.has("titleAr")) jsonObj.optString("titleAr") else null,
                            category = jsonObj.optString("category", ""),
                            categoryUr = if (jsonObj.has("categoryUr")) jsonObj.optString("categoryUr") else null,
                            categoryAr = if (jsonObj.has("categoryAr")) jsonObj.optString("categoryAr") else null,
                            description = jsonObj.optString("description", ""),
                            descriptionUr = if (jsonObj.has("descriptionUr")) jsonObj.optString("descriptionUr") else null,
                            descriptionAr = if (jsonObj.has("descriptionAr")) jsonObj.optString("descriptionAr") else null,
                            relevance = jsonObj.optString("relevance", ""),
                            acts = actsList,
                            relevanceUr = jsonObj.optString("relevanceUr").takeIf { it.isNotBlank() },
                            relevanceAr = jsonObj.optString("relevanceAr").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun syncContentFromFirebase(context: Context, onSyncComplete: (List<AamalItem>) -> Unit) {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val localVersion = prefs.getLong("aamal_version_local", 0L)
        val database = FirebaseDatabase.getInstance("https://shiapulse-pk-default-rtdb.asia-southeast1.firebasedatabase.app")
        val versionRef = database.reference.child("aamal_version")
        
        versionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(versionSnapshot: DataSnapshot) {
                val remoteVersion = versionSnapshot.getValue(Long::class.java) ?: 0L
                if (remoteVersion > localVersion || (localVersion == 0L && remoteVersion == 0L)) {
                    database.reference.child("aamal_json_string").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val jsonStringValue = dataSnapshot.getValue(String::class.java)
                            if (!jsonStringValue.isNullOrEmpty()) {
                                try {
                                    // Make sure it's valid JSON array
                                    val array = JSONArray(jsonStringValue)
                                    if (array.length() > 0) {
                                        val cacheFile = File(context.filesDir, "aamal_cache.json")
                                        cacheFile.writeText(jsonStringValue)
                                        val newVersionToSave = if (remoteVersion > 0L) remoteVersion else 1L
                                        prefs.edit().putLong("aamal_version_local", newVersionToSave).apply()
                                        
                                        // Reload to trigger UI
                                        onSyncComplete(loadAamal(context))
                                    }
                                } catch (e: Exception) {
                                    Log.e("AamalSync", "Error parsing updated content: ${e.message}")
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) { }
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) { }
        })
    }
}
