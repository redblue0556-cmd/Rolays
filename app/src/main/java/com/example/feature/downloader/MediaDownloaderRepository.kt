package com.example.feature.downloader

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

enum class MediaPlatform(val displayName: String) {
    TIKTOK("TikTok"),
    YOUTUBE("YouTube")
}

data class MediaResolutionOption(
    val id: String,
    val label: String,
    val qualityTag: String,
    val streamUrl: String?,
    val isAudioOnly: Boolean = false,
    val sizeMbLabel: String? = null
)

data class ResolvedMediaInfo(
    val originalUrl: String,
    val platform: MediaPlatform,
    val title: String,
    val author: String,
    val thumbnailUrl: String?,
    val durationLabel: String?,
    val options: List<MediaResolutionOption>,
    val statusNote: String? = null
)

data class DownloadedMediaItem(
    val id: String,
    val title: String,
    val platform: MediaPlatform,
    val resolutionLabel: String,
    val savedLocation: String
)

class MediaDownloaderRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun detectPlatform(rawUrl: String): MediaPlatform? {
        val url = rawUrl.trim().lowercase()
        return when {
            "tiktok.com" in url -> MediaPlatform.TIKTOK
            "youtube.com" in url || "youtu.be" in url -> MediaPlatform.YOUTUBE
            else -> null
        }
    }

    suspend fun analyzeUrl(rawUrl: String): Result<ResolvedMediaInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanUrl = rawUrl.trim()
            val platform = detectPlatform(cleanUrl)
                ?: throw IllegalArgumentException("Masukkan tautan valid dari TikTok atau YouTube.")

            when (platform) {
                MediaPlatform.TIKTOK -> fetchTikTokMetadata(cleanUrl)
                MediaPlatform.YOUTUBE -> fetchYouTubeMetadata(cleanUrl)
            }
        }
    }

    private fun fetchTikTokMetadata(url: String): ResolvedMediaInfo {
        val encodedUrl = URLEncoder.encode(url, "UTF-8")

        // 1. Query public TikWM endpoint for no-watermark streams + metadata
        val tikwmJson = runCatching {
            val req = Request.Builder()
                .url("https://www.tikwm.com/api/?url=$encodedUrl&hd=1")
                .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile) ROLAYS/1.0")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                JSONObject(body)
            }
        }.getOrNull()

        if (tikwmJson != null && tikwmJson.optInt("code", -1) == 0) {
            val data = tikwmJson.optJSONObject("data")
            if (data != null) {
                val title = data.optString("title").ifBlank { "Video TikTok Tanpa Watermark" }
                val authorObj = data.optJSONObject("author")
                val author = authorObj?.optString("nickname").orEmpty()
                    .ifBlank { authorObj?.optString("unique_id").orEmpty() }
                    .ifBlank { "Kreator TikTok" }
                val cover = data.optString("cover").ifBlank { data.optString("origin_cover") }
                    .ifBlank { null }
                val durationSec = data.optInt("duration", 0)
                val durationLabel = if (durationSec > 0) {
                    String.format("%02d:%02d", durationSec / 60, durationSec % 60)
                } else null

                val hdUrl = data.optString("hdplay").ifBlank { null }
                val sdUrl = data.optString("play").ifBlank { null }
                val audioUrl = data.optString("music").ifBlank { null }
                val hdSizeMb = data.optLong("hd_size", 0L).let { if (it > 0) String.format("%.1f MB", it / 1_048_576.0) else null }
                val sdSizeMb = data.optLong("size", 0L).let { if (it > 0) String.format("%.1f MB", it / 1_048_576.0) else null }

                val options = buildList {
                    if (hdUrl != null) {
                        add(
                            MediaResolutionOption(
                                id = "tt_hd",
                                label = "1080p Full HD (Tanpa Watermark)",
                                qualityTag = "HD MP4",
                                streamUrl = hdUrl,
                                sizeMbLabel = hdSizeMb
                            )
                        )
                    }
                    if (sdUrl != null) {
                        add(
                            MediaResolutionOption(
                                id = "tt_sd",
                                label = "720p HD Standar (Tanpa Watermark)",
                                qualityTag = "MP4",
                                streamUrl = sdUrl,
                                sizeMbLabel = sdSizeMb
                            )
                        )
                    }
                    if (audioUrl != null) {
                        add(
                            MediaResolutionOption(
                                id = "tt_audio",
                                label = "Audio Original (MP3)",
                                qualityTag = "MP3",
                                streamUrl = audioUrl,
                                isAudioOnly = true
                            )
                        )
                    }
                }

                if (options.isNotEmpty()) {
                    return ResolvedMediaInfo(
                        originalUrl = url,
                        platform = MediaPlatform.TIKTOK,
                        title = title,
                        author = "@$author",
                        thumbnailUrl = cover,
                        durationLabel = durationLabel,
                        options = options,
                        statusNote = "Siap diunduh bersih tanpa watermark."
                    )
                }
            }
        }

        // 2. Fallback to official TikTok oEmbed for verified metadata
        val oembedReq = Request.Builder()
            .url("https://www.tiktok.com/oembed?url=$encodedUrl")
            .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile) ROLAYS/1.0")
            .build()
        client.newCall(oembedReq).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("Gagal mengambil info TikTok (HTTP ${resp.code}). Periksa kembali tautan.")
            }
            val json = JSONObject(resp.body?.string().orEmpty())
            val title = json.optString("title", "Video TikTok")
            val author = json.optString("author_name", "Kreator TikTok")
            val thumb = json.optString("thumbnail_url").ifBlank { null }
            return ResolvedMediaInfo(
                originalUrl = url,
                platform = MediaPlatform.TIKTOK,
                title = title,
                author = author,
                thumbnailUrl = thumb,
                durationLabel = null,
                options = listOfNotNull(
                    thumb?.let {
                        MediaResolutionOption(
                            id = "tt_thumb",
                            label = "Simpan Sampul HD (JPG)",
                            qualityTag = "JPG",
                            streamUrl = it
                        )
                    }
                ),
                statusNote = "Metadata TikTok ditemukan, namun stream MP4 langsung sedang dibatasi oleh server sumber."
            )
        }
    }

    private fun fetchYouTubeMetadata(url: String): ResolvedMediaInfo {
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        val oembedReq = Request.Builder()
            .url("https://www.youtube.com/oembed?url=$encodedUrl&format=json")
            .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile) ROLAYS/1.0")
            .build()

        client.newCall(oembedReq).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("Gagal mengambil metadata YouTube (HTTP ${resp.code}). Pastikan tautan publik dan valid.")
            }
            val json = JSONObject(resp.body?.string().orEmpty())
            val title = json.optString("title", "Video YouTube")
            val author = json.optString("author_name", "YouTube Channel")
            val thumb = json.optString("thumbnail_url").ifBlank { null }
            val videoId = extractYouTubeId(url)
            val maxResThumb = videoId?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" } ?: thumb

            val options = buildList {
                if (maxResThumb != null) {
                    add(
                        MediaResolutionOption(
                            id = "yt_thumb_hd",
                            label = "Thumbnail YouTube HD (Tanpa Watermark)",
                            qualityTag = "JPG HD",
                            streamUrl = maxResThumb
                        )
                    )
                }
            }

            return ResolvedMediaInfo(
                originalUrl = url,
                platform = MediaPlatform.YOUTUBE,
                title = title,
                author = author,
                thumbnailUrl = maxResThumb,
                durationLabel = null,
                options = options,
                statusNote = "Metadata YouTube terverifikasi. Pengunduhan stream video YouTube secara langsung dilindungi oleh enkripsi/kebijakan YouTube, sehingga Anda dapat menyimpan Thumbnail HD atau membuat Ringkasan AI."
            )
        }
    }

    private fun extractYouTubeId(url: String): String? {
        val regex = Regex("""(?:v=|youtu\.be/|shorts/|embed/)([A-Za-z0-9_-]{11})""")
        return regex.find(url)?.groupValues?.getOrNull(1)
    }

    suspend fun downloadOption(
        context: Context,
        info: ResolvedMediaInfo,
        option: MediaResolutionOption,
        onProgress: (Int) -> Unit
    ): Result<DownloadedMediaItem> = withContext(Dispatchers.IO) {
        runCatching {
            val url = option.streamUrl
                ?: throw IllegalStateException("Stream URL tidak tersedia untuk opsi ini.")
            val ext = when {
                option.isAudioOnly -> "mp3"
                option.qualityTag.contains("JPG", ignoreCase = true) -> "jpg"
                else -> "mp4"
            }
            val mime = when (ext) {
                "mp3" -> "audio/mpeg"
                "jpg" -> "image/jpeg"
                else -> "video/mp4"
            }
            val safeTitle = info.title
                .replace(Regex("[^A-Za-z0-9_\\- ]"), "")
                .trim()
                .take(36)
                .ifBlank { "ROLAYS_Media" }
            val fileName = "${safeTitle}_${System.currentTimeMillis()}.$ext"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile) ROLAYS/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Server menolak unduhan (HTTP ${response.code}).")
                    }
                    val body = response.body ?: throw IllegalStateException("File unduhan kosong.")
                    val totalBytes = body.contentLength()

                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, mime)
                        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ROLAYS")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val targetUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: throw IllegalStateException("Gagal membuat file di folder Downloads.")

                    resolver.openOutputStream(targetUri)?.use { out ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8192)
                            var downloaded = 0L
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                out.write(buffer, 0, read)
                                downloaded += read
                                if (totalBytes > 0) {
                                    onProgress(((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100))
                                }
                            }
                        }
                    } ?: throw IllegalStateException("Tidak dapat menulis ke folder Downloads.")

                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(targetUri, values, null, null)
                    onProgress(100)
                }

                DownloadedMediaItem(
                    id = fileName,
                    title = info.title,
                    platform = info.platform,
                    resolutionLabel = option.label,
                    savedLocation = "Downloads/ROLAYS/$fileName"
                )
            } else {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val req = DownloadManager.Request(Uri.parse(url))
                    .setTitle(info.title)
                    .setDescription("ROLAYS • ${option.label}")
                    .setMimeType(mime)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ROLAYS/$fileName")
                dm.enqueue(req)
                onProgress(100)
                DownloadedMediaItem(
                    id = fileName,
                    title = info.title,
                    platform = info.platform,
                    resolutionLabel = option.label,
                    savedLocation = "Downloads/$fileName"
                )
            }
        }
    }
}
