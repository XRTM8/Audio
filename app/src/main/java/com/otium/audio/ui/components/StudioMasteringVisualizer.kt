package com.otium.audio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.otium.audio.audio.dsp.VisualizerBridge
import com.otium.audio.playback.EqualizerBand
import com.otium.audio.ui.theme.AccentCyan
import com.otium.audio.ui.theme.AccentTitaniumWhite
import kotlin.math.max
import kotlin.math.min

/**
 * Studio Mastering Response Curve & FFT Visualizer
 *
 * Combines real-time audio spectrum analysis with the physical parametric/graphic EQ response curve:
 * - Real-time FFT logarithmic spectrum bars (16 bands) driven by the native Rust DSP engine.
 * - Accurate acoustic transfer curve interpolated via smooth cubic Catmull-Rom spline through all 10 EQ bands.
 * - Sub-bass boost filter additions dynamically factored into low-end response.
 * - Calibrated dB grid (+12dB, 0dB baseline, -12dB) with zero recomposition overhead.
 */
@Composable
fun StudioMasteringVisualizer(
    bands: List<EqualizerBand>,
    bassBoostStrength: Int,
    isPlaying: Boolean,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = AccentTitaniumWhite
) {
    val spectrum by VisualizerBridge.spectrumBands.collectAsState()
    val smoothedBands = remember { FloatArray(16) { 0.05f } }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val width = size.width
        val height = size.height

        // dB Range: -15dB to +15dB
        val minDb = -15f
        val maxDb = 15f
        val dbSpan = maxDb - minDb
        val zeroY = height * (1f - (0f - minDb) / dbSpan)
        val plus12Y = height * (1f - (12f - minDb) / dbSpan)
        val minus12Y = height * (1f - (-12f - minDb) / dbSpan)

        // 1. Reference Grid Lines
        // +12 dB Guide
        drawLine(
            color = Color(0x18FFFFFF),
            start = Offset(0f, plus12Y),
            end = Offset(width, plus12Y),
            strokeWidth = 1f
        )
        // 0 dB Baseline (Master reference)
        drawLine(
            color = Color(0x35FFFFFF),
            start = Offset(0f, zeroY),
            end = Offset(width, zeroY),
            strokeWidth = 1.2f
        )
        // -12 dB Guide
        drawLine(
            color = Color(0x18FFFFFF),
            start = Offset(0f, minus12Y),
            end = Offset(width, minus12Y),
            strokeWidth = 1f
        )

        // Vertical Octave Calibration Markers
        val freqMarkers = listOf(0.12f, 0.35f, 0.65f, 0.88f)
        freqMarkers.forEach { xFrac ->
            drawLine(
                color = Color(0x0EFFFFFF),
                start = Offset(width * xFrac, 0f),
                end = Offset(width * xFrac, height),
                strokeWidth = 1f
            )
        }

        // 2. Real-Time FFT Spectrum Background
        val barCount = 16
        val gap = 3.dp.toPx()
        val totalGaps = gap * (barCount - 1)
        val barWidth = ((width - totalGaps) / barCount).coerceAtLeast(2f)
        val barRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        val hasEnergy = spectrum.any { it > 0.005f }
        val active = isPlaying && hasEnergy

        val spectrumBrush = Brush.verticalGradient(
            colors = if (active) {
                listOf(Color(0x38FFFFFF), Color(0x12FFFFFF))
            } else {
                listOf(Color(0x18FFFFFF), Color(0x06FFFFFF))
            },
            startY = 0f,
            endY = height
        )

        for (i in 0 until barCount) {
            val rawMag = if (active && spectrum.size > i) spectrum[i] else 0f
            val tilt = 1.0f + (i.toFloat() / (barCount - 1).coerceAtLeast(1)) * 2.0f
            val targetMag = (rawMag * tilt * 2.0f).coerceIn(0.04f, 1f)

            val prev = smoothedBands[i]
            val smoothed = if (targetMag >= prev) {
                targetMag
            } else {
                (prev * 0.82f + targetMag * 0.18f).coerceAtLeast(0.04f)
            }
            smoothedBands[i] = smoothed

            val barHeight = height * smoothed * 0.88f
            val left = i * (barWidth + gap)
            val top = height - barHeight

            drawRoundRect(
                brush = spectrumBrush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = barRadius
            )
        }

        // 3. Parametric EQ Acoustic Curve (10 Bands)
        if (bands.isNotEmpty()) {
            val count = bands.size
            val paddingX = 14.dp.toPx()
            val usableWidth = (width - paddingX * 2).coerceAtLeast(1f)

            // Calculate gain for each band, including low-end bass boost impact
            val points = ArrayList<Offset>(count + 2)

            for (i in 0 until count) {
                val band = bands[i]
                var effectiveGain = if (isEnabled) band.levelDb else 0f

                // Bass boost filter peaking impact on 31Hz and 62Hz
                if (isEnabled && bassBoostStrength > 0) {
                    val extraBass = (bassBoostStrength / 1000f) * 6.5f
                    if (i == 0) effectiveGain += extraBass * 0.85f
                    if (i == 1) effectiveGain += extraBass
                    if (i == 2) effectiveGain += extraBass * 0.45f
                }

                val x = paddingX + (i.toFloat() / (count - 1).coerceAtLeast(1)) * usableWidth
                val y = height * (1f - (effectiveGain.coerceIn(minDb, maxDb) - minDb) / dbSpan)
                points.add(Offset(x, y))
            }

            // Interpolate smooth cubic Catmull-Rom spline path
            val curvePath = Path()
            val fillPath = Path()

            // Left edge extension to 0 dB
            val startY = if (isEnabled) points[0].y else zeroY
            curvePath.moveTo(0f, startY)
            fillPath.moveTo(0f, zeroY)
            fillPath.lineTo(0f, startY)

            // Connect to first point
            curvePath.lineTo(points[0].x, points[0].y)
            fillPath.lineTo(points[0].x, points[0].y)

            for (i in 0 until points.size - 1) {
                val p0 = if (i > 0) points[i - 1] else points[i]
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = if (i + 2 < points.size) points[i + 2] else p2

                val c1x = p1.x + (p2.x - p0.x) / 5f
                val c1y = p1.y + (p2.y - p0.y) / 5f
                val c2x = p2.x - (p3.x - p1.x) / 5f
                val c2y = p2.y - (p3.y - p1.y) / 5f

                curvePath.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
                fillPath.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
            }

            // Right edge extension
            val endY = if (isEnabled) points.last().y else zeroY
            curvePath.lineTo(width, endY)
            fillPath.lineTo(width, endY)
            fillPath.lineTo(width, zeroY)
            fillPath.close()

            // Subtle luminous area fill under the curve to the 0 dB baseline
            if (isEnabled) {
                val fillGradient = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.22f),
                        accentColor.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
                drawPath(fillPath, brush = fillGradient)
            }

            // Continuous transfer curve line
            drawPath(
                path = curvePath,
                color = if (isEnabled) accentColor else Color(0x66FFFFFF),
                style = Stroke(
                    width = if (isEnabled) 2.2.dp.toPx() else 1.2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )

            // Frequency Nodes (Control Points on curve)
            if (isEnabled) {
                points.forEach { pt ->
                    val isNeutral = kotlin.math.abs(pt.y - zeroY) < 1.5f
                    if (!isNeutral) {
                        // Glowing halo
                        drawCircle(
                            color = accentColor.copy(alpha = 0.25f),
                            radius = 4.5.dp.toPx(),
                            center = pt
                        )
                        // Solid core
                        drawCircle(
                            color = accentColor,
                            radius = 2.5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color(0xFF0C0C10),
                            radius = 1.dp.toPx(),
                            center = pt
                        )
                    } else {
                        drawCircle(
                            color = Color(0x55FFFFFF),
                            radius = 1.5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}
