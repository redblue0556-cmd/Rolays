package com.example.core

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Provides resilient Gemini model execution for ROLAYS.
 * Uses `gemini-3.8-flash` as the primary model and automatically retries / falls back
 * to `gemini-3.5-flash-lite` when Google's primary model experiences temporary 503/high-demand spikes.
 */
object GeminiResilience {
    private const val TAG = "GeminiResilience"

    const val PRIMARY_FLASH_MODEL = "gemini-3.8-flash"
    const val FALLBACK_FLASH_MODEL = "gemini-3.5-flash-lite"

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
            Log.w(TAG, "Primary model $PRIMARY_FLASH_MODEL busy, retrying briefly...", primaryErr)
            delay(650L)
            try {
                primaryAction() to false
            } catch (e: CancellationException) {
                throw e
            } catch (retryErr: Exception) {
                if (!isTransientOverload(retryErr)) {
                    throw retryErr
                }
                Log.w(TAG, "Primary model still busy, switching to $FALLBACK_FLASH_MODEL", retryErr)
                fallbackAction() to true
            }
        }
    }

    fun formatUserErrorMessage(error: Throwable, fallbackMessage: String): String {
        return if (isTransientOverload(error)) {
            "Server AI sedang sangat sibuk (high demand). Ketuk kirim lagi dalam beberapa detik. (${error.localizedMessage ?: "503"})"
        } else {
            error.localizedMessage ?: fallbackMessage
        }
    }
}
