package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Private study data; it is intentionally separate from shared online Khatam. */
data class QuranCollection(
    val id: String,
    val name: String,
    val references: Set<String> = emptySet(),
    val isBuiltIn: Boolean = false
)

data class QuranStudyState(
    val isLoaded: Boolean = false,
    val notes: Map<String, String> = emptyMap(),
    val collections: List<QuranCollection> = emptyList()
)

object QuranStudyStore {
    const val FAVORITES = "favorites"
    const val STUDY_LATER = "study_later"
    const val MEMORIZATION = "memorization"
    const val DUA_AYAHS = "dua_ayahs"

    private const val PREFS_NAME = "quran_study"
    private const val NOTES_JSON = "notes_json"
    private const val COLLECTIONS_JSON = "collections_json"

    private val mutableState = MutableStateFlow(QuranStudyState())
    val state: StateFlow<QuranStudyState> = mutableState.asStateFlow()

    @Volatile
    private var preferences: SharedPreferences? = null

    @Synchronized
    fun initialize(context: Context) {
        if (preferences != null) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        preferences = prefs
        mutableState.value = readState(prefs).copy(isLoaded = true)
        if (mutableState.value.collections.isEmpty()) {
            val defaults = defaultCollections()
            prefs.edit().putString(COLLECTIONS_JSON, collectionsToJson(defaults)).apply()
            mutableState.value = mutableState.value.copy(collections = defaults)
        }
    }

    fun saveNote(context: Context, reference: String, text: String) {
        update(context) { state ->
            val cleanReference = reference.trim()
            val cleanText = text.trim()
            if (cleanReference.isBlank()) return@update state
            state.copy(
                notes = if (cleanText.isBlank()) state.notes - cleanReference
                else state.notes + (cleanReference to cleanText)
            )
        }
    }

    fun deleteNote(context: Context, reference: String) = saveNote(context, reference, "")

    fun createCollection(context: Context, name: String): String? {
        val cleanName = name.trim().replace(Regex("\\s+"), " ").take(60)
        if (cleanName.isBlank()) return null
        initialize(context)
        val id = "custom_" + UUID.randomUUID().toString().replace("-", "").take(16)
        update(context) { state ->
            state.copy(collections = state.collections + QuranCollection(id, cleanName))
        }
        return id
    }

    fun deleteCollection(context: Context, collectionId: String) {
        update(context) { state ->
            // Built-in collections stay available; only personal collections can be deleted.
            state.copy(collections = state.collections.filter { it.isBuiltIn || it.id != collectionId })
        }
    }

    fun toggleReference(context: Context, collectionId: String, reference: String) {
        update(context) { state ->
            state.copy(
                collections = state.collections.map { collection ->
                    if (collection.id != collectionId) collection
                    else collection.copy(
                        references = if (reference in collection.references) {
                            collection.references - reference
                        } else {
                            collection.references + reference
                        }
                    )
                }
            )
        }
    }

    fun removeReference(context: Context, collectionId: String, reference: String) {
        update(context) { state ->
            state.copy(
                collections = state.collections.map { collection ->
                    if (collection.id == collectionId) collection.copy(references = collection.references - reference)
                    else collection
                }
            )
        }
    }

    @Synchronized
    private fun update(context: Context, transform: (QuranStudyState) -> QuranStudyState) {
        initialize(context)
        val updated = transform(mutableState.value).copy(isLoaded = true)
        persist(requireNotNull(preferences), updated)
        mutableState.value = updated
    }

    private fun readState(prefs: SharedPreferences): QuranStudyState {
        val notes = mutableMapOf<String, String>()
        runCatching {
            val root = JSONObject(prefs.getString(NOTES_JSON, "{}").orEmpty())
            root.keys().forEach { key ->
                val value = root.optString(key).trim()
                if (key.isNotBlank() && value.isNotBlank()) notes[key] = value
            }
        }
        val collections = runCatching {
            collectionsFromJson(prefs.getString(COLLECTIONS_JSON, "[]").orEmpty())
        }.getOrDefault(emptyList())
        return QuranStudyState(notes = notes, collections = collections)
    }

    private fun persist(prefs: SharedPreferences, state: QuranStudyState) {
        val notes = JSONObject()
        state.notes.forEach { (reference, text) -> notes.put(reference, text) }
        prefs.edit()
            .putString(NOTES_JSON, notes.toString())
            .putString(COLLECTIONS_JSON, collectionsToJson(state.collections))
            .apply()
    }

    private fun defaultCollections() = listOf(
        QuranCollection(FAVORITES, "Favorite Ayahs", isBuiltIn = true),
        QuranCollection(STUDY_LATER, "Study Later", isBuiltIn = true),
        QuranCollection(MEMORIZATION, "Memorization", isBuiltIn = true),
        QuranCollection(DUA_AYAHS, "Dua Ayahs", isBuiltIn = true)
    )

    private fun collectionsToJson(collections: List<QuranCollection>): String {
        val array = JSONArray()
        collections.forEach { collection ->
            val references = JSONArray()
            collection.references.sorted().forEach { references.put(it) }
            array.put(
                JSONObject()
                    .put("id", collection.id)
                    .put("name", collection.name)
                    .put("builtIn", collection.isBuiltIn)
                    .put("references", references)
            )
        }
        return array.toString()
    }

    private fun collectionsFromJson(raw: String): List<QuranCollection> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val id = item.optString("id").trim()
                val name = item.optString("name").trim()
                if (id.isBlank() || name.isBlank()) continue
                val references = mutableSetOf<String>()
                val refs = item.optJSONArray("references") ?: JSONArray()
                for (refIndex in 0 until refs.length()) {
                    refs.optString(refIndex).trim().takeIf { it.isNotBlank() }?.let(references::add)
                }
                add(QuranCollection(id, name, references, item.optBoolean("builtIn")))
            }
        }
    }
}
