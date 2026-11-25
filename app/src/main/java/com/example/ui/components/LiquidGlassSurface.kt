package com.example.ui.components

import android.os.Build
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
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
        Image(
            painter = painterResource(id = R.drawable.img_liquid_aura_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.68f)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(18.dp)
                    } else {
                        Modifier
                    }
                )
        )

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
                        resolvedAccent.copy(alpha = 0.24f),
                        resolvedAccent.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = orb1Center,
                    radius = w * 0.65f
                ),
                center = orb1Center,
                radius = w * 0.65f
            )

            val orb2Center = Offset(
                x = w * (0.78f + 0.10f * sin(phase * 0.8f)),
                y = h * (0.62f + 0.10f * cos(phase * 0.8f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.violetAccent.copy(alpha = 0.20f),
                        glass.blueAccent.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = orb2Center,
                    radius = w * 0.72f
                ),
                center = orb2Center,
                radius = w * 0.72f
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
    val baseFill = tintColor ?: if (elevated) glass.glassFillElevated else glass.glassFill
    val interactionSource = remember { MutableInteractionSource() }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            glass.borderBright,
            glass.cyanAccent.copy(alpha = 0.24f),
            glass.borderSoft,
            glass.violetAccent.copy(alpha = 0.22f)
        )
    )

    val glassSurfaceBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (elevated) 0.17f else 0.12f),
            baseFill,
            glass.glassFillDeep.copy(alpha = 0.55f)
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = glassSurfaceBrush, shape = shape)
            .drawBehind {
                // Inner specular refraction sheen along top edge
                val radiusPx = cornerRadiusDp.toPx()
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent,
                            glass.cyanAccent.copy(alpha = 0.06f)
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
