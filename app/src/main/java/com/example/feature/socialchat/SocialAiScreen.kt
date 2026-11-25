package com.example.feature.socialchat

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
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
import com.example.ui.components.WorkflowStepBar
import com.example.ui.theme.LocalLiquidGlassPalette

@Composable
fun SocialAiScreen(
    modifier: Modifier = Modifier,
    onExitToHome: () -> Unit = {},
    viewModel: SocialAiViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = !state.isPickerVisible) {
        viewModel.backToPlatformPicker()
    }

    AnimatedContent(
        targetState = state.isPickerVisible,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            if (targetState) {
                (fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { -it / 6 }) togetherWith
                    (fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 6 })
            } else {
                (fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 6 }) togetherWith
                    (fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { -it / 6 })
            }
        },
        label = "social_hub_to_chat_transition"
    ) { showPicker ->
        if (showPicker) {
            val hasPayload = state.attachedBitmap != null || state.inputText.isNotBlank()
            SocialPlatformPickerScreen(
                searchQuery = state.searchQuery,
                showExtraPlatforms = state.showExtraPlatforms,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onToggleExtraPlatforms = { viewModel.toggleExtraPlatforms() },
                onPlatformSelected = { viewModel.openPlatformChat(it) },
                onBackPressed = onExitToHome,
                hasWorkflowPayload = hasPayload,
                workflowStepBar = {
                    WorkflowStepBar(
                        steps = listOf("Pilih Medsos", "Chat AI", "Salin/Bagikan"),
                        currentStepIndex = 0,
                        onStepClick = { step ->
                            if (step == 1) {
                                viewModel.openPlatformChat(state.selectedPlatform)
                            }
                        }
                    )
                }
            )
        } else {
            SocialPlatformChatRoom(
                state = state,
                onBackToPicker = { viewModel.backToPlatformPicker() },
                onClearChat = { viewModel.clearCurrentPlatformChat() },
                onUpdateInput = { viewModel.updateInput(it) },
                onAttachImage = { ctx, uri -> viewModel.attachImage(ctx, uri) },
                onClearImage = { viewModel.clearAttachedImage() },
                onSendPrompt = { viewModel.sendPrompt(it) }
            )
        }
    }
}

@Composable
private fun SocialPlatformChatRoom(
    state: SocialAiUiState,
    onBackToPicker: () -> Unit,
    onClearChat: () -> Unit,
    onUpdateInput: (String) -> Unit,
    onAttachImage: (android.content.Context, android.net.Uri) -> Unit,
    onClearImage: () -> Unit,
    onSendPrompt: (String?) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val glass = LocalLiquidGlassPalette.current
    val listState = rememberLazyListState()
    val platform = state.selectedPlatform
    val messages = state.activeMessages

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAttachImage(context, uri)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val lastMsgBuckets = (messages.lastOrNull()?.text?.length ?: 0) / 80
    LaunchedEffect(lastMsgBuckets) {
        if (messages.isNotEmpty() && lastMsgBuckets > 0) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            // Platform-specific Chat Header with Back to 3x4 Picker Grid
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                            .clickable(onClick = onBackToPicker)
                            .testTag("chat_back_to_picker"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Kembali ke daftar platform",
                            tint = glass.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    SocialPlatformIconBadge(spec = platform, size = 38.dp)

                    Column {
                        Text(
                            text = platform.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary
                        )
                        Text(
                            text = platform.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = platform.primaryColor
                        )
                    }
                }

                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("social_clear_chat")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Bersihkan chat",
                        tint = glass.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val hasAssistantReply = messages.size > 1 && messages.any { !it.isUser && !it.isStreaming && it.text.isNotBlank() }
            WorkflowStepBar(
                steps = listOf("Pilih Medsos", "Chat AI", "Salin/Bagikan"),
                currentStepIndex = if (hasAssistantReply) 2 else 1,
                onStepClick = { step ->
                    when (step) {
                        0 -> onBackToPicker()
                        1 -> if (!state.isGenerating && (state.inputText.isNotBlank() || state.attachedBitmap != null)) {
                            onSendPrompt(null)
                        }
                        2 -> {
                            val latestReply = messages.lastOrNull { !it.isUser && !it.isStreaming && !it.isError && it.text.isNotBlank() }
                            if (latestReply != null) {
                                clipboard.setText(AnnotatedString(latestReply.text))
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tailored Quick Prompts for the chosen Social Platform
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(platform.quickPrompts) { (label, prompt) ->
                    LiquidGlassSurface(
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(14.dp),
                        cornerRadiusDp = 14.dp,
                        tintColor = platform.primaryColor.copy(alpha = 0.16f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        onClick = { onSendPrompt(prompt) }
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
                items(messages, key = { it.id }) { msg ->
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
                                msg.isUser -> platform.primaryColor.copy(alpha = 0.24f)
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
                                        color = platform.primaryColor
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
                                            color = platform.primaryColor
                                        )
                                        Text(
                                            text = "Menyusun respons ${platform.title}...",
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
                                                    Intent.createChooser(sendIntent, "Bagikan ke ${platform.title}")
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

            if (state.attachedBitmap != null) {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        Image(
                            bitmap = state.attachedBitmap.asImageBitmap(),
                            contentDescription = "Lampiran gambar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        IconButton(
                            onClick = onClearImage,
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
                        text = "Foto siap dianalisis untuk ${platform.title}",
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
                        onValueChange = onUpdateInput,
                        placeholder = {
                            Text(
                                text = "Tanya ${platform.title}...",
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
                        onClick = { onSendPrompt(null) },
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

            Spacer(modifier = Modifier.height(108.dp))
        }
    }
}
