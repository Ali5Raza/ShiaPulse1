package com.example.ui.academy

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Real device text-to-speech. It never displays a playing state when a voice is unavailable. */
class AcademySpeechController(context: Context) {
    private val mutableSpeakingId = MutableStateFlow<String?>(null)
    val speakingId: StateFlow<String?> = mutableSpeakingId.asStateFlow()

    private var engine: TextToSpeech? = null
    @Volatile private var ready = false
    @Volatile private var destroyed = false

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit
        override fun onDone(utteranceId: String?) {
            if (mutableSpeakingId.value == utteranceId) mutableSpeakingId.value = null
        }

        @Deprecated("Deprecated by Android")
        override fun onError(utteranceId: String?) {
            if (mutableSpeakingId.value == utteranceId) mutableSpeakingId.value = null
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            if (mutableSpeakingId.value == utteranceId) mutableSpeakingId.value = null
        }
    }

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            if (destroyed) {
                engine?.shutdown()
                engine = null
            } else {
                ready = status == TextToSpeech.SUCCESS
                if (ready) engine?.setOnUtteranceProgressListener(listener)
            }
        }
        engine?.setOnUtteranceProgressListener(listener)
    }

    fun speak(id: String, text: String, languageCode: String): Boolean {
        val currentEngine = engine
        if (!ready || currentEngine == null || text.isBlank()) return false
        val locale = when (languageCode) {
            "ur" -> Locale("ur")
            "ar" -> Locale("ar")
            else -> Locale.ENGLISH
        }
        if (currentEngine.isLanguageAvailable(locale) < TextToSpeech.LANG_AVAILABLE) return false
        currentEngine.language = locale
        currentEngine.setSpeechRate(0.85f)
        val parameters = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id)
        }
        val result = currentEngine.speak(text, TextToSpeech.QUEUE_FLUSH, parameters, id)
        return if (result == TextToSpeech.SUCCESS) {
            mutableSpeakingId.value = id
            true
        } else {
            mutableSpeakingId.value = null
            false
        }
    }

    fun stop() {
        engine?.stop()
        mutableSpeakingId.value = null
    }

    fun shutdown() {
        destroyed = true
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
        mutableSpeakingId.value = null
    }
}
