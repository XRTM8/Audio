package com.otium.audio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.otium.audio.audio.dsp.VisualizerBridge
import com.otium.audio.ui.theme.LocalOtiumColors

/**
 * Precision Studio FFT Spectrum Visualizer.
 * Renders 16 logarithmic frequency bars computed in real-time by the Rust DSP engine.
 * Architectural Nagra/Braun minimalist aesthetic with zero recomposition overhead.
 */
@Composable
fun StudioSpectrumVisualizer(
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val spectrum by VisualizerBridge.spectrumBands.collectAsState()
    val isDark = LocalOtiumColors.current.isDark
    val activeColors = remember(accentColor, isDark) {
        if (isDark) listOf(Color.White, accentColor.copy(alpha = 0.85f))
        else listOf(Color(0xFF101217), accentColor.copy(alpha = 0.85f))
    }
    val inactiveColors = remember(isDark) {
        if (isDark) listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF))
        else listOf(Color(0x30101217), Color(0x14101217))
    }
    val smoothedBands = remember { FloatArray(16) { 0.08f } }

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val totalHeight = size.height
        val barCount = 16
        val gap = 3.5.dp.toPx()
        val barWidth = ((totalWidth - gap * (barCount - 1)) / barCount).coerceAtLeast(2.5f)
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

        val hasEnergy = spectrum.any { it > 0.005f }
        val active = isPlaying && hasEnergy

        val barBrush = Brush.verticalGradient(
            colors = if (active) activeColors else inactiveColors,
            startY = 0f,
            endY = totalHeight
        )

        for (i in 0 until barCount) {
            val rawMag = if (active && spectrum.size > i) spectrum[i] else 0f
            // Logarithmic tilt / pink-noise pre-emphasis: balances mid and treble energy against dominant bass
            val tilt = 1.0f + (i.toFloat() / (barCount - 1).coerceAtLeast(1)) * 2.2f
            val targetMag = (rawMag * tilt * 2.2f).coerceIn(0.08f, 1f)

            // Analog ballistic envelope: instantaneous attack for transients, smooth exponential decay for musical feel
            val prev = smoothedBands[i]
            val smoothed = if (targetMag >= prev) {
                targetMag
            } else {
                (prev * 0.84f + targetMag * 0.16f).coerceAtLeast(0.08f)
            }
            smoothedBands[i] = smoothed

            val barHeight = totalHeight * smoothed
            val left = i * (barWidth + gap)
            val top = totalHeight - barHeight

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = radius
            )
        }
    }
}
