package com.example.feature.enhancer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.core.GeminiResilience
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "EnhancerViewModel"

class EnhancerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EnhancerUiState())
    val uiState: StateFlow<EnhancerUiState> = _uiState.asStateFlow()

    private var sourceFullBitmap: Bitmap? = null
    private var enhanceJob: Job? = null

    private val analysisSchema = Schema.obj(
        properties = mapOf(
            "sharpness" to Schema.double(description = "Recommended sharpening 0.2 to 0.95"),
            "contrast" to Schema.double(description = "Recommended contrast 0.9 to 1.45"),
            "brightness" to Schema.double(description = "Recommended brightness offset -0.2 to 0.2"),
            "saturation" to Schema.double(description = "Recommended saturation 0.7 to 1.45"),
            "warmth" to Schema.double(description = "Recommended color warmth -0.25 to 0.25"),
            "vignette" to Schema.double(description = "Recommended vignette 0.0 to 0.45"),
            "summary" to Schema.string(description = "Short Indonesian summary of enhancements applied")
        )
    )

    private val analyzerConfig by lazy {
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = analysisSchema
        }
    }

    private val analyzerModel by lazy {
        GeminiResilience.createModel(
            modelName = GeminiResilience.PRIMARY_FLASH_MODEL,
            generationConfig = analyzerConfig
        )
    }

    private val fallbackAnalyzerModel by lazy {
        GeminiResilience.createModel(
            modelName = GeminiResilience.FALLBACK_FLASH_MODEL,
            generationConfig = analyzerConfig
        )
    }

    private val restoreImageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    fun initializeSampleIfNeeded(context: Context) {
        if (sourceFullBitmap != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val (beforeSample, hdBase) = ImageEnhancementEngine.loadSamplePair(
                context,
                R.drawable.img_enhancer_sample
            )
            sourceFullBitmap = hdBase
            val enhanced = ImageEnhancementEngine.enhanceBitmap(
                hdBase,
                _uiState.value.params,
                upscaleIfSmall = false
            )
            _uiState.update {
                it.copy(
                    beforeBitmap = beforeSample,
                    afterBitmap = enhanced,
                    beforeResolutionLabel = "${beforeSample.width / 3}x${beforeSample.height / 3} SD",
                    afterResolutionLabel = "${enhanced.width}x${enhanced.height} HD",
                    isProcessing = false
                )
            }
        }
    }

    fun setMediaType(type: EnhancerMediaType) {
        _uiState.update { it.copy(mediaType = type, saveMessage = null) }
    }

    fun onMediaSelected(context: Context, uri: Uri, mediaType: EnhancerMediaType) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    mediaType = mediaType,
                    selectedUri = uri,
                    isProcessing = true,
                    saveMessage = null,
                    aiState = AiEnhanceState.Idle
                )
            }
            val loaded = if (mediaType == EnhancerMediaType.PHOTO) {
                ImageEnhancementEngine.loadPhotoFromUri(context, uri)
            } else {
                ImageEnhancementEngine.extractVideoFrame(context, uri)
            }

            if (loaded != null) {
                sourceFullBitmap = loaded
                val enhanced = ImageEnhancementEngine.enhanceBitmap(loaded, _uiState.value.params)
                _uiState.update {
                    it.copy(
                        beforeBitmap = loaded,
                        afterBitmap = enhanced,
                        beforeResolutionLabel = "${loaded.width}x${loaded.height}",
                        afterResolutionLabel = "${enhanced.width}x${enhanced.height} HD",
                        isProcessing = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        saveMessage = "Gagal memuat media yang dipilih."
                    )
                }
            }
        }
    }

    fun loadBitmapForEnhance(bitmap: Bitmap, label: String = "Media Impor") {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    mediaType = EnhancerMediaType.PHOTO,
                    selectedUri = null,
                    isProcessing = true,
                    saveMessage = "Memuat $label ke HD Enhancer...",
                    aiState = AiEnhanceState.Idle
                )
            }
            sourceFullBitmap = bitmap
            val enhanced = ImageEnhancementEngine.enhanceBitmap(bitmap, _uiState.value.params)
            _uiState.update {
                it.copy(
                    beforeBitmap = bitmap,
                    afterBitmap = enhanced,
                    beforeResolutionLabel = "${bitmap.width}x${bitmap.height}",
                    afterResolutionLabel = "${enhanced.width}x${enhanced.height} HD",
                    isProcessing = false,
                    saveMessage = null
                )
            }
        }
    }

    fun selectPreset(preset: EnhancementPreset) {
        _uiState.update { it.copy(selectedPreset = preset, params = preset.params) }
        reprocessDebounced(immediate = true)
    }

    fun updateParams(transform: (EnhancementParams) -> EnhancementParams) {
        _uiState.update { state ->
            state.copy(params = transform(state.params), saveMessage = null)
        }
        reprocessDebounced(immediate = false)
    }

    private fun reprocessDebounced(immediate: Boolean) {
        enhanceJob?.cancel()
        val base = sourceFullBitmap ?: return
        enhanceJob = viewModelScope.launch {
            if (!immediate) delay(90L)
            _uiState.update { it.copy(isProcessing = true) }
            val result = ImageEnhancementEngine.enhanceBitmap(
                source = base,
                params = _uiState.value.params,
                upscaleIfSmall = _uiState.value.selectedUri != null
            )
            _uiState.update {
                it.copy(
                    afterBitmap = result,
                    afterResolutionLabel = "${result.width}x${result.height} HD",
                    isProcessing = false
                )
            }
        }
    }

    fun runAiAutoEnhance() {
        val current = _uiState.value.beforeBitmap ?: sourceFullBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(aiState = AiEnhanceState.Analyzing) }
            try {
                val analysisBitmap = ImageEnhancementEngine.compressForAiAnalysis(current)
                val promptContent = content {
                    image(analysisBitmap)
                    text(
                        "Analisis pencahayaan, ketajaman, warna, dan kontras gambar ini. " +
                            "Berikan parameter peningkatan kualitas HD yang natural, jernih, dan estetik dalam format JSON. " +
                            "Tulis ringkasan singkat (maksimal 18 kata) dalam Bahasa Indonesia."
                    )
                }
                val (response, _) = withTimeout(45_000L) {
                    GeminiResilience.executeWithFlashFallback(
                        primaryAction = { analyzerModel.generateContent(promptContent) },
                        fallbackAction = { fallbackAnalyzerModel.generateContent(promptContent) }
                    )
                }
                val jsonText = response.text ?: throw IllegalStateException("Respons AI kosong.")
                val json = JSONObject(jsonText)
                val aiParams = EnhancementParams(
                    sharpness = json.optDouble("sharpness", 0.72).toFloat().coerceIn(0f, 1f),
                    contrast = json.optDouble("contrast", 1.18).toFloat().coerceIn(0.75f, 1.55f),
                    brightness = json.optDouble("brightness", 0.04).toFloat().coerceIn(-0.25f, 0.25f),
                    saturation = json.optDouble("saturation", 1.16).toFloat().coerceIn(0.3f, 1.6f),
                    warmth = json.optDouble("warmth", 0.03).toFloat().coerceIn(-0.28f, 0.28f),
                    vignette = json.optDouble("vignette", 0.14).toFloat().coerceIn(0f, 0.6f)
                )
                val summary = json.optString(
                    "summary",
                    "Ketajaman kristal, kontras dinamis, dan keseimbangan warna HD diterapkan."
                )
                _uiState.update {
                    it.copy(
                        params = aiParams,
                        selectedPreset = EnhancementPreset.HD_CRYSTAL,
                        aiState = AiEnhanceState.Success(summary)
                    )
                }
                reprocessDebounced(immediate = true)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI auto-enhance timed out", e)
                _uiState.update {
                    it.copy(aiState = AiEnhanceState.Error("Analisis AI timeout. Silakan coba lagi."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI auto-enhance failed", e)
                _uiState.update {
                    it.copy(
                        aiState = AiEnhanceState.Error(
                            GeminiResilience.formatUserErrorMessage(e, "Gagal menganalisis gambar dengan Gemini AI.")
                        )
                    )
                }
            }
        }
    }

    fun runAiGenerativeRestore() {
        val current = _uiState.value.beforeBitmap ?: sourceFullBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(aiState = AiEnhanceState.RestoringImage) }
            try {
                val input = ImageEnhancementEngine.compressForAiAnalysis(current)
                val response = withTimeout(60_000L) {
                    restoreImageModel.generateContent(
                        content {
                            image(input)
                            text(
                                "Enhance and restore this image into ultra-sharp high-definition clarity, " +
                                    "removing blur and compression artifacts while keeping exact subjects, faces, colors, and composition natural."
                            )
                        }
                    )
                }
                val restored = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()?.image
                if (restored != null) {
                    sourceFullBitmap = restored
                    _uiState.update {
                        it.copy(
                            afterBitmap = restored,
                            afterResolutionLabel = "${restored.width}x${restored.height} AI-HD",
                            aiState = AiEnhanceState.Success("Restorasi gambar Nano Banana AI berhasil diterapkan.")
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(aiState = AiEnhanceState.Error("Model tidak mengembalikan gambar restorasi."))
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI image restore timed out", e)
                _uiState.update {
                    it.copy(aiState = AiEnhanceState.Error("Restorasi AI timeout setelah 60 detik."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI image restore failed", e)
                _uiState.update {
                    it.copy(
                        aiState = AiEnhanceState.Error(
                            e.localizedMessage ?: "Restorasi AI gagal (pastikan billing Blaze aktif untuk model gambar)."
                        )
                    )
                }
            }
        }
    }

    fun saveEnhancedToGallery(context: Context) {
        val output = _uiState.value.afterBitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveMessage = null) }
            val result = ImageEnhancementEngine.saveBitmapToGallery(context, output)
            _uiState.update {
                it.copy(
                    isSaving = false,
                    saveMessage = result.getOrElse { err -> err.localizedMessage ?: "Gagal menyimpan" }
                )
            }
        }
    }

    fun clearSaveMessage() {
        _uiState.update { it.copy(saveMessage = null) }
    }
}
