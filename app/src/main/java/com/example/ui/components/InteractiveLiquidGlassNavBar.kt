package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import kotlinx.coroutines.CoroutineStart
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
    var pressedTabIndex by remember { mutableIntStateOf(-1) }
    var dragVelocityStretch by remember { mutableFloatStateOf(0f) }
    var lastHapticIndex by remember { mutableIntStateOf(selectedIndex) }
    var hoveredIndex by remember { mutableIntStateOf(selectedIndex) }
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentOnDestinationSelected by rememberUpdatedState(onDestinationSelected)

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
                    dampingRatio = 0.68f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val barScale by animateFloatAsState(
        targetValue = when {
            isFluidDragging -> 1.035f
            isPressing -> 0.985f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 360f),
        label = "bar_scale"
    )

    val dropletScale by animateFloatAsState(
        targetValue = if (isFluidDragging) 1.10f else 1.0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 350f),
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
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xCC1E3154),
                                Color(0xCC273E68)
                            )
                        )
                    )
                    .border(
                        width = 1.1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.85f),
                                glass.cyanAccent.copy(alpha = 0.72f),
                                glass.violetAccent.copy(alpha = 0.62f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = previewDest.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = glass.textPrimary
                )
            }
        }

        val barShape = RoundedCornerShape(36.dp)
        val borderBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isFluidDragging) 0.94f else 0.82f),
                glass.cyanAccent.copy(alpha = if (isFluidDragging) 0.85f else 0.56f),
                glass.violetAccent.copy(alpha = 0.52f),
                glass.roseAccent.copy(alpha = 0.44f),
                Color.White.copy(alpha = 0.64f)
            )
        )

        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .height(74.dp)
                .graphicsLayer {
                    translationX = barHorizontalShift.value
                    translationY = barVerticalShift.value
                    scaleX = barScale
                    scaleY = barScale
                }
                .clip(barShape)
                .onSizeChanged { barWidthPx = it.width.toFloat() }
                .pointerInput(destinations.size) {
                    val touchSlop = viewConfiguration.touchSlop
                    val longPressDelayMs = 220L
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressing = true
                        val startX = down.position.x
                        val startY = down.position.y
                        var dragging = false
                        var prevX = startX
                        val effectiveWidthPx = size.width.toFloat().takeIf { it > 0f } ?: barWidthPx

                        val initialIdx = if (effectiveWidthPx > 0f) {
                            (startX / (effectiveWidthPx / destinations.size)).toInt()
                                .coerceIn(0, destinations.lastIndex)
                        } else {
                            currentSelectedIndex
                        }
                        hoveredIndex = initialIdx
                        pressedTabIndex = initialIdx

                        val holdTriggerJob = coroutineScope.launch {
                            delay(longPressDelayMs)
                            if (isPressing && !dragging) {
                                dragging = true
                                hoveredIndex = initialIdx
                                lastHapticIndex = initialIdx
                                isFluidDragging = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                dropletPosition.animateTo(
                                    initialIdx.toFloat(),
                                    spring(dampingRatio = 0.65f, stiffness = 420f)
                                )
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

                            val dragWidthPx = size.width.toFloat().takeIf { it > 0f } ?: barWidthPx
                            if (dragging && dragWidthPx > 0f) {
                                change.consume()
                                val segmentWidth = dragWidthPx / destinations.size
                                val unclamped = (change.position.x / segmentWidth) - 0.5f
                                val rawFraction = unclamped.coerceIn(-0.16f, (destinations.size - 1) + 0.16f)

                                val overscrollX = when {
                                    unclamped < 0f -> (unclamped * 24f).coerceAtLeast(-22f)
                                    unclamped > destinations.lastIndex ->
                                        ((unclamped - destinations.lastIndex) * 24f).coerceAtMost(22f)
                                    else -> 0f
                                }

                                val deltaX = change.position.x - prevX
                                prevX = change.position.x
                                dragVelocityStretch = (abs(deltaX) / 22f).coerceIn(0f, 0.26f)
                                val dampedY = (dyTotal * 0.20f).coerceIn(-20f, 14f)

                                coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                    dropletPosition.snapTo(rawFraction)
                                    barVerticalShift.snapTo(dampedY)
                                    barHorizontalShift.snapTo(overscrollX)
                                }

                                val currentHover = rawFraction.roundToInt().coerceIn(0, destinations.lastIndex)
                                hoveredIndex = currentHover
                                pressedTabIndex = currentHover
                                if (currentHover != lastHapticIndex) {
                                    lastHapticIndex = currentHover
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }

                        holdTriggerJob.cancel()
                        isPressing = false
                        pressedTabIndex = -1
                        dragVelocityStretch = 0f

                        val releaseWidthPx = size.width.toFloat().takeIf { it > 0f } ?: barWidthPx
                        if (dragging) {
                            isFluidDragging = false
                            val targetIdx = hoveredIndex.coerceIn(0, destinations.lastIndex)
                            currentOnDestinationSelected(destinations[targetIdx])
                            coroutineScope.launch {
                                barVerticalShift.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.55f, stiffness = 380f)
                                )
                            }
                            coroutineScope.launch {
                                barHorizontalShift.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.55f, stiffness = 380f)
                                )
                            }
                            coroutineScope.launch {
                                dropletPosition.animateTo(
                                    targetIdx.toFloat(),
                                    spring(dampingRatio = 0.65f, stiffness = 360f)
                                )
                            }
                        } else if (releaseWidthPx > 0f) {
                            val segmentWidth = releaseWidthPx / destinations.size
                            val tappedIdx = (startX / segmentWidth).toInt().coerceIn(0, destinations.lastIndex)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentOnDestinationSelected(destinations[tappedIdx])
                        }
                    }
                }
                .testTag("liquid_glass_nav_bar")
        ) {
            val totalWidth = constraints.maxWidth.toFloat()
            val slotWidthPx = totalWidth / destinations.size
            val slotWidthDp = with(density) { slotWidthPx.toDp() }
            val stretchFactor = 1f + dragVelocityStretch

            // Layer 1: Lightweight Frosted Sky-Mist Gradient Backdrop (no offscreen per-frame blur)
            val backdropBrush = remember(glass) {
                Brush.linearGradient(
                    colors = listOf(
                        glass.cyanAccent.copy(alpha = 0.22f),
                        Color(0xFF1E3052).copy(alpha = 0.64f),
                        glass.violetAccent.copy(alpha = 0.20f)
                    )
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(brush = backdropBrush)
            )

            // Layer 2: Translucent Balloon-Glass Overlay, Top Gloss Crescent & Draw-Only Caustic Follow
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radiusPx = 36.dp.toPx()
                val dropletCenterX = (dropletPosition.value + 0.5f) * slotWidthPx
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.24f),
                            Color(0xFF1D3052).copy(alpha = 0.46f),
                            Color(0xFF13223D).copy(alpha = 0.62f)
                        )
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
                drawOval(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.26f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(size.width * 0.05f, -size.height * 0.16f),
                    size = Size(size.width * 0.90f, size.height * 0.48f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glass.cyanAccent.copy(alpha = if (isFluidDragging) 0.40f else 0.22f),
                            glass.violetAccent.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(dropletCenterX, size.height * 0.5f),
                        radius = slotWidthPx * 0.95f
                    ),
                    center = Offset(dropletCenterX, size.height * 0.5f),
                    radius = slotWidthPx * 0.95f
                )
            }

            // Layer 3: Outer Soap-Bubble Iridescent Rim Border
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = if (isFluidDragging) 1.6.dp else 1.15.dp,
                        brush = borderBrush,
                        shape = barShape
                    )
            )

            // Layer 4: Elastic Morphing Soap-Bubble Droplet Indicator (layout + draw phase only)
            val dropletFillBrush = remember(glass, isFluidDragging) {
                Brush.linearGradient(
                    colors = listOf(
                        glass.cyanAccent.copy(alpha = if (isFluidDragging) 0.50f else 0.34f),
                        Color.White.copy(alpha = 0.18f),
                        glass.violetAccent.copy(alpha = if (isFluidDragging) 0.44f else 0.30f),
                        glass.roseAccent.copy(alpha = if (isFluidDragging) 0.32f else 0.20f)
                    )
                )
            }
            val dropletRimBrush = remember(glass) {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.88f),
                        glass.cyanAccent.copy(alpha = 0.52f),
                        glass.violetAccent.copy(alpha = 0.45f),
                        glass.roseAccent.copy(alpha = 0.36f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (dropletPosition.value * slotWidthPx).roundToInt(),
                            y = 0
                        )
                    }
                    .width(slotWidthDp)
                    .fillMaxHeight()
                    .padding(6.dp)
                    .graphicsLayer {
                        scaleX = dropletScale * stretchFactor
                        scaleY = dropletScale * (1f - dragVelocityStretch * 0.22f)
                    }
                    .clip(RoundedCornerShape(30.dp))
                    .background(brush = dropletFillBrush)
                    .border(
                        width = 1.15.dp,
                        brush = dropletRimBrush,
                        shape = RoundedCornerShape(30.dp)
                    )
                    .drawWithCache {
                        val topHighlight = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.44f),
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                        val bottomGlow = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                glass.cyanAccent.copy(alpha = 0.18f)
                            )
                        )
                        val topOvalPos = Offset(size.width * 0.10f, 3.dp.toPx())
                        val topOvalSize = Size(size.width * 0.80f, size.height * 0.34f)
                        val dotPos = Offset(size.width * 0.16f, 5.dp.toPx())
                        val dotSize = Size(14.dp.toPx(), 5.dp.toPx())
                        val bottomPos = Offset(size.width * 0.18f, size.height * 0.72f)
                        val bottomSize = Size(size.width * 0.64f, size.height * 0.28f)

                        onDrawBehind {
                            drawOval(brush = topHighlight, topLeft = topOvalPos, size = topOvalSize)
                            drawOval(color = Color.White.copy(alpha = 0.55f), topLeft = dotPos, size = dotSize)
                            drawOval(brush = bottomGlow, topLeft = bottomPos, size = bottomSize)
                        }
                    }
            )

            // Layer 5: Destination Icons & Labels with per-tab spring press bounce
            Row(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEachIndexed { index, dest ->
                    val isSelected = (index == selectedIndex && !isFluidDragging) ||
                        (isFluidDragging && index == hoveredIndex)
                    val isTabPressed = pressedTabIndex == index
                    val tabPressScale by animateFloatAsState(
                        targetValue = when {
                            isTabPressed -> 0.88f
                            isSelected -> 1.05f
                            else -> 1f
                        },
                        animationSpec = spring(dampingRatio = 0.60f, stiffness = Spring.StiffnessMediumLow),
                        label = "tab_press_scale_${dest.route}"
                    )
                    val tint by animateColorAsState(
                        targetValue = if (isSelected) glass.textPrimary else glass.textSecondary.copy(alpha = 0.76f),
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        label = "tab_tint_${dest.route}"
                    )
                    val isGeminiTab = dest == RolaysDestination.GEMINI_FLASH

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .minimumInteractiveComponentSize()
                            .graphicsLayer {
                                scaleX = tabPressScale
                                scaleY = tabPressScale
                            }
                            .testTag(dest.testTag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = dest.label,
                            tint = if (isSelected && isGeminiTab) glass.cyanAccent else tint,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    val dist = abs(dropletPosition.value - index)
                                    val ratio = (1f - dist).coerceIn(0f, 1f)
                                    val iconScale = 1f + 0.14f * ratio
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
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
