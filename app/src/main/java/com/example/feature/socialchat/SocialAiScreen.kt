package com.example.feature.socialchat

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
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

private val QuickSocialPrompts = listOf(
    "Caption Viral" to "Buatkan 3 variasi caption Instagram/TikTok yang engaging dengan hook kuat dan hashtag relevan tentang produk lifestyle.",
    "Balas DM" to "Buatkan 3 template balasan DM/WhatsApp yang ramah, profesional, dan cepat closing untuk pelanggan yang menanyakan harga.",
    "Skrip TikTok" to "Buatkan skrip video TikTok 30 detik (Hook 3 detik, Isi, Call-to-Action) untuk konten review gadget estetik.",
    "Thread X" to "Buatkan thread X (Twitter) 5 bagian yang ringkas, informatif, dan memancing diskusi tentang tren AI kreatif."
)

@Composable
fun SocialAiScreen(
    modifier: Modifier = Modifier,
    viewModel: SocialAiViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val glass = LocalLiquidGlassPalette.current
    val listState = rememberLazyListState()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachImage(context, uri)
        }
    }

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.text) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Social AI Copilot",
                        style = MaterialTheme.typography.headlineMedium,
                        color = glass.textPrimary
                    )
                    Text(
                        text = "Konten, balasan pesan & otomasi lintas platform",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textSecondary
                    )
                }
                IconButton(
                    onClick = { viewModel.clearChat() },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("social_clear_chat")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Bersihkan chat",
                        tint = glass.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Platform Selector Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SocialPlatform.entries) { platform ->
                    val selected = state.selectedPlatform == platform
                    LiquidGlassSurface(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("social_platform_${platform.name.lowercase()}"),
                        shape = RoundedCornerShape(16.dp),
                        cornerRadiusDp = 16.dp,
                        tintColor = if (selected) glass.cyanAccent.copy(alpha = 0.28f) else null,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        onClick = { viewModel.selectPlatform(platform) }
                    ) {
                        Text(
                            text = platform.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) glass.textPrimary else glass.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Automation Prompts
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(QuickSocialPrompts) { (label, prompt) ->
                    LiquidGlassSurface(
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(14.dp),
                        cornerRadiusDp = 14.dp,
                        tintColor = glass.violetAccent.copy(alpha = 0.16f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        onClick = { viewModel.sendPrompt(prompt) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        LiquidGlassSurface(
                            modifier = Modifier.widthIn(max = 500.dp),
                            shape = RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = if (msg.isUser) 20.dp else 6.dp,
                                bottomEnd = if (msg.isUser) 6.dp else 20.dp
                            ),
                            cornerRadiusDp = 20.dp,
                            tintColor = when {
                                msg.isError -> glass.roseAccent.copy(alpha = 0.20f)
                                msg.isUser -> glass.blueAccent.copy(alpha = 0.30f)
                                else -> null
                            },
                            elevated = !msg.isUser,
                            contentPadding = PaddingValues(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (msg.platformTag != null) {
                                    Text(
                                        text = "${msg.platformTag}${if (msg.hasImageAttachment) " • +Foto" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.cyanAccent
                                    )
                                }
                                if (msg.isStreaming && msg.text.isBlank()) {
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
                                            text = "Menyusun respons...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = glass.textSecondary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = glass.textPrimary
                                    )
                                }

                                if (!msg.isUser && !msg.isStreaming && !msg.isError && msg.text.isNotBlank()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { clipboard.setText(AnnotatedString(msg.text)) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ContentCopy,
                                                contentDescription = "Salin teks",
                                                tint = glass.cyanAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, msg.text)
                                                }
                                                context.startActivity(
                                                    Intent.createChooser(sendIntent, "Bagikan ke Media Sosial")
                                                )
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Share,
                                                contentDescription = "Bagikan ke media sosial",
                                                tint = glass.textSecondary,
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

            // Attached Image Thumbnail Preview
            if (state.attachedBitmap != null) {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        Image(
                            bitmap = state.attachedBitmap!!.asImageBitmap(),
                            contentDescription = "Lampiran gambar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        IconButton(
                            onClick = { viewModel.clearAttachedImage() },
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
                        text = "Foto siap dianalisis untuk konten/balasan",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.cyanAccent
                    )
                }
            }

            // Composer Bar
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
                            .testTag("social_attach_image")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Lampirkan foto",
                            tint = glass.cyanAccent
                        )
                    }

                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = { viewModel.updateInput(it) },
                        placeholder = {
                            Text(
                                text = "Perintah konten / balas chat...",
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
                            .testTag("social_chat_input")
                    )

                    IconButton(
                        onClick = { viewModel.sendPrompt() },
                        enabled = !state.isGenerating &&
                            (state.inputText.isNotBlank() || state.attachedBitmap != null),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("social_send_button")
                    ) {
                        if (state.isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = glass.cyanAccent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Kirim perintah",
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
