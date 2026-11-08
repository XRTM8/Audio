package com.otium.audio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.otium.audio.audio.dsp.VisualizerBridge

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

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val totalHeight = size.height
        val barCount = 16
        val gap = 3.5.dp.toPx()
        val barWidth = ((totalWidth - gap * (barCount - 1)) / barCount).coerceAtLeast(2.5f)
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

        for (i in 0 until barCount) {
            val rawMag = if (isPlaying && spectrum.size > i) spectrum[i] else 0f
            val mag = (rawMag * 1.85f).coerceIn(0.08f, 1f)
            val barHeight = totalHeight * mag
            val left = i * (barWidth + gap)
            val top = totalHeight - barHeight

            val barBrush = if (isPlaying) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        accentColor.copy(alpha = 0.85f)
                    ),
                    startY = top,
                    endY = totalHeight
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x33FFFFFF),
                        Color(0x1AFFFFFF)
                    ),
                    startY = top,
                    endY = totalHeight
                )
            }

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = radius
            )
        }
    }
}
