package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalLiquidGlassPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class RolaysDestination(
    val route: String,
    val label: String,
    val shortLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    ENHANCER(
        route = "enhancer",
        label = "Enhance HD",
        shortLabel = "Enhance HD",
        selectedIcon = Icons.Filled.AutoFixHigh,
        unselectedIcon = Icons.Outlined.AutoFixHigh,
        testTag = "nav_tab_enhancer"
    ),
    DOWNLOADER(
        route = "downloader",
        label = "Downloader",
        shortLabel = "Downloader",
        selectedIcon = Icons.Filled.Download,
        unselectedIcon = Icons.Outlined.Download,
        testTag = "nav_tab_downloader"
    ),
    SOCIAL_CHAT(
        route = "social_chat",
        label = "AI Chatbot",
        shortLabel = "AI Chatbot",
        selectedIcon = Icons.Filled.Forum,
        unselectedIcon = Icons.Outlined.Forum,
        testTag = "nav_tab_social_ai"
    ),
    GEMINI_FLASH(
        route = "gemini_flash",
        label = "Gemini 3.8 Flash",
        shortLabel = "Gemini 3.8",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_tab_gemini_flash"
    )
}

/**
 * Interactive Liquid Glass (Frosted Glass) Bottom Navigation Bar with fluid
 * press-and-hold + drag interaction logic.
 *
 * Gesture contract:
 * - Quick tap: immediately navigates to the tapped destination with a spring-animated droplet.
 * - Press-and-hold (>= 160ms, even while motionless) OR horizontal drag (> touchSlop):
 *   activates Fluid Drag Mode, swelling the refractive glass pill, emitting haptic ticks on tab
 *   crossings, stretching the droplet proportional to pointer velocity, and applying elastic
 *   edge resistance + vertical pull before snapping to the nearest tab on release.
 */
@Composable
fun InteractiveLiquidGlassNavBar(
    selectedDestination: RolaysDestination,
    onDestinationSelected: (RolaysDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val destinations = RolaysDestination.entries
    val selectedIndex = destinations.indexOf(selectedDestination).coerceAtLeast(0)
    val glass = LocalLiquidGlassPalette.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var barWidthPx by remember { mutableFloatStateOf(0f) }
    var isFluidDragging by remember { mutableStateOf(false) }
    var isPressing by remember { mutableStateOf(false) }
    var dragVelocityStretch by remember { mutableFloatStateOf(0f) }
    var lastHapticIndex by remember { mutableIntStateOf(selectedIndex) }
    var hoveredIndex by remember { mutableIntStateOf(selectedIndex) }

    val dropletPosition = remember { Animatable(selectedIndex.toFloat()) }
    val barVerticalShift = remember { Animatable(0f) }
    val barHorizontalShift = remember { Animatable(0f) }

    LaunchedEffect(selectedIndex, isFluidDragging) {
        if (!isFluidDragging) {
            hoveredIndex = selectedIndex
            lastHapticIndex = selectedIndex
            dropletPosition.animateTo(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.56f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val barScale by animateFloatAsState(
        targetValue = when {
            isFluidDragging -> 1.04f
            isPressing -> 0.985f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 380f),
        label = "bar_scale"
    )

    val dropletScale by animateFloatAsState(
        targetValue = if (isFluidDragging) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = 0.44f, stiffness = 360f),
        label = "droplet_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating destination indicator chip visible during fluid drag-and-hold
        AnimatedVisibility(
            visible = isFluidDragging,
            enter = fadeIn() + scaleIn(initialScale = 0.88f),
            exit = fadeOut() + scaleOut(targetScale = 0.90f)
        ) {
            val previewDest = destinations[hoveredIndex.coerceIn(0, destinations.lastIndex)]
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xCC0D1730),
                                Color(0xCC16254A)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(glass.cyanAccent.copy(alpha = 0.75f), glass.violetAccent.copy(alpha = 0.5f))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = previewDest.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.cyanAccent
                )
            }
        }

        val barShape = RoundedCornerShape(32.dp)
        val borderBrush = Brush.linearGradient(
            colors = listOf(
                if (isFluidDragging) glass.cyanAccent.copy(alpha = 0.90f) else glass.borderBright,
                glass.cyanAccent.copy(alpha = 0.40f),
                glass.borderSoft,
                glass.violetAccent.copy(alpha = 0.45f)
            )
        )

        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .height(74.dp)
                .offset {
                    IntOffset(
                        x = barHorizontalShift.value.roundToInt(),
                        y = barVerticalShift.value.roundToInt()
                    )
                }
                .scale(barScale)
                .clip(barShape)
                .onSizeChanged { barWidthPx = it.width.toFloat() }
                .pointerInput(destinations.size) {
                    val touchSlop = viewConfiguration.touchSlop
                    val longPressDelayMs = 160L
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressing = true
                        val startX = down.position.x
                        val startY = down.position.y
                        var dragging = false
                        var prevX = startX

                        // Timer-triggered press-and-hold activation even when finger stays still
                        val holdTriggerJob = coroutineScope.launch {
                            delay(longPressDelayMs)
                            if (isPressing && !dragging) {
                                dragging = true
                                isFluidDragging = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (barWidthPx > 0f) {
                                    val segmentWidth = barWidthPx / destinations.size
                                    val initialIdx = (startX / segmentWidth).toInt()
                                        .coerceIn(0, destinations.lastIndex)
                                    hoveredIndex = initialIdx
                                    lastHapticIndex = initialIdx
                                    dropletPosition.animateTo(
                                        initialIdx.toFloat(),
                                        spring(dampingRatio = 0.6f, stiffness = 450f)
                                    )
                                }
                            }
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }

                            val dxTotal = abs(change.position.x - startX)
                            val dyTotal = change.position.y - startY

                            if (!dragging && dxTotal > touchSlop) {
                                dragging = true
                                holdTriggerJob.cancel()
                                isFluidDragging = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            if (dragging && barWidthPx > 0f) {
                                change.consume()
                                val segmentWidth = barWidthPx / destinations.size
                                val unclamped = (change.position.x / segmentWidth) - 0.5f
                                val rawFraction = unclamped.coerceIn(-0.16f, (destinations.size - 1) + 0.16f)

                                // Elastic edge pull resistance if user drags past left/right bounds
                                val overscrollX = when {
                                    unclamped < 0f -> (unclamped * 24f).coerceAtLeast(-22f)
                                    unclamped > destinations.lastIndex ->
                                        ((unclamped - destinations.lastIndex) * 24f).coerceAtMost(22f)
                                    else -> 0f
                                }

                                val deltaX = change.position.x - prevX
                                prevX = change.position.x
                                dragVelocityStretch = (abs(deltaX) / 20f).coerceIn(0f, 0.30f)

                                // Elastic vertical lift/pull feedback while holding & dragging
                                val dampedY = (dyTotal * 0.20f).coerceIn(-20f, 14f)

                                coroutineScope.launch {
                                    dropletPosition.snapTo(rawFraction)
                                    barVerticalShift.snapTo(dampedY)
                                    barHorizontalShift.snapTo(overscrollX)
                                }

                                val currentHover = rawFraction.roundToInt().coerceIn(0, destinations.lastIndex)
                                hoveredIndex = currentHover
                                if (currentHover != lastHapticIndex) {
                                    lastHapticIndex = currentHover
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }

                        holdTriggerJob.cancel()
                        isPressing = false
                        dragVelocityStretch = 0f

                        if (dragging) {
                            isFluidDragging = false
                            val targetIdx = dropletPosition.value.roundToInt().coerceIn(0, destinations.lastIndex)
                            onDestinationSelected(destinations[targetIdx])
                            coroutineScope.launch {
                                barVerticalShift.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.44f, stiffness = 400f)
                                )
                            }
                            coroutineScope.launch {
                                barHorizontalShift.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.44f, stiffness = 400f)
                                )
                            }
                            coroutineScope.launch {
                                dropletPosition.animateTo(
                                    targetIdx.toFloat(),
                                    spring(dampingRatio = 0.50f, stiffness = 360f)
                                )
                            }
                        } else if (barWidthPx > 0f) {
                            val segmentWidth = barWidthPx / destinations.size
                            val tappedIdx = (startX / segmentWidth).toInt().coerceIn(0, destinations.lastIndex)
                            onDestinationSelected(destinations[tappedIdx])
                        }
                    }
                }
                .testTag("liquid_glass_nav_bar")
        ) {
            val totalWidth = constraints.maxWidth.toFloat()
            val slotWidthPx = totalWidth / destinations.size
            val slotWidthDp = with(density) { slotWidthPx.toDp() }
            val currentFrac = dropletPosition.value
            val stretchFactor = 1f + dragVelocityStretch
            val dropletCenterX = (currentFrac + 0.5f) * slotWidthPx

            // Layer 1: Frosted Glass Backdrop Blur + Refractive Caustic Sheen
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(22.dp)
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                glass.cyanAccent.copy(alpha = 0.16f),
                                Color(0xFF0E1834).copy(alpha = 0.72f),
                                glass.violetAccent.copy(alpha = 0.14f)
                            )
                        )
                    )
            )

            // Layer 2: Translucent Liquid Glass Overlay & Top Specular Edge
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color(0xFF0A1124).copy(alpha = 0.56f),
                            Color(0xFF060B18).copy(alpha = 0.76f)
                        )
                    ),
                    cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx())
                )
                // Dynamic caustic light follow behind active droplet
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glass.cyanAccent.copy(alpha = if (isFluidDragging) 0.34f else 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(dropletCenterX, size.height * 0.5f),
                        radius = slotWidthPx * 0.9f
                    ),
                    center = Offset(dropletCenterX, size.height * 0.5f),
                    radius = slotWidthPx * 0.9f
                )
            }

            // Layer 3: Outer Glass Specular Rim Border
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = if (isFluidDragging) 1.5.dp else 1.dp,
                        brush = borderBrush,
                        shape = barShape
                    )
            )

            // Layer 4: Elastic Morphing Liquid Droplet Indicator
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (currentFrac * slotWidthPx).roundToInt(),
                            y = 0
                        )
                    }
                    .width(slotWidthDp)
                    .fillMaxHeight()
                    .padding(6.dp)
                    .graphicsLayer {
                        scaleX = dropletScale * stretchFactor
                        scaleY = dropletScale * (1f - dragVelocityStretch * 0.24f)
                    }
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                glass.cyanAccent.copy(alpha = if (isFluidDragging) 0.44f else 0.28f),
                                glass.blueAccent.copy(alpha = 0.26f),
                                glass.violetAccent.copy(alpha = if (isFluidDragging) 0.38f else 0.24f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.68f),
                                glass.cyanAccent.copy(alpha = 0.26f)
                            )
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .drawBehind {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.28f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.35f, size.height * 0.2f),
                                radius = size.width * 0.72f
                            ),
                            cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx())
                        )
                    }
            )

            // Layer 5: Destination Icons & Labels
            Row(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEachIndexed { index, dest ->
                    val distance = abs(currentFrac - index)
                    val activeRatio = (1f - distance).coerceIn(0f, 1f)
                    val isSelected = (index == selectedIndex && !isFluidDragging) ||
                        (isFluidDragging && index == hoveredIndex)
                    val tint by animateColorAsState(
                        targetValue = if (isSelected) glass.textPrimary else glass.textSecondary.copy(alpha = 0.74f),
                        label = "tab_tint_${dest.route}"
                    )
                    val isGeminiTab = dest == RolaysDestination.GEMINI_FLASH

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .minimumInteractiveComponentSize()
                            .testTag(dest.testTag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = dest.label,
                            tint = if (isSelected && isGeminiTab) glass.cyanAccent else tint,
                            modifier = Modifier.size((21f + 3f * activeRatio).dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = dest.shortLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected && isGeminiTab) glass.cyanAccent else tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Adaptive Liquid Glass Navigation Rail for Expanded (tablet/foldable) screens.
 */
@Composable
fun LiquidGlassNavRail(
    selectedDestination: RolaysDestination,
    onDestinationSelected: (RolaysDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassPalette.current
    val destinations = RolaysDestination.entries

    LiquidGlassSurface(
        modifier = modifier
            .padding(start = 12.dp, top = 16.dp, bottom = 16.dp)
            .width(104.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(30.dp),
        cornerRadiusDp = 30.dp,
        elevated = true,
        contentPadding = PaddingValues(vertical = 20.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "ROLAYS",
                style = MaterialTheme.typography.labelMedium,
                color = glass.cyanAccent
            )
            Spacer(modifier = Modifier.height(8.dp))
            destinations.forEach { dest ->
                val selected = dest == selectedDestination
                val interactionSource = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (selected) {
                                Brush.linearGradient(
                                    listOf(
                                        glass.cyanAccent.copy(alpha = 0.30f),
                                        glass.violetAccent.copy(alpha = 0.24f)
                                    )
                                )
                            } else {
                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .border(
                            width = if (selected) 1.dp else 0.dp,
                            color = if (selected) Color.White.copy(alpha = 0.45f) else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(color = glass.cyanAccent),
                            onClick = { onDestinationSelected(dest) }
                        )
                        .padding(vertical = 12.dp, horizontal = 6.dp)
                        .testTag(dest.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = dest.label,
                        tint = if (selected) glass.cyanAccent else glass.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dest.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) glass.textPrimary else glass.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
