package com.otium.audio.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.otium.audio.audio.dsp.VisualizerBridge
import kotlin.math.abs
import kotlin.math.sin

/**
 * Precision Studio Waveform Console.
 * Minimalist, architectural, zero-allocation audio visualizer strip.
 * Reacts dynamically to real-time audio energy computed by Rust.
 */
@Composable
fun ReactiveWaveform(
    progress: Float,
    accent: Color,
    trackColor: Color,
    isPlaying: Boolean,
    waveformSeed: Long = 0L,
    modifier: Modifier = Modifier
) {
    val liveEnergy by VisualizerBridge.averageEnergy.collectAsState()
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
        val dynamicEnergy = if (isPlaying) (0.35f + liveEnergy * 0.65f) else 0.25f
        val seedOffset = ((waveformSeed and 0xFFFFL) * 0.001f).toFloat()

        for (index in 0 until barCount) {
            val position = index / (barCount - 1f)
            // Deterministic audio-like curve: stable between frames, breathing dynamically with Rust live energy
            val shape = 0.28f +
                0.46f * dynamicEnergy * abs(sin(index * 0.65f + seedOffset + phase * swing)) +
                0.22f * abs(sin(index * 1.8f + seedOffset * 1.3f))

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
