package com.example.feature.geminiflash

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.LocalLiquidGlassPalette

@Composable
fun GeminiFlashScreen(
    modifier: Modifier = Modifier,
    viewModel: GeminiFlashViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val glass = LocalLiquidGlassPalette.current
    val listState = rememberLazyListState()
    var showSystemConfig by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachImage(context, uri)
        }
    }

    LaunchedEffect(state.entries.size, state.entries.lastOrNull()?.text) {
        if (state.entries.isNotEmpty()) {
            listState.animateScrollToItem(state.entries.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            // Dedicated Independent Header Banner
            LiquidGlassSurface(
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                cornerRadiusDp = 22.dp,
                tintColor = glass.cyanAccent.copy(alpha = 0.16f),
                elevated = true,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(glass.cyanAccent.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Gemini 3.8 Flash",
                                tint = glass.cyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Gemini 3.8 Flash AI",
                                style = MaterialTheme.typography.titleLarge,
                                color = glass.textPrimary
                            )
                            Text(
                                text = "MODUL INDEPENDEN • ENGINE TERISOLASI",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.cyanAccent
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { showSystemConfig = !showSystemConfig },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("flash_system_config_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Atur instruksi sistem",
                                tint = if (showSystemConfig) glass.cyanAccent else glass.textSecondary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.resetIndependentSession() },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("flash_reset_session")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Reset sesi Gemini 3.8",
                                tint = glass.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Independent Engine Mode Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GeminiFlashEngineMode.entries) { mode ->
                    val selected = state.selectedMode == mode
                    LiquidGlassSurface(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("flash_mode_${mode.name.lowercase()}"),
                        shape = RoundedCornerShape(16.dp),
                        cornerRadiusDp = 16.dp,
                        tintColor = if (selected) glass.cyanAccent.copy(alpha = 0.28f) else null,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        onClick = { viewModel.selectMode(mode) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = if (selected) glass.cyanAccent else glass.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) glass.textPrimary else glass.textSecondary
                            )
                        }
                    }
                }
            }

            // Collapsible Custom System Instruction Panel
            AnimatedVisibility(visible = showSystemConfig) {
                LiquidGlassSurface(
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    cornerRadiusDp = 18.dp,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Instruksi Sistem Kustom (Opsional)",
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.cyanAccent
                        )
                        OutlinedTextField(
                            value = state.customSystemInstruction,
                            onValueChange = { viewModel.updateCustomSystemInstruction(it) },
                            placeholder = {
                                Text(
                                    text = "Misal: Jawab singkat dalam poin teknis Kotlin...",
                                    color = glass.textMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            maxLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = glass.cyanAccent,
                                unfocusedBorderColor = glass.borderSoft,
                                focusedTextColor = glass.textPrimary,
                                unfocusedTextColor = glass.textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Conversation Stream
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (entry.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        LiquidGlassSurface(
                            modifier = Modifier.widthIn(max = 520.dp),
                            shape = RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = if (entry.isUser) 20.dp else 6.dp,
                                bottomEnd = if (entry.isUser) 6.dp else 20.dp
                            ),
                            cornerRadiusDp = 20.dp,
                            tintColor = when {
                                entry.isError -> glass.roseAccent.copy(alpha = 0.22f)
                                entry.isUser -> glass.violetAccent.copy(alpha = 0.26f)
                                else -> null
                            },
                            elevated = !entry.isUser,
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!entry.isUser) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "GEMINI 3.8 FLASH",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glass.cyanAccent
                                        )
                                        if (entry.latencyMs != null) {
                                            Text(
                                                text = "${entry.latencyMs} ms",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = glass.mintAccent
                                            )
                                        }
                                    }
                                } else if (entry.hasImage) {
                                    Text(
                                        text = "+ LAMPIRAN VISUAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.cyanAccent
                                    )
                                }

                                if (entry.isStreaming && entry.text.isBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = glass.cyanAccent
                                        )
                                        Text(
                                            text = "Inferensi Gemini 3.8 Flash...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = glass.textSecondary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = entry.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = glass.textPrimary
                                    )
                                }

                                if (!entry.isUser && !entry.isStreaming && !entry.isError && entry.text.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        IconButton(
                                            onClick = { clipboard.setText(AnnotatedString(entry.text)) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ContentCopy,
                                                contentDescription = "Salin jawaban",
                                                tint = glass.cyanAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.attachedImage != null) {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        Image(
                            bitmap = state.attachedImage!!.asImageBitmap(),
                            contentDescription = "Gambar multimodal",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        IconButton(
                            onClick = { viewModel.removeAttachedImage() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Hapus gambar",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Text(
                        text = "Input visual multimodal siap dikirim",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.cyanAccent
                    )
                }
            }

            // Dedicated Prompt Input
            LiquidGlassSurface(
                shape = RoundedCornerShape(24.dp),
                cornerRadiusDp = 24.dp,
                elevated = true,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("flash_attach_image")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Unggah gambar ke Gemini 3.8",
                            tint = glass.cyanAccent
                        )
                    }

                    OutlinedTextField(
                        value = state.promptInput,
                        onValueChange = { viewModel.updatePrompt(it) },
                        placeholder = {
                            Text(
                                text = "Tanya atau perintahkan Gemini 3.8 Flash...",
                                color = glass.textMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = glass.textPrimary,
                            unfocusedTextColor = glass.textPrimary,
                            cursorColor = glass.cyanAccent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("flash_prompt_input")
                    )

                    IconButton(
                        onClick = { viewModel.sendToGeminiFlash() },
                        enabled = !state.isRunning &&
                            (state.promptInput.isNotBlank() || state.attachedImage != null),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("flash_send_button")
                    ) {
                        if (state.isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = glass.cyanAccent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Kirim ke Gemini 3.8 Flash",
                                tint = glass.cyanAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(104.dp))
        }
    }
}
