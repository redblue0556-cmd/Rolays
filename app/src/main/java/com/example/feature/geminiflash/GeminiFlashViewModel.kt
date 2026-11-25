package com.example.feature.geminiflash

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.GeminiResilience
import com.example.feature.enhancer.ImageEnhancementEngine
import com.google.firebase.ai.Chat
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

private const val TAG = "GeminiFlashViewModel"

enum class GeminiFlashEngineMode(
    val title: String,
    val modelId: String,
    val systemPrompt: String
) {
    FLASH_CORE(
        title = "3.8 Flash Core",
        modelId = "gemini-3.8-flash",
        systemPrompt = "Kamu adalah modul inti independen Gemini 3.8 Flash AI di ROLAYS. Berikan jawaban cepat, akurat, terstruktur, dan mendalam sesuai instruksi pengguna."
    ),
    CODE_ARCHITECT(
        title = "Koding & Sistem",
        modelId = "gemini-3.8-flash",
        systemPrompt = "Kamu adalah arsitek perangkat lunak senior di modul Gemini 3.8 Flash AI. Berikan solusi kode bersih, arsitektur sistem, debugging, dan penjelasan teknis presisi."
    ),
    DEEP_ANALYST(
        title = "Analisis Mendalam",
        modelId = "gemini-3.8-flash",
        systemPrompt = "Kamu adalah analis riset & logika mendalam pada mesin Gemini 3.8 Flash AI. Uraikan masalah kompleks langkah demi langkah dengan ringkas dan terverifikasi."
    )
}

data class FlashChatEntry(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val latencyMs: Long? = null,
    val hasImage: Boolean = false,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

data class GeminiFlashUiState(
    val selectedMode: GeminiFlashEngineMode = GeminiFlashEngineMode.FLASH_CORE,
    val customSystemInstruction: String = "",
    val entries: List<FlashChatEntry> = listOf(
        FlashChatEntry(
            isUser = false,
            text = "Modul Independen Gemini 3.8 Flash AI aktif. Ruang kerja ini terisolasi sepenuhnya untuk penalaran cepat, analisis multimodal, pemrograman, dan tugas AI berkinerja tinggi."
        )
    ),
    val promptInput: String = "",
    val attachedImage: Bitmap? = null,
    val isRunning: Boolean = false
)

/**
 * Strictly isolated ViewModel for the standalone "Gemini 3.8 Flash AI" module.
 * Maintains its own independent GenerativeModel and multi-turn Chat session,
 * completely separated from the Social Media Copilot and Media Enhancer.
 */
class GeminiFlashViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GeminiFlashUiState())
    val uiState: StateFlow<GeminiFlashUiState> = _uiState.asStateFlow()

    private var activeChatSession: Chat? = null
    private var activeConfigKey: String? = null
    private var activeModelId: String = GeminiResilience.PRIMARY_FLASH_MODEL

    private fun getOrCreateChatSession(modelNameOverride: String? = null): Chat {
        val state = _uiState.value
        val instruction = state.customSystemInstruction.ifBlank { state.selectedMode.systemPrompt }
        val targetModel = modelNameOverride ?: state.selectedMode.modelId
        val configKey = "${state.selectedMode.name}|$targetModel|$instruction"
        val existing = activeChatSession
        if (existing != null && activeConfigKey == configKey && modelNameOverride == null) {
            return existing
        }
        val history = state.entries
            .drop(1) // Skip static greeting banner
            .filter { !it.isStreaming && !it.isError && it.text.isNotBlank() }
            .dropLastWhile { it.isUser } // Exclude just-added pending user turn
            .map { entry ->
                content(role = if (entry.isUser) "user" else "model") {
                    text(entry.text)
                }
            }
        val model = GeminiResilience.createModel(
            modelName = targetModel,
            generationConfig = generationConfig {
                temperature = 0.7f
            },
            systemInstruction = content { text(instruction) }
        )
        return model.startChat(history = history).also {
            activeChatSession = it
            activeConfigKey = configKey
            activeModelId = targetModel
        }
    }

    fun selectMode(mode: GeminiFlashEngineMode) {
        _uiState.update { it.copy(selectedMode = mode) }
        activeChatSession = null
    }

    fun updateCustomSystemInstruction(instruction: String) {
        _uiState.update { it.copy(customSystemInstruction = instruction) }
        activeChatSession = null
    }

    fun updatePrompt(text: String) {
        _uiState.update { it.copy(promptInput = text) }
    }

    fun attachImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            val bmp = ImageEnhancementEngine.loadPhotoFromUri(context, uri)
                ?.let { ImageEnhancementEngine.compressForAiAnalysis(it) }
            _uiState.update { it.copy(attachedImage = bmp) }
        }
    }

    fun removeAttachedImage() {
        _uiState.update { it.copy(attachedImage = null) }
    }

    fun resetIndependentSession() {
        activeChatSession = null
        _uiState.update {
            it.copy(
                entries = listOf(
                    FlashChatEntry(
                        isUser = false,
                        text = "Sesi baru Gemini 3.8 Flash AI dimulai. Memori percakapan telah di-reset."
                    )
                ),
                attachedImage = null
            )
        }
    }

    fun sendToGeminiFlash(overridePrompt: String? = null) {
        val prompt = (overridePrompt ?: _uiState.value.promptInput).trim()
        val image = _uiState.value.attachedImage
        if (prompt.isBlank() && image == null) return
        if (_uiState.value.isRunning) return

        val displayPrompt = prompt.ifBlank { "Analisis gambar ini secara mendetail." }
        val userEntry = FlashChatEntry(
            isUser = true,
            text = displayPrompt,
            hasImage = image != null
        )
        val assistantId = UUID.randomUUID().toString()
        val pendingAssistant = FlashChatEntry(
            id = assistantId,
            isUser = false,
            text = "",
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                entries = it.entries + userEntry + pendingAssistant,
                promptInput = if (overridePrompt == null) "" else it.promptInput,
                attachedImage = null,
                isRunning = true
            )
        }

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val accumulated = StringBuilder()
            try {
                val payload = content {
                    if (image != null) image(image)
                    text(displayPrompt)
                }
                withTimeout(60_000L) {
                    val primaryChat = getOrCreateChatSession()
                    GeminiResilience.streamWithFlashFallback(
                        primaryStream = { primaryChat.sendMessageStream(payload) },
                        fallbackStream = {
                            getOrCreateChatSession(GeminiResilience.FALLBACK_FLASH_MODEL)
                                .sendMessageStream(payload)
                        },
                        hasEmittedText = { accumulated.isNotEmpty() },
                        onChunk = { chunkText ->
                            accumulated.append(chunkText)
                            updateEntry(
                                id = assistantId,
                                text = accumulated.toString(),
                                isStreaming = true,
                                latencyMs = null
                            )
                        }
                    )
                }
                val elapsed = System.currentTimeMillis() - startTime
                val finalOutput = accumulated.toString().ifBlank {
                    "Gemini 3.8 Flash tidak mengembalikan respons teks."
                }
                updateEntry(
                    id = assistantId,
                    text = finalOutput,
                    isStreaming = false,
                    latencyMs = elapsed
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini 3.8 Flash request timed out", e)
                updateEntry(
                    id = assistantId,
                    text = "Permintaan Gemini 3.8 Flash timeout setelah 60 detik.",
                    isStreaming = false,
                    latencyMs = null,
                    isError = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini 3.8 Flash inference failed", e)
                updateEntry(
                    id = assistantId,
                    text = GeminiResilience.formatUserErrorMessage(e, "Gagal menjalankan inferensi Gemini 3.8 Flash."),
                    isStreaming = false,
                    latencyMs = null,
                    isError = true
                )
            } finally {
                _uiState.update { it.copy(isRunning = false) }
            }
        }
    }

    private fun updateEntry(
        id: String,
        text: String,
        isStreaming: Boolean,
        latencyMs: Long?,
        isError: Boolean = false
    ) {
        _uiState.update { state ->
            state.copy(
                entries = state.entries.map { entry ->
                    if (entry.id == id) {
                        entry.copy(
                            text = text,
                            isStreaming = isStreaming,
                            latencyMs = latencyMs,
                            isError = isError
                        )
                    } else {
                        entry
                    }
                }
            )
        }
    }
}
