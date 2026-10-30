package com.example.ui.admin

import android.content.Context
import android.util.Log
import com.example.api.ShiaQuote
import com.example.api.ShiaQuoteJson
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

object ContentSyncManager {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    fun loadContent(context: Context, filename: String, cacheFilename: String): List<ShiaQuote>? {
        // Try cache first
        val cacheFile = File(context.filesDir, cacheFilename)
        if (cacheFile.exists()) {
            try {
                val jsonStr = cacheFile.readText()
                val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, ShiaQuoteJson::class.java)
                val adapter = moshi.adapter<List<ShiaQuoteJson>>(listType)
                val parsedList = adapter.fromJson(jsonStr)
                if (parsedList != null && parsedList.isNotEmpty()) {
                    return parsedList.map { ShiaQuote(arabic = it.arabic, english = it.english, source = it.source, urdu = it.urdu) }
                }
            } catch (e: Exception) {
                Log.e("ContentSyncManager", "Error reading cache: ${e.message}")
            }
        }
        // Fallback to assets
        return try {
            val jsonStr = context.assets.open(filename).bufferedReader().use { it.readText() }
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, ShiaQuoteJson::class.java)
            val adapter = moshi.adapter<List<ShiaQuoteJson>>(listType)
            val parsedList = adapter.fromJson(jsonStr)
            parsedList?.map { ShiaQuote(arabic = it.arabic, english = it.english, source = it.source, urdu = it.urdu) }
        } catch (e: Exception) {
            null
        }
    }

    fun syncContentFromFirebase(context: Context, contentType: ContentType, cacheFilename: String, onSyncComplete: (List<ShiaQuote>) -> Unit) {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val localVersion = prefs.getLong("${contentType.versionKey}_local", 0L)

        val database = FirebaseDatabase.getInstance("https://shiapulse-pk-default-rtdb.asia-southeast1.firebasedatabase.app")
        val versionRef = database.reference.child(contentType.versionKey)

        versionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(versionSnapshot: DataSnapshot) {
                val remoteVersion = versionSnapshot.getValue(Long::class.java) ?: 0L
                if (remoteVersion > localVersion || (localVersion == 0L && remoteVersion == 0L)) {
                    database.reference.child(contentType.dataKey).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val jsonStringValue = dataSnapshot.getValue(String::class.java)
                            if (!jsonStringValue.isNullOrEmpty()) {
                                try {
                                    val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, ShiaQuoteJson::class.java)
                                    val adapter = moshi.adapter<List<ShiaQuoteJson>>(listType)
                                    val parsedList = adapter.fromJson(jsonStringValue)
                                    
                                    if (parsedList != null && parsedList.isNotEmpty()) {
                                        // Save to cache
                                        val cacheFile = File(context.filesDir, cacheFilename)
                                        cacheFile.writeText(jsonStringValue)
                                        
                                        // Update version
                                        val newVersionToSave = if (remoteVersion > 0L) remoteVersion else 1L
                                        prefs.edit().putLong("${contentType.versionKey}_local", newVersionToSave).apply()
                                        
                                        val convertedList = parsedList.map { ShiaQuote(arabic = it.arabic, english = it.english, source = it.source, urdu = it.urdu) }
                                        onSyncComplete(convertedList)
                                    }
                                } catch (e: Exception) {
                                    Log.e("ContentSyncManager", "Error parsing updated content: ${e.message}")
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
