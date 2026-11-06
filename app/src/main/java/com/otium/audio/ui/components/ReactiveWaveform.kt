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
 * A thin progress strip drawn as a waveform. Bars behind the playhead are dim, the rest
 * carry the accent. While playing the whole strip breathes slowly, so the console looks
 * alive without faking a spectrum analyser we cannot measure.
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
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveform_phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 64
        val gap = 2.dp.toPx()
        val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(1f)
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val centreY = size.height / 2f
        val maxHeight = size.height
        val swing = if (isPlaying) 1f else 0.35f

        for (index in 0 until barCount) {
            val position = index / (barCount - 1f)
            // Deterministic shape so the strip is stable between frames.
            val shape = 0.35f +
                0.42f * abs(sin(index * 0.7f + phase * swing)) +
                0.18f * abs(sin(index * 1.9f))

            val height = (maxHeight * shape.coerceIn(0.18f, 1f))
            val left = index * (barWidth + gap)
            val played = position <= progress

            drawRoundRect(
                color = if (played) accent else trackColor,
                topLeft = Offset(left, centreY - height / 2f),
                size = Size(barWidth, height),
                cornerRadius = radius
            )
        }
    }
}
