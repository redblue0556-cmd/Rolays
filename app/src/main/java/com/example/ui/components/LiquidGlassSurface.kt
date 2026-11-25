package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_phase"
    )

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

        // Balanced midnight-indigo Liquid Glass atmospheric scrim so the artwork shines
        // while keeping bright sky areas readable behind glass cards & bottom navigation bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        0.0f to Color(0x99060914),
                        0.28f to Color(0x58091022),
                        0.68f to Color(0x66080E1E),
                        1.0f to Color(0xD8050812)
                    )
                )
        )

        // Subtle refractive caustic glow orbs layered lightly over the scrim
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val orb1Center = Offset(
                x = w * (0.24f + 0.12f * cos(phase)),
                y = h * (0.18f + 0.08f * sin(phase))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        resolvedAccent.copy(alpha = 0.14f),
                        resolvedAccent.copy(alpha = 0.02f),
                        Color.Transparent
                    ),
                    center = orb1Center,
                    radius = w * 0.60f
                ),
                center = orb1Center,
                radius = w * 0.60f
            )

            val orb2Center = Offset(
                x = w * (0.78f + 0.10f * sin(phase * 0.8f)),
                y = h * (0.64f + 0.10f * cos(phase * 0.8f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.violetAccent.copy(alpha = 0.12f),
                        glass.blueAccent.copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = orb2Center,
                    radius = w * 0.65f
                ),
                center = orb2Center,
                radius = w * 0.65f
            )
        }
    }
}

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    cornerRadiusDp: Dp = 24.dp,
    tintColor: Color? = null,
    elevated: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLiquidGlassPalette.current
    val baseFill = tintColor ?: if (elevated) Color(0xB80E162B) else Color(0x9E0B1224)
    val interactionSource = remember { MutableInteractionSource() }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            glass.borderBright,
            glass.cyanAccent.copy(alpha = 0.28f),
            glass.borderSoft,
            glass.violetAccent.copy(alpha = 0.26f)
        )
    )

    val glassSurfaceBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A2644).copy(alpha = if (elevated) 0.78f else 0.68f),
            baseFill,
            Color(0xFF070C1A).copy(alpha = if (elevated) 0.86f else 0.78f)
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = glassSurfaceBrush, shape = shape)
            .drawBehind {
                val radiusPx = cornerRadiusDp.toPx()
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent,
                            glass.cyanAccent.copy(alpha = 0.07f)
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height * 0.6f)
                    ),
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
            }
            .border(width = 1.dp, brush = borderBrush, shape = shape)
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
