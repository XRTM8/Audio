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
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

/**
 * Precision Studio Waveform Console.
 * Minimalist, architectural, zero-allocation audio visualizer strip.
 * Pure titanium white and dark obsidian glass contrast without AI slop or RGB gamer rainbow.
 */
@Composable
fun ReactiveWaveform(
    progress: Float,
    accent: Color,
    trackColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val animatedPhase = if (isPlaying) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "waveform_phase"
        )
    } else null

    Canvas(modifier = modifier) {
        val phase = animatedPhase?.value ?: 0f
        val barCount = 56
        val gap = 2.dp.toPx()
        val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(1.5f)
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val centreY = size.height / 2f
        val maxHeight = size.height
        val swing = if (isPlaying) 1f else 0.25f

        for (index in 0 until barCount) {
            val position = index / (barCount - 1f)
            // Deterministic audio-like curve: stable between frames, breathing smoothly with playback
            val shape = 0.28f +
                0.46f * abs(sin(index * 0.65f + phase * swing)) +
                0.22f * abs(sin(index * 1.8f))

            val height = (maxHeight * shape.coerceIn(0.18f, 1f))
            val left = index * (barWidth + gap)
            val played = position <= progress

            val barColor = if (played) accent else trackColor

            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, centreY - height / 2f),
                size = Size(barWidth, height),
                cornerRadius = radius
            )
        }
    }
}
