package com.example.feature.socialchat

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.enhancer.ImageEnhancementEngine
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
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

enum class SocialPlatform(val title: String, val roleHint: String) {
    ALL("Semua", "multi-platform media sosial (Instagram, TikTok, WhatsApp, X, YouTube, LinkedIn)"),
    INSTAGRAM("Instagram", "Instagram Reels, Carousel, Story, Bio & DM"),
    TIKTOK("TikTok", "TikTok FYP hook, skrip video pendek, & balasan komentar"),
    WHATSAPP("WhatsApp", "WhatsApp chat personal, bisnis, broadcast & auto-reply sopan"),
    X_TWITTER("X / Twitter", "Thread X, opini ringkas viral, & reply cerdas")
}

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
    val selectedPlatform: SocialPlatform = SocialPlatform.ALL,
    val messages: List<SocialChatMessage> = listOf(
        SocialChatMessage(
            isUser = false,
            text = "Halo! Saya Asisten AI Media Sosial ROLAYS. Pilih platform (Instagram, TikTok, WhatsApp, X) atau langsung perintahkan saya membuat caption, membalas DM, merangkai thread, atau menyusun skrip konten!"
        )
    ),
    val inputText: String = "",
    val attachedBitmap: Bitmap? = null,
    val isGenerating: Boolean = false
)

class SocialAiViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SocialAiUiState())
    val uiState: StateFlow<SocialAiUiState> = _uiState.asStateFlow()

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "Kamu adalah Asisten AI Universal untuk Media Sosial di dalam aplikasi ROLAYS. " +
                        "Bantu pengguna membuat konten, caption viral, balasan pesan DM/WhatsApp, skrip TikTok/Reels, " +
                        "thread X, kalender konten, dan strategi engagement di semua platform media sosial. " +
                        "Berikan hasil yang siap salin, kreatif, terstruktur rapi, dan relevan dalam Bahasa Indonesia " +
                        "(atau bahasa yang diminta pengguna)."
                )
            }
        )
    }

    fun selectPlatform(platform: SocialPlatform) {
        _uiState.update { it.copy(selectedPlatform = platform) }
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

    fun clearAttachedImage() {
        _uiState.update { it.copy(attachedBitmap = null) }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                messages = listOf(
                    SocialChatMessage(
                        isUser = false,
                        text = "Sesi baru siap! Mau buat konten, balasan chat, atau strategi media sosial apa hari ini?"
                    )
                ),
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
        val userDisplay = if (rawPrompt.isNotBlank()) rawPrompt else "Analisis gambar ini untuk konten media sosial."
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

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + placeholderAssistant,
                inputText = if (overridePrompt == null) "" else it.inputText,
                attachedBitmap = null,
                isGenerating = true
            )
        }

        viewModelScope.launch {
            val fullPrompt = "Konteks Platform: ${platform.roleHint}\nPermintaan Pengguna: $userDisplay"
            val builder = StringBuilder()
            try {
                withTimeout(60_000L) {
                    val inputContent = content {
                        if (image != null) image(image)
                        text(fullPrompt)
                    }
                    model.generateContentStream(inputContent).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        builder.append(chunkText)
                        updateAssistantMessage(assistantMsgId, builder.toString(), isStreaming = true)
                    }
                }
                val finalText = builder.toString().ifBlank {
                    "Maaf, AI tidak mengembalikan teks. Silakan ulangi perintah Anda."
                }
                updateAssistantMessage(assistantMsgId, finalText, isStreaming = false)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Social AI stream timed out", e)
                updateAssistantMessage(
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
                    assistantMsgId,
                    e.localizedMessage ?: "Gagal menghubungi asisten AI.",
                    isStreaming = false,
                    isError = true
                )
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    private fun updateAssistantMessage(
        id: String,
        text: String,
        isStreaming: Boolean,
        isError: Boolean = false
    ) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == id) {
                        msg.copy(text = text, isStreaming = isStreaming, isError = isError)
                    } else {
                        msg
                    }
                }
            )
        }
    }
}
