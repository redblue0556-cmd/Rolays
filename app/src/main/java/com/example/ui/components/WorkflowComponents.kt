package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalLiquidGlassPalette

/**
 * Compact 3-step Liquid Glass workflow indicator shown inside each feature screen
 * so users always see their current step and can tap steps to jump/trigger actions.
 */
@Composable
fun WorkflowStepBar(
    steps: List<String>,
    currentStepIndex: Int,
    onStepClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassPalette.current
    LiquidGlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        cornerRadiusDp = 18.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, label ->
                val isCompleted = index < currentStepIndex
                val isActive = index == currentStepIndex
                val pillColor by animateColorAsState(
                    targetValue = when {
                        isActive -> glass.cyanAccent.copy(alpha = 0.28f)
                        isCompleted -> glass.mintAccent.copy(alpha = 0.22f)
                        else -> Color.White.copy(alpha = 0.06f)
                    },
                    label = "step_pill_$index"
                )
                val borderColor = when {
                    isActive -> glass.cyanAccent.copy(alpha = 0.8f)
                    isCompleted -> glass.mintAccent.copy(alpha = 0.6f)
                    else -> Color.White.copy(alpha = 0.14f)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(pillColor)
                        .border(0.8.dp, borderColor, RoundedCornerShape(12.dp))
                        .then(
                            if (onStepClick != null) {
                                Modifier.clickable { onStepClick(index) }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isActive -> glass.cyanAccent
                                    isCompleted -> glass.mintAccent
                                    else -> Color.White.copy(alpha = 0.16f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color(0xFF060914),
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isActive) Color(0xFF060914) else glass.textPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isActive) glass.textPrimary else glass.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (index < steps.lastIndex) {
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    }
}

private data class WorkflowBlueprintItem(
    val number: String,
    val title: String,
    val stepsSummary: String,
    val destination: RolaysDestination,
    val icon: ImageVector,
    val accent: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowHubModalSheet(
    onDismiss: () -> Unit,
    onSelectWorkflow: (RolaysDestination) -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val workflows = listOf(
        WorkflowBlueprintItem(
            number = "ALUR 01",
            title = "HD Enhancer -> AI Medsos",
            stepsSummary = "1. Pilih Foto/Video  ->  2. AI Auto-HD & Filter  ->  3. Bandingkan Before/After  ->  4. Simpan atau kirim ke AI Chatbot.",
            destination = RolaysDestination.ENHANCER,
            icon = Icons.Filled.AutoFixHigh,
            accent = glass.cyanAccent
        ),
        WorkflowBlueprintItem(
            number = "ALUR 02",
            title = "Downloader Tanpa Watermark",
            stepsSummary = "1. Tempel Link TikTok/YouTube  ->  2. Pilih Resolusi HD/Audio  ->  3. Unduh bersih ke Downloads/ROLAYS atau olah di AI.",
            destination = RolaysDestination.DOWNLOADER,
            icon = Icons.Filled.Download,
            accent = glass.blueAccent
        ),
        WorkflowBlueprintItem(
            number = "ALUR 03",
            title = "Pilih Medsos -> Chatbot Spesifik",
            stepsSummary = "1. Pilih dari Grid 12+ Medsos  ->  2. Pakai Quick Prompt atau lampirkan foto  ->  3. Salin/Bagikan ke medsos.",
            destination = RolaysDestination.SOCIAL_CHAT,
            icon = Icons.Filled.Forum,
            accent = glass.violetAccent
        ),
        WorkflowBlueprintItem(
            number = "ALUR 04",
            title = "Gemini 3.8 Flash AI (Terpisah)",
            stepsSummary = "1. Pilih Mode Engine (Core/Koding/Analisis)  ->  2. Kirim Prompt & Gambar  ->  3. Ruang kerja AI independen.",
            destination = RolaysDestination.GEMINI_FLASH,
            icon = Icons.Filled.AutoAwesome,
            accent = glass.mintAccent
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0B1124),
        contentColor = glass.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Route,
                        contentDescription = null,
                        tint = glass.cyanAccent
                    )
                    Column {
                        Text(
                            text = "Alur Kerja ROLAYS",
                            style = MaterialTheme.typography.titleLarge,
                            color = glass.textPrimary
                        )
                        Text(
                            text = "Ketuk salah satu alur untuk langsung memulai",
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Tutup alur kerja",
                        tint = glass.textSecondary
                    )
                }
            }

            workflows.forEach { item ->
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workflow_card_${item.destination.route}"),
                    shape = RoundedCornerShape(20.dp),
                    cornerRadiusDp = 20.dp,
                    tintColor = item.accent.copy(alpha = 0.14f),
                    elevated = true,
                    contentPadding = PaddingValues(14.dp),
                    onClick = {
                        onDismiss()
                        onSelectWorkflow(item.destination)
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            item.accent.copy(alpha = 0.35f),
                                            item.accent.copy(alpha = 0.12f)
                                        )
                                    )
                                )
                                .border(1.dp, item.accent.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = item.number,
                                style = MaterialTheme.typography.labelSmall,
                                color = item.accent
                            )
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.textPrimary
                            )
                            Text(
                                text = item.stepsSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Buka alur",
                            tint = item.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
