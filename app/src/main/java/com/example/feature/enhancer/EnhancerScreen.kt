package com.example.feature.enhancer

import android.graphics.Bitmap
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.WorkflowStepBar
import com.example.ui.theme.LocalLiquidGlassPalette
import kotlin.math.roundToInt

@Composable
fun EnhancerScreen(
    modifier: Modifier = Modifier,
    onSendToSocialAi: (Bitmap) -> Unit = {},
    viewModel: EnhancerViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val glass = LocalLiquidGlassPalette.current
    var sliderSplit by remember { mutableFloatStateOf(0.52f) }
    var showAdjustments by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.initializeSampleIfNeeded(context)
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onMediaSelected(context, uri, state.mediaType)
        }
    }

    val fallbackDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onMediaSelected(context, uri, state.mediaType)
        }
    }

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
            // Header + Media Type Switch
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HD Enhancer AI",
                        style = MaterialTheme.typography.headlineMedium,
                        color = glass.textPrimary
                    )
                    Text(
                        text = "Jernihkan foto & video blur ke HD instan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textSecondary
                    )
                }

                LiquidGlassSurface(
                    shape = RoundedCornerShape(18.dp),
                    cornerRadiusDp = 18.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    onClick = {
                        val req = if (state.mediaType == EnhancerMediaType.PHOTO) {
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        } else {
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        }
                        runCatching { mediaPicker.launch(req) }.onFailure {
                            val mime = if (state.mediaType == EnhancerMediaType.PHOTO) {
                                arrayOf("image/*")
                            } else {
                                arrayOf("video/*")
                            }
                            fallbackDocPicker.launch(mime)
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pick_enhancer_media_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (state.mediaType == EnhancerMediaType.PHOTO) {
                                Icons.Filled.PhotoLibrary
                            } else {
                                Icons.Filled.Videocam
                            },
                            contentDescription = "Pilih media",
                            tint = glass.cyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Pilih",
                            style = MaterialTheme.typography.labelLarge,
                            color = glass.textPrimary
                        )
                    }
                }
            }

            // Workflow Progress Bar
            val enhancerStep = when {
                state.saveMessage != null -> 2
                state.selectedUri != null || state.aiState is AiEnhanceState.Success ||
                    state.selectedPreset != EnhancementPreset.ORIGINAL -> 1
                else -> 0
            }
            WorkflowStepBar(
                steps = listOf("Pilih", "Edit HD", "Simpan"),
                currentStepIndex = enhancerStep,
                onStepClick = { step ->
                    when (step) {
                        0 -> {
                            val req = if (state.mediaType == EnhancerMediaType.PHOTO) {
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            } else {
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            }
                            runCatching { mediaPicker.launch(req) }.onFailure {
                                val mime = if (state.mediaType == EnhancerMediaType.PHOTO) {
                                    arrayOf("image/*")
                                } else {
                                    arrayOf("video/*")
                                }
                                fallbackDocPicker.launch(mime)
                            }
                        }
                        1 -> viewModel.runAiAutoEnhance()
                        2 -> viewModel.saveEnhancedToGallery(context)
                    }
                }
            )

            // Photo / Video Mode Pill Toggle
            LiquidGlassSurface(
                shape = RoundedCornerShape(20.dp),
                cornerRadiusDp = 20.dp,
                contentPadding = PaddingValues(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    EnhancerMediaType.entries.forEach { type ->
                        val selected = state.mediaType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) {
                                        Brush.linearGradient(
                                            listOf(
                                                glass.cyanAccent.copy(alpha = 0.30f),
                                                glass.blueAccent.copy(alpha = 0.25f)
                                            )
                                        )
                                    } else {
                                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .clickable { viewModel.setMediaType(type) }
                                .padding(vertical = 10.dp)
                                .testTag("enhancer_mode_${type.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) glass.textPrimary else glass.textSecondary
                            )
                        }
                    }
                }
            }

            // Before / After Comparison Card
            LiquidGlassSurface(
                shape = RoundedCornerShape(26.dp),
                cornerRadiusDp = 26.dp,
                elevated = true,
                contentPadding = PaddingValues(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Hd,
                                contentDescription = null,
                                tint = glass.cyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${state.beforeResolutionLabel} -> ${state.afterResolutionLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                color = glass.cyanAccent
                            )
                        }
                        if (state.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = glass.cyanAccent
                            )
                        }
                    }

                    BeforeAfterComparisonCard(
                        beforeBitmap = state.beforeBitmap,
                        afterBitmap = state.afterBitmap,
                        splitRatio = sliderSplit,
                        onSplitChange = { sliderSplit = it }
                    )

                    // Explicit interactive Before/After scrub bar below image for effortless control
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Sebelum",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                        Slider(
                            value = sliderSplit,
                            onValueChange = { sliderSplit = it },
                            valueRange = 0.05f..0.95f,
                            colors = SliderDefaults.colors(
                                thumbColor = glass.cyanAccent,
                                activeTrackColor = glass.cyanAccent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("before_after_scrub_bar")
                        )
                        Text(
                            text = "Sesudah",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.cyanAccent
                        )
                    }

                    // Optional real video player preview when user picks a video
                    if (state.mediaType == EnhancerMediaType.VIDEO && state.selectedUri != null) {
                        Text(
                            text = "Pratinjau Video Terpilih (Frame di atas ditingkatkan ke HD)",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.textSecondary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        tag = state.selectedUri
                                        setVideoURI(state.selectedUri)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                update = { view ->
                                    if (view.tag != state.selectedUri) {
                                        view.tag = state.selectedUri
                                        view.setVideoURI(state.selectedUri)
                                        view.start()
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // AI Auto Enhance + Generative Restore Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassSurface(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("ai_auto_enhance_button"),
                    shape = RoundedCornerShape(20.dp),
                    cornerRadiusDp = 20.dp,
                    tintColor = glass.cyanAccent.copy(alpha = 0.22f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    onClick = { viewModel.runAiAutoEnhance() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.aiState is AiEnhanceState.Analyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = glass.textPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = glass.cyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Auto-HD",
                            style = MaterialTheme.typography.labelLarge,
                            color = glass.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                LiquidGlassSurface(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("ai_restore_button"),
                    shape = RoundedCornerShape(20.dp),
                    cornerRadiusDp = 20.dp,
                    tintColor = glass.violetAccent.copy(alpha = 0.22f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    onClick = { viewModel.runAiGenerativeRestore() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.aiState is AiEnhanceState.RestoringImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = glass.textPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Hd,
                                contentDescription = null,
                                tint = glass.violetAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Restorasi",
                            style = MaterialTheme.typography.labelLarge,
                            color = glass.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // AI Feedback Banner
            AnimatedVisibility(visible = state.aiState !is AiEnhanceState.Idle) {
                when (val ai = state.aiState) {
                    is AiEnhanceState.Analyzing -> StatusGlassBanner(
                        text = "Gemini 3.8 Flash menganalisis detail cahaya & ketajaman...",
                        accent = glass.cyanAccent
                    )
                    is AiEnhanceState.RestoringImage -> StatusGlassBanner(
                        text = "Merestorasi resolusi HD dengan Nano Banana AI (30-60 dtk)...",
                        accent = glass.violetAccent
                    )
                    is AiEnhanceState.Success -> StatusGlassBanner(
                        text = ai.summary,
                        accent = glass.mintAccent
                    )
                    is AiEnhanceState.Error -> StatusGlassBanner(
                        text = ai.message,
                        accent = glass.roseAccent
                    )
                    else -> Unit
                }
            }

            // Visual Filter Presets Strip
            Text(
                text = "Filter & Efek Visual",
                style = MaterialTheme.typography.titleSmall,
                color = glass.textPrimary
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(EnhancementPreset.entries) { preset ->
                    val selected = state.selectedPreset == preset
                    LiquidGlassSurface(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("preset_${preset.name.lowercase()}"),
                        shape = RoundedCornerShape(18.dp),
                        cornerRadiusDp = 18.dp,
                        tintColor = if (selected) glass.cyanAccent.copy(alpha = 0.28f) else null,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        onClick = { viewModel.selectPreset(preset) }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = preset.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) glass.cyanAccent else glass.textMuted
                            )
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = glass.textPrimary
                            )
                        }
                    }
                }
            }

            // Aesthetic Fine-Tuning Sliders
            LiquidGlassSurface(
                shape = RoundedCornerShape(24.dp),
                cornerRadiusDp = 24.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { showAdjustments = !showAdjustments },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = glass.cyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Penyesuaian Estetis Detail",
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.textPrimary
                            )
                        }
                        Text(
                            text = if (showAdjustments) "Sembunyikan" else "Buka",
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.cyanAccent
                        )
                    }

                    AnimatedVisibility(visible = showAdjustments) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            EnhancerSlider(
                                label = "Ketajaman HD",
                                value = state.params.sharpness,
                                valueRange = 0f..1f,
                                displayValue = "${(state.params.sharpness * 100).roundToInt()}%",
                                onValueChange = { v -> viewModel.updateParams { it.copy(sharpness = v) } }
                            )
                            EnhancerSlider(
                                label = "Kontras",
                                value = state.params.contrast,
                                valueRange = 0.7f..1.6f,
                                displayValue = "${((state.params.contrast - 1f) * 100).roundToInt()}%",
                                onValueChange = { v -> viewModel.updateParams { it.copy(contrast = v) } }
                            )
                            EnhancerSlider(
                                label = "Kecerahan",
                                value = state.params.brightness,
                                valueRange = -0.3f..0.3f,
                                displayValue = "${(state.params.brightness * 100).roundToInt()}%",
                                onValueChange = { v -> viewModel.updateParams { it.copy(brightness = v) } }
                            )
                            EnhancerSlider(
                                label = "Saturasi",
                                value = state.params.saturation,
                                valueRange = 0f..1.8f,
                                displayValue = "${(state.params.saturation * 100).roundToInt()}%",
                                onValueChange = { v -> viewModel.updateParams { it.copy(saturation = v) } }
                            )
                            EnhancerSlider(
                                label = "Kehangatan",
                                value = state.params.warmth,
                                valueRange = -0.3f..0.3f,
                                displayValue = "${(state.params.warmth * 100).roundToInt()}",
                                onValueChange = { v -> viewModel.updateParams { it.copy(warmth = v) } }
                            )
                        }
                    }
                }
            }

            // Save Button
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("save_enhanced_button"),
                shape = RoundedCornerShape(22.dp),
                cornerRadiusDp = 22.dp,
                tintColor = glass.blueAccent.copy(alpha = 0.34f),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                onClick = { viewModel.saveEnhancedToGallery(context) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = glass.textPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.SaveAlt,
                            contentDescription = "Simpan ke Galeri",
                            tint = glass.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Simpan Hasil HD ke Galeri",
                        style = MaterialTheme.typography.titleSmall,
                        color = glass.textPrimary
                    )
                }
            }

            if (state.saveMessage != null) {
                StatusGlassBanner(
                    text = state.saveMessage.orEmpty(),
                    accent = glass.mintAccent
                )
            }

            // Cross-Feature Workflow Handoff: HD Enhancer -> AI Chatbot Medsos
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("enhancer_to_social_button"),
                shape = RoundedCornerShape(20.dp),
                cornerRadiusDp = 20.dp,
                tintColor = glass.violetAccent.copy(alpha = 0.24f),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                onClick = {
                    val exportBitmap = state.afterBitmap ?: state.beforeBitmap
                    if (exportBitmap != null) {
                        onSendToSocialAi(exportBitmap)
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Forum,
                        contentDescription = null,
                        tint = glass.violetAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Buat Caption di AI Chatbot",
                        style = MaterialTheme.typography.labelLarge,
                        color = glass.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = glass.cyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(104.dp))
        }
    }
}

@Composable
private fun BeforeAfterComparisonCard(
    beforeBitmap: android.graphics.Bitmap?,
    afterBitmap: android.graphics.Bitmap?,
    splitRatio: Float,
    onSplitChange: (Float) -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(24.dp))
            .background(glass.background)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newRatio = (offset.x / size.width.toFloat()).coerceIn(0.05f, 0.95f)
                    onSplitChange(newRatio)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val newRatio = (change.position.x / size.width.toFloat()).coerceIn(0.05f, 0.95f)
                    onSplitChange(newRatio)
                }
            }
            .testTag("before_after_slider")
    ) {
        val containerWidth = maxWidth
        val widthPx = with(density) { containerWidth.toPx() }
        val splitX = widthPx * splitRatio

        // Base Layer: AFTER (HD Enhanced)
        if (afterBitmap != null) {
            Image(
                bitmap = afterBitmap.asImageBitmap(),
                contentDescription = "Sesudah HD",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Clipped Left Layer: BEFORE (SD / Original)
        if (beforeBitmap != null) {
            Image(
                bitmap = beforeBitmap.asImageBitmap(),
                contentDescription = "Sebelum Blur",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        clipRect(right = size.width * splitRatio) {
                            this@drawWithContent.drawContent()
                        }
                    }
            )
        }

        // Vertical luminous divider line
        Canvas(modifier = Modifier.fillMaxSize()) {
            val x = size.width * splitRatio
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        glass.cyanAccent,
                        Color.White.copy(alpha = 0.9f),
                        glass.cyanAccent
                    )
                ),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 3.dp.toPx()
            )
        }

        // Draggable Liquid Glass Handle Knob
        Box(
            modifier = Modifier
                .offset { IntOffset((splitX - 20.dp.toPx()).roundToInt(), 0) }
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.92f),
                            glass.cyanAccent.copy(alpha = 0.78f)
                        )
                    )
                )
                .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                contentDescription = "Geser perbandingan Sebelum dan Sesudah",
                tint = glass.background,
                modifier = Modifier.size(22.dp)
            )
        }

        // Labels
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.56f))
                .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "SEBELUM",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(glass.cyanAccent.copy(alpha = 0.28f))
                .border(0.8.dp, glass.cyanAccent.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "SESUDAH • HD",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EnhancerSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.labelMedium,
                color = glass.cyanAccent
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = glass.cyanAccent,
                activeTrackColor = glass.cyanAccent,
                inactiveTrackColor = Color.White.copy(alpha = 0.14f)
            )
        )
    }
}

@Composable
private fun StatusGlassBanner(
    text: String,
    accent: Color
) {
    LiquidGlassSurface(
        shape = RoundedCornerShape(16.dp),
        cornerRadiusDp = 16.dp,
        tintColor = accent.copy(alpha = 0.14f),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = LocalLiquidGlassPalette.current.textPrimary
        )
    }
}
