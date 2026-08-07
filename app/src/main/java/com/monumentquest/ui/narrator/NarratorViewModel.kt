package com.monumentquest.ui.narrator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monumentquest.data.model.GroqMessage
import com.monumentquest.data.model.GroqRequest
import com.monumentquest.data.remote.GroqApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

data class ChatMessage(
    val role: String,
    val message: String
)

@HiltViewModel
class NarratorViewModel @Inject constructor(
    private val groqApi: GroqApi,
    @Named("groq_api_key") private val apiKey: String
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var initializedMonument = ""

    fun initNarrator(monumentName: String) {
        if (initializedMonument == monumentName && _messages.value.isNotEmpty()) return
        initializedMonument = monumentName

        val initialGreeting = generateInitialGreeting(monumentName)
        _messages.value = listOf(ChatMessage("assistant", initialGreeting))
    }

    fun sendMessage(monumentName: String, userMessage: String) {
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(ChatMessage("user", userMessage))
        _messages.value = currentMessages

        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (apiKey.isNotBlank() && apiKey != "dummy_key") {
                    val systemInstruction = "You are an expert Cultural & Architectural Historian of $monumentName. " +
                            "Answer in character, grounding your answers in verified historical facts, sacred rituals, and architectural symbolism. " +
                            "Be concise, engaging, and atmospheric."

                    val groqMessages = mutableListOf<GroqMessage>()
                    groqMessages.add(GroqMessage("system", systemInstruction))

                    _messages.value.forEach {
                        groqMessages.add(GroqMessage(it.role, it.message))
                    }

                    val request = GroqRequest(
                        model = "llama-3.3-70b-versatile",
                        messages = groqMessages
                    )

                    val response = groqApi.getChatCompletion(apiKey, request)
                    val responseText = response.choices.firstOrNull()?.message?.content

                    if (!responseText.isNullOrBlank()) {
                        _messages.value = _messages.value + ChatMessage("assistant", responseText)
                    } else {
                        val fallback = generateLocalPersonaResponse(monumentName, userMessage)
                        _messages.value = _messages.value + ChatMessage("assistant", fallback)
                    }
                } else {
                    delay(800)
                    val fallback = generateLocalPersonaResponse(monumentName, userMessage)
                    _messages.value = _messages.value + ChatMessage("assistant", fallback)
                }
            } catch (e: Exception) {
                val fallback = generateLocalPersonaResponse(monumentName, userMessage)
                _messages.value = _messages.value + ChatMessage("assistant", fallback)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun generateInitialGreeting(monumentName: String): String {
        return when {
            monumentName.contains("Lingaraj", ignoreCase = true) -> {
                "Pranam, traveler! I am the Cultural Historian of Lingaraj Temple. Constructed in the 11th century by King Jajati Keshari of the Somavamsi dynasty, my 55-metre Deula spire represents the sacred Harihara synthesis of Shiva and Vishnu. Ask me about my ancient rituals, architecture, or myths!"
            }
            monumentName.contains("Mukteshvara", ignoreCase = true) -> {
                "Greetings! I am Mukteshvara, celebrated as the 'Gem of Kalinga Architecture' (950 AD). Famous for my intricate Torana arched gateway and sculptures of dancing figures. What would you like to explore about my sacred heritage?"
            }
            monumentName.contains("Dhauli", ignoreCase = true) -> {
                "Peace be upon you. Standing on Dhauli Hills above the Daya River, this is where Emperor Ashoka in 261 BC laid down his sword after the Kalinga War and embraced Dhamma. Ask me about Ashoka's rock edicts or the peace pagoda!"
            }
            else -> {
                "Greetings, explorer! I am the AI Cultural Historian for $monumentName. Ask me anything about its ancient history, architectural style, sacred rituals, or legendary stories!"
            }
        }
    }

    private fun generateLocalPersonaResponse(monumentName: String, query: String): String {
        val lower = query.lowercase()
        return when {
            lower.contains("who built") || lower.contains("builder") || lower.contains("king") || lower.contains("history") -> {
                "Historical records show that $monumentName was commissioned during the golden era of regional heritage by royal patrons. The architectural precision highlights the master stone craftsmen of the era."
            }
            lower.contains("architecture") || lower.contains("style") || lower.contains("stone") -> {
                "Architecturally, $monumentName features traditional Kalinga Deula design — dividing sacred spaces into Vimana (sanctum), Jagamohana (assembly hall), Natamandira (dance hall), and Bhogamandapa (offering hall)."
            }
            lower.contains("ritual") || lower.contains("festival") || lower.contains("worship") -> {
                "Sacred living traditions continue to this day, including annual chariot processions (Rath Yatra), morning dhupa rituals, and sacred lamp illuminations."
            }
            else -> {
                "That is a fascinating aspect of $monumentName! The site remains an active sanctuary of living heritage, combining ancient stone craftsmanship with centuries of unbroken cultural devotion."
            }
        }
    }
}
