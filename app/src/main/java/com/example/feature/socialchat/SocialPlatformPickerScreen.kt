package com.example.feature.socialchat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalLiquidGlassPalette

@Composable
fun SocialPlatformPickerScreen(
    searchQuery: String,
    showExtraPlatforms: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleExtraPlatforms: () -> Unit,
    onPlatformSelected: (SocialPlatformSpec) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    hasWorkflowPayload: Boolean = false,
    workflowStepBar: @Composable () -> Unit = {}
) {
    val glass = LocalLiquidGlassPalette.current
    val baseList = if (showExtraPlatforms || searchQuery.isNotBlank()) {
        SocialPlatformCatalog.allPlatforms
    } else {
        SocialPlatformCatalog.corePlatforms
    }
    val filteredPlatforms = remember(baseList, searchQuery) {
        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter {
                it.title.lowercase().contains(q) || it.subtitle.lowercase().contains(q)
            }
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
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Back circle + Liquid Glass ROLAYS Logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                        .clickable(onClick = onBackPressed)
                        .testTag("picker_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Kembali",
                        tint = glass.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_rolays_icon_fg),
                        contentDescription = "Logo ROLAYS",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(0.8.dp, glass.cyanAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    )
                    Text(
                        text = "ROLAYS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            brush = Brush.horizontalGradient(
                                listOf(glass.cyanAccent, glass.textPrimary, glass.violetAccent)
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Search Bar ("Cari platform medsos...")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.11f),
                                Color(0xFF0E162B).copy(alpha = 0.72f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.28f), glass.cyanAccent.copy(alpha = 0.18f))
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "Cari platform medsos...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Cari platform",
                            tint = glass.textSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Hapus pencarian",
                                    tint = glass.textSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = glass.textPrimary,
                        unfocusedTextColor = glass.textPrimary,
                        cursorColor = glass.cyanAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("social_platform_search_input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Subtitle
            Text(
                text = "Pilih Chatbot Medsos",
                style = MaterialTheme.typography.headlineMedium,
                color = glass.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pilih platform untuk memulai asisten AI khusus medsos Anda",
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            workflowStepBar()

            AnimatedVisibility(visible = hasWorkflowPayload) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(glass.violetAccent.copy(alpha = 0.22f))
                        .border(1.dp, glass.cyanAccent.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Media/draf dari alur sebelumnya siap — pilih platform medsos di bawah untuk langsung membuat konten!",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Column Liquid Glass Platform Grid (3 cols x 4 rows for core 12 platforms)
            if (filteredPlatforms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Platform tidak ditemukan. Coba kata kunci lain atau gunakan Universal AI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val rows = filteredPlatforms.chunked(3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { platform ->
                                LiquidGlassPlatformGridCard(
                                    spec = platform,
                                    onClick = { onPlatformSelected(platform) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Wide Glowing Universal AI Button ("✨ Semua Medsos (Universal AI)")
            UniversalSocialAiCard(
                onClick = { onPlatformSelected(SocialPlatformCatalog.universalPlatform) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive "Lihat platform lainnya..." toggle
            Text(
                text = if (showExtraPlatforms) {
                    "Sembunyikan platform tambahan"
                } else {
                    "Lihat platform lainnya..."
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = glass.textSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleExtraPlatforms)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("toggle_extra_platforms")
            )

            AnimatedVisibility(visible = showExtraPlatforms) {
                Text(
                    text = "Menampilkan platform tambahan (Snapchat, Twitch, LINE)",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.cyanAccent,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(108.dp))
        }
    }
}

@Composable
private fun LiquidGlassPlatformGridCard(
    spec: SocialPlatformSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = Spring.StiffnessMedium),
        label = "card_press_${spec.id}"
    )

    val shape = RoundedCornerShape(20.dp)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            if (isPressed) spec.primaryColor.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.36f),
            spec.primaryColor.copy(alpha = if (isPressed) 0.55f else 0.20f),
            Color.White.copy(alpha = 0.10f),
            spec.secondaryColor.copy(alpha = 0.25f)
        )
    )

    Box(
        modifier = modifier
            .aspectRatio(0.86f)
            .scale(scale)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.20f else 0.13f),
                        Color(0xFF141D35).copy(alpha = 0.68f),
                        Color(0xFF0A1020).copy(alpha = 0.86f)
                    )
                ),
                shape = shape
            )
            .drawBehind {
                // Soft inner neon refraction behind platform icon
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            spec.primaryColor.copy(alpha = if (isPressed) 0.32f else 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.30f),
                        radius = size.width * 0.55f
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.30f),
                    radius = size.width * 0.55f
                )
                // Glossy top-left glass catchlight
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.6f, size.height * 0.4f)
                    ),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                )
            }
            .border(width = if (isPressed) 1.4.dp else 1.dp, brush = borderBrush, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = spec.primaryColor),
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("platform_card_${spec.id}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SocialPlatformIconBadge(spec = spec, size = 44.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = spec.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 15.sp
                ),
                color = glass.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = spec.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                ),
                color = glass.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UniversalSocialAiCard(
    onClick: () -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "universal_press"
    )
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .scale(scale)
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        glass.violetAccent.copy(alpha = 0.34f),
                        Color(0xFF1D2849).copy(alpha = 0.80f),
                        glass.cyanAccent.copy(alpha = 0.32f)
                    )
                ),
                shape = shape
            )
            .border(
                width = 1.1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.60f),
                        glass.cyanAccent.copy(alpha = 0.45f),
                        glass.violetAccent.copy(alpha = 0.50f)
                    )
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = glass.cyanAccent),
                onClick = onClick
            )
            .padding(vertical = 14.dp, horizontal = 18.dp)
            .testTag("platform_card_universal"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = glass.cyanAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Semua Medsos (Universal AI)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = glass.textPrimary
            )
        }
    }
}
