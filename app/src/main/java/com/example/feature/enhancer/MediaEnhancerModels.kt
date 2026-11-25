package com.example.feature.enhancer

import android.graphics.Bitmap
import android.net.Uri

enum class EnhancerMediaType(val label: String) {
    PHOTO("Foto AI"),
    VIDEO("Video AI")
}

data class EnhancementParams(
    val sharpness: Float = 0.58f,    // 0f..1f
    val contrast: Float = 1.08f,     // 0.7f..1.6f
    val brightness: Float = 0.04f,   // -0.3f..0.3f
    val saturation: Float = 1.12f,   // 0f..1.8f
    val warmth: Float = 0.02f,       // -0.3f..0.3f
    val vignette: Float = 0.0f       // 0f..0.7f
)

enum class EnhancementPreset(
    val title: String,
    val badge: String,
    val params: EnhancementParams
) {
    ORIGINAL(
        title = "Asli",
        badge = "RAW",
        params = EnhancementParams(0f, 1.0f, 0f, 1.0f, 0f, 0f)
    ),
    HD_CRYSTAL(
        title = "HD Crystal",
        badge = "4K",
        params = EnhancementParams(0.62f, 1.09f, 0.04f, 1.12f, 0.02f, 0.0f)
    ),
    BEAUTY_GLOW(
        title = "Cantik Natural",
        badge = "GLOW",
        params = EnhancementParams(0.42f, 1.05f, 0.07f, 1.10f, 0.06f, 0.0f)
    ),
    CINEMATIC_GLOW(
        title = "Cinematic",
        badge = "CINE",
        params = EnhancementParams(0.48f, 1.16f, -0.01f, 0.98f, 0.10f, 0.22f)
    ),
    CYBER_NEON(
        title = "Cyber Glass",
        badge = "NEON",
        params = EnhancementParams(0.64f, 1.16f, 0.02f, 1.24f, -0.10f, 0.14f)
    ),
    HDR_VIVID(
        title = "HDR Vivid",
        badge = "HDR",
        params = EnhancementParams(0.72f, 1.15f, 0.05f, 1.26f, 0.03f, 0.0f)
    ),
    MONO_NOIR(
        title = "Mono Noir",
        badge = "B&W",
        params = EnhancementParams(0.58f, 1.20f, 0.02f, 0.0f, 0.0f, 0.24f)
    )
}

sealed interface AiEnhanceState {
    data object Idle : AiEnhanceState
    data object Analyzing : AiEnhanceState
    data object RestoringImage : AiEnhanceState
    data class Success(val summary: String) : AiEnhanceState
    data class Error(val message: String) : AiEnhanceState
}

data class EnhancerUiState(
    val mediaType: EnhancerMediaType = EnhancerMediaType.PHOTO,
    val selectedUri: Uri? = null,
    val beforeBitmap: Bitmap? = null,
    val afterBitmap: Bitmap? = null,
    val beforeResolutionLabel: String = "480x360 SD",
    val afterResolutionLabel: String = "1440x1080 HD",
    val selectedPreset: EnhancementPreset = EnhancementPreset.HD_CRYSTAL,
    val params: EnhancementParams = EnhancementPreset.HD_CRYSTAL.params,
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val saveMessage: String? = null,
    val aiState: AiEnhanceState = AiEnhanceState.Idle
)
