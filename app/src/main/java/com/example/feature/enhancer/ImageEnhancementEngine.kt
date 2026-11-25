package com.example.feature.enhancer

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object ImageEnhancementEngine {

    private const val MAX_WORKING_DIMENSION = 1600
    private const val HD_TARGET_MIN_DIMENSION = 1440

    suspend fun loadSamplePair(
        context: Context,
        @DrawableRes resId: Int
    ): Pair<Bitmap, Bitmap> = withContext(Dispatchers.IO) {
        val raw = BitmapFactory.decodeResource(context.resources, resId)
        val hdBase = scaleToMaxDimension(raw, 1280)
        // Create an authentic low-res / soft blur "Before" sample for Before-After comparison
        val downW = (hdBase.width / 5).coerceAtLeast(160)
        val downH = (hdBase.height / 5).coerceAtLeast(120)
        val tiny = Bitmap.createScaledBitmap(hdBase, downW, downH, true)
        val blurredBefore = Bitmap.createScaledBitmap(tiny, hdBase.width, hdBase.height, true)
        tiny.recycle()
        blurredBefore to hdBase
    }

    suspend fun loadPhotoFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val decoded = BitmapFactory.decodeStream(stream) ?: return@withContext null
            scaleToMaxDimension(decoded, MAX_WORKING_DIMENSION)
        }
    }

    suspend fun extractVideoFrame(
        context: Context,
        uri: Uri,
        timeUs: Long = 1_000_000L
    ): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: retriever.frameAtTime
            frame?.let { scaleToMaxDimension(it, MAX_WORKING_DIMENSION) }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun enhanceBitmap(
        source: Bitmap,
        params: EnhancementParams,
        upscaleIfSmall: Boolean = true
    ): Bitmap = withContext(Dispatchers.Default) {
        // 1. Progressive clean upscaling for low-resolution photos/frames
        val working = if (upscaleIfSmall && max(source.width, source.height) < HD_TARGET_MIN_DIMENSION) {
            progressiveUpscale(source, HD_TARGET_MIN_DIMENSION)
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }

        val w = working.width
        val h = working.height
        val pixels = IntArray(w * h)
        working.getPixels(pixels, 0, w, 0, 0, w, h)

        // 2. Edge-preserving denoise & anti-JPEG smoothing (makes blurry/compressed photos clean & smooth)
        val denoised = if (params.sharpness > 0.02f || params.contrast > 1.02f) {
            edgePreservingDenoise(pixels, w, h, strength = 0.48f)
        } else {
            pixels
        }

        // 3. Smooth S-curve tone mapping + shadow clarity + warmth + saturation (never crushes blacks/whites)
        val toned = applySmoothToneAndColor(denoised, params)

        // 4. Luminance-only multi-radius Unsharp Mask with noise gate & halo clamp (crisp HD without grain/artifacts)
        val finalPixels = if (params.sharpness > 0.02f) {
            applyCleanLuminanceSharpen(toned, w, h, params.sharpness)
        } else {
            toned
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(finalPixels, 0, w, 0, 0, w, h)

        // 5. Optional soft vignette only if requested
        if (params.vignette > 0.01f) {
            val canvas = Canvas(result)
            val cx = w / 2f
            val cy = h / 2f
            val radius = max(cx, cy) * 1.40f
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx,
                    cy,
                    radius,
                    intArrayOf(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                        Color.argb((params.vignette * 165).roundToInt().coerceIn(0, 180), 4, 6, 16)
                    ),
                    floatArrayOf(0f, 0.58f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), vignettePaint)
        }

        result
    }

    private fun progressiveUpscale(source: Bitmap, targetMaxDim: Int): Bitmap {
        val currentMax = max(source.width, source.height)
        if (currentMax >= targetMaxDim) return source.copy(Bitmap.Config.ARGB_8888, true)

        val totalFactor = (targetMaxDim.toFloat() / currentMax).coerceAtMost(2.5f)
        if (totalFactor <= 1.45f) {
            val nw = (source.width * totalFactor).roundToInt()
            val nh = (source.height * totalFactor).roundToInt()
            return Bitmap.createScaledBitmap(source, nw, nh, true)
        }

        // Two-step progressive upscale avoids soft single-pass smear
        val midFactor = sqrt(totalFactor)
        val midW = (source.width * midFactor).roundToInt()
        val midH = (source.height * midFactor).roundToInt()
        val mid = Bitmap.createScaledBitmap(source, midW, midH, true)

        val finalW = (source.width * totalFactor).roundToInt()
        val finalH = (source.height * totalFactor).roundToInt()
        val out = Bitmap.createScaledBitmap(mid, finalW, finalH, true)
        if (mid !== source) mid.recycle()
        return out
    }

    /**
     * Fast 5-tap edge-preserving smoothing that removes ugly JPEG compression blocks and speckle noise
     * while keeping strong edges intact.
     */
    private fun edgePreservingDenoise(
        pixels: IntArray,
        w: Int,
        h: Int,
        strength: Float
    ): IntArray {
        val out = pixels.copyOf()
        val threshold = 26

        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val idx = row + x
                val c = pixels[idx]
                val a = (c ushr 24) and 0xFF
                val cr = (c shr 16) and 0xFF
                val cg = (c shr 8) and 0xFF
                val cb = c and 0xFF
                val cLum = (cr * 77 + cg * 150 + cb * 29) shr 8

                var sumR = cr.toFloat()
                var sumG = cg.toFloat()
                var sumB = cb.toFloat()
                var sumW = 1.0f

                val neighbors = intArrayOf(
                    pixels[idx - 1],
                    pixels[idx + 1],
                    pixels[idx - w],
                    pixels[idx + w]
                )
                for (n in neighbors) {
                    val nr = (n shr 16) and 0xFF
                    val ng = (n shr 8) and 0xFF
                    val nb = n and 0xFF
                    val nLum = (nr * 77 + ng * 150 + nb * 29) shr 8
                    val diff = abs(nLum - cLum)
                    if (diff < threshold) {
                        val weight = (1f - diff.toFloat() / threshold) * strength
                        sumR += nr * weight
                        sumG += ng * weight
                        sumB += nb * weight
                        sumW += weight
                    }
                }

                val rr = (sumR / sumW).roundToInt().coerceIn(0, 255)
                val gg = (sumG / sumW).roundToInt().coerceIn(0, 255)
                val bb = (sumB / sumW).roundToInt().coerceIn(0, 255)
                out[idx] = (a shl 24) or (rr shl 16) or (gg shl 8) or bb
            }
        }
        return out
    }

    /**
     * Smooth S-curve contrast + gentle shadow lift + vibrance/saturation + warmth.
     * Avoids harsh linear clipping so highlights and shadows remain clean and natural.
     */
    private fun applySmoothToneAndColor(
        pixels: IntArray,
        params: EnhancementParams
    ): IntArray {
        val out = IntArray(pixels.size)

        // Build 256-entry tone LUT for smooth contrast & brightness without crushing
        val lut = IntArray(256)
        val cStrength = (params.contrast - 1.0f) * 0.42f
        val brightShift = params.brightness * 95f
        val shadowLift = if (params.brightness >= 0f) params.brightness * 32f else 0f

        for (i in 0..255) {
            val norm = i / 255f
            // Smooth sinusoidal S-curve
            val sCurve = norm - cStrength * sin(2.0 * PI * norm).toFloat() * 0.25f
            val lifted = sCurve * 255f + shadowLift * (1f - norm) * (1f - norm) + brightShift
            lut[i] = lifted.roundToInt().coerceIn(0, 255)
        }

        val sat = params.saturation.coerceIn(0f, 1.8f)
        val warmShift = params.warmth * 30f

        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            var r = lut[(p shr 16) and 0xFF].toFloat()
            var g = lut[(p shr 8) and 0xFF].toFloat()
            var b = lut[p and 0xFF].toFloat()

            // Warmth adjustment
            if (abs(warmShift) > 0.2f) {
                r += warmShift
                g += warmShift * 0.22f
                b -= warmShift
            }

            // Smart saturation / vibrance around perceptual luminance
            if (abs(sat - 1f) > 0.01f) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val maxCh = max(r, max(g, b))
                val minCh = min(r, min(g, b))
                val currentSat = if (maxCh > 1f) (maxCh - minCh) / maxCh else 0f
                // Boost muted tones more than already saturated tones when sat > 1
                val effectiveSat = if (sat > 1f) {
                    1f + (sat - 1f) * (1f - currentSat * 0.45f)
                } else {
                    sat
                }
                r = lum + (r - lum) * effectiveSat
                g = lum + (g - lum) * effectiveSat
                b = lum + (b - lum) * effectiveSat
            }

            out[i] = (a shl 24) or
                (r.roundToInt().coerceIn(0, 255) shl 16) or
                (g.roundToInt().coerceIn(0, 255) shl 8) or
                b.roundToInt().coerceIn(0, 255)
        }
        return out
    }

    /**
     * Separable 2-pass Luminance Unsharp Mask (radius 2px) with noise threshold and halo clamping.
     * Sharpens edges and fine detail cleanly without color fringing or pixelated destruction.
     */
    private fun applyCleanLuminanceSharpen(
        pixels: IntArray,
        w: Int,
        h: Int,
        amount: Float
    ): IntArray {
        val total = w * h
        val lum = FloatArray(total)
        for (i in 0 until total) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            lum[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        // Separable horizontal box blur (radius 2 => 5-tap)
        val horiz = FloatArray(total)
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                var sum = 0f
                var count = 0
                val x0 = max(0, x - 2)
                val x1 = min(w - 1, x + 2)
                for (kx in x0..x1) {
                    sum += lum[row + kx]
                    count++
                }
                horiz[row + x] = sum / count
            }
        }

        // Separable vertical box blur (radius 2 => 5-tap)
        val blurredLum = FloatArray(total)
        for (y in 0 until h) {
            val y0 = max(0, y - 2)
            val y1 = min(h - 1, y + 2)
            val row = y * w
            for (x in 0 until w) {
                var sum = 0f
                var count = 0
                for (ky in y0..y1) {
                    sum += horiz[ky * w + x]
                    count++
                }
                blurredLum[row + x] = sum / count
            }
        }

        val out = IntArray(total)
        val gain = (amount.coerceIn(0f, 1f) * 1.15f)
        val noiseThreshold = 3.2f
        val maxHaloBoost = 28f

        for (i in 0 until total) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            val detail = lum[i] - blurredLum[i]

            if (abs(detail) <= noiseThreshold) {
                out[i] = p
                continue
            }

            // Soft-knee detail boost clamped to avoid halos
            val signedDetail = if (detail > 0f) detail - noiseThreshold else detail + noiseThreshold
            val delta = (signedDetail * gain).coerceIn(-maxHaloBoost, maxHaloBoost)

            val r = (((p shr 16) and 0xFF) + delta).roundToInt().coerceIn(0, 255)
            val g = (((p shr 8) and 0xFF) + delta).roundToInt().coerceIn(0, 255)
            val b = ((p and 0xFF) + delta).roundToInt().coerceIn(0, 255)
            out[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }

    fun compressForAiAnalysis(bitmap: Bitmap): Bitmap {
        return scaleToMaxDimension(bitmap, 1024)
    }

    private fun scaleToMaxDimension(bitmap: Bitmap, maxDim: Int): Bitmap {
        val currentMax = max(bitmap.width, bitmap.height)
        if (currentMax <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / currentMax
        val targetW = max(1, (bitmap.width * ratio).roundToInt())
        val targetH = max(1, (bitmap.height * ratio).roundToInt())
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val filename = "ROLAYS_HD_${System.currentTimeMillis()}.jpg"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/ROLAYS"
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("Gagal membuat entri MediaStore")

                resolver.openOutputStream(uri)?.use { out ->
                    val bos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, bos)
                    out.write(bos.toByteArray())
                } ?: error("Gagal membuka output stream")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                "Tersimpan ke Galeri: Pictures/ROLAYS/$filename"
            }
        }
}
