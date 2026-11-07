package com.otium.audio.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.sin

/**
 * High-performance dynamic anti-boredom spectrum waveform.
 * Guaranteed 0% yellow or gold. Smoothly cycles through vibrant audiophile neon hues
 * (Intense Power Red, Electric Magenta, Neon Violet, Cyber Cyan, Emerald Mint, Vivid Scarlet).
 */
private val ANTI_BOREDOM_HUES = longArrayOf(
    0xFFFF0033L, // Intense Pure Power Red
    0xFFFF0055L, // Vivid Scarlet
    0xFFFF007FL, // Electric Magenta
    0xFFD500F9L, // Vivid Orchid
    0xFF9D00FFL, // Neon Purple
    0xFF651FFFL, // Deep Royal Violet
    0xFF00B0FFL, // Cyber Blue
    0xFF00E5FFL, // Electric Cyan
    0xFF00E676L  // Neon Mint
)

@Composable
fun ReactiveWaveform(
    progress: Float,
    accent: Color,
    trackColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val phase = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "waveform")
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "waveform_phase"
        )
        animatedPhase
    } else 0f

    Canvas(modifier = modifier) {
        val barCount = 64
        val gap = 2.dp.toPx()
        val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(1f)
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val centreY = size.height / 2f
        val maxHeight = size.height
        val swing = if (isPlaying) 1f else 0.35f
        val phaseShift = (phase * 3.5f).toInt()

        for (index in 0 until barCount) {
            val position = index / (barCount - 1f)
            // Deterministic shape so the strip is stable between frames.
            val shape = 0.35f +
                0.42f * abs(sin(index * 0.7f + phase * swing)) +
                0.18f * abs(sin(index * 1.9f))

            val height = (maxHeight * shape.coerceIn(0.18f, 1f))
            val left = index * (barWidth + gap)
            val played = position <= progress

            // Dynamic anti-boredom color selection across bars (strictly zero yellow/gold)
            val barColor = if (played) {
                val colorIdx = ((index + phaseShift) % ANTI_BOREDOM_HUES.size + ANTI_BOREDOM_HUES.size) % ANTI_BOREDOM_HUES.size
                Color(ANTI_BOREDOM_HUES[colorIdx])
            } else {
                trackColor
            }

            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, centreY - height / 2f),
                size = Size(barWidth, height),
                cornerRadius = radius
            )
        }
    }
}
