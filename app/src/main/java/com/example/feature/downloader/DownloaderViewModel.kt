package com.example.feature.downloader

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "DownloaderViewModel"

sealed interface DownloaderFetchState {
    data object Idle : DownloaderFetchState
    data object Loading : DownloaderFetchState
    data class Ready(val info: ResolvedMediaInfo) : DownloaderFetchState
    data class Error(val message: String) : DownloaderFetchState
}

data class DownloaderUiState(
    val urlInput: String = "",
    val fetchState: DownloaderFetchState = DownloaderFetchState.Idle,
    val selectedOptionId: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val downloadStatus: String? = null,
    val aiCaptionResult: String? = null,
    val isGeneratingAiNote: Boolean = false,
    val isHandoffLoading: Boolean = false,
    val history: List<DownloadedMediaItem> = emptyList()
)

class DownloaderViewModel : ViewModel() {

    private val repository = MediaDownloaderRepository()
    private val _uiState = MutableStateFlow(DownloaderUiState())
    val uiState: StateFlow<DownloaderUiState> = _uiState.asStateFlow()

    private val geminiModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    fun updateUrl(newUrl: String) {
        _uiState.update { it.copy(urlInput = newUrl, downloadStatus = null) }
    }

    fun pasteAndAnalyze(pastedText: String) {
        _uiState.update { it.copy(urlInput = pastedText, downloadStatus = null, aiCaptionResult = null) }
        analyzeLink()
    }

    fun analyzeLink() {
        val url = _uiState.value.urlInput.trim()
        if (url.isBlank()) {
            _uiState.update {
                it.copy(fetchState = DownloaderFetchState.Error("Tempel tautan TikTok atau YouTube terlebih dahulu."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    fetchState = DownloaderFetchState.Loading,
                    downloadStatus = null,
                    aiCaptionResult = null
                )
            }
            val result = repository.analyzeUrl(url)
            result.onSuccess { info ->
                _uiState.update {
                    it.copy(
                        fetchState = DownloaderFetchState.Ready(info),
                        selectedOptionId = info.options.firstOrNull()?.id
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        fetchState = DownloaderFetchState.Error(
                            err.localizedMessage ?: "Gagal memproses tautan media."
                        )
                    )
                }
            }
        }
    }

    fun selectOption(optionId: String) {
        _uiState.update { it.copy(selectedOptionId = optionId) }
    }

    fun startDownload(context: Context) {
        val ready = _uiState.value.fetchState as? DownloaderFetchState.Ready ?: return
        val option = ready.info.options.firstOrNull { it.id == _uiState.value.selectedOptionId }
            ?: ready.info.options.firstOrNull()
            ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isDownloading = true, downloadProgress = 0, downloadStatus = "Mengunduh media tanpa watermark...")
            }
            val result = repository.downloadOption(
                context = context,
                info = ready.info,
                option = option,
                onProgress = { pct ->
                    _uiState.update { it.copy(downloadProgress = pct) }
                }
            )
            result.onSuccess { item ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadProgress = 100,
                        downloadStatus = "Berhasil disimpan ke ${item.savedLocation}",
                        history = listOf(item) + it.history
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadStatus = "Gagal mengunduh: ${err.localizedMessage ?: "Terjadi kesalahan jaringan."}"
                    )
                }
            }
        }
    }

    fun generateViralSummaryWithGemini() {
        val ready = _uiState.value.fetchState as? DownloaderFetchState.Ready ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingAiNote = true, aiCaptionResult = null) }
            try {
                val response = withTimeout(45_000L) {
                    geminiModel.generateContent(
                        """
                        Analisis konten ${ready.info.platform.displayName} ini dan buat paket siap posting dalam Bahasa Indonesia:
                        - Judul: ${ready.info.title}
                        - Kreator: ${ready.info.author}
                        - Tautan: ${ready.info.originalUrl}
                        
                        Berikan:
                        1. Hook caption menarik (2 variasi)
                        2. 8 hashtag relevan yang sedang tren
                        3. Ide repost/komentar interaksi audiens
                        """.trimIndent()
                    )
                }
                _uiState.update {
                    it.copy(
                        isGeneratingAiNote = false,
                        aiCaptionResult = response.text ?: "Tidak ada output dari Gemini."
                    )
                }
            } catch (e: TimeoutCancellationException) {
                _uiState.update {
                    it.copy(isGeneratingAiNote = false, aiCaptionResult = "Waktu analisis Gemini habis.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini summary failed", e)
                _uiState.update {
                    it.copy(
                        isGeneratingAiNote = false,
                        aiCaptionResult = "Gagal membuat saran AI: ${e.localizedMessage ?: "Error"}"
                    )
                }
            }
        }
    }

    fun fetchThumbnailAndHandoff(
        url: String,
        title: String,
        onReady: (Bitmap, String) -> Unit
    ) {
        if (_uiState.value.isHandoffLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isHandoffLoading = true, downloadStatus = null) }
            val bitmap = repository.loadThumbnailBitmap(url)
            _uiState.update { it.copy(isHandoffLoading = false) }
            if (bitmap != null) {
                onReady(bitmap, title)
            } else {
                _uiState.update {
                    it.copy(downloadStatus = "Gagal memuat thumbnail untuk HD Enhancer.")
                }
            }
        }
    }
}
