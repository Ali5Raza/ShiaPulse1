package com.example.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.network.Content
import com.example.network.GeminiClient
import com.example.network.GenerateContentRequest
import com.example.network.Part
import com.example.utils.LocalizationUtility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.util.Locale


data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false
)

class AIChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentLanguage: String = "en"
    private var lastMessageTime: Long = 0L

    init {
        _messages.value = listOf(
            ChatMessage(
                text = LocalizationUtility.get("ai_greeting", currentLanguage),
                isUser = false
            )
        )
    }

    fun setLanguage(languageCode: String) {
        val normalized = languageCode.normalizedLanguage()
        if (normalized == currentLanguage) return
        currentLanguage = normalized

        // Translate the untouched welcome message, but never rewrite a real conversation.
        val currentMessages = _messages.value
        if (currentMessages.size == 1 && !currentMessages[0].isUser && !currentMessages[0].isError) {
            _messages.value = listOf(
                ChatMessage(
                    text = LocalizationUtility.get("ai_greeting", normalized),
                    isUser = false
                )
            )
        }
    }

    /**
     * Returns true only when the message was accepted, allowing the UI to retain input after
     * an accidental double tap or during the cooldown.
     */
    fun sendMessage(text: String, languageCode: String = currentLanguage): Boolean {
        setLanguage(languageCode)
        val cleanText = text.trim()
        if (cleanText.isEmpty() || _isLoading.value) return false

        val currentTime = SystemClock.elapsedRealtime()
        if (lastMessageTime != 0L && currentTime - lastMessageTime < COOLDOWN_MS) {
            _messages.value = _messages.value + ChatMessage(
                text = LocalizationUtility.get("ai_cooldown", currentLanguage),
                isUser = false,
                isError = true
            )
            return false
        }
        lastMessageTime = currentTime

        _messages.value = _messages.value + ChatMessage(text = cleanText, isUser = true)

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("PLACEHOLDER")) {
                    _messages.value = _messages.value + ChatMessage(
                        text = LocalizationUtility.get("ai_unavailable", currentLanguage),
                        isUser = false,
                        isError = true
                    )
                    return@launch
                }

                val contents = _messages.value.mapNotNull { msg ->
                    if (msg.isError) return@mapNotNull null
                    Content(
                        role = if (msg.isUser) "user" else "model",
                        parts = listOf(Part(text = msg.text))
                    )
                }

                val systemInstruction = Content(
                    role = "user",
                    parts = listOf(Part(text = buildSystemInstruction(currentLanguage)))
                )

                val request = GenerateContentRequest(
                    contents = contents,
                    systemInstruction = systemInstruction
                )

                val response = GeminiClient.service.generateContent(apiKey, request)
                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?.takeIf { it.isNotBlank() }
                    ?: LocalizationUtility.get("ai_no_response", currentLanguage)

                _messages.value = _messages.value + ChatMessage(text = replyText, isUser = false)
            } catch (e: HttpException) {
                val errorMsg = if (e.code() == 429) {
                    LocalizationUtility.get("ai_rate_limit", currentLanguage)
                } else {
                    String.format(
                        Locale.getDefault(),
                        LocalizationUtility.get("ai_http_error", currentLanguage),
                        e.code()
                    )
                }
                _messages.value = _messages.value + ChatMessage(text = errorMsg, isUser = false, isError = true)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    text = LocalizationUtility.get("ai_network_error", currentLanguage),
                    isUser = false,
                    isError = true
                )
            } finally {
                _isLoading.value = false
            }
        }
        return true
    }

    private fun buildSystemInstruction(languageCode: String): String {
        val languageName = when (languageCode.normalizedLanguage()) {
            "ur" -> "Urdu"
            "ar" -> "Arabic"
            else -> "English"
        }
        return """
            You are ShiaPulse's careful Islamic study companion, with expertise in Shia Islam and the teachings of Ahlul Bayt (as).
            Respond entirely in $languageName unless the user explicitly requests another language or you are quoting original Arabic text.
            Be respectful, concise, humble, and useful for questions about Islamic teachings, history, jurisprudence, and daily practices.
            Never invent Qur'an verses, hadith, narrators, citations, dates, or religious rulings. State uncertainty clearly and distinguish between reported narrations when relevant.
            Do not issue a fatwa or present yourself as a marja. For binding religious rulings, direct the user to their qualified Shia marja or a trusted scholar.
        """.trimIndent()
    }

    private fun String.normalizedLanguage(): String = when (this) {
        "ur", "ar" -> this
        else -> "en"
    }

    private companion object {
        const val COOLDOWN_MS = 3000L
    }
}
