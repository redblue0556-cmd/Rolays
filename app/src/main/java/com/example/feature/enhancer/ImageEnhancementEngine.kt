package com.example.feature.enhancer

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ImageEnhancementEngine {

    private const val MAX_WORKING_DIMENSION = 1440

    suspend fun loadSamplePair(
        context: Context,
        @DrawableRes resId: Int
    ): Pair<Bitmap, Bitmap> = withContext(Dispatchers.IO) {
        val raw = BitmapFactory.decodeResource(context.resources, resId)
        val hdBase = scaleToMaxDimension(raw, 1200)
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

    suspend fun extractVideoFrame(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(
                1_000_000L,
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
        // 1. Upscale low-res images cleanly to HD
        val targetBitmap = if (upscaleIfSmall && max(source.width, source.height) < 1080) {
            val factor = (1080f / max(source.width, source.height)).coerceAtMost(2.2f)
            val newW = (source.width * factor).roundToInt()
            val newH = (source.height * factor).roundToInt()
            Bitmap.createScaledBitmap(source, newW, newH, true)
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }

        // 2. Apply ColorMatrix (contrast, brightness, saturation, warmth)
        val colorAdjusted = Bitmap.createBitmap(
            targetBitmap.width,
            targetBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(colorAdjusted)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val cm = buildColorMatrix(params)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(targetBitmap, 0f, 0f, paint)

        // 3. Apply radial vignette if enabled
        if (params.vignette > 0.01f) {
            val cx = colorAdjusted.width / 2f
            val cy = colorAdjusted.height / 2f
            val radius = max(cx, cy) * 1.35f
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx,
                    cy,
                    radius,
                    intArrayOf(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                        Color.argb((params.vignette * 190).roundToInt().coerceIn(0, 210), 4, 6, 16)
                    ),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, colorAdjusted.width.toFloat(), colorAdjusted.height.toFloat(), vignettePaint)
        }

        // 4. Apply pixel unsharp-mask / edge sharpening kernel
        if (params.sharpness > 0.03f) {
            applySharpenFilter(colorAdjusted, params.sharpness)
        } else {
            colorAdjusted
        }
    }

    private fun buildColorMatrix(params: EnhancementParams): ColorMatrix {
        val result = ColorMatrix()

        // Saturation
        val satMatrix = ColorMatrix().apply { setSaturation(params.saturation) }
        result.postConcat(satMatrix)

        // Contrast & Brightness
        val c = params.contrast
        val t = (1f - c) * 128f + (params.brightness * 255f)
        val cbMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
        result.postConcat(cbMatrix)

        // Warmth (shift R up, B down for warm; inverse for cool)
        if (kotlin.math.abs(params.warmth) > 0.005f) {
            val w = params.warmth * 36f
            val warmthMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, w,
                    0f, 1f, 0f, 0f, w * 0.25f,
                    0f, 0f, 1f, 0f, -w,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            result.postConcat(warmthMatrix)
        }

        return result
    }

    private fun applySharpenFilter(src: Bitmap, amount: Float): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = pixels.copyOf()

        val strength = (amount * 1.45f).coerceIn(0f, 1.5f)
        val centerWeight = 1f + 4f * strength

        for (y in 1 until h - 1) {
            val rowOffset = y * w
            for (x in 1 until w - 1) {
                val idx = rowOffset + x
                val c = pixels[idx]
                val top = pixels[idx - w]
                val bottom = pixels[idx + w]
                val left = pixels[idx - 1]
                val right = pixels[idx + 1]

                val r = (((c shr 16) and 0xFF) * centerWeight -
                    (((top shr 16) and 0xFF) + ((bottom shr 16) and 0xFF) +
                        ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF)) * strength)
                    .roundToInt().coerceIn(0, 255)

                val g = (((c shr 8) and 0xFF) * centerWeight -
                    (((top shr 8) and 0xFF) + ((bottom shr 8) and 0xFF) +
                        ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF)) * strength)
                    .roundToInt().coerceIn(0, 255)

                val b = ((c and 0xFF) * centerWeight -
                    ((top and 0xFF) + (bottom and 0xFF) +
                        (left and 0xFF) + (right and 0xFF)) * strength)
                    .roundToInt().coerceIn(0, 255)

                val a = (c ushr 24) and 0xFF
                out[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    fun compressForAiAnalysis(bitmap: Bitmap): Bitmap {
        return scaleToMaxDimension(bitmap, 768)
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
