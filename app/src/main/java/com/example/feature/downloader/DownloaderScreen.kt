package com.example.feature.downloader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.LocalLiquidGlassPalette

@Composable
fun DownloaderScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloaderViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val glass = LocalLiquidGlassPalette.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "Media Downloader",
                    style = MaterialTheme.typography.headlineMedium,
                    color = glass.textPrimary
                )
                Text(
                    text = "Unduh video TikTok & info YouTube bersih tanpa watermark",
                    style = MaterialTheme.typography.bodyMedium,
                    color = glass.textSecondary
                )
            }

            // Platform Badges
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlatformPill(label = "TikTok • Tanpa Watermark", accent = glass.cyanAccent)
                PlatformPill(label = "YouTube • HD Info", accent = glass.violetAccent)
            }

            // URL Paste Input Card
            LiquidGlassSurface(
                shape = RoundedCornerShape(24.dp),
                cornerRadiusDp = 24.dp,
                elevated = true,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.urlInput,
                        onValueChange = { viewModel.updateUrl(it) },
                        placeholder = {
                            Text(
                                text = "Tempel tautan TikTok atau YouTube di sini...",
                                color = glass.textMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Link,
                                contentDescription = null,
                                tint = glass.cyanAccent
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = glass.cyanAccent,
                            unfocusedBorderColor = glass.borderSoft,
                            focusedTextColor = glass.textPrimary,
                            unfocusedTextColor = glass.textPrimary,
                            cursorColor = glass.cyanAccent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("downloader_url_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidGlassSurface(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("paste_url_button"),
                            shape = RoundedCornerShape(18.dp),
                            cornerRadiusDp = 18.dp,
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
                            onClick = {
                                val text = clipboard.getText()?.text.orEmpty()
                                if (text.isNotBlank()) {
                                    viewModel.pasteAndAnalyze(text)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentPaste,
                                    contentDescription = "Tempel link",
                                    tint = glass.cyanAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tempel",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = glass.textPrimary
                                )
                            }
                        }

                        LiquidGlassSurface(
                            modifier = Modifier
                                .weight(1.3f)
                                .minimumInteractiveComponentSize()
                                .testTag("analyze_url_button"),
                            shape = RoundedCornerShape(18.dp),
                            cornerRadiusDp = 18.dp,
                            tintColor = glass.cyanAccent.copy(alpha = 0.28f),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
                            onClick = { viewModel.analyzeLink() }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Analisis link",
                                    tint = glass.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ambil Media",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = glass.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Fetch Result Area
            when (val fetch = state.fetchState) {
                is DownloaderFetchState.Idle -> {
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(22.dp),
                        cornerRadiusDp = 22.dp,
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Cara Cepat Tanpa Watermark",
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.cyanAccent
                            )
                            Text(
                                text = "1. Salin tautan video TikTok atau YouTube.\n2. Ketuk 'Tempel' atau tempel manual di kolom atas.\n3. Pilih resolusi HD lalu unduh langsung ke folder Downloads/ROLAYS.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textSecondary
                            )
                        }
                    }
                }

                is DownloaderFetchState.Loading -> {
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(22.dp),
                        cornerRadiusDp = 22.dp,
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = glass.cyanAccent
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Mengambil metadata & opsi resolusi HD...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textPrimary
                            )
                        }
                    }
                }

                is DownloaderFetchState.Error -> {
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(20.dp),
                        cornerRadiusDp = 20.dp,
                        tintColor = glass.roseAccent.copy(alpha = 0.16f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text(
                            text = fetch.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textPrimary
                        )
                    }
                }

                is DownloaderFetchState.Ready -> {
                    val info = fetch.info
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(24.dp),
                        cornerRadiusDp = 24.dp,
                        elevated = true,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (info.thumbnailUrl != null) {
                                    AsyncImage(
                                        model = info.thumbnailUrl,
                                        contentDescription = info.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(84.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.Black.copy(alpha = 0.3f))
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Verified,
                                            contentDescription = null,
                                            tint = glass.mintAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "${info.platform.displayName}${info.durationLabel?.let { " • $it" } ?: ""}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = glass.mintAccent
                                        )
                                    }
                                    Text(
                                        text = info.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = glass.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = info.author,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textSecondary
                                    )
                                }
                            }

                            if (!info.statusNote.isNullOrBlank()) {
                                Text(
                                    text = info.statusNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.cyanAccent
                                )
                            }

                            // Resolution Options
                            if (info.options.isNotEmpty()) {
                                Text(
                                    text = "Pilih Kualitas & Format",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = glass.textPrimary
                                )
                                info.options.forEach { option ->
                                    val selected = state.selectedOptionId == option.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (selected) glass.cyanAccent.copy(alpha = 0.16f)
                                                else Color.White.copy(alpha = 0.04f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (selected) glass.cyanAccent.copy(alpha = 0.6f)
                                                else glass.borderSoft,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable { viewModel.selectOption(option.id) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = { viewModel.selectOption(option.id) },
                                            colors = RadioButtonDefaults.colors(selectedColor = glass.cyanAccent)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = option.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = glass.textPrimary
                                            )
                                            if (option.sizeMbLabel != null) {
                                                Text(
                                                    text = "Ukuran: ${option.sizeMbLabel}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = glass.textSecondary
                                                )
                                            }
                                        }
                                        Text(
                                            text = option.qualityTag,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = glass.cyanAccent
                                        )
                                    }
                                }

                                LiquidGlassSurface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("download_media_button"),
                                    shape = RoundedCornerShape(18.dp),
                                    cornerRadiusDp = 18.dp,
                                    tintColor = glass.blueAccent.copy(alpha = 0.36f),
                                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                                    onClick = { if (!state.isDownloading) viewModel.startDownload(context) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Download,
                                            contentDescription = "Unduh sekarang",
                                            tint = glass.textPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (state.isDownloading) {
                                                "Mengunduh ${state.downloadProgress}%..."
                                            } else {
                                                "Unduh Sekarang"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = glass.textPrimary
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = state.isDownloading) {
                                LinearProgressIndicator(
                                    progress = { state.downloadProgress / 100f },
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                                    color = glass.cyanAccent,
                                    trackColor = Color.White.copy(alpha = 0.12f)
                                )
                            }

                            if (state.downloadStatus != null) {
                                Text(
                                    text = state.downloadStatus.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glass.mintAccent
                                )
                            }

                            // AI Viral Caption Helper
                            LiquidGlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("downloader_ai_summary_button"),
                                shape = RoundedCornerShape(16.dp),
                                cornerRadiusDp = 16.dp,
                                tintColor = glass.violetAccent.copy(alpha = 0.18f),
                                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp),
                                onClick = { viewModel.generateViralSummaryWithGemini() }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (state.isGeneratingAiNote) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = glass.textPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = glass.violetAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Buat Caption & Hashtag Viral (Gemini AI)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = glass.textPrimary
                                    )
                                }
                            }

                            if (state.aiCaptionResult != null) {
                                LiquidGlassSurface(
                                    shape = RoundedCornerShape(16.dp),
                                    cornerRadiusDp = 16.dp,
                                    contentPadding = PaddingValues(12.dp)
                                ) {
                                    Text(
                                        text = state.aiCaptionResult.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Downloads
            if (state.history.isNotEmpty()) {
                Text(
                    text = "Riwayat Unduhan Sesi Ini",
                    style = MaterialTheme.typography.titleSmall,
                    color = glass.textPrimary
                )
                state.history.forEach { item ->
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        cornerRadiusDp = 18.dp,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = glass.mintAccent
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glass.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.platform.displayName} • ${item.savedLocation}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(104.dp))
        }
    }
}

@Composable
private fun PlatformPill(label: String, accent: Color) {
    LiquidGlassSurface(
        shape = RoundedCornerShape(14.dp),
        cornerRadiusDp = 14.dp,
        tintColor = accent.copy(alpha = 0.14f),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
    }
}
