package com.example.feature.socialchat

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.GeminiResilience
import com.example.feature.enhancer.ImageEnhancementEngine
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

private const val TAG = "SocialAiViewModel"

data class SocialChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val platformTag: String? = null,
    val hasImageAttachment: Boolean = false,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

data class SocialAiUiState(
    val isPickerVisible: Boolean = true,
    val searchQuery: String = "",
    val showExtraPlatforms: Boolean = false,
    val selectedPlatform: SocialPlatformSpec = SocialPlatformCatalog.universalPlatform,
    val conversationsByPlatform: Map<String, List<SocialChatMessage>> = emptyMap(),
    val inputText: String = "",
    val attachedBitmap: Bitmap? = null,
    val isGenerating: Boolean = false
) {
    val activeMessages: List<SocialChatMessage>
        get() = conversationsByPlatform[selectedPlatform.id] ?: listOf(
            SocialChatMessage(
                isUser = false,
                text = "Halo! Asisten ${selectedPlatform.title} siap membantu (${selectedPlatform.subtitle}). Ketik perintah Anda atau pilih templat cepat di atas!"
            )
        )
}

class SocialAiViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SocialAiUiState())
    val uiState: StateFlow<SocialAiUiState> = _uiState.asStateFlow()

    private val socialSystemInstruction by lazy {
        content {
            text(
                "Kamu adalah Asisten AI Universal untuk Media Sosial di aplikasi ROLAYS. " +
                    "Bantu pengguna membuat konten, caption viral, balasan pesan DM/WhatsApp, skrip TikTok/Reels, " +
                    "thread X, kalender konten, dan moderasi komunitas di platform spesifik yang dipilih pengguna. " +
                    "Berikan hasil yang siap salin, kreatif, terstruktur rapi, dan relevan dalam Bahasa Indonesia " +
                    "(atau bahasa yang diminta pengguna)."
            )
        }
    }

    private val model by lazy {
        GeminiResilience.createModel(
            modelName = GeminiResilience.PRIMARY_FLASH_MODEL,
            systemInstruction = socialSystemInstruction
        )
    }

    private val fallbackModel by lazy {
        GeminiResilience.createModel(
            modelName = GeminiResilience.FALLBACK_FLASH_MODEL,
            systemInstruction = socialSystemInstruction
        )
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleExtraPlatforms() {
        _uiState.update { it.copy(showExtraPlatforms = !it.showExtraPlatforms) }
    }

    fun openPlatformChat(platform: SocialPlatformSpec) {
        _uiState.update {
            it.copy(
                selectedPlatform = platform,
                isPickerVisible = false
            )
        }
    }

    fun backToPlatformPicker() {
        _uiState.update { it.copy(isPickerVisible = true) }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun attachImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            val bmp = ImageEnhancementEngine.loadPhotoFromUri(context, uri)
                ?.let { ImageEnhancementEngine.compressForAiAnalysis(it) }
            _uiState.update { it.copy(attachedBitmap = bmp) }
        }
    }

    fun attachBitmapFromWorkflow(bitmap: Bitmap, suggestedPrompt: String) {
        val compressed = ImageEnhancementEngine.compressForAiAnalysis(bitmap)
        _uiState.update {
            it.copy(
                attachedBitmap = compressed,
                inputText = suggestedPrompt,
                isPickerVisible = true
            )
        }
    }

    fun prefillPromptFromWorkflow(prompt: String) {
        _uiState.update {
            it.copy(
                inputText = prompt,
                isPickerVisible = true
            )
        }
    }

    fun clearAttachedImage() {
        _uiState.update { it.copy(attachedBitmap = null) }
    }

    fun clearCurrentPlatformChat() {
        val platform = _uiState.value.selectedPlatform
        _uiState.update { state ->
            val updatedMap = state.conversationsByPlatform.toMutableMap()
            updatedMap[platform.id] = listOf(
                SocialChatMessage(
                    isUser = false,
                    text = "Sesi baru ${platform.title} siap! Apa yang ingin kita buat sekarang?"
                )
            )
            state.copy(
                conversationsByPlatform = updatedMap,
                attachedBitmap = null
            )
        }
    }

    fun sendPrompt(overridePrompt: String? = null) {
        val rawPrompt = (overridePrompt ?: _uiState.value.inputText).trim()
        val image = _uiState.value.attachedBitmap
        if (rawPrompt.isBlank() && image == null) return
        if (_uiState.value.isGenerating) return

        val platform = _uiState.value.selectedPlatform
        val userDisplay = if (rawPrompt.isNotBlank()) rawPrompt else "Analisis gambar ini untuk ${platform.title}."
        val userMessage = SocialChatMessage(
            isUser = true,
            text = userDisplay,
            platformTag = platform.title,
            hasImageAttachment = image != null
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderAssistant = SocialChatMessage(
            id = assistantMsgId,
            isUser = false,
            text = "",
            isStreaming = true
        )

        _uiState.update { state ->
            val currentList = state.activeMessages
            val updatedMap = state.conversationsByPlatform.toMutableMap()
            updatedMap[platform.id] = currentList + userMessage + placeholderAssistant
            state.copy(
                conversationsByPlatform = updatedMap,
                inputText = if (overridePrompt == null) "" else state.inputText,
                attachedBitmap = null,
                isGenerating = true
            )
        }

        viewModelScope.launch {
            val fullPrompt = "Platform Spesifik: ${platform.title} (${platform.roleHint})\nPermintaan Pengguna: $userDisplay"
            val builder = StringBuilder()
            try {
                withTimeout(60_000L) {
                    val inputContent = content {
                        if (image != null) image(image)
                        text(fullPrompt)
                    }
                    GeminiResilience.streamWithFlashFallback(
                        primaryStream = { model.generateContentStream(inputContent) },
                        fallbackStream = { fallbackModel.generateContentStream(inputContent) },
                        hasEmittedText = { builder.isNotEmpty() },
                        onChunk = { chunkText ->
                            builder.append(chunkText)
                            updateAssistantMessage(platform.id, assistantMsgId, builder.toString(), isStreaming = true)
                        }
                    )
                }
                val finalText = builder.toString().ifBlank {
                    "Maaf, AI tidak mengembalikan teks. Silakan ulangi perintah Anda."
                }
                updateAssistantMessage(platform.id, assistantMsgId, finalText, isStreaming = false)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Social AI stream timed out", e)
                updateAssistantMessage(
                    platform.id,
                    assistantMsgId,
                    "Koneksi AI timeout setelah 60 detik. Silakan coba lagi.",
                    isStreaming = false,
                    isError = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Social AI stream failed", e)
                updateAssistantMessage(
                    platform.id,
                    assistantMsgId,
                    GeminiResilience.formatUserErrorMessage(e, "Gagal menghubungi asisten AI."),
                    isStreaming = false,
                    isError = true
                )
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    private fun updateAssistantMessage(
        platformId: String,
        msgId: String,
        text: String,
        isStreaming: Boolean,
        isError: Boolean = false
    ) {
        _uiState.update { state ->
            val updatedMap = state.conversationsByPlatform.toMutableMap()
            val list = updatedMap[platformId].orEmpty().map { msg ->
                if (msg.id == msgId) {
                    msg.copy(text = text, isStreaming = isStreaming, isError = isError)
                } else {
                    msg
                }
            }
            updatedMap[platformId] = list
            state.copy(conversationsByPlatform = updatedMap)
        }
    }
}
