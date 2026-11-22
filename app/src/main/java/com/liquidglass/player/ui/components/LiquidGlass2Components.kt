package com.liquidglass.player.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.liquidglass.player.data.PlayerCustomizationConfig
import com.liquidglass.player.data.VisualizerStyle
import com.liquidglass.player.ui.theme.PlayerPalette
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hardware-accelerated directional Motion Blur (Android 12–16 RenderEffect).
 */
fun Modifier.liquidMotionBlur(
    radiusX: Float,
    radiusY: Float = 0f
): Modifier = this.graphicsLayer {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (radiusX > 0.4f || radiusY > 0.4f)) {
        renderEffect = RenderEffect.createBlurEffect(
            radiusX.coerceAtLeast(0.01f),
            radiusY.coerceAtLeast(0.01f),
            Shader.TileMode.DECAL
        ).asComposeRenderEffect()
    }
}

/**
 * Next-gen 4-orb reactive Aurora Mesh background that pulses subtly when music plays.
 */
@Composable
fun LiquidGlass2AuroraBackground(
    palette: PlayerPalette,
    config: PlayerCustomizationConfig,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val speedFactor = if (isPlaying) config.animationSpeed * 1.35f else config.animationSpeed * 0.65f
    val durationMs = (14000f / speedFactor.coerceIn(0.2f, 3.0f)).toInt().coerceAtLeast(2800)

    val infiniteTransition = rememberInfiniteTransition(label = "aurora_2")
    val phase by if (config.animatedBackground) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
    } else {
        remember { mutableFloatStateOf(1.1f) }
    }

    val pulse by animateFloatAsState(
        targetValue = if (isPlaying) 1.08f else 0.96f,
        animationSpec = spring(
            dampingRatio = config.smoothness.damping,
            stiffness = config.smoothness.stiffness
        ),
        label = "bg_pulse"
    )

    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && config.blurRadius > 1f) {
        Modifier.graphicsLayer {
            val r = (config.blurRadius * 1.55f).coerceIn(1f, 80f)
            renderEffect = RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP).asComposeRenderEffect()
        }
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bgBase)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(blurModifier)
        ) {
            val w = size.width
            val h = size.height
            val a = if (palette.isLight) 0.45f else 0.62f

            val c1 = Offset(w * (0.22f + 0.25f * cos(phase)), h * (0.18f + 0.14f * sin(phase * 1.2f)))
            val c2 = Offset(w * (0.80f + 0.20f * sin(phase * 0.9f)), h * (0.48f + 0.22f * cos(phase * 1.1f)))
            val c3 = Offset(w * (0.42f + 0.28f * cos(phase * 0.7f + 1.8f)), h * (0.80f + 0.14f * sin(phase * 0.8f)))
            val c4 = Offset(w * (0.68f + 0.18f * sin(phase * 1.3f + 0.9f)), h * (0.24f + 0.16f * cos(phase * 0.7f)))

            drawCircle(
                brush = Brush.radialGradient(listOf(palette.orb1.copy(alpha = a), Color.Transparent), c1, w * 0.68f * pulse),
                radius = w * 0.68f * pulse,
                center = c1
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(palette.orb2.copy(alpha = a), Color.Transparent), c2, w * 0.72f * pulse),
                radius = w * 0.72f * pulse,
                center = c2
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(palette.orb3.copy(alpha = a * 0.9f), Color.Transparent), c3, w * 0.65f * pulse),
                radius = w * 0.65f * pulse,
                center = c3
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(palette.accent.copy(alpha = a * 0.55f), Color.Transparent), c4, w * 0.52f * pulse),
                radius = w * 0.52f * pulse,
                center = c4
            )
        }
    }
}

/**
 * Liquid Glass 2.0 Panel with chromatic prism edge dispersion, inner specular highlight,
 * and layered glass sheen.
 */
@Composable
fun LiquidGlass2Panel(
    palette: PlayerPalette,
    config: PlayerCustomizationConfig,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(config.cornerRadiusDp.dp),
    tintOverride: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val baseTint = tintOverride ?: palette.glassTint
    val op = config.glassOpacity.coerceIn(0.06f, 0.65f)
    val spec = config.specularIntensity.coerceIn(0.05f, 1.0f)
    val chroma = config.chromaticEdge.coerceIn(0f, 1.0f)

    val fillBrush = Brush.linearGradient(
        colors = listOf(
            baseTint.copy(alpha = (op * 1.45f).coerceAtMost(0.88f)),
            baseTint.copy(alpha = op * 0.55f),
            palette.accent.copy(alpha = op * 0.25f * chroma),
            baseTint.copy(alpha = (op * 1.05f).coerceAtMost(0.75f))
        ),
        start = Offset(0f, 0f),
        end = Offset(900f, 1100f)
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.85f * spec),
            palette.accent.copy(alpha = 0.55f * chroma * spec),
            Color.White.copy(alpha = 0.14f * spec),
            palette.secondaryAccent.copy(alpha = 0.52f * chroma * spec),
            Color.White.copy(alpha = 0.45f * spec)
        ),
        start = Offset(0f, 0f),
        end = Offset(700f, 900f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(fillBrush)
            .border(width = 1.25.dp, brush = borderBrush, shape = shape)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Ultra-smooth 120Hz Spring-physics Liquid Glass Icon/Action Button with press Motion Blur.
 */
@Composable
fun LiquidGlassActionButton(
    palette: PlayerPalette,
    config: PlayerCustomizationConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(50),
    accentFill: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = config.smoothness.damping,
            stiffness = config.smoothness.stiffness
        ),
        label = "action_scale"
    )

    val pressBlur by animateFloatAsState(
        targetValue = if (pressed && config.motionBlurEnabled) 5.5f * config.motionBlurIntensity else 0f,
        animationSpec = tween(75),
        label = "action_blur"
    )

    LiquidGlass2Panel(
        palette = palette,
        config = config,
        shape = shape,
        tintOverride = if (accentFill) palette.accent else palette.glassTint,
        contentPadding = contentPadding,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .liquidMotionBlur(pressBlur, pressBlur * 0.7f)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (config.hapticFeedback) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            },
        content = content
    )
}

/**
 * Smooth 120fps reactive audio visualizer (Wave / Spectrum Bars / Glowing Orb).
 */
@Composable
fun LiquidAudioVisualizer(
    style: VisualizerStyle,
    isPlaying: Boolean,
    palette: PlayerPalette,
    modifier: Modifier = Modifier
) {
    if (style == VisualizerStyle.OFF) return

    val infiniteTransition = rememberInfiniteTransition(label = "viz")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "viz_t"
    )

    val amp by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.12f,
        animationSpec = tween(350),
        label = "viz_amp"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        when (style) {
            VisualizerStyle.BARS -> {
                val count = 28
                val gap = w / (count * 1.85f)
                val barW = (w - gap * (count - 1)) / count
                for (i in 0 until count) {
                    val norm = i.toFloat() / count
                    val envelope = sin(norm * PI).toFloat()
                    val wave = (0.45f + 0.55f * abs(sin(t * 2.2f + i * 0.55f) * cos(t * 1.3f - i * 0.3f)))
                    val barH = (h * 0.88f * envelope * wave * amp).coerceAtLeast(4f)
                    val x = i * (barW + gap)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(palette.accent, palette.secondaryAccent)
                        ),
                        topLeft = Offset(x, midY - barH / 2f),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }
            VisualizerStyle.WAVE -> {
                val path1 = Path()
                val path2 = Path()
                val steps = 72
                for (i in 0..steps) {
                    val x = w * i / steps
                    val norm = i.toFloat() / steps
                    val env = sin(norm * PI).toFloat()
                    val y1 = midY + sin(norm * 10f + t * 2.5f) * (h * 0.38f) * env * amp
                    val y2 = midY + cos(norm * 7f - t * 2.0f) * (h * 0.28f) * env * amp
                    if (i == 0) {
                        path1.moveTo(x, y1)
                        path2.moveTo(x, y2)
                    } else {
                        path1.lineTo(x, y1)
                        path2.lineTo(x, y2)
                    }
                }
                drawPath(
                    path = path2,
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, palette.secondaryAccent.copy(alpha = 0.7f), Color.Transparent)),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = path1,
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, palette.accent, Color.Transparent)),
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }
            VisualizerStyle.ORB -> {
                val r = (minOf(w, h) * 0.34f) * (0.85f + 0.22f * sin(t * 2f) * amp)
                val center = Offset(w / 2f, h / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.accent.copy(alpha = 0.75f), palette.secondaryAccent.copy(alpha = 0.2f), Color.Transparent),
                        center = center,
                        radius = r * 1.8f
                    ),
                    radius = r * 1.8f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = r * 0.45f,
                    center = center,
                    style = Stroke(width = 3f)
                )
            }
            VisualizerStyle.OFF -> Unit
        }
    }
}
