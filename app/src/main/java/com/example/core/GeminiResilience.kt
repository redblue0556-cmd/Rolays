package com.example.core

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * Provides resilient Gemini model execution for ROLAYS.
 * Uses `gemini-3.8-flash` as the primary model and automatically retries / falls back
 * to `gemini-3.5-flash-lite` when Google's primary model experiences temporary 503/high-demand spikes.
 */
object GeminiResilience {
    private const val TAG = "GeminiResilience"

    const val PRIMARY_FLASH_MODEL = "gemini-3.8-flash"
    const val FALLBACK_FLASH_MODEL = "gemini-3.5-flash-lite"
    const val PRIMARY_IMAGE_MODEL = "gemini-nano-banana-2.1"
    const val FALLBACK_IMAGE_MODEL = "gemini-3.1-flash-lite-image"

    fun createModel(
        modelName: String,
        generationConfig: GenerationConfig? = null,
        systemInstruction: Content? = null
    ): GenerativeModel {
        return Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = generationConfig,
            systemInstruction = systemInstruction
        )
    }

    fun isTransientOverload(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            val msg = current.message?.lowercase().orEmpty()
            if ("high demand" in msg ||
                "overloaded" in msg ||
                "temporarily" in msg ||
                "try again later" in msg ||
                "unavailable" in msg ||
                "503" in msg ||
                "429" in msg ||
                "resource exhausted" in msg ||
                "quota" in msg
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun containsAnyIgnoreCase(error: Throwable, vararg needles: String): Boolean {
        var current: Throwable? = error
        while (current != null) {
            val msg = current.message?.lowercase().orEmpty()
            if (needles.any { it in msg }) {
                return true
            }
            current = current.cause
        }
        return false
    }

    suspend fun <T> executeWithFlashFallback(
        primaryAction: suspend () -> T,
        fallbackAction: suspend () -> T
    ): Pair<T, Boolean> {
        return try {
            primaryAction() to false
        } catch (e: CancellationException) {
            throw e
        } catch (primaryErr: Exception) {
            if (!isTransientOverload(primaryErr)) {
                throw primaryErr
            }
            Log.w(TAG, "Primary model busy, retrying briefly...", primaryErr)
            delay(1100L)
            try {
                primaryAction() to false
            } catch (e: CancellationException) {
                throw e
            } catch (retryErr: Exception) {
                if (!isTransientOverload(retryErr)) {
                    throw retryErr
                }
                Log.w(TAG, "Primary model still busy, switching to fallback model", retryErr)
                try {
                    fallbackAction() to true
                } catch (e: CancellationException) {
                    throw e
                } catch (fallbackErr: Exception) {
                    if (!isTransientOverload(fallbackErr)) {
                        throw fallbackErr
                    }
                    Log.w(TAG, "Fallback model also busy, retrying fallback once...", fallbackErr)
                    delay(1400L)
                    fallbackAction() to true
                }
            }
        }
    }

    suspend fun streamWithFlashFallback(
        primaryStream: () -> Flow<GenerateContentResponse>,
        fallbackStream: () -> Flow<GenerateContentResponse>,
        hasEmittedText: () -> Boolean,
        onChunk: (String) -> Unit
    ): Boolean {
        try {
            primaryStream().collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) onChunk(text)
            }
            return false
        } catch (e: CancellationException) {
            throw e
        } catch (primaryErr: Exception) {
            if (hasEmittedText() || !isTransientOverload(primaryErr)) {
                throw primaryErr
            }
            Log.w(TAG, "Primary stream busy, retrying briefly...", primaryErr)
            delay(1100L)
            try {
                primaryStream().collect { chunk ->
                    val text = chunk.text.orEmpty()
                    if (text.isNotEmpty()) onChunk(text)
                }
                return false
            } catch (e: CancellationException) {
                throw e
            } catch (retryErr: Exception) {
                if (hasEmittedText() || !isTransientOverload(retryErr)) {
                    throw retryErr
                }
                Log.w(TAG, "Primary stream still busy, switching to $FALLBACK_FLASH_MODEL", retryErr)
                try {
                    fallbackStream().collect { chunk ->
                        val text = chunk.text.orEmpty()
                        if (text.isNotEmpty()) onChunk(text)
                    }
                    return true
                } catch (e: CancellationException) {
                    throw e
                } catch (fallbackErr: Exception) {
                    if (hasEmittedText() || !isTransientOverload(fallbackErr)) {
                        throw fallbackErr
                    }
                    Log.w(TAG, "Fallback stream busy, retrying once...", fallbackErr)
                    delay(1400L)
                    fallbackStream().collect { chunk ->
                        val text = chunk.text.orEmpty()
                        if (text.isNotEmpty()) onChunk(text)
                    }
                    return true
                }
            }
        }
    }

    fun formatUserErrorMessage(error: Throwable, fallbackMessage: String): String {
        return when {
            containsAnyIgnoreCase(error, "firebaseapp is not initialized", "initializeapp") -> {
                "Konfigurasi Firebase (app/google-services.json) belum ikut terpasang di APK ini. Pastikan file app/google-services.json sudah ada di GitHub (atau simpan di GitHub Secret GOOGLE_SERVICES_JSON) lalu build ulang APK."
            }
            containsAnyIgnoreCase(error, "app check", "attestation", "403") -> {
                "Verifikasi keamanan Firebase App Check ditolak (403). Untuk pemakaian di HP langsung, gunakan APK Release bertanda tangan atau pastikan token debug terdaftar."
            }
            containsAnyIgnoreCase(error, "billing", "blaze", "upgrade your plan") -> {
                "Model gambar AI ini memerlukan paket Firebase Blaze aktif. Gunakan tombol AI Auto-Detect (Gemini) & preset HD untuk peningkatan HD gratis."
            }
            isTransientOverload(error) -> {
                "Server Gemini sedang sangat padat (high demand). Coba ketuk kirim lagi beberapa detik lagi."
            }
            else -> error.localizedMessage ?: fallbackMessage
        }
    }
}
