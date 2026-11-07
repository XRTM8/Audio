package com.otium.audio.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.AccentGoldLight
import com.otium.audio.ui.theme.AccentPowerRed

@Composable
fun AudioWaveformVisualizer(
    isPlaying: Boolean,
    primaryColor: Color = Color.White,
    secondaryColor: Color = Color(0xFFD1D1D6),
    barCount: Int = 28,
    maxHeight: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    // Generate diverse phased animations for natural rhythm
    val animPhase1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase1"
    )

    val animPhase2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(580, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase2"
    )

    val animPhase3 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(330, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase3"
    )

    val playbackScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.15f,
        animationSpec = tween(400),
        label = "playback_scale"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barSpacing = 4.dp.toPx()
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = ((totalWidth - totalSpacing) / barCount).coerceAtLeast(2.dp.toPx())
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(primaryColor, secondaryColor),
            startY = 0f,
            endY = canvasHeight
        )

        for (i in 0 until barCount) {
            val normalizedIndex = i.toFloat() / barCount.toFloat()
            val phaseFactor = when {
                i % 3 == 0 -> animPhase1
                i % 3 == 1 -> animPhase2
                else -> animPhase3
            }

            // Bell-curve shape modulation + animated phase
            val bellCurve = (1.0f - kotlin.math.abs(normalizedIndex - 0.5f) * 1.5f).coerceIn(0.25f, 1.0f)
            val computedHeight = (canvasHeight * bellCurve * phaseFactor * playbackScale).coerceIn(3.dp.toPx(), canvasHeight)

            val x = i * (barWidth + barSpacing)
            val y = (canvasHeight - computedHeight) / 2f

            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, computedHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}
