package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalLiquidGlassPalette
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiquidGlassAmbientBackground(
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    val glass = LocalLiquidGlassPalette.current
    val resolvedAccent = accentColor ?: glass.cyanAccent
    val transition = rememberInfiniteTransition(label = "liquid_aura")
    val phaseState = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_phase"
    )

    val scrimBrush = remember {
        Brush.verticalGradient(
            0.0f to Color(0x5E16243D),
            0.26f to Color(0x281B2B49),
            0.66f to Color(0x3A16243E),
            1.0f to Color(0x94101B32)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(glass.background)
    ) {
        // User-selected full-bleed background framed cleanly around the central subject
        Image(
            painter = painterResource(id = R.drawable.img_user_app_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(horizontalBias = -0.06f, verticalBias = -0.05f),
            modifier = Modifier.fillMaxSize()
        )

        // Airy twilight-sky mist scrim so the silver-blue sky & illustration glow naturally
        // while keeping FrostWhite text crisp behind bubble-glass cards & nav bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = scrimBrush)
        )

        // Drifting luminous soap-bubble caustics & delicate iridescent bubble rings (draw-only phase)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val phase = phaseState.value

            val orb1Center = Offset(
                x = w * (0.22f + 0.10f * cos(phase)),
                y = h * (0.16f + 0.06f * sin(phase))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        resolvedAccent.copy(alpha = 0.16f),
                        glass.blueAccent.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = orb1Center,
                    radius = w * 0.54f
                ),
                center = orb1Center,
                radius = w * 0.54f
            )

            val orb2Center = Offset(
                x = w * (0.80f + 0.08f * sin(phase * 0.8f)),
                y = h * (0.62f + 0.09f * cos(phase * 0.8f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.violetAccent.copy(alpha = 0.15f),
                        glass.roseAccent.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = orb2Center,
                    radius = w * 0.58f
                ),
                center = orb2Center,
                radius = w * 0.58f
            )

            val bubbleSpecs = listOf(
                Triple(
                    Offset(w * (0.14f + 0.03f * sin(phase * 0.9f)), h * (0.26f + 0.025f * cos(phase))),
                    w * 0.085f,
                    resolvedAccent
                ),
                Triple(
                    Offset(w * (0.86f + 0.025f * cos(phase * 1.1f)), h * (0.34f + 0.03f * sin(phase * 0.7f))),
                    w * 0.062f,
                    glass.violetAccent
                ),
                Triple(
                    Offset(w * (0.76f + 0.03f * sin(phase * 0.7f)), h * (0.80f + 0.02f * cos(phase * 1.2f))),
                    w * 0.095f,
                    glass.roseAccent
                )
            )

            val strokeWidth = 1.1.dp.toPx()
            bubbleSpecs.forEach { (center, radius, rimTint) ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            rimTint.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(center.x - radius * 0.25f, center.y - radius * 0.28f),
                        radius = radius
                    ),
                    center = center,
                    radius = radius
                )
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            rimTint.copy(alpha = 0.20f),
                            glass.violetAccent.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.12f)
                        ),
                        start = Offset(center.x - radius, center.y - radius),
                        end = Offset(center.x + radius, center.y + radius)
                    ),
                    center = center,
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    cornerRadiusDp: Dp = 28.dp,
    tintColor: Color? = null,
    elevated: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glass_press_scale"
    )

    val borderBrush = remember(glass, elevated) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (elevated) 0.84f else 0.72f),
                glass.cyanAccent.copy(alpha = 0.58f),
                glass.violetAccent.copy(alpha = 0.52f),
                glass.roseAccent.copy(alpha = 0.42f),
                Color.White.copy(alpha = 0.56f)
            )
        )
    }

    val glassSurfaceBrush = remember(tintColor, elevated) {
        val coreMistColor = if (elevated) Color(0xFF23375B) else Color(0xFF1B2D4C)
        val deepMistColor = if (elevated) Color(0xFF172642) else Color(0xFF13213B)
        val blendedMid = if (tintColor != null) {
            lerp(coreMistColor, tintColor.copy(alpha = 1f), 0.24f)
        } else {
            coreMistColor
        }
        Brush.verticalGradient(
            colors = listOf(
                lerp(coreMistColor, Color.White, 0.14f).copy(alpha = if (elevated) 0.68f else 0.58f),
                blendedMid.copy(alpha = if (elevated) 0.72f else 0.62f),
                deepMistColor.copy(alpha = if (elevated) 0.78f else 0.70f)
            )
        )
    }

    Box(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(brush = glassSurfaceBrush, shape = shape)
            .drawWithCache {
                val radiusPx = cornerRadiusDp.toPx()
                val diagonalSheen = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        glass.cyanAccent.copy(alpha = 0.08f),
                        Color.Transparent,
                        glass.violetAccent.copy(alpha = 0.09f)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                val topCatchlight = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (elevated) 0.30f else 0.24f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                )
                val bottomCaustic = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        glass.cyanAccent.copy(alpha = 0.10f),
                        glass.roseAccent.copy(alpha = 0.08f)
                    )
                )
                val hasPillDot = size.width > 48.dp.toPx() && size.height > 32.dp.toPx()
                val dotTopLeft = Offset(size.width * 0.08f, 6.dp.toPx())
                val dotSize = Size((size.width * 0.14f).coerceIn(14.dp.toPx(), 42.dp.toPx()), 5.dp.toPx())
                val topOvalPos = Offset(size.width * 0.06f, -size.height * 0.12f)
                val topOvalSize = Size(size.width * 0.88f, (size.height * 0.42f).coerceAtMost(72.dp.toPx()))
                val bottomOvalPos = Offset(size.width * 0.12f, size.height * 0.78f)
                val bottomOvalSize = Size(size.width * 0.76f, size.height * 0.28f)

                onDrawBehind {
                    drawRoundRect(
                        brush = diagonalSheen,
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                    drawOval(
                        brush = topCatchlight,
                        topLeft = topOvalPos,
                        size = topOvalSize
                    )
                    if (hasPillDot) {
                        drawOval(
                            color = Color.White.copy(alpha = 0.34f),
                            topLeft = dotTopLeft,
                            size = dotSize
                        )
                    }
                    drawOval(
                        brush = bottomCaustic,
                        topLeft = bottomOvalPos,
                        size = bottomOvalSize
                    )
                }
            }
            .border(width = if (elevated) 1.25.dp else 1.05.dp, brush = borderBrush, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = glass.cyanAccent),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        content = content
    )
}
